package com.recommender.web;

import com.recommender.service.IndexService;
import com.recommender.service.IngestService;
import com.recommender.web.dto.IngestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for ingesting papers from external APIs.
 * Automatically rebuilds in-memory indexes after each ingest.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final IngestService ingestService;
    private final IndexService  indexService;

    public IngestController(IngestService ingestService, IndexService indexService) {
        this.ingestService = ingestService;
        this.indexService  = indexService;
    }

    /**
     * Search Semantic Scholar and save matching papers (with SPECTER2 embeddings).
     *
     * @param query full-text search query
     * @param limit number of papers (max 100 per request)
     */
    @PostMapping("/s2")
    public ResponseEntity<IngestResult> ingestFromS2(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {

        if (query == null || query.isBlank()) return ResponseEntity.badRequest().build();
        limit = Math.max(1, Math.min(limit, 100));
        IngestResult result = ingestService.ingestFromSemanticScholar(query, limit);
        indexService.reindex();
        return ResponseEntity.ok(result);
    }

    /**
     * Fetch one paper from S2 by its paperId (or "ARXIV:...", "DOI:..."),
     * plus up to {@code maxRefs} of its references.
     * Citation edges are written to the DB so PageRank can use them.
     *
     * @param paperId Semantic Scholar paperId or qualified id
     * @param maxRefs number of reference papers to also ingest (default 50)
     */
    @PostMapping("/s2/{paperId}")
    public ResponseEntity<IngestResult> ingestPaperDeep(
            @PathVariable String paperId,
            @RequestParam(defaultValue = "50") int maxRefs) {

        maxRefs = Math.max(0, Math.min(maxRefs, 500));
        IngestResult result = ingestService.ingestPaperDeep(paperId, maxRefs);
        indexService.reindex();
        return ResponseEntity.ok(result);
    }

    /**
     * Search arXiv and save matching papers (no embeddings; add them later via S2 batch).
     *
     * @param query      arXiv query string (e.g. "ti:BERT AND cat:cs.CL")
     * @param maxResults number of results (max 200 recommended to avoid timeouts)
     */
    @PostMapping("/arxiv")
    public ResponseEntity<IngestResult> ingestFromArxiv(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int maxResults) {

        if (query == null || query.isBlank()) return ResponseEntity.badRequest().build();
        maxResults = Math.max(1, Math.min(maxResults, 200));
        IngestResult result = ingestService.ingestFromArxiv(query, maxResults);
        indexService.reindex();
        return ResponseEntity.ok(result);
    }
}
