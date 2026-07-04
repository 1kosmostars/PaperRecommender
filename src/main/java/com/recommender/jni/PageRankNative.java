package com.recommender.jni;

/**
 * JNI wrapper for the C PageRank implementation (libpagerank).
 *
 * <p>Usage:
 * <pre>{@code
 *   double[] scores = PageRankNative.compute(citing, cited, numEdges, numNodes, 0.85, 20);
 * }</pre>
 */
public final class PageRankNative {

    static {
        NativeLoader.load("pagerank");
    }

    private PageRankNative() {}

    /**
     * Compute PageRank via power iteration.
     *
     * @param citing     edge sources (citing node index), length numEdges
     * @param cited      edge targets (cited node index),  length numEdges
     * @param numEdges   number of directed edges
     * @param numNodes   total number of nodes
     * @param damping    damping factor (typically 0.85)
     * @param iterations number of power-iteration steps
     * @return           PageRank score per node (indexed 0..numNodes-1)
     */
    public static native double[] compute(
            int[] citing,
            int[] cited,
            int numEdges,
            int numNodes,
            double damping,
            int iterations
    );
}
