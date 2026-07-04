#ifndef HNSW_H
#define HNSW_H

#include <stdlib.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ── Structures ─────────────────────────────────────────────────────────── */

typedef struct { int id; float dist; } HnswEntry;

/** Min- or max-heap (is_max=1 → max-heap, =0 → min-heap). */
typedef struct {
    HnswEntry *data;
    int        size, cap, is_max;
} HnswHeap;

/** Adjacency list for one node at one layer. */
typedef struct {
    int *ids;
    int  size, cap;
} HnswNeighbors;

/** Single node: vector + per-layer neighbour lists. */
typedef struct {
    float         *vec;     /* float[dim] */
    int            level;   /* max layer index */
    HnswNeighbors *layers;  /* layers[0..level] */
} HnswNode;

/** The index. */
typedef struct {
    int    M, M0;           /* max neighbours per layer; M0 = 2*M for layer 0 */
    int    ef_construction; /* beam search width */
    double mL;              /* 1/ln(M) */
    int    dim; /* vector dimensions */

    HnswNode *nodes;
    int       size, cap;

    int      entry_point;
    int      max_level;

    uint64_t rng_state;
} HnswIndex;

/* ── API ─────────────────────────────────────────────────────────────────── */

/** Create an empty index. */
HnswIndex *hnsw_create(int dim, int M, int ef_construction);

/** Free all memory. */
void hnsw_free(HnswIndex *idx);

/**
 * Insert a vector into the index.
 * @return the assigned node id (sequential, starting at 0)
 */
int hnsw_add(HnswIndex *idx, const float *vec);

/**
 * Approximate k-NN search.
 *
 * @param out_ids   caller-allocated int[k]
 * @param out_dists caller-allocated float[k] (may be NULL)
 * @return actual number of results (< k if the index has fewer nodes)
 */
int hnsw_search(const HnswIndex *idx,
                const float *query,
                int k, int ef,
                int *out_ids, float *out_dists);

#ifdef __cplusplus
}
#endif
#endif /* HNSW_H */
