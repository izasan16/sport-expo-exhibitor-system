package com.sportexpo.exhibitor.registration.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationResponse(UUID id, UUID eventId, UUID exhibitorId, String status, String category,
                                   String categoryName, UUID zoneId, BigDecimal intentAreaSqm, String contactName,
                                   String contactPhone, String remark, String reviewComment, UUID reviewedBy,
                                   OffsetDateTime reviewedAt, OffsetDateTime submittedAt, OffsetDateTime createdAt,
                                   OffsetDateTime updatedAt) {
}
