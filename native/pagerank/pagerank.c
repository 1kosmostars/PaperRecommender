#include "pagerank.h"
#include <stdlib.h>
#include <string.h>

void pagerank_compute(
    const int* citing,
    const int* cited,
    int num_edges,
    int num_nodes,
    double damping,
    int iterations,
    double* scores_out)
{
    if (num_nodes <= 0) return;

    double* curr = scores_out;
    double* next = (double*)malloc((size_t)num_nodes * sizeof(double));
    int*    out_deg = (int*)calloc((size_t)num_nodes, sizeof(int));

    if (!next || !out_deg) { free(next); free(out_deg); return; }

    /* Count out-degrees */
    for (int e = 0; e < num_edges; e++) {
        int s = citing[e];
        if (s >= 0 && s < num_nodes) out_deg[s]++;
    }

    /* Uniform initialisation */
    double inv_n = 1.0 / num_nodes;
    for (int i = 0; i < num_nodes; i++) curr[i] = inv_n;

    for (int it = 0; it < iterations; it++) {
        memset(next, 0, (size_t)num_nodes * sizeof(double));

        /* Collect dangling mass (nodes with no out-edges) */
        double dangling = 0.0;
        for (int i = 0; i < num_nodes; i++)
            if (out_deg[i] == 0) dangling += curr[i];

        /* Propagate through edges */
        for (int e = 0; e < num_edges; e++) {
            int s = citing[e], d = cited[e];
            if (s >= 0 && s < num_nodes &&
                d >= 0 && d < num_nodes &&
                out_deg[s] > 0) {
                next[d] += damping * curr[s] / out_deg[s];
            }
        }

        /* Teleportation + re-distribute dangling mass uniformly */
        double base = ((1.0 - damping) + damping * dangling) * inv_n;
        for (int i = 0; i < num_nodes; i++)
            next[i] += base;

        /* Swap buffers */
        memcpy(curr, next, (size_t)num_nodes * sizeof(double));
    }

    free(next);
    free(out_deg);
    /* Result is already in scores_out (== curr) */
}
