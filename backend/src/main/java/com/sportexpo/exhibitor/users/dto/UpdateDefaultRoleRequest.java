package com.sportexpo.exhibitor.users.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDefaultRoleRequest(@NotBlank String defaultRole) {
}
