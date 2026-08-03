package com.sportexpo.exhibitor.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SalesFollowUpRequest(
        @NotNull UUID exhibitorId,
        @NotBlank @Size(max = 40) String method,
        @NotBlank @Size(max = 1000) String content,
        OffsetDateTime nextFollowUpAt,
        @NotBlank @Size(max = 40) String status
) {
}
