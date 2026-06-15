package com.sportexpo.exhibitor.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID eventId, String recipientType, UUID recipientId, String title,
                                   String content, String type, String relatedType, UUID relatedId,
                                   UUID relatedOrderId, OffsetDateTime readAt, OffsetDateTime createdAt) {
}
