package com.sportexpo.exhibitor.booth.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BoothResponse(UUID id, UUID eventId, String boothNo, BigDecimal areaSqm, String boothType, String status) {
}
