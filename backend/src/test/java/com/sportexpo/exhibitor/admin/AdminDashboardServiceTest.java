package com.sportexpo.exhibitor.admin;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.payment.PaymentService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminDashboardServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired PaymentService paymentService;
    @Autowired AdminDashboardService dashboardService;

    @Test
    void aggregatesPendingWorkbenchCountsAndTopItems() {
        var salesOrder = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        var paymentOrder = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(
                paymentOrder.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"));
        var selection = fixture.createPendingSelection("9.00", "STANDARD");

        var dashboard = dashboardService.getDashboard();

        assertThat(dashboard.generatedAt()).isNotNull();
        assertThat(dashboard.pendingSalesConfirmCount()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.pendingFinanceConfirmCount()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.pendingSelectionReviewCount()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.todayNewOrderCount()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.pendingSalesConfirmOrders()).anyMatch(item -> item.id().equals(salesOrder.id()));
        assertThat(dashboard.pendingFinanceConfirmPayments()).anyMatch(item -> item.id().equals(payment.id())
                && item.status().equals(BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM));
        assertThat(dashboard.pendingSelectionReviews()).anyMatch(item -> item.id().equals(selection.id()));
    }
}
