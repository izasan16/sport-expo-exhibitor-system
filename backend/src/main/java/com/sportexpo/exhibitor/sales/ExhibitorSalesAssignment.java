package com.sportexpo.exhibitor.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exhibitor_sales_assignments")
public class ExhibitorSalesAssignment {
    public static final String TYPE_PRIMARY = "PRIMARY";
    public static final String TYPE_COLLABORATOR = "COLLABORATOR";

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "exhibitor_id")
    private UUID exhibitorId;
    @Column(name = "sales_user_id")
    private UUID salesUserId;
    @Column(name = "assignment_type")
    private String assignmentType;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ExhibitorSalesAssignment() {
    }

    public ExhibitorSalesAssignment(UUID id, UUID exhibitorId, UUID salesUserId, String assignmentType,
                                    OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.exhibitorId = exhibitorId;
        this.salesUserId = salesUserId;
        this.assignmentType = assignmentType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getExhibitorId() { return exhibitorId; }
    public UUID getSalesUserId() { return salesUserId; }
    public String getAssignmentType() { return assignmentType; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
}
