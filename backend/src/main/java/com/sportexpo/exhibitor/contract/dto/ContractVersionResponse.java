package com.sportexpo.exhibitor.contract.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ContractVersionResponse(
        UUID id,
        UUID eventId,
        UUID contractId,
        int versionNo,
        String reason,
        Map<String, Object> contentSnapshot,
        BigDecimal amount,
        BigDecimal areaSqm,
        String signStatus,
        UUID signerId,
        OffsetDateTime signedAt,
        UUID fileId,
        OffsetDateTime createdAt,
        UUID createdBy
) {
}
