package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_source_files")
public class SceneSourceFile {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "scene_id")
    private UUID sceneId;

    @Column(name = "scene_revision_id")
    private UUID sceneRevisionId;

    @Column(name = "file_id")
    private UUID storedFileId;

    @Column(name = "source_format")
    private String sourceFormat;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "upload_status")
    private String uploadStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected SceneSourceFile() {
    }

    public SceneSourceFile(UUID id, UUID sceneId, UUID sceneRevisionId, UUID storedFileId, String sourceFormat,
                           String originalName, long fileSize, String uploadStatus, OffsetDateTime createdAt,
                           UUID createdBy) {
        this.id = id;
        this.sceneId = sceneId;
        this.sceneRevisionId = sceneRevisionId;
        this.storedFileId = storedFileId;
        this.sourceFormat = sourceFormat;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.uploadStatus = uploadStatus;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSceneId() {
        return sceneId;
    }

    public UUID getSceneRevisionId() {
        return sceneRevisionId;
    }

    public UUID getStoredFileId() {
        return storedFileId;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public String getOriginalName() {
        return originalName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
