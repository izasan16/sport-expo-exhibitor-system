package com.sportexpo.exhibitor.users.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RoleDetailResponse(
        UUID id,
        String code,
        String name,
        String roleType,
        boolean enabled,
        String description,
        List<String> permissionCodes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
