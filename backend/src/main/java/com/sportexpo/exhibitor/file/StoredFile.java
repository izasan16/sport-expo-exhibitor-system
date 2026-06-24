package com.sportexpo.exhibitor.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stored_files")
public class StoredFile {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected StoredFile() {
    }

    public StoredFile(UUID id, String originalName, String mimeType, String storagePath, String publicUrl,
                      long fileSize, Integer width, Integer height, OffsetDateTime createdAt, UUID createdBy) {
        this.id = id;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.publicUrl = publicUrl;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
