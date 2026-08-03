package com.sportexpo.exhibitor.users.dto;

import java.util.UUID;

public record RoleSummaryResponse(
        UUID id,
        String code,
        String name,
        String roleType,
        boolean enabled,
        String description
) {
}
