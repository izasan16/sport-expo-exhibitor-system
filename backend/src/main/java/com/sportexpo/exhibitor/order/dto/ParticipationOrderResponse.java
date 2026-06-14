package com.sportexpo.exhibitor.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ParticipationOrderResponse(
        UUID id,
        UUID eventId,
        UUID exhibitorId,
        UUID zoneId,
        UUID areaTierId,
        BigDecimal areaSqm,
        String boothType,
        String status,
        BigDecimal areaFee,
        BigDecimal serviceFee,
        BigDecimal discountAmount,
        BigDecimal adjustmentAmount,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
