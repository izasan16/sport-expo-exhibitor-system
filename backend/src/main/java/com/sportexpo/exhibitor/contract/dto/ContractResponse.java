package com.sportexpo.exhibitor.contract.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID eventId,
        UUID orderId,
        UUID currentVersionId,
        String contractNo,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
}
