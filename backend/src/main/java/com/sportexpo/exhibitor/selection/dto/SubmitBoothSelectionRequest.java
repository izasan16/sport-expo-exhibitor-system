package com.sportexpo.exhibitor.selection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SubmitBoothSelectionRequest(@NotNull UUID orderId,
                                          @NotNull @Size(min = 1) List<UUID> boothIds,
                                          @Size(max = 500) String exhibitorRemark) {
}
