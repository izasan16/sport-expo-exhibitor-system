package com.sportexpo.exhibitor.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateParticipationOrderRequest(
        @NotNull UUID eventId,
        @NotNull UUID exhibitorId,
        UUID registrationId,
        UUID zoneId,
        @NotNull UUID areaTierId,
        @NotBlank String boothType
) {
}
