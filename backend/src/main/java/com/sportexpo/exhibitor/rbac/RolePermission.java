package com.sportexpo.exhibitor.rbac;

import com.sportexpo.exhibitor.users.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "role_permissions")
public class RolePermission {
    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private java.util.UUID createdBy;

    protected RolePermission() {
    }

    public RolePermission(Role role, Permission permission, OffsetDateTime createdAt, java.util.UUID createdBy) {
        this.id = new RolePermissionId(role.getId(), permission.getId());
        this.role = role;
        this.permission = permission;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public RolePermissionId getId() { return id; }
    public Role getRole() { return role; }
    public Permission getPermission() { return permission; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public java.util.UUID getCreatedBy() { return createdBy; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RolePermission that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
