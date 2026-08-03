package com.sportexpo.exhibitor.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PermissionRequest(
        @NotBlank @Size(max = 200) String code,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 20) String resourceType,
        @NotBlank @Size(max = 80) String moduleKey,
        UUID parentId,
        @Size(max = 255) String routePath,
        @Size(max = 160) String actionKey,
        int sortOrder,
        boolean enabled,
        @Size(max = 500) String description
) {
}
