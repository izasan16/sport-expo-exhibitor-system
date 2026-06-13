package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.common.BusinessStatuses;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "selection_eligibilities")
public class SelectionEligibility {
    @Id
    @Column(name = "id") private UUID id;
    @Column(name = "event_id") private UUID eventId;
    @Column(name = "order_id") private UUID orderId;
    @Column(name = "status") private String status;
    @Column(name = "pause_reason") private String pauseReason;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    protected SelectionEligibility() {
    }

    public SelectionEligibility(UUID id, UUID eventId, UUID orderId, String status, String pauseReason,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.orderId = orderId;
        this.status = status;
        this.pauseReason = pauseReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getPauseReason() { return pauseReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void markAvailable(OffsetDateTime now) {
        this.status = BusinessStatuses.ELIGIBILITY_AVAILABLE;
        this.pauseReason = null;
        this.updatedAt = now;
    }

    public void markPaused(String pauseReason, OffsetDateTime now) {
        this.status = BusinessStatuses.ELIGIBILITY_PAUSED;
        this.pauseReason = pauseReason;
        this.updatedAt = now;
    }

    public void markClosed(OffsetDateTime now) {
        this.status = BusinessStatuses.ELIGIBILITY_CLOSED;
        this.pauseReason = null;
        this.updatedAt = now;
    }

    public void markNotOpen(OffsetDateTime now) {
        this.status = BusinessStatuses.ELIGIBILITY_NOT_OPEN;
        this.pauseReason = null;
        this.updatedAt = now;
    }
}
