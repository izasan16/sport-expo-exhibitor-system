package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.common.BusinessStatuses;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrations")
public class Registration {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "exhibitor_id")
    private UUID exhibitorId;
    @Column(name = "status")
    private String status;
    @Column(name = "category")
    private String category;
    @Column(name = "zone_id")
    private UUID zoneId;
    @Column(name = "intent_area_sqm")
    private BigDecimal intentAreaSqm;
    @Column(name = "contact_name")
    private String contactName;
    @Column(name = "contact_phone")
    private String contactPhone;
    @Column(name = "remark")
    private String remark;
    @Column(name = "review_comment")
    private String reviewComment;
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Registration() {
    }

    public Registration(UUID id, UUID eventId, UUID exhibitorId, String status, String category, UUID zoneId,
                        BigDecimal intentAreaSqm, String contactName, String contactPhone, String remark,
                        UUID actorId) {
        var now = OffsetDateTime.now();
        this.id = id;
        this.eventId = eventId;
        this.exhibitorId = exhibitorId;
        this.status = status;
        this.category = category;
        this.zoneId = zoneId;
        this.intentAreaSqm = intentAreaSqm;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.remark = remark;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void markRejected(UUID reviewerId, String reason) {
        this.status = BusinessStatuses.REGISTRATION_REJECTED;
        this.reviewComment = reason;
        this.reviewedBy = reviewerId;
        this.reviewedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = reviewerId;
    }

    public void updateEditableFields(String category, UUID zoneId, BigDecimal intentAreaSqm, String contactName,
                                     String contactPhone, String remark, UUID actorId) {
        this.category = category;
        this.zoneId = zoneId;
        this.intentAreaSqm = intentAreaSqm;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.remark = remark;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public void submit(UUID actorId) {
        this.status = BusinessStatuses.REGISTRATION_SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
        this.reviewComment = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public void approve(UUID reviewerId) {
        this.status = BusinessStatuses.REGISTRATION_APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = reviewerId;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getExhibitorId() { return exhibitorId; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public UUID getZoneId() { return zoneId; }
    public BigDecimal getIntentAreaSqm() { return intentAreaSqm; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getRemark() { return remark; }
    public String getReviewComment() { return reviewComment; }
    public UUID getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
