package com.sportexpo.exhibitor.scene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SaveSceneDraftRequest(
        @Min(100) int canvasWidth,
        @Min(100) int canvasHeight,
        @NotNull @DecimalMin("0.0000") BigDecimal pixelToMeterRatio,
        @Valid List<SceneFeaturePayload> features
) {
    public List<SceneFeaturePayload> safeFeatures() {
        return features == null ? List.of() : features;
    }
}
