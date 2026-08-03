package com.sportexpo.exhibitor.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String username,
        List<String> roles,
        UUID exhibitorId,
        int jwtVersion,
        String tokenId,
        Instant issuedAt,
        Instant expiresAt
) {
}
