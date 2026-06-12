package com.sportexpo.exhibitor.booth;

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
    @Column(name = "booth_no")
    private String boothNo;
    @Column(name = "area_sqm")
    private BigDecimal areaSqm;
    @Column(name = "booth_type")
    private String boothType;
    @Column(name = "status")
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getBoothNo() { return boothNo; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getBoothType() { return boothType; }
    public String getStatus() { return status; }
}
