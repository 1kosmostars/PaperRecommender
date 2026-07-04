package com.recommender.algorithm;

import com.recommender.algorithm.bm25.Bm25Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bm25IndexTest {

    private Bm25Index idx;

    @BeforeEach
    void setUp() {
        idx = new Bm25Index(1.2, 0.75);
        idx.addDocument(0, List.of("neural", "network", "deep", "learning"));
        idx.addDocument(1, List.of("neural", "network", "graph"));
        idx.addDocument(2, List.of("graph", "paper"));
        idx.addDocument(3, List.of("paper", "deep", "learning"));
        idx.addDocument(4, List.of("deep", "neural", "graph", "network"));
        idx.build();
    }

    @Test
    void searchReturnsResultsForKnownTerm() {
        List<Bm25Index.Result> results = idx.search(List.of("neural"), 5);
        assertFalse(results.isEmpty());
        // docs 0, 1, 4 contain "neural"
        List<Integer> ids = results.stream().map(Bm25Index.Result::docId).toList();
        assertTrue(ids.contains(0));
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(4));
    }

    @Test
    void neuralNetworkQueryRanksCorrectDocs() {
        List<Bm25Index.Result> results = idx.search(List.of("neural", "network"), 3);
        List<Integer> top3 = results.stream().map(Bm25Index.Result::docId).toList();
        // docs 0 and 1 both have both terms; 4 has both too
        assertTrue(top3.contains(0) || top3.contains(1));
    }

    @Test
    void unknownTermReturnsEmpty() {
        List<Bm25Index.Result> results = idx.search(List.of("zzz_unknown"), 5);
        assertTrue(results.isEmpty());
    }

    @Test
    void scoresArePositive() {
        List<Bm25Index.Result> results = idx.search(List.of("graph"), 5);
        results.forEach(r -> assertTrue(r.score() > 0, "score must be positive"));
    }

    @Test
    void topKLimitsResults() {
        List<Bm25Index.Result> results = idx.search(List.of("neural", "network", "graph"), 2);
        assertTrue(results.size() <= 2);
    }

    @Test
    void searchBeforeBuildThrows() {
        Bm25Index unbuilt = new Bm25Index();
        unbuilt.addDocument(0, List.of("a", "b"));
        assertThrows(IllegalStateException.class,
                () -> unbuilt.search(List.of("a"), 5));
    }

    @Test
    void singleDocumentScoreMatchesSearch() {
        double scoreFromSearch = idx.search(List.of("deep"), 5)
                .stream().filter(r -> r.docId() == 0).findFirst()
                .map(Bm25Index.Result::score).orElse(-1.0);

        double scoreFromDirect = idx.scoreDocument(0, List.of("deep"));
        assertEquals(scoreFromSearch, scoreFromDirect, 1e-9);
    }
}
