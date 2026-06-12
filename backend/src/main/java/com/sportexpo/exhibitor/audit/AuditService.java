package com.sportexpo.exhibitor.audit;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UUID eventId, UUID actorId, String action, String objectType, UUID objectId,
                       String oldStatus, String newStatus, String reason) {
        repository.save(new AuditLog(
                UUID.randomUUID(),
                eventId,
                actorId,
                action,
                objectType,
                objectId,
                oldStatus,
                newStatus,
                reason,
                "api",
                OffsetDateTime.now()
        ));
    }
}
