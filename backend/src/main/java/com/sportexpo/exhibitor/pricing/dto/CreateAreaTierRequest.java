package com.sportexpo.exhibitor.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAreaTierRequest(@NotNull UUID eventId, UUID zoneId,
                                    @NotNull @DecimalMin("0.01") BigDecimal areaSqm,
                                    boolean enabled) {
}
