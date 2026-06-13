package com.sportexpo.exhibitor.selection.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectBoothSelectionRequest(@NotBlank String reason) {
}
