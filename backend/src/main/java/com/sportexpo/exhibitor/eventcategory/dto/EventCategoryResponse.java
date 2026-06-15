package com.sportexpo.exhibitor.eventcategory.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventCategoryResponse(UUID id, UUID eventId, String code, String name, boolean enabled,
                                    int sortOrder, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
