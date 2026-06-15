package com.sportexpo.exhibitor.eventcategory.dto;

import java.util.UUID;

public record EventCategoryRequest(UUID eventId, String code, String name, boolean enabled, int sortOrder) {
}
