package com.sportexpo.exhibitor.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RolePermissionUpdateRequest(
        @NotNull List<@NotBlank String> permissionCodes
) {
}
