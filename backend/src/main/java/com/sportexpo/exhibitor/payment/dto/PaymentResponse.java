package com.sportexpo.exhibitor.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID eventId, UUID orderId, BigDecimal amount, String status, String method,
                              String failureReason, OffsetDateTime confirmedAt, OffsetDateTime createdAt,
                              OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
}
