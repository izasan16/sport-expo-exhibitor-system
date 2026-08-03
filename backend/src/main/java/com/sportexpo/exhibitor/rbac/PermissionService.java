package com.sportexpo.exhibitor.rbac;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.rbac.dto.PermissionRequest;
import com.sportexpo.exhibitor.rbac.dto.PermissionResourceResponse;
import com.sportexpo.exhibitor.rbac.dto.PermissionTreeNodeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
public class PermissionService {
    private static final String PERMISSION_CODE_CONFLICT_CODE = "E_PERMISSION_CODE_CONFLICT";

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public PermissionService(PermissionRepository permissionRepository, AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PermissionResourceResponse> listPermissions() {
        return permissionRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(this::toResourceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionTreeNodeResponse> treePermissions() {
        var permissions = permissionRepository.findAllByOrderBySortOrderAscCodeAsc();
        var childrenByParentId = new LinkedHashMap<UUID, List<Permission>>();
        for (var permission : permissions) {
            childrenByParentId.computeIfAbsent(permission.getParentId(), ignored -> new java.util.ArrayList<>())
                    .add(permission);
        }
        return permissions.stream()
                .filter(permission -> permission.getParentId() == null)
                .map(permission -> toTreeNode(permission, childrenByParentId))
                .toList();
    }

    @Transactional
    public PermissionResourceResponse createPermission(PermissionRequest request, UUID actorId) {
        validatePermissionCodeAvailable(request.code(), null);
        validateParent(request.parentId(), null);
        var now = OffsetDateTime.now();
        var permission = permissionRepository.saveAndFlush(new Permission(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                request.resourceType().trim(),
                request.moduleKey().trim(),
                request.parentId(),
                normalizeToNull(request.routePath()),
                normalizeToNull(request.actionKey()),
                request.sortOrder(),
                request.enabled(),
                normalizeToNull(request.description()),
                now,
                now,
                actorId,
                actorId
        ));
        auditService.record(null, actorId, "PERMISSION_CREATED", "Permission", permission.getId(), null,
                permission.getCode(), "create permission");
        return toResourceResponse(permission);
    }

    @Transactional
    public PermissionResourceResponse updatePermission(UUID id, PermissionRequest request, UUID actorId) {
        var permission = getPermission(id);
        validatePermissionCodeAvailable(request.code(), id);
        validateParent(request.parentId(), id);
        permission.update(
                request.code().trim(),
                request.name().trim(),
                request.resourceType().trim(),
                request.moduleKey().trim(),
                request.parentId(),
                normalizeToNull(request.routePath()),
                normalizeToNull(request.actionKey()),
                request.sortOrder(),
                request.enabled(),
                normalizeToNull(request.description()),
                actorId,
                OffsetDateTime.now()
        );
        auditService.record(null, actorId, "PERMISSION_UPDATED", "Permission", permission.getId(), null,
                permission.getCode(), "update permission");
        return toResourceResponse(permission);
    }

    @Transactional
    public PermissionResourceResponse togglePermissionStatus(UUID id, boolean enabled, UUID actorId) {
        var permission = getPermission(id);
        permission.updateStatus(enabled, actorId, OffsetDateTime.now());
        auditService.record(null, actorId, "PERMISSION_STATUS_UPDATED", "Permission", permission.getId(), null,
                permission.getCode(), enabled ? "enable permission" : "disable permission");
        return toResourceResponse(permission);
    }

    private Permission getPermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Permission does not exist"));
    }

    private void validatePermissionCodeAvailable(String code, UUID currentPermissionId) {
        var existing = permissionRepository.findByCode(code.trim());
        if (existing.isPresent() && !existing.get().getId().equals(currentPermissionId)) {
            throw new BusinessException("Permission code already exists", PERMISSION_CODE_CONFLICT_CODE);
        }
    }

    private void validateParent(UUID parentId, UUID currentPermissionId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(currentPermissionId)) {
            throw new BusinessException("Permission parent cannot reference itself");
        }
        if (!permissionRepository.existsById(parentId)) {
            throw new BusinessException("Permission parent does not exist");
        }
    }

    private PermissionTreeNodeResponse toTreeNode(Permission permission, java.util.Map<UUID, List<Permission>> childrenByParentId) {
        var children = childrenByParentId.getOrDefault(permission.getId(), List.of()).stream()
                .map(child -> toTreeNode(child, childrenByParentId))
                .toList();
        return new PermissionTreeNodeResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getResourceType(),
                permission.getModuleKey(),
                permission.getParentId(),
                permission.getRoutePath(),
                permission.getActionKey(),
                permission.getSortOrder(),
                permission.isEnabled(),
                permission.getDescription(),
                children
        );
    }

    private PermissionResourceResponse toResourceResponse(Permission permission) {
        return new PermissionResourceResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getResourceType(),
                permission.getModuleKey(),
                permission.getParentId(),
                permission.getRoutePath(),
                permission.getActionKey(),
                permission.getSortOrder(),
                permission.isEnabled(),
                permission.getDescription()
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
