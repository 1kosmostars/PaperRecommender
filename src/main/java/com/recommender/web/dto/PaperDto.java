package com.recommender.web.dto;

import com.recommender.db.entity.PaperEntity;

import java.util.List;
import java.util.UUID;

/**
 * API response for a single paper. Embedding is excluded (too large for JSON).
 */
public record PaperDto(
        UUID id,
        String s2Id,
        String arxivId,
        String doi,
        String title,
        String abstractText,
        Integer year,
        int citationCount,
        int referenceCount,
        boolean hasEmbedding,
        List<String> topics,
        List<String> authorNames
) {
    public static PaperDto from(PaperEntity e) {
        return new PaperDto(
                e.getId(),
                e.getS2Id(),
                e.getArxivId(),
                e.getDoi(),
                e.getTitle(),
                e.getAbstractText(),
                e.getYear(),
                e.getCitationCount(),
                e.getReferenceCount(),
                e.getEmbedding() != null,
                List.copyOf(e.getTopics()),
                List.copyOf(e.getAuthorNames())
        );
    }
}
