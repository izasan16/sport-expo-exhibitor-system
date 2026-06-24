package com.sportexpo.exhibitor.scene.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublishedSceneResponse(
        UUID sceneId,
        UUID revisionId,
        String status,
        int canvasWidth,
        int canvasHeight,
        BigDecimal pixelToMeterRatio,
        SceneBackgroundResponse background,
        List<SceneFeaturePayload> features
) {
}
