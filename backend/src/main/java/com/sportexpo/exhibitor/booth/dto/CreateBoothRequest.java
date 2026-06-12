package com.sportexpo.exhibitor.booth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBoothRequest(
        @NotNull UUID eventId,
        @NotBlank String boothNo,
        @NotNull @Positive BigDecimal areaSqm,
        @NotBlank String boothType
) {
}
