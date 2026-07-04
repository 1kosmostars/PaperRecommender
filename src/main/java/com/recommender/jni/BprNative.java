package com.recommender.jni;

/**
 * JNI wrapper for the C BPR (Bayesian Personalized Ranking) model (libbpr).
 *
 * <p>Lifecycle:
 * <pre>{@code
 *   long ptr = BprNative.createModel(numUsers, numItems, 64, 42L);
 *   BprNative.fit(ptr, userArr, itemArr, n, 100, 0.01f, 0.001f);
 *   int[] top10 = BprNative.recommend(ptr, userId, excludeArr, 10);
 *   BprNative.freeModel(ptr);
 * }</pre>
 */
public final class BprNative {

    static {
        NativeLoader.load("bpr");
    }

    private BprNative() {}

    /**
     * Allocate and initialise a BPR model.
     *
     * @param numUsers   total number of users
     * @param numItems   total number of items (papers)
     * @param numFactors latent factor dimension
     * @param seed       random seed for weight initialisation
     * @return           native pointer (opaque handle)
     */
    public static native long createModel(int numUsers, int numItems, int numFactors, long seed);

    /** Free the native model. Always call this when done. */
    public static native void freeModel(long handle);

    /**
     * Train via SGD on (user, positive_item) pairs.
     *
     * @param users     user index per interaction
     * @param posItems  positive item index per interaction
     * @param nPairs    number of (user, item) pairs
     * @param numEpochs SGD epochs
     * @param lr        learning rate (e.g. 0.01)
     * @param reg       L2 regularisation (e.g. 0.001)
     */
    public static native void fit(long handle,
                                  int[] users, int[] posItems, int nPairs,
                                  int numEpochs, float lr, float reg);

    /**
     * Recommend top-k items for a user.
     *
     * @param userId  user index
     * @param exclude item indices to skip (already seen)
     * @param k       number of results
     * @return        item indices sorted best-first
     */
    public static native int[] recommend(long handle, int userId, int[] exclude, int k);
}
