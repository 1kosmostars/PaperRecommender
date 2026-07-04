package com.recommender.web;

import com.recommender.service.RecommendationService;
import com.recommender.service.RecommendationService.RecommendExplain;
import com.recommender.web.dto.PaperDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendationService recommendationService;

    public RecommendController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Get personalised recommendations.
     *
     * @param userId  optional — personalises via HNSW profile + BPR;
     *                omit for cold-start (PageRank + BM25 only)
     * @param q       optional free-text query fed into BM25
     * @param k       number of results (default 10, max 100)
     */
    @GetMapping
    public ResponseEntity<List<PaperDto>> recommend(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int k) {

        k = Math.max(1, Math.min(k, 100));

        List<UUID> ids = recommendationService.recommend(userId, q, k);
        List<PaperDto> papers = recommendationService.fetchPapers(ids);

        return ResponseEntity.ok(papers);
    }

    public record PaperWithExplain(
            PaperDto paper,
            List<String> signals,
            double rrfScore,
            double pageRankScore
    ) {}

    /** Recommendations with per-paper explanation of which algorithms contributed. */
    @GetMapping("/explain")
    public ResponseEntity<List<PaperWithExplain>> explainedRecommend(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int k) {

        k = Math.max(1, Math.min(k, 100));

        RecommendExplain explain = recommendationService.recommendExplained(userId, q, k);

        List<UUID> ids = explain.results().stream()
                .map(RecommendExplain.PaperSignal::paperId)
                .toList();

        List<PaperDto> papers = recommendationService.fetchPapers(ids);

        Map<UUID, RecommendExplain.PaperSignal> signalMap = new HashMap<>();
        for (RecommendExplain.PaperSignal s : explain.results()) signalMap.put(s.paperId(), s);

        List<PaperWithExplain> result = new ArrayList<>(papers.size());
        for (PaperDto paper : papers) {
            RecommendExplain.PaperSignal s = signalMap.get(paper.id());
            result.add(new PaperWithExplain(
                    paper,
                    s != null ? s.signals() : List.of(),
                    s != null ? s.rrfScore() : 0.0,
                    s != null ? s.pageRankScore() : 0.0
            ));
        }
        return ResponseEntity.ok(result);
    }
}
