package com.sportexpo.exhibitor.users.dto;

import java.util.List;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String phone,
        String email,
        String userType,
        UUID exhibitorId,
        String status,
        String defaultRole,
        List<String> roleCodes
) {
}
