package com.sportexpo.exhibitor.eventcategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_categories")
public class EventCategory {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "enabled")
    private boolean enabled;
    @Column(name = "sort_order")
    private int sortOrder;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected EventCategory() {
    }

    public EventCategory(UUID id, UUID eventId, String code, String name, boolean enabled, int sortOrder) {
        var now = OffsetDateTime.now();
        this.id = id;
        this.eventId = eventId;
        this.code = code;
        this.name = name;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String code, String name, boolean enabled, int sortOrder) {
        this.code = code;
        this.name = name;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public int getSortOrder() { return sortOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
