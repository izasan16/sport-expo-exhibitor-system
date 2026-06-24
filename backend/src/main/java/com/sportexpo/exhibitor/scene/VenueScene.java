package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "venue_scenes")
public class VenueScene {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "status")
    private String status;

    @Column(name = "canvas_width")
    private int canvasWidth;

    @Column(name = "canvas_height")
    private int canvasHeight;

    @Column(name = "pixel_to_meter_ratio")
    private BigDecimal pixelToMeterRatio;

    @Column(name = "published_revision_id")
    private UUID publishedRevisionId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected VenueScene() {
    }

    public VenueScene(UUID id, UUID eventId, String status, int canvasWidth, int canvasHeight,
                      BigDecimal pixelToMeterRatio, UUID publishedRevisionId,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.status = status;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.pixelToMeterRatio = pixelToMeterRatio;
        this.publishedRevisionId = publishedRevisionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public int getCanvasWidth() {
        return canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public BigDecimal getPixelToMeterRatio() {
        return pixelToMeterRatio;
    }

    public UUID getPublishedRevisionId() {
        return publishedRevisionId;
    }

    public void updateDraftMetrics(int canvasWidth, int canvasHeight, BigDecimal pixelToMeterRatio,
                                   UUID actorId, OffsetDateTime now) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.pixelToMeterRatio = pixelToMeterRatio;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markPublished(UUID revisionId, UUID actorId, OffsetDateTime now) {
        this.publishedRevisionId = revisionId;
        this.status = "PUBLISHED";
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
