package com.sportexpo.exhibitor.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_rules")
public class PriceRule {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "zone_id")
    private UUID zoneId;
    @Column(name = "booth_type")
    private String boothType;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    @Column(name = "enabled")
    private boolean enabled;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected PriceRule() {
    }

    public PriceRule(UUID id, UUID eventId, UUID zoneId, String boothType, BigDecimal unitPrice, boolean enabled) {
        var now = OffsetDateTime.now();
        this.id = id;
        this.eventId = eventId;
        this.zoneId = zoneId;
        this.boothType = boothType;
        this.unitPrice = unitPrice;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getZoneId() { return zoneId; }
    public String getBoothType() { return boothType; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public boolean isEnabled() { return enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
