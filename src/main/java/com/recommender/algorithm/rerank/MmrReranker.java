package com.recommender.algorithm.rerank;

import java.util.*;

/**
 * Maximal Marginal Relevance (MMR) reranker for diversity.
 *
 * <p>Reference: Carbonell & Goldstein (1998) "The Use of MMR, Diversity-Based Reranking
 * for Reordering Documents and Producing Summaries."
 *
 * <p>At each step, the next item is selected by:
 * <pre>
 *   argmax_{d ∈ R∖S}  [ λ · rel(d, q) − (1−λ) · max_{s ∈ S} sim(d, s) ]
 * </pre>
 * where R is the candidate set, S is the set of already selected items,
 * rel(d, q) is the relevance score (e.g. from BM25/PPR), and
 * sim(d, s) is the embedding similarity between candidates.
 *
 * <p>λ = 1 → pure relevance ranking;  λ = 0 → pure diversity.
 */
public class MmrReranker {

    private MmrReranker() {}

    /**
     * Rerank {@code candidates} for diversity using their embeddings.
     *
     * @param candidates   ordered list of candidate paper ids (best-first from upstream ranker)
     * @param relevance    paperId → relevance score (higher = more relevant)
     * @param embeddings   paperId → embedding vector (e.g. SPECTER2, 768-dim)
     * @param lambda       trade-off parameter: 1.0 = all relevance, 0.0 = all diversity
     * @param k            number of results to return
     * @return             reranked list of paper ids
     */
    public static List<Long> rerank(
            List<Long> candidates,
            Map<Long, Double> relevance,
            Map<Long, float[]> embeddings,
            double lambda,
            int k) {

        if (candidates.isEmpty()) return Collections.emptyList();

        Set<Long> remaining = new LinkedHashSet<>(candidates);
        List<Long> selected = new ArrayList<>(Math.min(k, candidates.size()));

        while (selected.size() < k && !remaining.isEmpty()) {
            Long best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Long candidate : remaining) {
                double rel = relevance.getOrDefault(candidate, 0.0);

                // Max similarity to already selected items
                double maxSim = 0.0;
                if (!selected.isEmpty()) {
                    float[] ce = embeddings.get(candidate);
                    for (Long s : selected) {
                        float[] se = embeddings.get(s);
                        if (ce != null && se != null) {
                            double sim = cosineSimilarity(ce, se);
                            if (sim > maxSim) maxSim = sim;
                        }
                    }
                }

                double score = lambda * rel - (1.0 - lambda) * maxSim;
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            selected.add(best);
            remaining.remove(best);
        }

        return selected;
    }

    /**
     * Overload that accepts raw float[][] arrays parallel to the candidates list.
     *
     * @param embeddingMatrix embeddingMatrix[i] = embedding of candidates.get(i)
     */
    public static List<Long> rerank(
            List<Long> candidates,
            Map<Long, Double> relevance,
            float[][] embeddingMatrix,
            double lambda,
            int k) {

        Map<Long, float[]> embMap = new HashMap<>(candidates.size() * 2);
        for (int i = 0; i < candidates.size(); i++) {
            if (i < embeddingMatrix.length) embMap.put(candidates.get(i), embeddingMatrix[i]);
        }
        return rerank(candidates, relevance, embMap, lambda, k);
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    /** Cosine similarity ∈ [−1, 1]. Returns 0 if either vector is zero. */
    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
