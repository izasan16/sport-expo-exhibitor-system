package com.sportexpo.exhibitor.admin;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.PaymentService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.registration.RegistrationService;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.eventcategory.EventCategoryService;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.common.DemoActors;
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
    @Autowired EventService eventService;
    @Autowired ExhibitorService exhibitorService;
    @Autowired EventCategoryService eventCategoryService;
    @Autowired RegistrationService registrationService;

    @Test
    void aggregatesPendingWorkbenchCountsAndTopItems() {
        var before = dashboardService.getDashboard();
        var salesOrder = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        var paymentOrder = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(
                paymentOrder.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID,
                DemoActors.DEMO_EXHIBITOR_ID);
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

    @Test
    void dashboardIncludesPendingRegistrationReviewCountAndTopItems() {
        var before = dashboardService.getDashboard();
        var event = eventService.create(new CreateEventRequest("报名工作台测试展", 2026));
        var exhibitor = exhibitorService.create(new ExhibitorRequest(
                "待审展商", "CREDIT-" + java.util.UUID.randomUUID(), "王老板", "13800000000",
                "demo@example.com", "上海市", "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID);
        eventCategoryService.create(new EventCategoryRequest(event.id(), "SPORTS_EQUIPMENT", "体育用品", true, 10),
                DemoActors.DEMO_ADMIN_ID);
        var registration = registrationService.create(new RegistrationRequest(
                event.id(), exhibitor.id(), "SPORTS_EQUIPMENT", null, new BigDecimal("18.00"),
                "王老板", "13800000000", "报名参展"
        ), DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.submit(registration.id(), DemoActors.DEMO_EXHIBITOR_ID);

        var dashboard = dashboardService.getDashboard();

        assertThat(dashboard.pendingRegistrationReviewCount()).isEqualTo(before.pendingRegistrationReviewCount() + 1);
        assertThat(dashboard.pendingRegistrationReviews()).anyMatch(item -> item.id().equals(registration.id()));
    }
}
