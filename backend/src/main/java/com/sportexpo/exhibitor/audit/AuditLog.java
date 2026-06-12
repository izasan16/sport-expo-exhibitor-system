package com.sportexpo.exhibitor.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "action")
    private String action;
    @Column(name = "object_type")
    private String objectType;
    @Column(name = "object_id")
    private UUID objectId;
    @Column(name = "old_status")
    private String oldStatus;
    @Column(name = "new_status")
    private String newStatus;
    @Column(name = "reason")
    private String reason;
    @Column(name = "request_source")
    private String requestSource;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID id, UUID eventId, UUID actorId, String action, String objectType, UUID objectId,
                    String oldStatus, String newStatus, String reason, String requestSource, OffsetDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.actorId = actorId;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.requestSource = requestSource;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getObjectType() { return objectType; }
    public UUID getObjectId() { return objectId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public String getRequestSource() { return requestSource; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
