package com.sportexpo.exhibitor.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participation_orders")
public class ParticipationOrder {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "exhibitor_id")
    private UUID exhibitorId;
    @Column(name = "registration_id")
    private UUID registrationId;
    @Column(name = "zone_id")
    private UUID zoneId;
    @Column(name = "area_tier_id")
    private UUID areaTierId;
    @Column(name = "area_sqm")
    private BigDecimal areaSqm;
    @Column(name = "booth_type")
    private String boothType;
    @Column(name = "status")
    private String status;
    @Column(name = "area_fee")
    private BigDecimal areaFee;
    @Column(name = "service_fee")
    private BigDecimal serviceFee;
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;
    @Column(name = "adjustment_amount")
    private BigDecimal adjustmentAmount;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ParticipationOrder() {
    }

    public ParticipationOrder(UUID id, UUID eventId, UUID exhibitorId, UUID registrationId, UUID zoneId,
                              UUID areaTierId, BigDecimal areaSqm, String boothType, String status,
                              BigDecimal areaFee, BigDecimal serviceFee, BigDecimal discountAmount,
                              BigDecimal adjustmentAmount, BigDecimal totalAmount, OffsetDateTime createdAt,
                              OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.exhibitorId = exhibitorId;
        this.registrationId = registrationId;
        this.zoneId = zoneId;
        this.areaTierId = areaTierId;
        this.areaSqm = areaSqm;
        this.boothType = boothType;
        this.status = status;
        this.areaFee = areaFee;
        this.serviceFee = serviceFee;
        this.discountAmount = discountAmount;
        this.adjustmentAmount = adjustmentAmount;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getExhibitorId() { return exhibitorId; }
    public UUID getRegistrationId() { return registrationId; }
    public UUID getZoneId() { return zoneId; }
    public UUID getAreaTierId() { return areaTierId; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getBoothType() { return boothType; }
    public String getStatus() { return status; }
    public BigDecimal getAreaFee() { return areaFee; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void changeStatus(String status, OffsetDateTime now, UUID actorId) {
        this.status = status;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
