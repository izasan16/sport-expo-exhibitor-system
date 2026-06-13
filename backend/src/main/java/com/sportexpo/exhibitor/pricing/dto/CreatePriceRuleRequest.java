package com.sportexpo.exhibitor.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePriceRuleRequest(@NotNull UUID eventId, UUID zoneId, @NotBlank String boothType,
                                     @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
                                     boolean enabled) {
}
