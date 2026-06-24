package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_background_assets")
public class SceneBackgroundAsset {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "scene_id")
    private UUID sceneId;

    @Column(name = "scene_revision_id")
    private UUID sceneRevisionId;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "offset_x")
    private BigDecimal offsetX;

    @Column(name = "offset_y")
    private BigDecimal offsetY;

    @Column(name = "scale")
    private BigDecimal scale;

    @Column(name = "opacity")
    private BigDecimal opacity;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SceneBackgroundAsset() {
    }

    public SceneBackgroundAsset(UUID id, UUID sceneId, UUID sceneRevisionId, UUID fileId, String fileName,
                                String mimeType, String sourceType, Integer width, Integer height,
                                BigDecimal offsetX, BigDecimal offsetY, BigDecimal scale, BigDecimal opacity,
                                boolean locked, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.sceneId = sceneId;
        this.sceneRevisionId = sceneRevisionId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sourceType = sourceType;
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = scale;
        this.opacity = opacity;
        this.locked = locked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public BigDecimal getOffsetX() {
        return offsetX;
    }

    public BigDecimal getOffsetY() {
        return offsetY;
    }

    public BigDecimal getScale() {
        return scale;
    }

    public BigDecimal getOpacity() {
        return opacity;
    }

    public boolean isLocked() {
        return locked;
    }
}
