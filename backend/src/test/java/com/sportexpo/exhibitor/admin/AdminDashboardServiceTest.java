package com.sportexpo.exhibitor.admin;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.PaymentService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminDashboardServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired PaymentService paymentService;
    @Autowired AdminDashboardService dashboardService;

    @Test
    void aggregatesPendingWorkbenchCountsAndTopItems() {
        var before = dashboardService.getDashboard();
        var salesOrder = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        var paymentOrder = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(
                paymentOrder.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"));
        var selection = fixture.createPendingSelection("9.00", "STANDARD");

        var dashboard = dashboardService.getDashboard();

        assertThat(dashboard.generatedAt()).isNotNull();
        assertThat(dashboard.pendingSalesConfirmCount()).isEqualTo(before.pendingSalesConfirmCount() + 1);
        assertThat(dashboard.pendingFinanceConfirmCount()).isEqualTo(before.pendingFinanceConfirmCount() + 1);
        assertThat(dashboard.pendingSelectionReviewCount()).isEqualTo(before.pendingSelectionReviewCount() + 1);
        assertThat(dashboard.todayNewOrderCount()).isGreaterThanOrEqualTo(before.todayNewOrderCount() + 3);
        assertThat(dashboard.pendingSalesConfirmOrders()).anyMatch(item -> item.id().equals(salesOrder.id()));
        assertThat(dashboard.pendingFinanceConfirmPayments()).anyMatch(item -> item.id().equals(payment.id())
                && item.status().equals(BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM));
        assertThat(dashboard.pendingSelectionReviews()).anyMatch(item -> item.id().equals(selection.id()));
    }

    @Test
    void pendingSalesConfirmOrdersAreLimitedToLatestFive() {
        var created = new ArrayList<ParticipationOrderResponse>();
        for (var i = 0; i < 6; i++) {
            created.add(fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD"));
        }

        var dashboard = dashboardService.getDashboard();
        var expectedLatestIds = created.subList(1, created.size()).stream()
                .map(ParticipationOrderResponse::id)
                .toList();

        assertThat(dashboard.pendingSalesConfirmOrders()).hasSizeLessThanOrEqualTo(5);
        assertThat(dashboard.pendingSalesConfirmOrders())
                .extracting(ParticipationOrderResponse::id)
                .containsExactlyElementsOf(List.of(
                        expectedLatestIds.get(4),
                        expectedLatestIds.get(3),
                        expectedLatestIds.get(2),
                        expectedLatestIds.get(1),
                        expectedLatestIds.get(0)
                ));
    }
}
