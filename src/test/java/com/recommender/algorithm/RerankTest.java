package com.recommender.algorithm;

import com.recommender.algorithm.rerank.FusionReranker;
import com.recommender.algorithm.rerank.MmrReranker;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RerankTest {

    // ── FusionReranker ────────────────────────────────────────────────────────

    @Test
    void rrfTopDocumentConsistentlyTopRanked() {
        // Doc 1L is ranked first in all three lists
        List<List<Long>> rankings = List.of(
                List.of(1L, 2L, 3L),
                List.of(1L, 3L, 2L),
                List.of(1L, 2L, 3L)
        );
        List<Long> fused = FusionReranker.reciprocalRankFusion(rankings);
        assertEquals(1L, fused.get(0), "doc ranked #1 in all lists should win RRF");
    }

    @Test
    void rrfCombinesDisjointLists() {
        List<List<Long>> rankings = List.of(
                List.of(1L, 2L),
                List.of(3L, 4L)
        );
        List<Long> fused = FusionReranker.reciprocalRankFusion(rankings);
        assertEquals(4, fused.size());
        assertTrue(fused.containsAll(List.of(1L, 2L, 3L, 4L)));
    }

    @Test
    void rrfRewardsAppearingInMoreLists() {
        // Doc 2 is #2 in both lists: 2/(60+2) ≈ 0.0323 > 1/(60+1) ≈ 0.0164 for docs 1 and 3
        List<List<Long>> rankings = List.of(
                List.of(1L, 2L),
                List.of(3L, 2L)
        );
        List<Long> fused = FusionReranker.reciprocalRankFusion(rankings);
        assertEquals(2L, fused.get(0), "consensus across lists should beat a single first place");
    }

    // ── MmrReranker ───────────────────────────────────────────────────────────

    @Test
    void mmrLambda1IsPureRelevance() {
        List<Long> candidates = List.of(1L, 2L, 3L);
        Map<Long, Double> rel = Map.of(1L, 0.9, 2L, 0.7, 3L, 0.5);

        // With λ=1 (pure relevance) MMR order = relevance order
        float[] e1 = {1, 0, 0};
        float[] e2 = {1, 0, 0}; // identical to e1 (high similarity)
        float[] e3 = {0, 1, 0};
        Map<Long, float[]> emb = Map.of(1L, e1, 2L, e2, 3L, e3);

        List<Long> result = MmrReranker.rerank(candidates, rel, emb, 1.0, 3);
        assertEquals(1L, result.get(0), "with lambda=1, most relevant doc should be first");
    }

    @Test
    void mmrLambda0PrefersDiversity() {
        // Docs 1 and 2 are identical (cos-sim = 1); doc 3 is orthogonal
        List<Long> candidates = List.of(1L, 2L, 3L);
        Map<Long, Double> rel = Map.of(1L, 0.9, 2L, 0.85, 3L, 0.3);

        float[] e1 = {1, 0};
        float[] e2 = {1, 0}; // same direction as e1
        float[] e3 = {0, 1}; // orthogonal
        Map<Long, float[]> emb = Map.of(1L, e1, 2L, e2, 3L, e3);

        // With λ=0 (pure diversity): after picking 1, doc 3 should be preferred over doc 2
        List<Long> result = MmrReranker.rerank(candidates, rel, emb, 0.0, 3);
        int pos3 = result.indexOf(3L);
        int pos2 = result.indexOf(2L);
        assertTrue(pos3 < pos2, "orthogonal doc should be ranked higher when lambda=0");
    }

    @Test
    void mmrResultSizeRespectedK() {
        List<Long> candidates = List.of(1L, 2L, 3L, 4L, 5L);
        Map<Long, Double> rel = new HashMap<>();
        for (long id : candidates) rel.put(id, Math.random());
        Map<Long, float[]> emb = new HashMap<>();
        for (long id : candidates) emb.put(id, new float[]{(float) id, 0});

        List<Long> result = MmrReranker.rerank(candidates, rel, emb, 0.5, 3);
        assertEquals(3, result.size());
    }
}
