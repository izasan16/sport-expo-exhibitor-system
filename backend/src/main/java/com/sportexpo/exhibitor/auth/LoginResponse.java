package com.sportexpo.exhibitor.auth;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String displayName,
        UUID exhibitorId,
        List<String> roles
) {
}
