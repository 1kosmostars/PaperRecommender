package com.recommender.web;

import com.recommender.db.entity.InteractionEntity;
import com.recommender.db.entity.PaperEntity;
import com.recommender.db.entity.UserEntity;
import com.recommender.db.repository.InteractionRepository;
import com.recommender.db.repository.PaperRepository;
import com.recommender.db.repository.UserRepository;
import com.recommender.service.IndexService;
import com.recommender.service.IngestService;
import com.recommender.service.RecommendationService;
import com.recommender.service.util.Tokenizer;
import com.recommender.web.dto.IngestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = Logger.getLogger(AdminController.class.getName());

    private final IndexService          indexService;
    private final IngestService         ingestService;
    private final RecommendationService recommendationService;
    private final PaperRepository       paperRepo;
    private final UserRepository        userRepo;
    private final InteractionRepository interactionRepo;

    public AdminController(IndexService indexService,
                           IngestService ingestService,
                           RecommendationService recommendationService,
                           PaperRepository paperRepo,
                           UserRepository userRepo,
                           InteractionRepository interactionRepo) {
        this.indexService          = indexService;
        this.ingestService         = ingestService;
        this.recommendationService = recommendationService;
        this.paperRepo             = paperRepo;
        this.userRepo              = userRepo;
        this.interactionRepo       = interactionRepo;
    }

    // ── index management ─────────────────────────────────────────────────────

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        long start = System.currentTimeMillis();
        indexService.reindex();
        long elapsed = System.currentTimeMillis() - start;
        return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "paperCount",  indexService.paperCount(),
                "elapsedMs",   elapsed,
                "completedAt", Instant.now().toString()
        ));
    }

    @PostMapping("/deep-ingest-all")
    public ResponseEntity<Map<String, Object>> deepIngestAll(
            @RequestParam(defaultValue = "20") int maxRefs,
            @RequestParam(defaultValue = "300") long delayMs) {

        maxRefs = Math.max(0, Math.min(maxRefs, 200));
        delayMs = Math.max(0, Math.min(delayMs, 5000));

        List<PaperEntity> papers = paperRepo.findAll();
        List<String> errors = new ArrayList<>();
        int processed = 0, totalSaved = 0, skipped = 0;

        for (PaperEntity p : papers) {
            if (p.getS2Id() == null) { skipped++; continue; }
            try {
                IngestResult r = ingestService.ingestPaperDeep(p.getS2Id(), maxRefs);
                totalSaved += r.saved();
                errors.addAll(r.errors());
                processed++;
                if (delayMs > 0) Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                errors.add(p.getS2Id() + ": " + e.getMessage());
                log.warning("[AdminController] deep-ingest-all error for " + p.getS2Id() + ": " + e.getMessage());
            }
        }
        indexService.reindex();
        return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "processed",   processed,
                "skipped",     skipped,
                "saved",       totalSaved,
                "errors",      errors,
                "completedAt", Instant.now().toString()
        ));
    }

    // ── diagnostics (legacy) ─────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "papersInDb",    paperRepo.count(),
                "papersInIndex", indexService.paperCount(),
                "bm25Docs",      indexService.bm25DocCount(),
                "bprTrained",    indexService.isBprTrained()
        ));
    }

    @GetMapping("/search-debug")
    public ResponseEntity<Map<String, Object>> searchDebug(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int k) {
        List<String> tokens = Tokenizer.tokenize(q);
        List<Integer> hits  = indexService.searchBm25(tokens, k);
        return ResponseEntity.ok(Map.of(
                "query",        q,
                "tokens",       tokens,
                "bm25DocCount", indexService.bm25DocCount(),
                "hits",         hits
        ));
    }

    // ── analytics: users ─────────────────────────────────────────────────────

    @GetMapping("/users-overview")
    public ResponseEntity<List<Map<String, Object>>> usersOverview() {
        List<UserEntity> users = userRepo.findAll();
        List<InteractionEntity> allInts = interactionRepo.findAll();

        Map<UUID, List<InteractionEntity>> byUser = new HashMap<>();
        for (InteractionEntity ie : allInts) {
            byUser.computeIfAbsent(ie.getUserId(), k -> new ArrayList<>()).add(ie);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserEntity u : users) {
            List<InteractionEntity> ints = byUser.getOrDefault(u.getId(), List.of());
            Map<String, Long> counts = new LinkedHashMap<>();
            for (InteractionEntity ie : ints) counts.merge(ie.getActionType().name(), 1L, Long::sum);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",                u.getId());
            row.put("name",              u.getName());
            row.put("createdAt",         u.getCreatedAt());
            row.put("totalInteractions", ints.size());
            row.put("actionCounts",      counts);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-detail/{userId}")
    public ResponseEntity<Map<String, Object>> userDetail(@PathVariable UUID userId) {
        Optional<UserEntity> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        UserEntity user = userOpt.get();
        List<InteractionEntity> ints = interactionRepo.findByUserId(userId);

        Set<UUID> paperIds = new HashSet<>();
        for (InteractionEntity ie : ints) paperIds.add(ie.getPaperId());
        Map<UUID, String> titleMap = new HashMap<>();
        if (!paperIds.isEmpty()) {
            paperRepo.findAllById(paperIds).forEach(p -> titleMap.put(p.getId(), p.getTitle()));
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (InteractionEntity ie : ints) counts.merge(ie.getActionType().name(), 1L, Long::sum);

        List<InteractionEntity> sorted = new ArrayList<>(ints);
        sorted.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<Map<String, Object>> interactions = new ArrayList<>();
        for (InteractionEntity ie : sorted) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paperId",     ie.getPaperId());
            m.put("paperTitle",  titleMap.getOrDefault(ie.getPaperId(), "Unknown"));
            m.put("actionType",  ie.getActionType().name());
            m.put("weight",      ie.getWeight());
            m.put("createdAt",   ie.getCreatedAt());
            interactions.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", Map.of("id", user.getId(), "name", user.getName(), "createdAt", user.getCreatedAt()));
        result.put("interactions",      interactions);
        result.put("actionCounts",      counts);
        result.put("totalInteractions", ints.size());
        return ResponseEntity.ok(result);
    }

    // ── analytics: papers & index ────────────────────────────────────────────

    @GetMapping("/paper-stats")
    public ResponseEntity<Map<String, Object>> paperStats() {
        List<PaperEntity> papers = paperRepo.findAll();

        int withEmb = 0, withoutEmb = 0;
        for (PaperEntity p : papers) {
            if (p.getEmbedding() != null) withEmb++; else withoutEmb++;
        }

        Map<Integer, Long> byYear = new LinkedHashMap<>();
        for (Object[] row : paperRepo.countByYear()) {
            byYear.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        Map<String, Long> byTopic = new LinkedHashMap<>();
        for (Object[] row : paperRepo.countByTopic()) {
            byTopic.put((String) row[0], ((Number) row[1]).longValue());
        }

        double[] prScores = indexService.getAllPageRankScores();
        List<Map<String, Object>> topByPR = new ArrayList<>();
        for (PaperEntity p : papers) {
            Integer idx = indexService.paperUuidToIdx(p.getId());
            if (idx == null || idx >= prScores.length) continue;
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id",            p.getId());
            pm.put("title",         p.getTitle());
            pm.put("year",          p.getYear());
            pm.put("pageRankScore", prScores[idx]);
            pm.put("citationCount", p.getCitationCount());
            topByPR.add(pm);
        }
        topByPR.sort((a, b) -> Double.compare((Double) b.get("pageRankScore"), (Double) a.get("pageRankScore")));
        if (topByPR.size() > 10) topByPR = topByPR.subList(0, 10);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPapers",    papers.size());
        result.put("withEmbedding",  withEmb);
        result.put("withoutEmbedding", withoutEmb);
        result.put("byYear",         byYear);
        result.put("byTopic",        byTopic);
        result.put("topByPageRank",  topByPR);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/index-detail")
    public ResponseEntity<Map<String, Object>> indexDetail() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("papersInDb",         paperRepo.count());
        result.put("papersInIndex",       indexService.paperCount());
        result.put("embeddingCount",      indexService.getEmbeddingCount());
        result.put("userCount",           indexService.getUserCount());
        result.put("bm25Docs",            indexService.bm25DocCount());
        result.put("bprTrained",          indexService.isBprTrained());
        result.put("embeddingDim",        768);
        result.put("hnswM",               16);
        result.put("hnswEfConstruction",  200);
        result.put("hnswEfSearch",        50);
        result.put("bprFactors",          64);
        result.put("bprEpochs",           100);
        result.put("bprLr",               0.01);
        result.put("bprLambda",           0.001);
        result.put("pageRankDamping",     0.85);
        result.put("pageRankIterations",  20);
        result.put("mmrLambda",           0.7);
        result.put("candidateFactor",     5);
        result.put("mmrRerankFactor",     2);
        result.put("retrievedAt",         Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    // ── analytics: recommendation explain ────────────────────────────────────

    @GetMapping("/recommend-explain")
    public ResponseEntity<Map<String, Object>> recommendExplain(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int k) {

        k = Math.max(1, Math.min(k, 50));
        RecommendationService.RecommendExplain ex = recommendationService.recommendExplained(userId, q, k);

        List<UUID> ids = new ArrayList<>();
        for (RecommendationService.RecommendExplain.PaperSignal ps : ex.results()) ids.add(ps.paperId());
        Map<UUID, PaperEntity> paperMap = new HashMap<>();
        paperRepo.findAllById(ids).forEach(p -> paperMap.put(p.getId(), p));

        List<Map<String, Object>> papers = new ArrayList<>();
        int rank = 1;
        for (RecommendationService.RecommendExplain.PaperSignal ps : ex.results()) {
            PaperEntity p = paperMap.get(ps.paperId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank",          rank++);
            m.put("id",            ps.paperId());
            m.put("title",         p != null ? p.getTitle() : "Unknown");
            m.put("year",          p != null ? p.getYear()  : null);
            m.put("signals",       ps.signals());
            m.put("rrfScore",      ps.rrfScore());
            m.put("pageRankScore", ps.pageRankScore());
            m.put("citationCount", p != null ? p.getCitationCount() : 0);
            papers.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hnswActive",      ex.hnswActive());
        result.put("bprActive",       ex.bprActive());
        result.put("bm25Active",      ex.bm25Active());
        result.put("pageRankActive",  true);
        result.put("totalResults",    papers.size());
        result.put("papers",          papers);
        return ResponseEntity.ok(result);
    }
}
