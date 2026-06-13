package com.sportexpo.exhibitor.pricing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AreaTierResponse(UUID id, UUID eventId, UUID zoneId, BigDecimal areaSqm, boolean enabled) {
}
