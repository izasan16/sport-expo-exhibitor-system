package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booth_scene_bindings")
public class BoothSceneBinding {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "scene_revision_id")
    private UUID sceneRevisionId;

    @Column(name = "scene_feature_id")
    private UUID sceneFeatureId;

    @Column(name = "booth_id")
    private UUID boothId;

    @Column(name = "binding_status")
    private String bindingStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected BoothSceneBinding() {
    }

    public BoothSceneBinding(UUID id, UUID eventId, UUID sceneRevisionId, UUID sceneFeatureId, UUID boothId,
                             String bindingStatus, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.sceneRevisionId = sceneRevisionId;
        this.sceneFeatureId = sceneFeatureId;
        this.boothId = boothId;
        this.bindingStatus = bindingStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getSceneRevisionId() {
        return sceneRevisionId;
    }

    public UUID getSceneFeatureId() {
        return sceneFeatureId;
    }

    public UUID getBoothId() {
        return boothId;
    }

    public void rebind(UUID sceneRevisionId, UUID sceneFeatureId, OffsetDateTime now) {
        this.sceneRevisionId = sceneRevisionId;
        this.sceneFeatureId = sceneFeatureId;
        this.updatedAt = now;
    }
}
