package com.recommender.jni;

import java.util.ArrayList;
import java.util.List;

/**
 * JNI wrapper for the native HNSW index (hnsw.dll / libhnsw.so).
 *
 * <p>Life-cycle:
 * <pre>{@code
 *   long ptr = HnswNative.createIndex(768, 16, 200);
 *   HnswNative.addVector(ptr, embedding);
 *   int[] neighbours = HnswNative.search(ptr, query, 10, 50);
 *   HnswNative.freeIndex(ptr);
 * }</pre>
 *
 * <p>Thread safety: {@code addVector} must not be called concurrently with itself
 * or with {@code search}. Concurrent {@code search} calls are safe.
 */
public final class HnswNative {

    static {
        NativeLoader.load("hnsw");
    }

    private HnswNative() {}

    // ── lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Create a new HNSW index.
     *
     * @param dim             vector dimension (e.g. 768 for SPECTER2)
     * @param M               max connections per node per layer (e.g. 16)
     * @param efConstruction  beam width during insertion (e.g. 200)
     * @return opaque native pointer
     */
    public static native long createIndex(int dim, int M, int efConstruction);

    /** Free all native memory. Must be called exactly once per pointer. */
    public static native void freeIndex(long indexPtr);

    // ── mutation ──────────────────────────────────────────────────────────────

    /**
     * Insert a vector into the index.
     *
     * @param vector float[dim]
     * @return the assigned node id (sequential, 0-based)
     */
    public static native int addVector(long indexPtr, float[] vector);

    // ── search ────────────────────────────────────────────────────────────────

    /**
     * Return the ids of the k approximate nearest neighbours of {@code query}.
     *
     * @param k  number of results
     * @param ef exploration factor (ef ≥ k; larger → better recall, slower)
     * @return int[] of node ids, sorted nearest-first; length may be < k if index is small
     */
    public static native int[] search(long indexPtr, float[] query, int k, int ef);

    /**
     * Like {@link #search} but also returns distances.
     *
     * @return float[] of length 2*n interleaved as [id0_bits, dist0, id1_bits, dist1, ...]
     *         where id is stored as raw float bits (use Float.floatToRawIntBits to decode)
     */
    public static native float[] searchWithDistances(long indexPtr, float[] query, int k, int ef);

    /** Number of vectors currently in the index. */
    public static native int size(long indexPtr);

    // ── convenience helpers ───────────────────────────────────────────────────

    /**
     * Convenience: search and return results as a list of (nodeId, distance) records.
     */
    public static List<SearchResult> searchResults(long indexPtr, float[] query, int k, int ef) {
        float[] raw = searchWithDistances(indexPtr, query, k, ef);
        int n = raw.length / 2;
        List<SearchResult> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int id   = Float.floatToRawIntBits(raw[i * 2]);
            float dist = raw[i * 2 + 1];
            out.add(new SearchResult(id, dist));
        }
        return out;
    }

    public record SearchResult(int nodeId, float distance) {}
}
