package com.sportexpo.exhibitor.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, String username, Set<String> roles, UUID exhibitorId) {
    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        var normalizedRole = role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        return roles.contains(normalizedRole);
    }
}
