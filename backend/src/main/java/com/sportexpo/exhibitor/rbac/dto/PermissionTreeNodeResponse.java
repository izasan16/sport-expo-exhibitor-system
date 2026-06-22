package com.sportexpo.exhibitor.rbac.dto;

import java.util.List;
import java.util.UUID;

public record PermissionTreeNodeResponse(
        UUID id,
        String code,
        String name,
        String resourceType,
        String moduleKey,
        UUID parentId,
        String routePath,
        String actionKey,
        int sortOrder,
        boolean enabled,
        String description,
        List<PermissionTreeNodeResponse> children
) {
}
