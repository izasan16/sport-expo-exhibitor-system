package com.sportexpo.exhibitor.auth;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String username,
        String displayName,
        List<String> roles,
        String defaultRole,
        List<String> activeAvailableRoles,
        UUID exhibitorId,
        List<String> permissions
) {
}
