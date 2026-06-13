package com.sportexpo.exhibitor.pricing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceRuleResponse(UUID id, UUID eventId, UUID zoneId, String boothType, BigDecimal unitPrice,
                                boolean enabled) {
}
