#ifndef PAGERANK_H
#define PAGERANK_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Compute PageRank via power iteration.
 *
 * @param citing      edge sources  (citing_node_idx), length num_edges
 * @param cited       edge targets  (cited_node_idx),  length num_edges
 * @param num_edges   number of directed edges
 * @param num_nodes   total number of nodes
 * @param damping     damping factor (typically 0.85)
 * @param iterations  number of power-iteration steps
 * @param scores_out  output buffer, length num_nodes — must be pre-allocated by caller
 */
void pagerank_compute(
    const int* citing,
    const int* cited,
    int num_edges,
    int num_nodes,
    double damping,
    int iterations,
    double* scores_out
);

#ifdef __cplusplus
}
#endif
#endif /* PAGERANK_H */
