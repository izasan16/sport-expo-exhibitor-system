package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.exhibitor.ExhibitorRepository;
import com.sportexpo.exhibitor.users.dto.CreateUserRequest;
import com.sportexpo.exhibitor.users.dto.ResetPasswordRequest;
import com.sportexpo.exhibitor.users.dto.UpdateDefaultRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserRolesRequest;
import com.sportexpo.exhibitor.users.dto.UserDetailResponse;
import com.sportexpo.exhibitor.users.dto.UserSummaryResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserManagementService {
    private static final String USERNAME_CONFLICT_CODE = "E_USER_USERNAME_CONFLICT";
    private static final String USER_TYPE_EXHIBITOR = "EXHIBITOR";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ExhibitorRepository exhibitorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserManagementService(UserRepository userRepository, RoleRepository roleRepository,
                                 ExhibitorRepository exhibitorRepository, PasswordEncoder passwordEncoder,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.exhibitorRepository = exhibitorRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> list(String username, String status, String roleCode) {
        var normalizedUsername = normalizeToNull(username);
        var normalizedStatus = normalizeToNull(status);
        var normalizedRoleCode = normalizeToNull(roleCode);
        return userRepository.findAll().stream()
                .filter(user -> normalizedUsername == null || user.getUsername().toLowerCase(Locale.ROOT)
                        .contains(normalizedUsername.toLowerCase(Locale.ROOT)))
                .filter(user -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(user.getStatus()))
                .filter(user -> normalizedRoleCode == null || user.roleCodes().contains(normalizedRoleCode))
                .sorted(Comparator.comparing(User::getUsername))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailResponse get(UUID id) {
        return toDetail(getUser(id));
    }

    @Transactional
    public UserDetailResponse create(CreateUserRequest request, UUID actorId) {
        validateUsernameAvailable(request.username(), null);
        validateUserBinding(request.userType(), request.exhibitorId());
        var roles = resolveRoles(request.roleCodes());
        validateDefaultRoleForRoles(request.defaultRole(), roles);

        var now = OffsetDateTime.now();
        var user = new User(
                UUID.randomUUID(),
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                normalizeToNull(request.phone()),
                normalizeToNull(request.email()),
                request.userType().trim(),
                request.exhibitorId(),
                request.status().trim(),
                request.defaultRole().trim(),
                0,
                null,
                null,
                0,
                now,
                now,
                actorId,
                actorId
        );
        user.replaceRoles(roles);
        var saved = userRepository.saveAndFlush(user);
        auditService.record(null, actorId, "USER_CREATED", "User", saved.getId(), null, saved.getStatus(), "create user");
        return toDetail(saved);
    }

    @Transactional
    public UserDetailResponse update(UUID id, UpdateUserRequest request, UUID actorId) {
        var user = getUser(id);
        validateUsernameAvailable(request.username(), id);
        validateUserBinding(request.userType(), request.exhibitorId());
        user.updateProfile(
                request.username().trim(),
                request.displayName().trim(),
                normalizeToNull(request.phone()),
                normalizeToNull(request.email()),
                request.userType().trim(),
                request.exhibitorId(),
                actorId,
                OffsetDateTime.now()
        );
        auditService.record(null, actorId, "USER_UPDATED", "User", user.getId(), null, user.getStatus(), "update user");
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse updateStatus(UUID id, boolean enabled, UUID actorId) {
        var user = getUser(id);
        var now = OffsetDateTime.now();
        user.updateStatus(enabled ? STATUS_ACTIVE : STATUS_DISABLED, actorId, now);
        if (!enabled) {
            user.incrementJwtVersion(now);
        }
        auditService.record(null, actorId, "USER_STATUS_UPDATED", "User", user.getId(), null, user.getStatus(),
                enabled ? "enable user" : "disable user");
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse updateRoles(UUID id, UpdateUserRolesRequest request, UUID actorId) {
        var user = getUser(id);
        var roles = resolveRoles(request.roleCodes());
        validateDefaultRoleForRoles(request.defaultRole(), roles);
        var now = OffsetDateTime.now();
        user.replaceRoles(roles);
        user.updateDefaultRole(request.defaultRole().trim(), actorId, now);
        user.incrementJwtVersion(now);
        auditService.record(null, actorId, "USER_ROLES_UPDATED", "User", user.getId(), null, user.getStatus(),
                "update user roles");
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse updateDefaultRole(UUID id, UpdateDefaultRoleRequest request, UUID actorId) {
        var user = getUser(id);
        validateDefaultRoleForCodes(request.defaultRole(), user.roleCodes());
        var now = OffsetDateTime.now();
        user.updateDefaultRole(request.defaultRole().trim(), actorId, now);
        user.incrementJwtVersion(now);
        auditService.record(null, actorId, "USER_DEFAULT_ROLE_UPDATED", "User", user.getId(), null, user.getStatus(),
                "update default role");
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse resetPassword(UUID id, ResetPasswordRequest request, UUID actorId) {
        var user = getUser(id);
        user.resetPassword(passwordEncoder.encode(request.newPassword()), actorId, OffsetDateTime.now());
        auditService.record(null, actorId, "USER_PASSWORD_RESET", "User", user.getId(), null, user.getStatus(),
                "reset password");
        return toDetail(user);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User does not exist"));
    }

    private void validateUsernameAvailable(String username, UUID currentUserId) {
        var existing = userRepository.findByUsername(username.trim());
        if (existing.isPresent() && !existing.get().getId().equals(currentUserId)) {
            throw new BusinessException("Username already exists", USERNAME_CONFLICT_CODE);
        }
    }

    private void validateUserBinding(String userType, UUID exhibitorId) {
        var normalizedUserType = userType.trim();
        if (USER_TYPE_EXHIBITOR.equalsIgnoreCase(normalizedUserType)) {
            if (exhibitorId == null) {
                throw new BusinessException("EXHIBITOR user requires exhibitor binding");
            }
            if (!exhibitorRepository.existsById(exhibitorId)) {
                throw new BusinessException("Bound exhibitor does not exist");
            }
            return;
        }
        if (exhibitorId != null) {
            throw new BusinessException("Non-EXHIBITOR user cannot bind exhibitor");
        }
    }

    private List<Role> resolveRoles(List<String> roleCodes) {
        var normalizedCodes = roleCodes == null
                ? List.<String>of()
                : roleCodes.stream()
                        .map(this::normalizeToNull)
                        .filter(code -> code != null)
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                                List::copyOf
                        ));
        if (normalizedCodes.isEmpty()) {
            throw new BusinessException("Role codes must not be empty");
        }
        var rolesByCode = roleRepository.findByCodeIn(normalizedCodes).stream()
                .collect(java.util.stream.Collectors.toMap(Role::getCode, role -> role, (left, right) -> left, LinkedHashMap::new));
        if (rolesByCode.size() != normalizedCodes.size()) {
            throw new BusinessException("Role code does not exist");
        }
        return normalizedCodes.stream().map(rolesByCode::get).toList();
    }

    private void validateDefaultRoleForRoles(String defaultRole, List<Role> roles) {
        validateDefaultRoleForCodes(defaultRole, roles.stream().map(Role::getCode).toList());
    }

    private void validateDefaultRoleForCodes(String defaultRole, List<String> roleCodes) {
        var normalizedDefaultRole = defaultRole == null ? null : defaultRole.trim();
        if (normalizedDefaultRole == null || !roleCodes.contains(normalizedDefaultRole)) {
            throw new BusinessException("User default role must belong to assigned roles");
        }
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhone(),
                user.getEmail(),
                user.getUserType(),
                user.getExhibitorId(),
                user.getStatus(),
                user.getDefaultRole(),
                user.roleCodes()
        );
    }

    private UserDetailResponse toDetail(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhone(),
                user.getEmail(),
                user.getUserType(),
                user.getExhibitorId(),
                user.getStatus(),
                user.getDefaultRole(),
                user.roleCodes(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
