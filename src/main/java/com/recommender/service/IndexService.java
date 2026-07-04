package com.recommender.service;

import com.recommender.algorithm.bm25.Bm25Index;
import com.recommender.db.entity.InteractionEntity;
import com.recommender.db.entity.PaperEntity;
import com.recommender.db.entity.UserEntity;
import com.recommender.db.repository.PaperRepository;
import com.recommender.db.repository.UserRepository;
import com.recommender.jni.BprNative;
import com.recommender.jni.HnswNative;
import com.recommender.jni.PageRankNative;
import com.recommender.service.util.Tokenizer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Manages all in-memory search structures backed by native C libraries.
 *
 * <p>Algorithms:
 * <ul>
 *   <li>HNSW  — {@link HnswNative}     (C, JNI)
 *   <li>BM25  — {@link Bm25Index}      (Java — kept for simplicity; vocab mapping avoids JNI overhead)
 *   <li>BPR   — {@link BprNative}      (C, JNI)
 *   <li>PageRank — {@link PageRankNative} (C, JNI)
 * </ul>
 *
 * <p>ID convention: every paper gets a sequential integer index (paperIdx, 0-based)
 * that serves as the universal ID across all four structures. Convert back to UUID
 * via {@link #paperIdxToUuid(int)}.
 */
@Service
public class IndexService {

    private static final Logger log = Logger.getLogger(IndexService.class.getName());

    /* HNSW */
    private static final int HNSW_M               = 16;
    private static final int HNSW_EF_CONSTRUCTION = 200;
    private static final int HNSW_EF_SEARCH       = 50;
    private static final int EMBEDDING_DIM        = 768;

    /* BPR */
    private static final int   BPR_FACTORS = 64;
    private static final int   BPR_EPOCHS  = 100;
    private static final float BPR_LR      = 0.01f;
    private static final float BPR_LAMBDA  = 0.001f;
    private static final long  BPR_SEED    = 42L;

    /* PageRank */
    private static final int    PAGERANK_ITERATIONS = 20;
    private static final double PAGERANK_DAMPING    = 0.85;

    private final PaperRepository    paperRepo;
    private final UserRepository     userRepo;
    private final InteractionService interactionService;

    private final AtomicBoolean reindexing = new AtomicBoolean(false);

    /* ── Native handles ─────────────────────────────────────────────────────── */
    private volatile long hnswPtr  = 0L;  /* C HNSW index pointer  */
    private volatile long bprPtr   = 0L;  /* C BPR model pointer   */

    /* ── Java structures ────────────────────────────────────────────────────── */
    private volatile Bm25Index bm25Index;   /* Java BM25 (vocab-mapped) */

    /* ── Index mappings ─────────────────────────────────────────────────────── */
    private volatile UUID[]             idxToPaperUuid = new UUID[0];
    private volatile Map<UUID,Integer>  paperUuidToIdx = new HashMap<>();
    private volatile UUID[]             idxToUserUuid  = new UUID[0];
    private volatile Map<UUID,Integer>  userUuidToIdx  = new HashMap<>();

    /* ── Cached data ─────────────────────────────────────────────────────────── */
    private volatile float[][]  embeddings;       /* [paperIdx][768] */
    private volatile double[]   pageRankScores;   /* [paperIdx] */
    private volatile boolean    bprTrained = false;

    public IndexService(PaperRepository paperRepo,
                        UserRepository userRepo,
                        InteractionService interactionService) {
        this.paperRepo         = paperRepo;
        this.userRepo          = userRepo;
        this.interactionService = interactionService;
    }

    @PostConstruct
    public void scheduleWarmUp() {
        /* actual warm-up triggered by IndexWarmUpRunner after context is ready */
    }

    @PreDestroy
    public void freeNative() {
        if (hnswPtr != 0) { HnswNative.freeIndex(hnswPtr); hnswPtr = 0; }
        if (bprPtr  != 0) { BprNative.freeModel(bprPtr);   bprPtr  = 0; }
    }

    // ── Public: reindex ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public void reindex() {
        if (!reindexing.compareAndSet(false, true)) {
            log.warning("[IndexService] Reindex already in progress — skipping.");
            return;
        }
        try {
            warmUp();
        } finally {
            reindexing.set(false);
        }
    }

    // ── Core warm-up ─────────────────────────────────────────────────────────

    private void warmUp() {
        log.info("[IndexService] Starting warm-up…");

        List<PaperEntity> papers = paperRepo.findAllWithCitations();
        List<UserEntity>  users  = userRepo.findAll();

        if (papers.isEmpty()) {
            log.info("[IndexService] No papers in DB — skipping.");
            initEmpty();
            return;
        }

        buildMappings(papers, users);
        buildHnsw(papers);
        buildBm25(papers);
        trainBpr();
        computePageRank(papers);

        log.info("[IndexService] Done — papers=" + papers.size()
                + " users=" + users.size()
                + " bprTrained=" + bprTrained);
    }

    private void initEmpty() {
        idxToPaperUuid = new UUID[0];
        idxToUserUuid  = new UUID[0];
        embeddings     = new float[0][];
        pageRankScores = new double[0];
        bm25Index      = new Bm25Index();
        bm25Index.build();
    }

    // ── Mappings ──────────────────────────────────────────────────────────────

    private void buildMappings(List<PaperEntity> papers, List<UserEntity> users) {
        idxToPaperUuid = new UUID[papers.size()];
        embeddings     = new float[papers.size()][];
        Map<UUID,Integer> pm = new HashMap<>(papers.size() * 2);
        for (int i = 0; i < papers.size(); i++) {
            UUID id = papers.get(i).getId();
            idxToPaperUuid[i] = id;
            pm.put(id, i);
            embeddings[i] = papers.get(i).getEmbedding();
        }
        paperUuidToIdx = pm;

        idxToUserUuid = new UUID[users.size()];
        Map<UUID,Integer> um = new HashMap<>(users.size() * 2);
        for (int i = 0; i < users.size(); i++) {
            UUID id = users.get(i).getId();
            idxToUserUuid[i] = id;
            um.put(id, i);
        }
        userUuidToIdx = um;
    }

    // ── HNSW (C JNI) ─────────────────────────────────────────────────────────

    private void buildHnsw(List<PaperEntity> papers) {
        if (hnswPtr != 0) { HnswNative.freeIndex(hnswPtr); hnswPtr = 0; }

        long ptr = HnswNative.createIndex(EMBEDDING_DIM, HNSW_M, HNSW_EF_CONSTRUCTION);
        int  withEmb = 0;
        float[] zero = new float[EMBEDDING_DIM];

        for (PaperEntity p : papers) {
            float[] emb = p.getEmbedding();
            if (emb != null && emb.length == EMBEDDING_DIM) {
                HnswNative.addVector(ptr, emb);
                withEmb++;
            } else {
                HnswNative.addVector(ptr, zero); // placeholder keeps nodeId == paperIdx
            }
        }
        hnswPtr = ptr;
        log.info("[IndexService] HNSW built — with real embeddings: " + withEmb + "/" + papers.size());
    }

    // ── BM25 (Java) ──────────────────────────────────────────────────────────

    private void buildBm25(List<PaperEntity> papers) {
        Bm25Index idx = new Bm25Index();
        for (int i = 0; i < papers.size(); i++) {
            PaperEntity p = papers.get(i);
            idx.addDocument(i, Tokenizer.tokenizePaper(p.getTitle(), p.getAbstractText()));
        }
        idx.build();
        bm25Index = idx;
        log.info("[IndexService] BM25 built — docs: " + bm25Index.size());
    }

    // ── BPR (C JNI) ──────────────────────────────────────────────────────────

    private void trainBpr() {
        if (bprPtr != 0) { BprNative.freeModel(bprPtr); bprPtr = 0; }

        Map<Integer,Set<Integer>> positive =
                interactionService.buildBprPositiveItems(userUuidToIdx, paperUuidToIdx);

        if (positive.isEmpty()) {
            log.info("[IndexService] No interactions — BPR not trained.");
            bprTrained = false;
            return;
        }

        /* Flatten to parallel int arrays */
        int nPairs = positive.values().stream().mapToInt(Set::size).sum();
        int[] userArr = new int[nPairs];
        int[] itemArr = new int[nPairs];
        int k = 0;
        for (Map.Entry<Integer,Set<Integer>> e : positive.entrySet()) {
            int u = e.getKey();
            for (int item : e.getValue()) { userArr[k] = u; itemArr[k++] = item; }
        }

        long ptr = BprNative.createModel(
                idxToUserUuid.length, idxToPaperUuid.length, BPR_FACTORS, BPR_SEED);
        BprNative.fit(ptr, userArr, itemArr, nPairs, BPR_EPOCHS, BPR_LR, BPR_LAMBDA);
        bprPtr    = ptr;
        bprTrained = true;
        log.info("[IndexService] BPR trained — users=" + idxToUserUuid.length
                + " papers=" + idxToPaperUuid.length
                + " pairs=" + nPairs);
    }

    // ── PageRank (C JNI) ─────────────────────────────────────────────────────

    private void computePageRank(List<PaperEntity> papers) {
        /* Build flat edge arrays */
        int totalEdges = papers.stream()
                .mapToInt(p -> p.getCitedPaperIds().size()).sum();
        int[] citing = new int[totalEdges];
        int[] cited  = new int[totalEdges];
        int   e = 0;
        for (PaperEntity p : papers) {
            int srcIdx = paperUuidToIdx.getOrDefault(p.getId(), -1);
            if (srcIdx >= 0) {
                for (UUID citedId : p.getCitedPaperIds()) {
                    Integer dstIdx = paperUuidToIdx.get(citedId);
                    if (dstIdx != null) {
                        citing[e] = srcIdx;
                        cited[e]  = dstIdx;
                        e++;
                    }
                }
            }
        }

        double[] scores = PageRankNative.compute(
                citing, cited, e, papers.size(), PAGERANK_DAMPING, PAGERANK_ITERATIONS);
        pageRankScores = (scores != null) ? scores : new double[papers.size()];
        log.info("[IndexService] PageRank computed — edges=" + e);
    }

    // ── Search API ────────────────────────────────────────────────────────────

    /** HNSW nearest neighbours (C). Returns paperIdx list, best-first. */
    public List<Integer> searchHnsw(float[] query, int k) {
        if (hnswPtr == 0 || idxToPaperUuid.length == 0) return List.of();
        int[] ids = HnswNative.search(hnswPtr, query, k, HNSW_EF_SEARCH);
        List<Integer> out = new ArrayList<>(ids.length);
        for (int id : ids) out.add(id);
        return out;
    }

    /** BM25 text search (Java). Returns paperIdx list, best-first. */
    public List<Integer> searchBm25(List<String> tokens, int k) {
        if (bm25Index == null || bm25Index.size() == 0) return List.of();
        return bm25Index.search(tokens, k).stream()
                .map(Bm25Index.Result::docId)
                .toList();
    }

    /** BPR top-k for a user (C). Returns paperIdx list. */
    public List<Integer> recommendBpr(UUID userId, int k, Set<UUID> seenUuids) {
        if (!bprTrained || bprPtr == 0) return List.of();
        Integer userIdx = userUuidToIdx.get(userId);
        if (userIdx == null) return List.of();

        int[] excl = seenUuids.stream()
                .map(paperUuidToIdx::get)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();

        int[] results = BprNative.recommend(bprPtr, userIdx, excl, k);
        List<Integer> out = new ArrayList<>(results.length);
        for (int id : results) out.add(id);
        return out;
    }

    /** Exponential decay half-life for interaction recency weighting (30 days). */
    private static final double RECENCY_HALF_LIFE_DAYS = 30.0;
    private static final double RECENCY_LAMBDA         = Math.log(2) / RECENCY_HALF_LIFE_DAYS;
    private static final double SECONDS_PER_DAY        = 86400.0;

    /**
     * Weighted-average embedding of a user's interaction history.
     * Recent interactions count more: effective_weight = action_weight * exp(-λ * days_ago).
     */
    public float[] userProfileVector(UUID userId,
                                     List<InteractionEntity> interactions) {
        float[] profile = new float[EMBEDDING_DIM];
        double  total   = 0.0;
        Instant now     = Instant.now();
        for (InteractionEntity ie : interactions) {
            Integer idx = paperUuidToIdx.get(ie.getPaperId());
            if (idx != null) {
                float[] emb = embeddings[idx];
                if (emb != null && emb.length == EMBEDDING_DIM) {
                    double daysSince = Duration.between(ie.getCreatedAt(), now).toSeconds() / SECONDS_PER_DAY;
                    double w = ie.getWeight() * Math.exp(-RECENCY_LAMBDA * daysSince);
                    for (int i = 0; i < EMBEDDING_DIM; i++) profile[i] += w * emb[i];
                    total += w;
                }
            }
        }
        if (total == 0.0) return null;
        for (int i = 0; i < EMBEDDING_DIM; i++) profile[i] /= (float) total;
        return profile;
    }

    /** Top-k papers by PageRank score. Returns paperIdx list. */
    public List<Integer> topByPageRank(int k) {
        int n = pageRankScores.length;
        List<Integer> all = new ArrayList<>(n);
        for (int i = 0; i < n; i++) all.add(i);
        all.sort((a, b) -> Double.compare(pageRankScores[b], pageRankScores[a]));
        return all.subList(0, Math.min(k, n));
    }

    // ── Coordinate conversion ─────────────────────────────────────────────────

    public UUID    paperIdxToUuid(int idx)   { return idxToPaperUuid[idx]; }
    public Integer paperUuidToIdx(UUID id)   { return paperUuidToIdx.get(id); }
    public float[] embeddingAt(int idx)      { return embeddings[idx]; }
    public double  pageRankAt(int idx)       { return pageRankScores[idx]; }
    public boolean  isBprTrained()            { return bprTrained; }
    public int      paperCount()              { return idxToPaperUuid.length; }
    public int      bm25DocCount()            { return bm25Index == null ? 0 : bm25Index.size(); }
    public double[] getAllPageRankScores()     { double[] s = pageRankScores; return s != null ? s.clone() : new double[0]; }
    public int      getUserCount()            { return idxToUserUuid.length; }
    public int      getEmbeddingCount()       {
        float[][] e = embeddings;
        if (e == null) return 0;
        int n = 0; for (float[] v : e) if (v != null) n++; return n;
    }
}
