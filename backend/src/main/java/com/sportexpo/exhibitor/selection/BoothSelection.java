package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.common.BusinessStatuses;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booth_selections")
public class BoothSelection {
    @Id
    @Column(name = "id") private UUID id;
    @Column(name = "event_id") private UUID eventId;
    @Column(name = "order_id") private UUID orderId;
    @Column(name = "exhibitor_id") private UUID exhibitorId;
    @Column(name = "status") private String status;
    @Column(name = "total_area_sqm") private BigDecimal totalAreaSqm;
    @Column(name = "lock_started_at") private OffsetDateTime lockStartedAt;
    @Column(name = "lock_expires_at") private OffsetDateTime lockExpiresAt;
    @Column(name = "helper_user_id") private UUID helperUserId;
    @Column(name = "reviewer_user_id") private UUID reviewerUserId;
    @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;
    @Column(name = "review_comment") private String reviewComment;
    @Column(name = "exhibitor_remark") private String exhibitorRemark;
    @Column(name = "recommended_booths") private String recommendedBooths;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    protected BoothSelection() {
    }

    public BoothSelection(UUID id, UUID eventId, UUID orderId, UUID exhibitorId, String status,
                          BigDecimal totalAreaSqm, OffsetDateTime lockStartedAt, OffsetDateTime lockExpiresAt,
                          UUID helperUserId, UUID reviewerUserId, OffsetDateTime reviewedAt, String reviewComment,
                          String exhibitorRemark, String recommendedBooths, OffsetDateTime createdAt,
                          OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.eventId = eventId;
        this.orderId = orderId;
        this.exhibitorId = exhibitorId;
        this.status = status;
        this.totalAreaSqm = totalAreaSqm;
        this.lockStartedAt = lockStartedAt;
        this.lockExpiresAt = lockExpiresAt;
        this.helperUserId = helperUserId;
        this.reviewerUserId = reviewerUserId;
        this.reviewedAt = reviewedAt;
        this.reviewComment = reviewComment;
        this.exhibitorRemark = exhibitorRemark;
        this.recommendedBooths = recommendedBooths;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public UUID getExhibitorId() { return exhibitorId; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAreaSqm() { return totalAreaSqm; }
    public OffsetDateTime getLockStartedAt() { return lockStartedAt; }
    public OffsetDateTime getLockExpiresAt() { return lockExpiresAt; }
    public UUID getHelperUserId() { return helperUserId; }
    public UUID getReviewerUserId() { return reviewerUserId; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewComment() { return reviewComment; }
    public String getExhibitorRemark() { return exhibitorRemark; }
    public String getRecommendedBooths() { return recommendedBooths; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void markConfirmed(UUID reviewerUserId, OffsetDateTime now) {
        this.status = BusinessStatuses.SELECTION_CONFIRMED;
        this.reviewerUserId = reviewerUserId;
        this.reviewedAt = now;
        this.reviewComment = null;
        this.recommendedBooths = null;
        this.updatedAt = now;
        this.updatedBy = reviewerUserId;
    }

    public void markRejected(UUID reviewerUserId, String reviewComment, String recommendedBooths,
                             OffsetDateTime now) {
        this.status = BusinessStatuses.SELECTION_REJECTED;
        this.reviewerUserId = reviewerUserId;
        this.reviewedAt = now;
        this.reviewComment = reviewComment;
        this.recommendedBooths = recommendedBooths;
        this.updatedAt = now;
        this.updatedBy = reviewerUserId;
    }

    public void markTimeoutReleased(String reviewComment, UUID actorId, OffsetDateTime now) {
        this.status = BusinessStatuses.SELECTION_TIMEOUT;
        this.reviewedAt = now;
        this.reviewComment = reviewComment;
        this.recommendedBooths = null;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
