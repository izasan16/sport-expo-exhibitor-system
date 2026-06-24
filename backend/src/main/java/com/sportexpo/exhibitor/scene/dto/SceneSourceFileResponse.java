package com.sportexpo.exhibitor.scene.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SceneSourceFileResponse(
        UUID id,
        UUID sceneId,
        UUID sceneRevisionId,
        UUID storedFileId,
        String sourceFormat,
        String originalName,
        long fileSize,
        String uploadStatus,
        OffsetDateTime createdAt,
        UUID createdBy,
        SceneConversionJobResponse job
) {
}
