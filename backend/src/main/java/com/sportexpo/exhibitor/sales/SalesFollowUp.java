package com.sportexpo.exhibitor.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales_follow_ups")
public class SalesFollowUp {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "exhibitor_id")
    private UUID exhibitorId;
    @Column(name = "sales_user_id")
    private UUID salesUserId;
    @Column(name = "method")
    private String method;
    @Column(name = "content")
    private String content;
    @Column(name = "next_follow_up_at")
    private OffsetDateTime nextFollowUpAt;
    @Column(name = "status")
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SalesFollowUp() {
    }

    public SalesFollowUp(UUID id, UUID exhibitorId, UUID salesUserId, String method, String content,
                         OffsetDateTime nextFollowUpAt, String status, OffsetDateTime createdAt,
                         OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.exhibitorId = exhibitorId;
        this.salesUserId = salesUserId;
        this.method = method;
        this.content = content;
        this.nextFollowUpAt = nextFollowUpAt;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public void updateBySales(String method, String content, OffsetDateTime nextFollowUpAt, String status,
                              UUID actorId, OffsetDateTime now) {
        this.method = method;
        this.content = content;
        this.nextFollowUpAt = nextFollowUpAt;
        this.status = status;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void updateStatusByAdmin(String status, UUID actorId, OffsetDateTime now) {
        this.status = status;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public UUID getId() { return id; }
    public UUID getExhibitorId() { return exhibitorId; }
    public UUID getSalesUserId() { return salesUserId; }
    public String getMethod() { return method; }
    public String getContent() { return content; }
    public OffsetDateTime getNextFollowUpAt() { return nextFollowUpAt; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
}
