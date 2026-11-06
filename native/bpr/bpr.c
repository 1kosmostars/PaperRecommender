#include "bpr.h"
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* ── Constants ───────────────────────────────────────────────────────────────── */

#define LCG_MULTIPLIER   1664525u     /* Knuth multiplicative LCG constant      */
#define LCG_INCREMENT    1013904223u  /* Knuth additive LCG constant            */
#define LCG_FLOAT_SCALE  (1u << 24)  /* divisor to map 24-bit int to [0, 1)    */
#define BPR_SEED_XOR     0xabcdeabc  /* XOR mask applied to the user seed       */
#define BPR_INIT_SCALE   0.01f       /* uniform initialisation range [0, scale] */
#define BPR_EXCLUDED_SCORE (-1e30f)  /* score assigned to excluded items        */

/* ── LCG random ─────────────────────────────────────────────────────────────── */

static inline unsigned lcg_next(unsigned* state) {
    *state = *state * LCG_MULTIPLIER + LCG_INCREMENT;
    return *state;
}

static inline float lcg_float(unsigned* state) {
    return (float)(lcg_next(state) >> 8) / (float)LCG_FLOAT_SCALE;
}

/* ── Helpers ─────────────────────────────────────────────────────────────────── */

static inline float dot(const float* a, const float* b, int n) {
    float s = 0.0f;
    for (int i = 0; i < n; i++) s += a[i] * b[i];
    return s;
}

/* ── Public API ──────────────────────────────────────────────────────────────── */

BprModel* bpr_create(int num_users, int num_items, int num_factors, unsigned seed) {
    BprModel* m = (BprModel*)malloc(sizeof(BprModel));
    if (!m) return NULL;

    m->num_users   = num_users;
    m->num_items   = num_items;
    m->num_factors = num_factors;
    m->rng_state   = seed ^ BPR_SEED_XOR;

    size_t usz = (size_t)num_users * num_factors;
    size_t isz = (size_t)num_items * num_factors;

    m->user_factors = (float*)malloc(usz * sizeof(float));
    m->item_factors = (float*)malloc(isz * sizeof(float));
    if (!m->user_factors || !m->item_factors) {
        free(m->user_factors); free(m->item_factors); free(m);
        return NULL;
    }

    /* Small uniform initialisation in [0, 0.01] */
    for (size_t i = 0; i < usz; i++)
        m->user_factors[i] = lcg_float(&m->rng_state) * BPR_INIT_SCALE;
    for (size_t i = 0; i < isz; i++)
        m->item_factors[i] = lcg_float(&m->rng_state) * BPR_INIT_SCALE;

    return m;
}

void bpr_free(BprModel* m) {
    if (!m) return;
    free(m->user_factors);
    free(m->item_factors);
    free(m);
}

void bpr_fit(BprModel* m,
             const int* users, const int* pos_items, int n_pairs,
             int num_epochs, float lr, float reg)
{
    if (!m || n_pairs == 0) return;

    int F = m->num_factors;

    /* Shuffle index array for stochastic order */
    int* order = (int*)malloc((size_t)n_pairs * sizeof(int));
    if (!order) return;
    for (int i = 0; i < n_pairs; i++) order[i] = i;

    for (int epoch = 0; epoch < num_epochs; epoch++) {
        /* Fisher-Yates shuffle */
        for (int i = n_pairs - 1; i > 0; i--) {
            int j = (int)(lcg_next(&m->rng_state) % (unsigned)(i + 1));
            int tmp = order[i]; order[i] = order[j]; order[j] = tmp;
        }

        for (int k = 0; k < n_pairs; k++) {
            int idx = order[k];
            int u   = users[idx];
            int i   = pos_items[idx];

            /* Sample a negative item uniformly */
            int j = (int)(lcg_next(&m->rng_state) % (unsigned)m->num_items);
            /* Avoid trivial negatives (same as positive) */
            if (j == i) j = (j + 1) % m->num_items;

            float* uf = m->user_factors + (size_t)u * F;
            float* vp = m->item_factors + (size_t)i * F;
            float* vn = m->item_factors + (size_t)j * F;

            /* x_uij = u · (v_pos - v_neg) */
            float x_uij = 0.0f;
            for (int f = 0; f < F; f++)
                x_uij += uf[f] * (vp[f] - vn[f]);

            /* Gradient of log sigmoid */
            float sig  = 1.0f / (1.0f + expf(-x_uij));
            float grad = 1.0f - sig;

            /* SGD update */
            for (int f = 0; f < F; f++) {
                float wu   = uf[f];            /* pre-update user factor */
                float diff = vp[f] - vn[f];
                uf[f] += lr * (grad * diff - reg * wu);
                vp[f] += lr * (grad * wu   - reg * vp[f]);
                vn[f] += lr * (-grad * wu  - reg * vn[f]);
            }
        }
    }

    free(order);
}

void bpr_recommend(const BprModel* m,
                   int user_id,
                   const int* exclude, int n_exclude,
                   int k, int* results_out)
{
    if (!m || user_id < 0 || user_id >= m->num_users) return;

    int F  = m->num_factors;
    int N  = m->num_items;
    const float* uf = m->user_factors + (size_t)user_id * F;

    float* scores = (float*)malloc((size_t)N * sizeof(float));
    int*   idx    = (int*)  malloc((size_t)N * sizeof(int));
    if (!scores || !idx) { free(scores); free(idx); return; }

    /* Compute all item scores */
    for (int i = 0; i < N; i++) {
        scores[i] = dot(uf, m->item_factors + (size_t)i * F, F);
        idx[i] = i;
    }

    /* Zero out excluded items */
    for (int e = 0; e < n_exclude; e++) {
        int ex = exclude[e];
        if (ex >= 0 && ex < N) scores[ex] = BPR_EXCLUDED_SCORE;
    }

    /* Partial selection sort for top-k (k is small, typically ≤ 100) */
    int lim = k < N ? k : N;
    for (int t = 0; t < lim; t++) {
        int best = t;
        for (int i = t + 1; i < N; i++)
            if (scores[idx[i]] > scores[idx[best]]) best = i;
        int tmp = idx[t]; idx[t] = idx[best]; idx[best] = tmp;
        results_out[t] = idx[t];
    }

    free(scores);
    free(idx);
}
