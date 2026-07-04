#ifndef BPR_H
#define BPR_H

#ifdef __cplusplus
extern "C" {
#endif

/* ── Model ─────────────────────────────────────────────────────────────────── */

typedef struct {
    int    num_users;
    int    num_items;
    int    num_factors;
    float* user_factors;   /* [num_users  × num_factors] row-major */
    float* item_factors;   /* [num_items  × num_factors] row-major */
    unsigned rng_state;    /* LCG random state for negative sampling */
} BprModel;

/* ── API ────────────────────────────────────────────────────────────────────── */

/**
 * Allocate and initialise a BPR model with small random weights.
 * @param seed  random seed for weight initialisation
 */
BprModel* bpr_create(int num_users, int num_items, int num_factors, unsigned seed);

/** Free all memory held by the model. */
void bpr_free(BprModel* model);

/**
 * Train via SGD on (user, positive_item) pairs.
 * Negative items are sampled uniformly at random per step.
 *
 * @param users     user index for each interaction, length n_pairs
 * @param pos_items positive item index for each interaction, length n_pairs
 * @param n_pairs   number of (user, item) pairs
 * @param num_epochs SGD epochs
 * @param lr        learning rate
 * @param reg       L2 regularisation coefficient
 */
void bpr_fit(BprModel* model,
             const int* users,
             const int* pos_items,
             int n_pairs,
             int num_epochs,
             float lr,
             float reg);

/**
 * Recommend top-k items for a user.
 * Items in the exclude list are skipped.
 *
 * @param results_out  output buffer of length k — filled with item indices best-first
 */
void bpr_recommend(const BprModel* model,
                   int user_id,
                   const int* exclude,
                   int n_exclude,
                   int k,
                   int* results_out);

#ifdef __cplusplus
}
#endif
#endif /* BPR_H */
