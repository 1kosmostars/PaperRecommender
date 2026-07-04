package com.recommender.web.dto;

import java.util.UUID;

/**
 * Request body for recording a user–paper interaction.
 * actionType must be one of: VIEW, CLICK, SAVE, DOWNLOAD, CITE
 */
public record InteractionRequest(
        UUID userId,
        UUID paperId,
        String actionType
) {}
