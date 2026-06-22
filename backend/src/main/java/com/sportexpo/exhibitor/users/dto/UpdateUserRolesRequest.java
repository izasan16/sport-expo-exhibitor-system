package com.sportexpo.exhibitor.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateUserRolesRequest(
        @NotEmpty List<@NotBlank String> roleCodes,
        @NotBlank String defaultRole
) {
}
