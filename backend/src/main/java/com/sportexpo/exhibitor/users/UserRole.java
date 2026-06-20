package com.sportexpo.exhibitor.users;

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
@Table(name = "user_roles")
public class UserRole {
    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected UserRole() {
    }

    public UserRole(User user, Role role, OffsetDateTime createdAt) {
        this.id = new UserRoleId(user.getId(), role.getId());
        this.user = user;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UserRoleId getId() { return id; }
    public User getUser() { return user; }
    public Role getRole() { return role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserRole that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
