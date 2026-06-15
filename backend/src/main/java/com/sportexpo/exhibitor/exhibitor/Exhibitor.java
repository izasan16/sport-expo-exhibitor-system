package com.sportexpo.exhibitor.exhibitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exhibitors")
public class Exhibitor {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "credit_code")
    private String creditCode;
    @Column(name = "contact_name")
    private String contactName;
    @Column(name = "contact_phone")
    private String contactPhone;
    @Column(name = "contact_email")
    private String contactEmail;
    @Column(name = "address")
    private String address;
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

    protected Exhibitor() {
    }

    public Exhibitor(UUID id, String companyName, String creditCode, String contactName, String contactPhone,
                     String contactEmail, String address, String status, UUID actorId) {
        var now = OffsetDateTime.now();
        this.id = id;
        this.companyName = companyName;
        this.creditCode = creditCode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.address = address;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void update(String companyName, String creditCode, String contactName, String contactPhone,
                       String contactEmail, String address, String status, UUID actorId) {
        this.companyName = companyName;
        this.creditCode = creditCode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.address = address;
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public UUID getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getCreditCode() { return creditCode; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
