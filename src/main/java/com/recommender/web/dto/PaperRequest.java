package com.recommender.web.dto;

import java.util.List;

/**
 * Request body for creating / updating a paper.
 * The embedding field is optional — omit it if the vector is not available yet.
 */
public record PaperRequest(
        String s2Id,
        String arxivId,
        String doi,
        String title,
        String abstractText,
        Integer year,
        List<String> topics,
        List<String> authorNames,
        int citationCount,
        int referenceCount,
        float[] embedding          // optional SPECTER2 vector (768 floats)
) {}
