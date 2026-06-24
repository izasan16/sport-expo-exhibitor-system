package com.sportexpo.exhibitor.scene.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SceneBackgroundResponse(
        UUID fileId,
        String fileName,
        String mimeType,
        String url,
        Integer width,
        Integer height,
        BigDecimal offsetX,
        BigDecimal offsetY,
        BigDecimal scale,
        BigDecimal opacity,
        boolean locked
) {
}
