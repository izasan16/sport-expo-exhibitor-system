package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_revisions")
public class SceneRevision {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "scene_id")
    private UUID sceneId;

    @Column(name = "revision_no")
    private int revisionNo;

    @Column(name = "status")
    private String status;

    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "snapshot_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode snapshotJson;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SceneRevision() {
    }

    public SceneRevision(UUID id, UUID sceneId, int revisionNo, String status, String changeSummary, JsonNode snapshotJson,
                         OffsetDateTime publishedAt, UUID publishedBy, OffsetDateTime createdAt,
                         OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.sceneId = sceneId;
        this.revisionNo = revisionNo;
        this.status = status;
        this.changeSummary = changeSummary;
        this.snapshotJson = snapshotJson;
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSceneId() {
        return sceneId;
    }

    public int getRevisionNo() {
        return revisionNo;
    }

    public String getStatus() {
        return status;
    }

    public JsonNode getSnapshotJson() {
        return snapshotJson;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateSnapshot(JsonNode snapshotJson, UUID actorId, OffsetDateTime now) {
        this.snapshotJson = snapshotJson;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markPublished(String changeSummary, UUID actorId, OffsetDateTime now) {
        this.status = "PUBLISHED";
        this.changeSummary = changeSummary;
        this.publishedAt = now;
        this.publishedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
