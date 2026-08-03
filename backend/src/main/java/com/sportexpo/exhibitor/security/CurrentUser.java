package com.sportexpo.exhibitor.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String username,
        String displayName,
        Set<String> roles,
        String defaultRole,
        UUID exhibitorId,
        Set<String> permissions
) {
    public CurrentUser {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        var normalizedRole = role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        return roles.contains(normalizedRole);
    }
}
