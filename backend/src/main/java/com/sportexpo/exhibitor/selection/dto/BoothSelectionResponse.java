package com.sportexpo.exhibitor.selection.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BoothSelectionResponse(UUID id, UUID eventId, UUID orderId, UUID exhibitorId, String status,
                                     BigDecimal totalAreaSqm, OffsetDateTime lockStartedAt,
                                     OffsetDateTime lockExpiresAt, UUID helperUserId, UUID reviewerUserId,
                                     OffsetDateTime reviewedAt, String reviewComment, List<UUID> boothIds,
                                     OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
                                     UUID updatedBy) {
}
