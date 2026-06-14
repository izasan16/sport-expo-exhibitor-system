package com.sportexpo.exhibitor.order.dto;

import java.util.List;
import java.util.UUID;

public record BatchConfirmOrdersResponse(int successCount, int failureCount, List<BatchFailure> failures) {
    public record BatchFailure(UUID orderId, String reason) {
    }
}
