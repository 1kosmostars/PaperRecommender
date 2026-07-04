package com.recommender.web.dto;

import java.util.List;

/**
 * Result of a single ingest operation.
 */
public record IngestResult(
        int saved,
        int skipped,
        List<String> errors
) {
    public static IngestResult of(int saved, int skipped, List<String> errors) {
        return new IngestResult(saved, skipped, errors);
    }
}
