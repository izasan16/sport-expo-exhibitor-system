package com.sportexpo.exhibitor.scene.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SceneRevisionResponse(
        UUID revisionId,
        int revisionNo,
        String status,
        String changeSummary,
        OffsetDateTime publishedAt,
        UUID publishedBy,
        OffsetDateTime createdAt
) {
}
