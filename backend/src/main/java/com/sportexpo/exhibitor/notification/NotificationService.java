package com.sportexpo.exhibitor.notification;

import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.notification.dto.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationResponse create(UUID eventId, String recipientType, UUID recipientId, String title,
                                       String content, String type) {
        var now = OffsetDateTime.now();
        var notification = repository.save(new Notification(
                UUID.randomUUID(),
                eventId,
                recipientType,
                recipientId,
                title,
                content,
                type,
                null,
                now
        ));
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UUID recipientId) {
        var notification = repository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new BusinessException("Notification does not exist"));
        if (notification.getReadAt() == null) {
            notification.markRead(OffsetDateTime.now());
            repository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public int cleanupReadNotificationsOlderThan(OffsetDateTime cutoff) {
        return repository.deleteReadBefore(cutoff);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getRecipientType(),
                notification.getRecipientId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
