package com.sportexpo.exhibitor.sales;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SalesFollowUpResponse(
        UUID id,
        UUID exhibitorId,
        UUID salesUserId,
        String method,
        String content,
        OffsetDateTime nextFollowUpAt,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
