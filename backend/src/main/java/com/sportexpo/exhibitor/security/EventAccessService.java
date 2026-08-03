package com.sportexpo.exhibitor.security;

import com.sportexpo.exhibitor.registration.RegistrationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("eventAccessService")
public class EventAccessService {
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;
    private final RegistrationRepository registrationRepository;

    public EventAccessService(CurrentUserService currentUserService,
                              DataScopeService dataScopeService,
                              RegistrationRepository registrationRepository) {
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
        this.registrationRepository = registrationRepository;
    }

    public boolean canAccessEvent(Authentication authentication, UUID eventId) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        if (currentUser.hasRole("SYSTEM_ADMIN") || currentUser.hasRole("ORGANIZER_ADMIN")) {
            return true;
        }
        if (currentUser.hasRole("EXHIBITOR")) {
            return currentUser.exhibitorId() != null
                    && registrationRepository.existsByEventIdAndExhibitorId(eventId, currentUser.exhibitorId());
        }
        if (currentUser.hasRole("SALES")) {
            return dataScopeService.accessibleExhibitorIds(authentication).stream()
                    .anyMatch(exhibitorId -> registrationRepository.existsByEventIdAndExhibitorId(eventId, exhibitorId));
        }
        return false;
    }

    public boolean canManageEvent(Authentication authentication, UUID eventId) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        return currentUser.hasRole("SYSTEM_ADMIN") || currentUser.hasRole("ORGANIZER_ADMIN");
    }
}
