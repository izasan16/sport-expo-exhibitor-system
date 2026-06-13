package com.sportexpo.exhibitor.contract;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "current_version_id")
    private UUID currentVersionId;
    @Column(name = "contract_no")
    private String contractNo;
    @Column(name = "status")
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Contract() {
    }

    public Contract(UUID id, UUID eventId, UUID orderId, UUID currentVersionId, String contractNo, String status,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.orderId = orderId;
        this.currentVersionId = currentVersionId;
        this.contractNo = contractNo;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public String getContractNo() { return contractNo; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void assignCurrentVersion(UUID currentVersionId, OffsetDateTime now, UUID actorId) {
        this.currentVersionId = currentVersionId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void changeStatus(String status, OffsetDateTime now, UUID actorId) {
        this.status = status;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
