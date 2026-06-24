package com.sportexpo.exhibitor.scene.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SceneConversionJobResponse(
        UUID id,
        UUID sceneId,
        UUID sceneRevisionId,
        UUID sourceFileId,
        String jobStatus,
        String targetFormat,
        UUID resultFileId,
        String errorCode,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime createdAt
) {
}
