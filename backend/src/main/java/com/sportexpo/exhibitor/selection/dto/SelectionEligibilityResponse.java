package com.sportexpo.exhibitor.selection.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SelectionEligibilityResponse(UUID id, UUID eventId, UUID orderId, String status, String pauseReason,
                                           OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
                                           UUID updatedBy) {
}
