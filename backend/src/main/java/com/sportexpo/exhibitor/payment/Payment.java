package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.common.BusinessStatuses;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "status")
    private String status;
    @Column(name = "method")
    private String method;
    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Payment() {
    }

    public Payment(UUID id, UUID eventId, UUID orderId, BigDecimal amount, String status, String method,
                   OffsetDateTime confirmedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                   UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMethod() { return method; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void markConfirmed(UUID actorId, OffsetDateTime now) {
        this.status = BusinessStatuses.PAYMENT_CONFIRMED;
        this.confirmedAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markFailed(UUID actorId, OffsetDateTime now) {
        this.status = BusinessStatuses.PAYMENT_CONFIRM_FAILED;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
