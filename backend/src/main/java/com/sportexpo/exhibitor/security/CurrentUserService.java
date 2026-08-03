package com.sportexpo.exhibitor.security;

import com.sportexpo.exhibitor.auth.AuthErrorCodes;
import com.sportexpo.exhibitor.auth.AuthException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {
    public CurrentUser getCurrentUser() {
        return getCurrentUser(SecurityContextHolder.getContext().getAuthentication());
    }

    public CurrentUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token");
        }
        return currentUser;
    }

    public UUID requireCurrentExhibitorId() {
        return requireCurrentExhibitorId(SecurityContextHolder.getContext().getAuthentication());
    }

    public UUID requireCurrentExhibitorId(Authentication authentication) {
        var currentUser = getCurrentUser(authentication);
        if (currentUser.exhibitorId() == null) {
            throw new AccessDeniedException("Current user has no exhibitor scope");
        }
        return currentUser.exhibitorId();
    }
}
