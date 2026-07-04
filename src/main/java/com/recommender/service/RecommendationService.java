package com.recommender.service;

import com.recommender.algorithm.rerank.FusionReranker;
import com.recommender.algorithm.rerank.MmrReranker;
import com.recommender.db.entity.InteractionEntity;
import com.recommender.db.entity.PaperEntity;
import com.recommender.db.repository.PaperRepository;
import com.recommender.service.util.Tokenizer;
import com.recommender.web.dto.PaperDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Orchestrates the full recommendation pipeline:
 *
 * <pre>
 *  1. Content-based  — HNSW nearest neighbours on user profile embedding
 *  2. Collaborative  — BPR top-k for the user
 *  3. Text search    — BM25 on textQuery (optional)
 *  4. Graph boost    — top papers by PageRank
 *  5. Fusion         — Reciprocal Rank Fusion across all signals
 *  6. Diversity      — MMR reranking (λ = 0.7 by default)
 *  7. Exclusion      — remove papers already seen by the user
 * </pre>
 *
 * All signals are optional: if there is no data for a signal (e.g. the user
 * is new and has no interactions) that signal is simply omitted from the fusion.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    /** MMR trade-off: 1.0 = pure relevance, 0.0 = pure diversity. */
    private static final double MMR_LAMBDA = 0.7;

    /** Candidate multiplier: fetch (k * CANDIDATE_FACTOR) per signal before fusion. */
    private static final int CANDIDATE_FACTOR = 5;

    /** MMR rerank pool size multiplier: rerank (k * MMR_RERANK_FACTOR) fused results. */
    private static final int MMR_RERANK_FACTOR = 2;

    private final IndexService       indexService;
    private final InteractionService interactionService;
    private final PaperRepository    paperRepo;

    public RecommendationService(IndexService indexService,
                                 InteractionService interactionService,
                                 PaperRepository paperRepo) {
        this.indexService       = indexService;
        this.interactionService = interactionService;
        this.paperRepo          = paperRepo;
    }

    /**
     * Produce a ranked, deduplicated, diversity-adjusted list of recommended paper UUIDs.
     *
     * @param userId    the requesting user (may be null for anonymous/cold-start)
     * @param textQuery optional free-text query (may be null or blank)
     * @param k         number of results to return
     */
    public List<UUID> recommend(UUID userId, String textQuery, int k) {
        int candidates = k * CANDIDATE_FACTOR;

        Set<UUID>                 seen       = userId != null
                ? interactionService.getSeenPaperIds(userId)
                : Set.of();
        List<InteractionEntity>   userHistory = userId != null
                ? interactionService.findByUser(userId)
                : List.of();

        // ── signal 1: content-based (HNSW) ────────────────────────────────────
        List<Long> hnswRanking = List.of();
        float[] profileVector  = userId != null
                ? indexService.userProfileVector(userId, userHistory)
                : null;
        if (profileVector != null) {
            List<Integer> hits = indexService.searchHnsw(profileVector, candidates);
            hnswRanking = toIdList(hits);
        }

        // ── signal 2: collaborative filtering (BPR) ────────────────────────────
        List<Long> bprRanking = List.of();
        if (userId != null && indexService.isBprTrained()) {
            List<Integer> hits = indexService.recommendBpr(userId, candidates, seen);
            bprRanking = toIdList(hits);
        }

        // ── signal 3: text search (BM25) ──────────────────────────────────────
        List<Long> bm25Ranking = List.of();
        if (textQuery != null && !textQuery.isBlank()) {
            List<String> tokens = Tokenizer.tokenize(textQuery);
            List<Integer> hits  = indexService.searchBm25(tokens, candidates);
            bm25Ranking = toIdList(hits);
        }

        // ── signal 4: graph (PageRank) ────────────────────────────────────────
        List<Long> prRanking = toIdList(indexService.topByPageRank(candidates));

        // ── collect non-empty lists for RRF ───────────────────────────────────
        List<List<Long>> rankings = new ArrayList<>();
        if (!hnswRanking.isEmpty()) rankings.add(hnswRanking);
        if (!bprRanking.isEmpty())  rankings.add(bprRanking);
        if (!bm25Ranking.isEmpty()) rankings.add(bm25Ranking);
        if (!prRanking.isEmpty())   rankings.add(prRanking);

        if (rankings.isEmpty()) return List.of();

        // ── fusion (RRF) ───────────────────────────────────────────────────────
        List<Long> fused = FusionReranker.reciprocalRankFusion(rankings);

        // ── build relevance map and embedding map for MMR ──────────────────────
        Map<Long, Double> relevance  = new LinkedHashMap<>();
        Map<Long, float[]> embMap    = new HashMap<>();
        for (int rank = 0; rank < fused.size(); rank++) {
            long id = fused.get(rank);
            relevance.put(id, 1.0 / (1 + rank));
            float[] emb = indexService.embeddingAt((int) id);
            if (emb != null) embMap.put(id, emb);
        }

        // ── MMR diversity reranking ────────────────────────────────────────────
        List<Long> reranked = MmrReranker.rerank(fused, relevance, embMap, MMR_LAMBDA, k * MMR_RERANK_FACTOR);

        // ── exclude seen, convert idx → UUID, trim to k ───────────────────────
        List<UUID> result = new ArrayList<>(k);
        Iterator<Long> it = reranked.iterator();
        while (result.size() < k && it.hasNext()) {
            UUID paperId = indexService.paperIdxToUuid((int) (long) it.next());
            if (!seen.contains(paperId)) result.add(paperId);
        }

        return result;
    }

    /**
     * Fetch papers for a list of UUIDs and map to DTOs within the transaction
     * so lazy collections (topics, authorNames) are initialized before session closes.
     */
    public List<PaperDto> fetchPapers(List<UUID> ids) {
        Map<UUID, PaperEntity> byId = new LinkedHashMap<>();
        paperRepo.findAllById(ids).forEach(p -> byId.put(p.getId(), p));
        List<PaperDto> ordered = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            PaperEntity p = byId.get(id);
            if (p != null) ordered.add(PaperDto.from(p));
        }
        return ordered;
    }

    // ── explain ───────────────────────────────────────────────────────────────

    public record RecommendExplain(
        List<PaperSignal> results,
        boolean hnswActive,
        boolean bprActive,
        boolean bm25Active
    ) {
        public record PaperSignal(
            UUID paperId,
            List<String> signals,
            double rrfScore,
            double pageRankScore
        ) {}
    }

    public RecommendExplain recommendExplained(UUID userId, String textQuery, int k) {
        int candidates = k * CANDIDATE_FACTOR;

        Set<UUID> seen = userId != null ? interactionService.getSeenPaperIds(userId) : Set.of();
        List<InteractionEntity> userHistory = userId != null ? interactionService.findByUser(userId) : List.of();

        List<Long> hnswRanking = List.of();
        Set<Long>  hnswSet     = new HashSet<>();
        boolean    hnswActive  = false;
        float[] profileVector  = userId != null ? indexService.userProfileVector(userId, userHistory) : null;
        if (profileVector != null) {
            hnswRanking = toIdList(indexService.searchHnsw(profileVector, candidates));
            hnswSet.addAll(hnswRanking);
            hnswActive = true;
        }

        List<Long> bprRanking = List.of();
        Set<Long>  bprSet     = new HashSet<>();
        boolean    bprActive  = false;
        if (userId != null && indexService.isBprTrained()) {
            bprRanking = toIdList(indexService.recommendBpr(userId, candidates, seen));
            bprSet.addAll(bprRanking);
            bprActive = true;
        }

        List<Long> bm25Ranking = List.of();
        Set<Long>  bm25Set     = new HashSet<>();
        boolean    bm25Active  = false;
        if (textQuery != null && !textQuery.isBlank()) {
            List<String> tokens = Tokenizer.tokenize(textQuery);
            bm25Ranking = toIdList(indexService.searchBm25(tokens, candidates));
            bm25Set.addAll(bm25Ranking);
            bm25Active = true;
        }

        List<Long> prRanking = toIdList(indexService.topByPageRank(candidates));
        Set<Long>  prSet     = new HashSet<>(prRanking);

        List<List<Long>> rankings = new ArrayList<>();
        if (!hnswRanking.isEmpty()) rankings.add(hnswRanking);
        if (!bprRanking.isEmpty())  rankings.add(bprRanking);
        if (!bm25Ranking.isEmpty()) rankings.add(bm25Ranking);
        if (!prRanking.isEmpty())   rankings.add(prRanking);
        if (rankings.isEmpty()) return new RecommendExplain(List.of(), false, false, false);

        List<Long> fused = FusionReranker.reciprocalRankFusion(rankings);

        Map<Long, Double> relevance = new LinkedHashMap<>();
        Map<Long, float[]> embMap  = new HashMap<>();
        for (int rank = 0; rank < fused.size(); rank++) {
            long id = fused.get(rank);
            relevance.put(id, 1.0 / (1 + rank));
            float[] emb = indexService.embeddingAt((int) id);
            if (emb != null) embMap.put(id, emb);
        }

        List<Long> reranked = MmrReranker.rerank(fused, relevance, embMap, MMR_LAMBDA, k * MMR_RERANK_FACTOR);

        double[] allPR = indexService.getAllPageRankScores();
        List<RecommendExplain.PaperSignal> results = new ArrayList<>();
        Iterator<Long> rit = reranked.iterator();
        while (results.size() < k && rit.hasNext()) {
            Long idx = rit.next();
            UUID paperId = indexService.paperIdxToUuid((int) (long) idx);
            if (!seen.contains(paperId)) {
                List<String> signals = new ArrayList<>();
                if (hnswSet.contains(idx)) signals.add("HNSW");
                if (bprSet.contains(idx))  signals.add("BPR");
                if (bm25Set.contains(idx)) signals.add("BM25");
                if (prSet.contains(idx))   signals.add("PageRank");

                double rrfScore = relevance.getOrDefault(idx, 0.0);
                double prScore  = (allPR.length > idx) ? allPR[(int) (long) idx] : 0.0;
                results.add(new RecommendExplain.PaperSignal(paperId, signals, rrfScore, prScore));
            }
        }

        return new RecommendExplain(results, hnswActive, bprActive, bm25Active);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Convert int paperIdx list to Long list (FusionReranker / MmrReranker use Long). */
    private static List<Long> toIdList(List<Integer> indices) {
        List<Long> out = new ArrayList<>(indices.size());
        for (int i : indices) out.add((long) i);
        return out;
    }
}
