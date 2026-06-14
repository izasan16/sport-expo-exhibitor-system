package com.sportexpo.exhibitor.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BatchConfirmOrdersRequest(@NotEmpty List<@NotNull UUID> orderIds) {
}
