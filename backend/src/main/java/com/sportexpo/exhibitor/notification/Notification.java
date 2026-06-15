package com.sportexpo.exhibitor.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @Column(name = "id") private UUID id;
    @Column(name = "event_id") private UUID eventId;
    @Column(name = "recipient_type") private String recipientType;
    @Column(name = "recipient_id") private UUID recipientId;
    @Column(name = "title") private String title;
    @Column(name = "content") private String content;
    @Column(name = "type") private String type;
    @Column(name = "related_type") private String relatedType;
    @Column(name = "related_id") private UUID relatedId;
    @Column(name = "related_order_id") private UUID relatedOrderId;
    @Column(name = "read_at") private OffsetDateTime readAt;
    @Column(name = "created_at") private OffsetDateTime createdAt;

    protected Notification() {
    }

    public Notification(UUID id, UUID eventId, String recipientType, UUID recipientId, String title,
                        String content, String type, OffsetDateTime readAt, OffsetDateTime createdAt) {
        this(id, eventId, recipientType, recipientId, title, content, type, null, null, null, readAt, createdAt);
    }

    public Notification(UUID id, UUID eventId, String recipientType, UUID recipientId, String title,
                        String content, String type, String relatedType, UUID relatedId, UUID relatedOrderId,
                        OffsetDateTime readAt, OffsetDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
        this.relatedOrderId = relatedOrderId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getRecipientType() { return recipientType; }
    public UUID getRecipientId() { return recipientId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public String getRelatedType() { return relatedType; }
    public UUID getRelatedId() { return relatedId; }
    public UUID getRelatedOrderId() { return relatedOrderId; }
    public OffsetDateTime getReadAt() { return readAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void markRead(OffsetDateTime now) {
        this.readAt = now;
    }
}
