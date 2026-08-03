package com.sportexpo.exhibitor.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, max = 120) String password,
        @NotBlank String displayName,
        String phone,
        @Email String email,
        @NotBlank String userType,
        UUID exhibitorId,
        @NotBlank String status,
        @NotEmpty List<@NotBlank String> roleCodes,
        @NotBlank String defaultRole
) {
}
