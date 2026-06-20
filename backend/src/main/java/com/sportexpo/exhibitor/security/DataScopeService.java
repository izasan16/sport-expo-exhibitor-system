package com.sportexpo.exhibitor.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class DataScopeService {
    public static final String SALES_ACCESSIBLE_EXHIBITOR_IDS_CACHE = "sales:accessible_exhibitor_ids";

    private final CurrentUserService currentUserService;
    private final SalesDataScopeService salesDataScopeService;

    public DataScopeService(CurrentUserService currentUserService, SalesDataScopeService salesDataScopeService) {
        this.currentUserService = currentUserService;
        this.salesDataScopeService = salesDataScopeService;
    }

    public boolean canAccessExhibitor(Authentication authentication, UUID exhibitorId) {
        if (exhibitorId == null) {
            return false;
        }
        var currentUser = currentUserService.getCurrentUser(authentication);
        if (isAdmin(currentUser)) {
            return true;
        }
        if (currentUser.hasRole("EXHIBITOR")) {
            return exhibitorId.equals(currentUser.exhibitorId());
        }
        if (currentUser.hasRole("SALES")) {
            return salesDataScopeService.accessibleExhibitorIds(currentUser.userId()).contains(exhibitorId);
        }
        return false;
    }

    public void requireCanAccessExhibitor(Authentication authentication, UUID exhibitorId) {
        if (!canAccessExhibitor(authentication, exhibitorId)) {
            throw new AccessDeniedException("Access denied for exhibitor");
        }
    }

    public Set<UUID> accessibleExhibitorIds(Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        if (isAdmin(currentUser)) {
            return Set.of();
        }
        if (currentUser.hasRole("EXHIBITOR")) {
            return currentUser.exhibitorId() == null ? Set.of() : Set.of(currentUser.exhibitorId());
        }
        if (currentUser.hasRole("SALES")) {
            return salesDataScopeService.accessibleExhibitorIds(currentUser.userId());
        }
        return Set.of();
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser.hasRole("SYSTEM_ADMIN") || currentUser.hasRole("ORGANIZER_ADMIN");
    }
}
