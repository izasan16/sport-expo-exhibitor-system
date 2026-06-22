package com.sportexpo.exhibitor.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "module_key")
    private String moduleKey;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "route_path")
    private String routePath;

    @Column(name = "action_key")
    private String actionKey;

    @Column(name = "sort_order")
    private int sortOrder;

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

    protected Permission() {
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getResourceType() { return resourceType; }
    public String getModuleKey() { return moduleKey; }
    public UUID getParentId() { return parentId; }
    public String getRoutePath() { return routePath; }
    public String getActionKey() { return actionKey; }
    public int getSortOrder() { return sortOrder; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
}
