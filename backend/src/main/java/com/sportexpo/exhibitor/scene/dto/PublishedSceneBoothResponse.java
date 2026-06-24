package com.sportexpo.exhibitor.scene.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PublishedSceneBoothResponse(
        UUID boothId,
        String boothNo,
        String boothType,
        String status,
        BigDecimal areaSqm,
        Object geometry
) {
}
