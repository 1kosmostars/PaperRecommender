package com.recommender.algorithm.bm25;

import java.util.*;

/**
 * Pure-Java BM25 (Okapi BM25) index for text retrieval.
 *
 * <p>Score for document d given query Q:
 * <pre>
 *   BM25(d, Q) = Σ_{t ∈ Q}  IDF(t) · tf(t,d)·(k1+1) / (tf(t,d) + k1·(1 − b + b·|d|/avgdl))
 * </pre>
 * IDF (Robertson–Sparck Jones):
 * <pre>
 *   IDF(t) = ln((N − df(t) + 0.5) / (df(t) + 0.5) + 1)
 * </pre>
 *
 * <p>Usage:
 * <pre>{@code
 *   Bm25Index idx = new Bm25Index(1.2, 0.75);
 *   idx.addDocument(0, List.of("neural", "network", "deep", "learning"));
 *   idx.addDocument(1, List.of("graph", "neural", "network"));
 *   idx.build();
 *
 *   List<Bm25Index.Result> top = idx.search(List.of("neural", "network"), 5);
 * }</pre>
 */
public class Bm25Index {

    /** BM25 result: document id + score. */
    public record Result(int docId, double score) {}

    // ── defaults ──────────────────────────────────────────────────────────────
    private static final double DEFAULT_K1     = 1.2;   // TF saturation (typical range 1.2–2.0)
    private static final double DEFAULT_B      = 0.75;  // length normalisation
    private static final double IDF_SMOOTHING  = 0.5;   // Robertson–Sparck Jones smoothing term

    // ── config ────────────────────────────────────────────────────────────────
    private final double k1;
    private final double b;

    // ── forward index ─────────────────────────────────────────────────────────
    /** docId → list of term strings (with repetitions, preserving length). */
    private final List<List<String>> documents = new ArrayList<>();

    // ── inverted index (built on demand) ──────────────────────────────────────
    /** term → list of [docId, rawTf] pairs. */
    private Map<String, List<int[]>> invertedIndex;
    /** term → IDF score. */
    private Map<String, Double> idf;
    /** Per-document length (token count). */
    private int[] docLengths;
    private double avgdl;
    private boolean built = false;

    /**
     * @param k1 TF saturation coefficient (default 1.2)
     * @param b  length normalisation coefficient (default 0.75)
     */
    public Bm25Index(double k1, double b) {
        this.k1 = k1;
        this.b  = b;
    }

    /** Default parameters (k1=1.2, b=0.75). */
    public Bm25Index() {
        this(DEFAULT_K1, DEFAULT_B);
    }

    // ── indexing ──────────────────────────────────────────────────────────────

    /**
     * Add a document to the index.
     *
     * @param docId  external document id (must equal the current size, i.e. 0, 1, 2, …)
     * @param tokens pre-tokenised and normalised list of terms (may contain duplicates)
     */
    public void addDocument(int docId, List<String> tokens) {
        if (docId != documents.size())
            throw new IllegalArgumentException("docId must be " + documents.size() + " (sequential)");
        documents.add(new ArrayList<>(tokens));
        built = false;
    }

    /**
     * Build (or rebuild) the inverted index and IDF scores.
     * Must be called after all documents have been added and before {@link #search}.
     */
    public void build() {
        int N = documents.size();
        docLengths = new int[N];
        long totalLen = 0;

        // Count TF per document
        Map<String, Map<Integer, Integer>> tf = new HashMap<>();
        for (int d = 0; d < N; d++) {
            List<String> doc = documents.get(d);
            docLengths[d] = doc.size();
            totalLen += doc.size();
            for (String term : doc) {
                tf.computeIfAbsent(term, k -> new HashMap<>())
                  .merge(d, 1, Integer::sum);
            }
        }
        avgdl = (double) totalLen / N;

        // Build inverted index and compute IDF
        invertedIndex = new HashMap<>(tf.size() * 2);
        idf = new HashMap<>(tf.size() * 2);

        for (Map.Entry<String, Map<Integer, Integer>> termEntry : tf.entrySet()) {
            String term = termEntry.getKey();
            Map<Integer, Integer> docTf = termEntry.getValue();

            int df = docTf.size();
            double idfScore = Math.log((N - df + IDF_SMOOTHING) / (df + IDF_SMOOTHING) + 1.0);
            idf.put(term, idfScore);

            List<int[]> postings = new ArrayList<>(df);
            for (Map.Entry<Integer, Integer> e : docTf.entrySet()) {
                postings.add(new int[]{e.getKey(), e.getValue()});
            }
            invertedIndex.put(term, postings);
        }

        built = true;
    }

    // ── search ────────────────────────────────────────────────────────────────

    /**
     * Score all documents against the query and return top-k results.
     *
     * @param queryTerms pre-tokenised query terms
     * @param k          number of results (0 = all)
     * @return results sorted by score descending
     */
    public List<Result> search(List<String> queryTerms, int k) {
        if (!built) throw new IllegalStateException("Call build() first");

        Map<Integer, Double> scores = new HashMap<>();

        for (String term : queryTerms) {
            List<int[]> postings = invertedIndex.get(term);
            if (postings != null) {
                double idfScore = idf.get(term);
                for (int[] posting : postings) {
                    int docId = posting[0];
                    double freq = posting[1];
                    double dl = docLengths[docId];
                    double tfNorm = freq * (k1 + 1.0)
                            / (freq + k1 * (1.0 - b + b * dl / avgdl));
                    scores.merge(docId, idfScore * tfNorm, Double::sum);
                }
            }
        }

        List<Result> results = new ArrayList<>(scores.size());
        for (Map.Entry<Integer, Double> e : scores.entrySet()) {
            results.add(new Result(e.getKey(), e.getValue()));
        }
        results.sort(Comparator.comparingDouble(Result::score).reversed());

        return (k > 0 && k < results.size()) ? results.subList(0, k) : results;
    }

    /** Score a single document against query terms (requires {@link #build()} to have been called). */
    public double scoreDocument(int docId, List<String> queryTerms) {
        if (!built) throw new IllegalStateException("Call build() first");

        double score = 0.0;
        double dl = docLengths[docId];

        for (String term : queryTerms) {
            List<int[]> postings = invertedIndex.get(term);
            if (postings != null) {
                int freq = postings.stream()
                        .filter(p -> p[0] == docId)
                        .mapToInt(p -> p[1])
                        .findFirst()
                        .orElse(0);
                if (freq > 0) {
                    double idfScore = idf.get(term);
                    score += idfScore * freq * (k1 + 1.0)
                            / (freq + k1 * (1.0 - b + b * dl / avgdl));
                }
            }
        }
        return score;
    }

    // ── accessors ─────────────────────────────────────────────────────────────

    public int size()    { return documents.size(); }
    public double getAvgdl() { ensureBuilt(); return avgdl; }
    public double getIdf(String term) { ensureBuilt(); return idf.getOrDefault(term, 0.0); }

    private void ensureBuilt() {
        if (!built) throw new IllegalStateException("Call build() first");
    }
}
