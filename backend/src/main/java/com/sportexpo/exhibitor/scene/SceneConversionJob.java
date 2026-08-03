package com.sportexpo.exhibitor.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scene_conversion_jobs")
public class SceneConversionJob {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "scene_id")
    private UUID sceneId;

    @Column(name = "scene_revision_id")
    private UUID sceneRevisionId;

    @Column(name = "source_file_id")
    private UUID sourceFileId;

    @Column(name = "job_status")
    private String jobStatus;

    @Column(name = "target_format")
    private String targetFormat;

    @Column(name = "result_file_id")
    private UUID resultFileId;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected SceneConversionJob() {
    }

    public SceneConversionJob(UUID id, UUID sceneId, UUID sceneRevisionId, UUID sourceFileId, String jobStatus,
                              String targetFormat, UUID resultFileId, String errorCode, String errorMessage,
                              OffsetDateTime startedAt, OffsetDateTime finishedAt, OffsetDateTime createdAt) {
        this.id = id;
        this.sceneId = sceneId;
        this.sceneRevisionId = sceneRevisionId;
        this.sourceFileId = sourceFileId;
        this.jobStatus = jobStatus;
        this.targetFormat = targetFormat;
        this.resultFileId = resultFileId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
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

    public UUID getSourceFileId() {
        return sourceFileId;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public UUID getResultFileId() {
        return resultFileId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
