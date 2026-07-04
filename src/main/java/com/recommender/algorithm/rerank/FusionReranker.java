package com.recommender.algorithm.rerank;

import java.util.*;

/**
 * Reciprocal Rank Fusion (Cormack et al., 2009):
 * {@code score(d) = Σ_r 1 / (k + rank_r(d))}, robust to score scale differences.
 */
public class FusionReranker {

    private static final int DEFAULT_RRF_K = 60;  // RRF smoothing constant (Cormack et al., 2009)

    private FusionReranker() {}

    /**
     * Fuse multiple ranked lists using RRF.
     *
     * @param rankings  each inner list is a ranked list of paper ids (rank 0 = best)
     * @param k         RRF smoothing constant (typically 60)
     * @return          combined ranking: list of paper ids sorted best-first
     */
    public static List<Long> reciprocalRankFusion(List<List<Long>> rankings, int k) {
        Map<Long, Double> scores = new HashMap<>();

        for (List<Long> ranking : rankings) {
            for (int rank = 0; rank < ranking.size(); rank++) {
                long paperId = ranking.get(rank);
                scores.merge(paperId, 1.0 / (k + rank + 1), Double::sum);
            }
        }

        return sortedByScore(scores);
    }

    /** Convenience overload with default k = 60. */
    public static List<Long> reciprocalRankFusion(List<List<Long>> rankings) {
        return reciprocalRankFusion(rankings, DEFAULT_RRF_K);
    }

    /** Sort a paperId→score map in descending score order, return id list. */
    public static List<Long> sortedByScore(Map<Long, Double> scores) {
        List<Long> ids = new ArrayList<>(scores.keySet());
        ids.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        return ids;
    }
}
