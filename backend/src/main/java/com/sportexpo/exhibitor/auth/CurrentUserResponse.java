package com.sportexpo.exhibitor.auth;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String username,
        List<String> roles,
        UUID exhibitorId
) {
}
