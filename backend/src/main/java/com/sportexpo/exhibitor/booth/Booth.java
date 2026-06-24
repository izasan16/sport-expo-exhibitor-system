package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.common.BusinessStatuses;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booths")
public class Booth {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "pavilion_id")
    private UUID pavilionId;
    @Column(name = "zone_id")
    private UUID zoneId;
    @Column(name = "booth_no")
    private String boothNo;
    @Column(name = "area_sqm")
    private BigDecimal areaSqm;
    @Column(name = "booth_type")
    private String boothType;
    @Column(name = "status")
    private String status;
    @Column(name = "x")
    private BigDecimal x;
    @Column(name = "y")
    private BigDecimal y;
    @Column(name = "width")
    private BigDecimal width;
    @Column(name = "height")
    private BigDecimal height;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Booth() {
    }

    public Booth(UUID id, UUID eventId, String boothNo, BigDecimal areaSqm, String boothType,
                 String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.boothNo = boothNo;
        this.areaSqm = areaSqm;
        this.boothType = boothType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Booth(UUID id, UUID eventId, UUID pavilionId, UUID zoneId, String boothNo, BigDecimal areaSqm,
                 String boothType, String status, BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height,
                 OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.pavilionId = pavilionId;
        this.zoneId = zoneId;
        this.boothNo = boothNo;
        this.areaSqm = areaSqm;
        this.boothType = boothType;
        this.status = status;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getPavilionId() { return pavilionId; }
    public UUID getZoneId() { return zoneId; }
    public String getBoothNo() { return boothNo; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getBoothType() { return boothType; }
    public String getStatus() { return status; }
    public BigDecimal getX() { return x; }
    public BigDecimal getY() { return y; }
    public BigDecimal getWidth() { return width; }
    public BigDecimal getHeight() { return height; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void applySceneSnapshot(BigDecimal areaSqm, String boothType, BigDecimal x, BigDecimal y,
                                   BigDecimal width, BigDecimal height, UUID actorId, OffsetDateTime now) {
        this.areaSqm = areaSqm;
        this.boothType = boothType;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markLocked(OffsetDateTime now, UUID actorId) {
        this.status = BusinessStatuses.BOOTH_LOCKED;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markAvailable(OffsetDateTime now, UUID actorId) {
        this.status = BusinessStatuses.BOOTH_AVAILABLE;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markConfirmed(OffsetDateTime now, UUID actorId) {
        this.status = BusinessStatuses.BOOTH_CONFIRMED;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
