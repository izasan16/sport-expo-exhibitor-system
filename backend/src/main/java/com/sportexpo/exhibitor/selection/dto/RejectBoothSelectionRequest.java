package com.sportexpo.exhibitor.selection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectBoothSelectionRequest(@NotBlank @Size(max = 500) String reason) {
}
