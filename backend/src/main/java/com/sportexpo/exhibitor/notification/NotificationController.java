package com.sportexpo.exhibitor.notification;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.notification.dto.NotificationResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@RequestParam(required = false) UUID recipientId,
                                                        Authentication authentication) {
        var resolvedRecipientId = resolveRecipientId(recipientId, authentication);
        return ApiResponse.ok(service.list(resolvedRecipientId));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId,
                                                      @RequestParam(required = false) UUID recipientId,
                                                      Authentication authentication) {
        var resolvedRecipientId = resolveRecipientId(recipientId, authentication);
        return ApiResponse.ok(service.markRead(notificationId, resolvedRecipientId));
    }

    private UUID resolveRecipientId(UUID requestedRecipientId, Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        if (requestedRecipientId == null) {
            if (currentUser.hasRole("EXHIBITOR")) {
                return currentUserService.requireCurrentExhibitorId(authentication);
            }
            return currentUser.userId();
        }
        if (currentUser.hasRole("SYSTEM_ADMIN") || currentUser.hasRole("ORGANIZER_ADMIN")
                || requestedRecipientId.equals(currentUser.userId())
                || requestedRecipientId.equals(currentUser.exhibitorId())) {
            return requestedRecipientId;
        }
        throw new AccessDeniedException("Access denied for notification recipient");
    }
}
