package com.sportexpo.exhibitor.selection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booth_locks")
public class BoothLock {
    @Id
    @Column(name = "id") private UUID id;
    @Column(name = "event_id") private UUID eventId;
    @Column(name = "order_id") private UUID orderId;
    @Column(name = "selection_id") private UUID selectionId;
    @Column(name = "booth_id") private UUID boothId;
    @Column(name = "status") private String status;
    @Column(name = "lock_expires_at") private OffsetDateTime lockExpiresAt;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    protected BoothLock() {
    }

    public BoothLock(UUID id, UUID eventId, UUID orderId, UUID selectionId, UUID boothId, String status,
                     OffsetDateTime lockExpiresAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                     UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.orderId = orderId;
        this.selectionId = selectionId;
        this.boothId = boothId;
        this.status = status;
        this.lockExpiresAt = lockExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public UUID getSelectionId() { return selectionId; }
    public UUID getBoothId() { return boothId; }
    public String getStatus() { return status; }
    public OffsetDateTime getLockExpiresAt() { return lockExpiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
}
