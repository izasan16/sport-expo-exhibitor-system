package com.sportexpo.exhibitor.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateUserRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        String phone,
        @Email String email,
        @NotBlank String userType,
        UUID exhibitorId
) {
}
