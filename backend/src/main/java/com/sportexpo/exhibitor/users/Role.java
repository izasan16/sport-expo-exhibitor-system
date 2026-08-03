package com.sportexpo.exhibitor.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "role_type")
    private String roleType;
    @Column(name = "enabled")
    private boolean enabled;
    @Column(name = "description")
    private String description;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Role() {
    }

    public Role(UUID id, String code, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, code, name, "SYSTEM", true, null, createdAt, updatedAt, null, null);
    }

    public Role(UUID id, String code, String name, String roleType, boolean enabled, String description,
                OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.roleType = roleType;
        this.enabled = enabled;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public void update(String code, String name, String description, UUID actorId, OffsetDateTime now) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void updateStatus(boolean enabled, UUID actorId, OffsetDateTime now) {
        this.enabled = enabled;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getRoleType() { return roleType; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
}
