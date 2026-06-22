package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.rbac.Permission;
import com.sportexpo.exhibitor.rbac.PermissionRepository;
import com.sportexpo.exhibitor.rbac.RolePermission;
import com.sportexpo.exhibitor.rbac.RolePermissionRepository;
import com.sportexpo.exhibitor.rbac.dto.RolePermissionUpdateRequest;
import com.sportexpo.exhibitor.users.dto.CopyRoleRequest;
import com.sportexpo.exhibitor.users.dto.CreateRoleRequest;
import com.sportexpo.exhibitor.users.dto.RoleDetailResponse;
import com.sportexpo.exhibitor.users.dto.RoleSummaryResponse;
import com.sportexpo.exhibitor.users.dto.UpdateRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateRoleStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleManagementService {
    private static final String ROLE_CODE_CONFLICT_CODE = "E_ROLE_CODE_CONFLICT";
    private static final String ROLE_TYPE_CUSTOM = "CUSTOM";
    private static final String ROLE_TYPE_SYSTEM = "SYSTEM";
    private static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;

    public RoleManagementService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                 RolePermissionRepository rolePermissionRepository, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleSummaryResponse> listRoles() {
        return roleRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getRole(UUID id) {
        var role = getRoleEntity(id);
        return toDetail(role, permissionCodes(role.getId()));
    }

    @Transactional
    public RoleDetailResponse createRole(CreateRoleRequest request, UUID actorId) {
        validateRoleCodeAvailable(request.code(), null);
        var now = OffsetDateTime.now();
        var role = roleRepository.saveAndFlush(new Role(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                ROLE_TYPE_CUSTOM,
                true,
                normalizeToNull(request.description()),
                now,
                now,
                actorId,
                actorId
        ));
        auditService.record(null, actorId, "ROLE_CREATED", "Role", role.getId(), null, "ENABLED", "create role");
        return toDetail(role, List.of());
    }

    @Transactional
    public RoleDetailResponse updateRole(UUID id, UpdateRoleRequest request, UUID actorId) {
        var role = getRoleEntity(id);
        var normalizedCode = request.code().trim();
        if (ROLE_TYPE_SYSTEM.equalsIgnoreCase(role.getRoleType()) && !role.getCode().equals(normalizedCode)) {
            throw new BusinessException("Built-in role code cannot be changed");
        }
        validateRoleCodeAvailable(normalizedCode, role.getId());
        role.update(normalizedCode, request.name().trim(), normalizeToNull(request.description()), actorId, OffsetDateTime.now());
        auditService.record(null, actorId, "ROLE_UPDATED", "Role", role.getId(), null,
                role.isEnabled() ? "ENABLED" : "DISABLED", "update role");
        return toDetail(role, permissionCodes(role.getId()));
    }

    @Transactional
    public RoleDetailResponse copyRole(UUID sourceRoleId, CopyRoleRequest request, UUID actorId) {
        var source = getRoleEntity(sourceRoleId);
        validateRoleCodeAvailable(request.code(), null);
        var now = OffsetDateTime.now();
        var copied = roleRepository.saveAndFlush(new Role(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                ROLE_TYPE_CUSTOM,
                true,
                normalizeToNull(request.description()),
                now,
                now,
                actorId,
                actorId
        ));
        var sourcePermissions = rolePermissionRepository.findByRoleId(sourceRoleId);
        if (!sourcePermissions.isEmpty()) {
            rolePermissionRepository.saveAll(sourcePermissions.stream()
                    .map(existing -> new RolePermission(copied, existing.getPermission(), now, actorId))
                    .toList());
        }
        auditService.record(null, actorId, "ROLE_COPIED", "Role", copied.getId(), source.getCode(), copied.getCode(),
                "copy role");
        return toDetail(copied, permissionCodes(copied.getId()));
    }

    @Transactional
    public RoleDetailResponse toggleRoleStatus(UUID id, UpdateRoleStatusRequest request, UUID actorId) {
        var role = getRoleEntity(id);
        role.updateStatus(request.enabled(), actorId, OffsetDateTime.now());
        auditService.record(null, actorId, "ROLE_STATUS_UPDATED", "Role", role.getId(), null,
                request.enabled() ? "ENABLED" : "DISABLED", "toggle role status");
        return toDetail(role, permissionCodes(role.getId()));
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getRolePermissions(UUID roleId) {
        var role = getRoleEntity(roleId);
        return toDetail(role, permissionCodes(roleId));
    }

    @Transactional
    public RoleDetailResponse updateRolePermissions(UUID roleId, RolePermissionUpdateRequest request, UUID actorId) {
        var role = getRoleEntity(roleId);
        var permissions = resolvePermissions(request.permissionCodes());
        if (SYSTEM_ADMIN_ROLE_CODE.equals(role.getCode())) {
            var allPermissionCodes = permissionRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                    .map(Permission::getCode)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            var requestedCodes = permissions.stream()
                    .map(Permission::getCode)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!allPermissionCodes.equals(requestedCodes)) {
                throw new BusinessException("SYSTEM_ADMIN must retain all permissions");
            }
        }

        var existing = rolePermissionRepository.findByRoleId(roleId);
        if (!existing.isEmpty()) {
            rolePermissionRepository.deleteAll(existing);
            rolePermissionRepository.flush();
        }
        var now = OffsetDateTime.now();
        if (!permissions.isEmpty()) {
            rolePermissionRepository.saveAll(permissions.stream()
                    .map(permission -> new RolePermission(role, permission, now, actorId))
                    .toList());
        }
        auditService.record(null, actorId, "ROLE_PERMISSIONS_UPDATED", "Role", role.getId(), null, role.getCode(),
                "update role permissions");
        return toDetail(role, permissionCodes(roleId));
    }

    private Role getRoleEntity(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Role does not exist"));
    }

    private void validateRoleCodeAvailable(String code, UUID currentRoleId) {
        var existing = roleRepository.findByCode(code.trim());
        if (existing.isPresent() && !existing.get().getId().equals(currentRoleId)) {
            throw new BusinessException("Role code already exists", ROLE_CODE_CONFLICT_CODE);
        }
    }

    private List<Permission> resolvePermissions(List<String> permissionCodes) {
        var normalizedCodes = permissionCodes == null
                ? List.<String>of()
                : permissionCodes.stream()
                        .map(this::normalizeToNull)
                        .filter(code -> code != null)
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                                List::copyOf
                        ));
        if (normalizedCodes.isEmpty()) {
            return List.of();
        }
        var permissionsByCode = normalizedCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessException("Permission code does not exist")))
                .collect(java.util.stream.Collectors.toMap(Permission::getCode, permission -> permission,
                        (left, right) -> left, LinkedHashMap::new));
        return normalizedCodes.stream().map(permissionsByCode::get).toList();
    }

    private List<String> permissionCodes(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .toList();
    }

    private RoleSummaryResponse toSummary(Role role) {
        return new RoleSummaryResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getRoleType(),
                role.isEnabled(),
                role.getDescription()
        );
    }

    private RoleDetailResponse toDetail(Role role, List<String> permissionCodes) {
        return new RoleDetailResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getRoleType(),
                role.isEnabled(),
                role.getDescription(),
                permissionCodes,
                role.getCreatedAt(),
                role.getUpdatedAt()
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
