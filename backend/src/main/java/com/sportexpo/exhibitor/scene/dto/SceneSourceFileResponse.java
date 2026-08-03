package com.sportexpo.exhibitor.scene.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SceneSourceFileResponse(
        UUID sourceFileId,
        String sourceFormat,
        String originalName,
        OffsetDateTime uploadedAt,
        SceneConversionJobResponse job
) {
}
