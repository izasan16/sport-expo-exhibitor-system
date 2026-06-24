package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_features")
public class SceneFeature {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "scene_revision_id")
    private UUID sceneRevisionId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "feature_type")
    private String featureType;

    @Column(name = "geometry_type")
    private String geometryType;

    @Column(name = "geometry_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode geometryJson;

    @Column(name = "label")
    private String label;

    @Column(name = "code")
    private String code;

    @Column(name = "layer_order")
    private int layerOrder;

    @Column(name = "style_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode styleJson;

    @Column(name = "meta_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metaJson;

    @Column(name = "area_px2")
    private BigDecimal areaPx2;

    @Column(name = "area_sqm")
    private BigDecimal areaSqm;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected SceneFeature() {
    }

    public SceneFeature(UUID id, UUID sceneRevisionId, UUID eventId, String featureType, String geometryType,
                        JsonNode geometryJson, String label, String code, int layerOrder,
                        JsonNode styleJson, JsonNode metaJson, BigDecimal areaPx2, BigDecimal areaSqm,
                        boolean locked, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.sceneRevisionId = sceneRevisionId;
        this.eventId = eventId;
        this.featureType = featureType;
        this.geometryType = geometryType;
        this.geometryJson = geometryJson;
        this.label = label;
        this.code = code;
        this.layerOrder = layerOrder;
        this.styleJson = styleJson;
        this.metaJson = metaJson;
        this.areaPx2 = areaPx2;
        this.areaSqm = areaSqm;
        this.locked = locked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSceneRevisionId() {
        return sceneRevisionId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getFeatureType() {
        return featureType;
    }

    public String getGeometryType() {
        return geometryType;
    }

    public JsonNode getGeometryJson() {
        return geometryJson;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    public int getLayerOrder() {
        return layerOrder;
    }

    public JsonNode getMetaJson() {
        return metaJson;
    }

    public JsonNode getStyleJson() {
        return styleJson;
    }

    public BigDecimal getAreaPx2() {
        return areaPx2;
    }

    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public boolean isLocked() {
        return locked;
    }
}
