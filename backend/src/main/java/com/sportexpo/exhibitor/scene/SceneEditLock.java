package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_edit_locks")
public class SceneEditLock {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "revision_id")
    private UUID revisionId;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "locked_by_name")
    private String lockedByName;

    @Column(name = "heartbeat_at")
    private OffsetDateTime heartbeatAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected SceneEditLock() {
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
