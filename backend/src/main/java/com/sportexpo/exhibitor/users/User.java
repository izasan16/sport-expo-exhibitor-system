package com.sportexpo.exhibitor.users;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "username")
    private String username;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "phone")
    private String phone;
    @Column(name = "email")
    private String email;
    @Column(name = "user_type")
    private String userType;
    @Column(name = "exhibitor_id")
    private UUID exhibitorId;
    @Column(name = "status")
    private String status;
    @Column(name = "default_role")
    private String defaultRole;
    @Column(name = "failed_login_count")
    private int failedLoginCount;
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;
    @Column(name = "jwt_version")
    private int jwtVersion;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserRole> userRoles = new LinkedHashSet<>();

    protected User() {
    }

    public User(UUID id, String username, String passwordHash, String displayName, String phone, String email,
                String userType, UUID exhibitorId, String status, String defaultRole, int failedLoginCount,
                OffsetDateTime lockedUntil, OffsetDateTime lastLoginAt, int jwtVersion, OffsetDateTime createdAt,
                OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phone = phone;
        this.email = email;
        this.userType = userType;
        this.exhibitorId = exhibitorId;
        this.status = status;
        this.defaultRole = defaultRole;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.jwtVersion = jwtVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public void assignRole(Role role) {
        userRoles.add(new UserRole(this, role, OffsetDateTime.now()));
    }

    public boolean isDisabled() {
        return !STATUS_ACTIVE.equals(status);
    }

    public boolean isLockedAt(OffsetDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(int maxFailures, Duration lockDuration, OffsetDateTime now) {
        failedLoginCount++;
        if (failedLoginCount >= maxFailures) {
            lockedUntil = now.plus(lockDuration);
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(OffsetDateTime now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public List<String> roleCodes() {
        return userRoles.stream()
                .map(UserRole::getRole)
                .map(Role::getCode)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public UUID getExhibitorId() { return exhibitorId; }
    public String getStatus() { return status; }
    public String getDefaultRole() { return defaultRole; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public int getJwtVersion() { return jwtVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Set<UserRole> getUserRoles() { return Set.copyOf(userRoles); }
}
