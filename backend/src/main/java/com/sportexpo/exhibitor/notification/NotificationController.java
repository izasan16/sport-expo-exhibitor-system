package com.sportexpo.exhibitor.notification;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.notification.dto.NotificationResponse;
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

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@RequestParam(required = false) UUID recipientId) {
        var resolvedRecipientId = recipientId == null ? DemoActors.DEMO_EXHIBITOR_ID : recipientId;
        return ApiResponse.ok(service.list(resolvedRecipientId));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId,
                                                      @RequestParam(required = false) UUID recipientId) {
        var resolvedRecipientId = recipientId == null ? DemoActors.DEMO_EXHIBITOR_ID : recipientId;
        return ApiResponse.ok(service.markRead(notificationId, resolvedRecipientId));
    }
}
