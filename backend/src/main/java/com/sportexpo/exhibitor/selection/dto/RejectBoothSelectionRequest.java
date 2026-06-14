package com.sportexpo.exhibitor.selection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectBoothSelectionRequest(@NotBlank @Size(max = 500) String reason,
                                          @Size(max = 500) String recommendedBooths) {
    public RejectBoothSelectionRequest(String reason) {
        this(reason, null);
    }
}
