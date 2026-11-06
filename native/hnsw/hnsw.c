#include "hnsw.h"
#include <math.h>
#include <string.h>

/* ── Constants ───────────────────────────────────────────────────────────── */

#define HNSW_INITIAL_NODE_CAP  128   /* initial capacity of the node array     */
#define NEIGHBOR_CAP_MARGIN      4   /* extra slots in neighbour list on alloc  */
#define HEAP_CAP_MARGIN         16   /* extra slots in heap on alloc            */
#define RNG_DOUBLE_EPSILON    1e-15  /* lower bound to keep rng_double > 0      */
#define RNG_SHIFT_A             13   /* xorshift64 first shift                  */
#define RNG_SHIFT_B              7   /* xorshift64 second shift                 */
#define RNG_SHIFT_C             17   /* xorshift64 third shift                  */
#define RNG_SEED  UINT64_C(0xAABBCCDDEEFFAABB)  /* initial rng state           */

/* ── RNG: xorshift64 ────────────────────────────────────────────────────── */

static uint64_t rng_next(uint64_t *s) {
    uint64_t x = *s;
    x ^= x << RNG_SHIFT_A; x ^= x >> RNG_SHIFT_B; x ^= x << RNG_SHIFT_C;
    return (*s = x);
}

/* Uniform double in (0, 1]. */
static double rng_double(uint64_t *s) {
    /* Use upper 53 bits → full double mantissa precision. */
    double v = (double)(rng_next(s) >> 11) / (double)(UINT64_C(1) << 53);
    return v == 0.0 ? RNG_DOUBLE_EPSILON : v;
}

/* ── Distance: cosine ────────────────────────────────────────────────────── */

static float cosine_dist(const float *a, const float *b, int dim)
 {
 /*dist = 1-(a*b)/(|a|*|b|)*/
    float dot = 0.f, na = 0.f, nb = 0.f;
    /*dot - scalar product, na - |a|^2, nb - |b|^2 */
    for (int i = 0; i < dim; i++) {
        dot += a[i] * b[i];
        na  += a[i] * a[i];
        nb  += b[i] * b[i];
    }
    if (na == 0.f || nb == 0.f) return 1.f;
    return 1.f - dot / sqrtf(na * nb);
    /*sqrtf(na*nb) - |a|*|b|*/
}

/* ── Heap ────────────────────────────────────────────────────────────────── */

static void heap_init(HnswHeap *h, int init_cap, int is_max) {
    h->data   = (HnswEntry *)malloc(init_cap * sizeof(HnswEntry));
    h->size   = 0;
    h->cap    = init_cap;
    h->is_max = is_max;
}

static void heap_clear(HnswHeap *h) { h->size = 0; }

static void heap_free_data(HnswHeap *h) { free(h->data); h->data = NULL; }

static void heap_grow(HnswHeap *h) {
    h->cap *= 2;
    h->data = (HnswEntry *)realloc(h->data, h->cap * sizeof(HnswEntry));
 }

/* 1 if element at index i should be above element at index j in the heap. */
static int heap_wins(const HnswHeap *h, int i, int j) {
    return h->is_max
        ? (h->data[i].dist > h->data[j].dist)
        : (h->data[i].dist < h->data[j].dist);
}

static void heap_swap(HnswHeap *h, int i, int j) {
    HnswEntry t = h->data[i]; h->data[i] = h->data[j]; h->data[j] = t;
}

static void heap_push(HnswHeap *h, HnswEntry e) {
    if (h->size == h->cap) heap_grow(h);
    h->data[h->size] = e;
    int i = h->size++;
    while (i > 0 && heap_wins(h, i, (i - 1) >> 1)) {
        int p = (i - 1) >> 1;
        heap_swap(h, i, p);
        i = p;
    }
}

static HnswEntry heap_pop(HnswHeap *h) {
    HnswEntry top = h->data[0];
    h->data[0] = h->data[--h->size];
    int i = 0, moved;
    do {
        int l = 2*i+1, r = 2*i+2, best = i;
        if (l < h->size && heap_wins(h, l, best)) best = l;
        if (r < h->size && heap_wins(h, r, best)) best = r;
        moved = (best != i);
        if (moved) { heap_swap(h, i, best); i = best; }
    } while (moved);
    return top;
}

/* ── Neighbour list ──────────────────────────────────────────────────────── */

static void nl_init(HnswNeighbors *n, int cap) {
    n->ids  = (int *)malloc(cap * sizeof(int));
    n->size = 0;
    n->cap  = cap;
}

static void nl_free(HnswNeighbors *n) { free(n->ids); }

static void nl_push(HnswNeighbors *n, int id) {
    if (n->size == n->cap) {
        n->cap *= 2;
        n->ids  = (int *)realloc(n->ids, n->cap * sizeof(int));
    }
    n->ids[n->size++] = id;
}

/* ── searchLayer ─────────────────────────────────────────────────────────── */
/*
 * BFS from ep at the given layer.
 * Returns a max-heap W (furthest at top) of up to ef nearest nodes.
 * The heap must already be initialised by the caller; it is cleared first.
 */
 /* - ep - entry point, узел с которого начинаем обход
     - ef - сколько максимум узлов держим в результате
     - layer - на каком слое графа ищем
     - W - max-heap, результат: ef ближайших найденных узлов
     - cands - min-heap, рабочая очередь кандидатов для обхода
     - visited - булев массив размером idx->size, отмечает посещённые узлы
 */
static void search_layer(
        const HnswIndex *idx,
        const float     *query,
        int ep, int ef, int layer,
        HnswHeap *W,        /* out: max-heap, ef nearest */
        HnswHeap *cands,    /* scratch: min-heap, reused across calls */
        char     *visited)  /* scratch: bool array of size idx->size */
{

    heap_clear(W);
    heap_clear(cands);

    float d0 = cosine_dist(query, idx->nodes[ep].vec, idx->dim);
    HnswEntry start = {ep, d0};
    heap_push(W,     start);
    heap_push(cands, start);
    visited[ep] = 1;

    int exploring = 1;
    while (cands->size > 0 && exploring) {
        HnswEntry c = heap_pop(cands);
        float furthest = W->data[0].dist;   /* max-heap: worst is at root */
        exploring = (c.dist <= furthest);
        if (exploring) {
            const HnswNode *cn = &idx->nodes[c.id];
            if (layer > cn->level) continue;    /* this node doesn't exist here */
            const HnswNeighbors *nb = &cn->layers[layer];

            for (int k = 0; k < nb->size; k++) {
                int nid = nb->ids[k];
                if (!visited[nid]) {
                    visited[nid] = 1;
                    float ed = cosine_dist(query, idx->nodes[nid].vec, idx->dim);
                    if (ed < furthest || W->size < ef) {
                        HnswEntry ne = {nid, ed};
                        heap_push(cands, ne);
                        heap_push(W, ne);
                        if (W->size > ef) heap_pop(W);   /* evict furthest */
                        furthest = W->data[0].dist;
                    }
                }
            }
        }
    }
}

/* ── Helpers for neighbor selection ─────────────────────────────────────── */

/* Compare entries by dist ascending (for qsort). */
static int entry_cmp_asc(const void *a, const void *b) {
    float da = ((const HnswEntry *)a)->dist;
    float db = ((const HnswEntry *)b)->dist;
    return (da > db) - (da < db);
}

/*
 * Extract all entries from max-heap W into out_ids/out_dists (nearest-first).
 * Returns count (<= M). W is cleared after this call.
 */
static int select_neighbors(HnswHeap *W, int M,
                             int *out_ids, float *out_dists) {
    int n = W->size;
    /* Sort ascending by distance. */
    qsort(W->data, (size_t)n, sizeof(HnswEntry), entry_cmp_asc);
    int take = n < M ? n : M;
    for (int i = 0; i < take; i++) {
        out_ids[i] = W->data[i].id;
        if (out_dists) out_dists[i] = W->data[i].dist;
    }
    heap_clear(W);
    return take;
}

/* ── Pruning ─────────────────────────────────────────────────────────────── */
/* Keep only mMax nearest neighbours of pivot in nb_list (insertion sort). */
static void prune_neighbors(HnswNeighbors *nb, const float *pivot_vec,
                             const HnswIndex *idx, int mMax) {
    int n = nb->size;
    /* Build (dist, id) array */
    HnswEntry *tmp = (HnswEntry *)malloc(n * sizeof(HnswEntry));
    for (int i = 0; i < n; i++) {
        tmp[i].id   = nb->ids[i];
        tmp[i].dist = cosine_dist(pivot_vec, idx->nodes[nb->ids[i]].vec, idx->dim);
    }
    qsort(tmp, (size_t)n, sizeof(HnswEntry), entry_cmp_asc);
    int keep = n < mMax ? n : mMax;
    for (int i = 0; i < keep; i++) nb->ids[i] = tmp[i].id;
    nb->size = keep;
    free(tmp);
}

/* ── hnsw_create / hnsw_free ─────────────────────────────────────────────── */

HnswIndex *hnsw_create(int dim, int M, int ef_construction) {
    HnswIndex *idx = (HnswIndex *)calloc(1, sizeof(HnswIndex));
    idx->dim             = dim;
    idx->M               = M;
    idx->M0              = M * 2;
    idx->ef_construction = ef_construction;
    idx->mL              = 1.0 / log((double)M);
    idx->cap             = HNSW_INITIAL_NODE_CAP;
    idx->nodes           = (HnswNode *)calloc((size_t)idx->cap, sizeof(HnswNode));
    idx->entry_point     = -1;
    idx->max_level       = -1;
    idx->rng_state       = RNG_SEED;
    return idx;
}

void hnsw_free(HnswIndex *idx) {
    for (int i = 0; i < idx->size; i++) {
        HnswNode *n = &idx->nodes[i];
        free(n->vec);
        for (int l = 0; l <= n->level; l++) nl_free(&n->layers[l]);
        free(n->layers);
    }
    free(idx->nodes);
    free(idx);
}

/* ── hnsw_add ────────────────────────────────────────────────────────────── */

int hnsw_add(HnswIndex *idx, const float *vec) {
    /* Grow node array if needed. */
    if (idx->size == idx->cap) {
        idx->cap *= 2;
        idx->nodes = (HnswNode *)realloc(idx->nodes,
                                         (size_t)idx->cap * sizeof(HnswNode));
    }

    int new_id = idx->size++;
    HnswNode *node = &idx->nodes[new_id];

    /* Copy vector. */
    node->vec = (float *)malloc((size_t)idx->dim * sizeof(float));
    memcpy(node->vec, vec, (size_t)idx->dim * sizeof(float));

    /* Sample level. */
    double r = rng_double(&idx->rng_state);
    int level = (int)(-log(r) * idx->mL);
    node->level  = level;
    node->layers = (HnswNeighbors *)malloc((size_t)(level + 1) * sizeof(HnswNeighbors));
    for (int l = 0; l <= level; l++) {
        nl_init(&node->layers[l], (l == 0 ? idx->M0 : idx->M) + NEIGHBOR_CAP_MARGIN);
    }

    if (idx->entry_point < 0) {
        idx->entry_point = new_id;
        idx->max_level   = level;
        return new_id;
    }

    /* Allocate scratch buffers (reused across layers). */
    char     *visited = (char *)calloc((size_t)idx->size, 1);
    HnswHeap  W, cands;
    heap_init(&W,     idx->ef_construction + HEAP_CAP_MARGIN, 1); /* max-heap */
    heap_init(&cands, idx->ef_construction + HEAP_CAP_MARGIN, 0); /* min-heap */

    int ep = idx->entry_point;

    /* Phase 1: greedy descent from max_level to level+1 (ef=1). */
    for (int l = idx->max_level; l > level; l--) {
        memset(visited, 0, (size_t)idx->size);
        search_layer(idx, vec, ep, 1, l, &W, &cands, visited);
        /* Nearest = min dist in max-heap W. */
        if (W.size > 0) {
            float best_d = W.data[0].dist; int best_id = W.data[0].id;
            for (int i = 1; i < W.size; i++) {
                if (W.data[i].dist < best_d) { best_d = W.data[i].dist; best_id = W.data[i].id; }
            }
            ep = best_id;
        }
    }

    /* Temp arrays for selected neighbours. */
    int   *sel_ids  = (int *)  malloc((size_t)idx->M0 * sizeof(int));
    float *sel_dsts = (float *)malloc((size_t)idx->M0 * sizeof(float));

    /* Phase 2: insert from min(level, max_level) down to 0. */
    int lim = level < idx->max_level ? level : idx->max_level;
    for (int l = lim; l >= 0; l--) {
        int mMax = (l == 0) ? idx->M0 : idx->M;

        memset(visited, 0, (size_t)idx->size);
        search_layer(idx, vec, ep, idx->ef_construction, l, &W, &cands, visited);

        int n_sel = select_neighbors(&W, mMax, sel_ids, sel_dsts);

        /* Set new node's neighbours at layer l. */
        node->layers[l].size = 0;
        for (int k = 0; k < n_sel; k++) nl_push(&node->layers[l], sel_ids[k]);

        /* Bidirectional edges. */
        for (int k = 0; k < n_sel; k++) {
            int nb_id = sel_ids[k];
            HnswNode *nb_node = &idx->nodes[nb_id];
            if (l <= nb_node->level) {
                nl_push(&nb_node->layers[l], new_id);
                if (nb_node->layers[l].size > mMax) {
                    prune_neighbors(&nb_node->layers[l], nb_node->vec, idx, mMax);
                }
            }
        }

        /* Update entry point for the next (lower) layer. */
        if (n_sel > 0) ep = sel_ids[0]; /* nearest-first after select */
    }

    free(sel_ids); free(sel_dsts);
    heap_free_data(&W); heap_free_data(&cands);
    free(visited);

    /* Update global entry point. */
    if (level > idx->max_level) {
        idx->max_level   = level;
        idx->entry_point = new_id;
    }

    return new_id;
}

/* ── hnsw_search ─────────────────────────────────────────────────────────── */

int hnsw_search(const HnswIndex *idx,
                const float *query,
                int k, int ef,
                int *out_ids, float *out_dists) {
    if (idx->entry_point < 0 || idx->size == 0) return 0;
    if (ef < k) ef = k;

    char     *visited = (char *)calloc((size_t)idx->size, 1);
    HnswHeap  W, cands;
    heap_init(&W,     ef + HEAP_CAP_MARGIN, 1);
    heap_init(&cands, ef + HEAP_CAP_MARGIN, 0);

    int ep = idx->entry_point;

    /* Greedy descent to layer 1. */
    for (int l = idx->max_level; l > 0; l--) {
        memset(visited, 0, (size_t)idx->size);
        search_layer(idx, query, ep, 1, l, &W, &cands, visited);
        if (W.size > 0) {
            float best_d = W.data[0].dist; int best_id = W.data[0].id;
            for (int i = 1; i < W.size; i++) {
                if (W.data[i].dist < best_d) { best_d = W.data[i].dist; best_id = W.data[i].id; }
            }
            ep = best_id;
        }
    }

    /* Full ef-beam at layer 0. */
    memset(visited, 0, (size_t)idx->size);
    search_layer(idx, query, ep, ef, 0, &W, &cands, visited);

    int n = select_neighbors(&W, k, out_ids, out_dists);

    heap_free_data(&W); heap_free_data(&cands);
    free(visited);
    return n;
}
