package com.sportexpo.exhibitor.admin.dto;

import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminDashboardResponse(
        OffsetDateTime generatedAt,
        long pendingSalesConfirmCount,
        long pendingFinanceConfirmCount,
        long pendingSelectionReviewCount,
        long todayNewOrderCount,
        List<ParticipationOrderResponse> pendingSalesConfirmOrders,
        List<PaymentResponse> pendingFinanceConfirmPayments,
        List<BoothSelectionResponse> pendingSelectionReviews
) {
}
