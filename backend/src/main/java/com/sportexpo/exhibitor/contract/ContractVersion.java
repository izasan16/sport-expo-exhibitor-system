package com.sportexpo.exhibitor.contract;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract_versions")
public class ContractVersion {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "contract_id")
    private UUID contractId;
    @Column(name = "version_no")
    private int versionNo;
    @Column(name = "reason")
    private String reason;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_snapshot", columnDefinition = "jsonb")
    private String contentSnapshot;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "area_sqm")
    private BigDecimal areaSqm;
    @Column(name = "sign_status")
    private String signStatus;
    @Column(name = "signer_id")
    private UUID signerId;
    @Column(name = "signed_at")
    private OffsetDateTime signedAt;
    @Column(name = "file_id")
    private UUID fileId;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "created_by")
    private UUID createdBy;

    protected ContractVersion() {
    }

    public ContractVersion(UUID id, UUID eventId, UUID contractId, int versionNo, String reason,
                           String contentSnapshot, BigDecimal amount, BigDecimal areaSqm, String signStatus,
                           UUID signerId, OffsetDateTime signedAt, UUID fileId, OffsetDateTime createdAt,
                           UUID createdBy) {
        this.id = id;
        this.eventId = eventId;
        this.contractId = contractId;
        this.versionNo = versionNo;
        this.reason = reason;
        this.contentSnapshot = contentSnapshot;
        this.amount = amount;
        this.areaSqm = areaSqm;
        this.signStatus = signStatus;
        this.signerId = signerId;
        this.signedAt = signedAt;
        this.fileId = fileId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getContractId() { return contractId; }
    public int getVersionNo() { return versionNo; }
    public String getReason() { return reason; }
    public String getContentSnapshot() { return contentSnapshot; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getSignStatus() { return signStatus; }
    public UUID getSignerId() { return signerId; }
    public OffsetDateTime getSignedAt() { return signedAt; }
    public UUID getFileId() { return fileId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    public void markSigned(UUID signerId, OffsetDateTime signedAt, String signStatus) {
        this.signerId = signerId;
        this.signedAt = signedAt;
        this.signStatus = signStatus;
    }
}
