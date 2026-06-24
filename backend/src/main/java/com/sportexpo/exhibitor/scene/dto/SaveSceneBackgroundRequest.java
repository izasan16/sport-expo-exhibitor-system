package com.sportexpo.exhibitor.scene.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SaveSceneBackgroundRequest(
        @NotNull UUID fileId,
        @NotBlank String fileName,
        @NotBlank String mimeType,
        BigDecimal offsetX,
        BigDecimal offsetY,
        @NotNull @DecimalMin("0.0001") BigDecimal scale,
        @NotNull @DecimalMin("0.0000") BigDecimal opacity,
        boolean locked
) {
}
