package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.notification.NotificationRepository;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.selection.SelectionEligibilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

    @Test
    void partialPaymentKeepsEligibilityClosedAndFullPaymentOpensSelection() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");

        var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        paymentService.confirm(p1.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PARTIALLY_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_NOT_OPEN);

        var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        paymentService.confirm(p2.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
    }

    @Test
    void confirmCreatesChineseExhibitorNotification() {
        var order = fixture.createPendingPaymentOrder("9.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10800.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);

        paymentService.confirm(payment.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(order.exhibitorId()))
                .filteredOn(notification -> notification.getType().equals(NotificationTypes.PAYMENT_CONFIRMED))
                .filteredOn(notification -> order.id().equals(notification.getRelatedOrderId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationTypes.PAYMENT_CONFIRMED);
                    assertThat(notification.getTitle()).isEqualTo("付款已确认");
                    assertThat(notification.getContent()).isEqualTo("您的付款已由财务确认，可以继续办理后续参展流程。");
                    assertThat(notification.getRelatedOrderId()).isEqualTo(order.id());
                });
    }

    @Test
    void concurrentFinanceConfirmationsConvergeToPaid() throws Exception {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);

        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(2);
        var errors = new CopyOnWriteArrayList<Throwable>();

        for (var paymentId : List.of(p1.id(), p2.id())) {
            pool.submit(() -> {
                try {
                    start.await();
                    paymentService.confirm(paymentId, DemoActors.DEMO_ADMIN_ID);
                } catch (Throwable error) {
                    errors.add(error);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(errors).isEmpty();
        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
        assertThat(paymentRepository.findByOrderIdAndStatus(order.id(), BusinessStatuses.PAYMENT_CONFIRMED))
                .hasSize(2);
    }

    @Test
    void extraPaymentDoesNotReopenPausedEligibility() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var fullPayment = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        var extraPayment = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("100.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);

        paymentService.confirm(fullPayment.id(), DemoActors.DEMO_ADMIN_ID);
        var eligibility = eligibilityRepository.findByOrderId(order.id()).orElseThrow();
        eligibility.markPaused(BusinessStatuses.PAUSE_SELECTION_PENDING, java.time.OffsetDateTime.now());
        eligibilityRepository.save(eligibility);

        paymentService.confirm(extraPayment.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_PAUSED);
    }

    @Test
    void extraPaymentDoesNotReopenClosedEligibility() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var fullPayment = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        var extraPayment = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("100.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);

        paymentService.confirm(fullPayment.id(), DemoActors.DEMO_ADMIN_ID);
        var eligibility = eligibilityRepository.findByOrderId(order.id()).orElseThrow();
        eligibility.markClosed(java.time.OffsetDateTime.now());
        eligibilityRepository.save(eligibility);

        paymentService.confirm(extraPayment.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_CLOSED);
    }

    @Test
    void createRejectsPaymentAmountsWithMoreThanTwoDecimalPlaces() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");

        assertThatThrownBy(() -> paymentService.create(new CreatePaymentRequest(
                order.id(), new BigDecimal("0.004"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment amount must have at most 2 decimal places");
        assertThatThrownBy(() -> paymentService.create(new CreatePaymentRequest(
                order.id(), new BigDecimal("21599.995"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment amount must have at most 2 decimal places");
    }

    @Test
    void failRequiresAndPersistsFailureReason() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(
                order.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);

        var failed = paymentService.fail(payment.id(), DemoActors.DEMO_ADMIN_ID, "户名不符");

        assertThat(failed.status()).isEqualTo(BusinessStatuses.PAYMENT_CONFIRM_FAILED);
        assertThat(failed.failureReason()).isEqualTo("户名不符");
        assertThat(paymentRepository.findById(payment.id()).orElseThrow().getFailureReason())
                .isEqualTo("户名不符");
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(order.exhibitorId()))
                .filteredOn(notification -> notification.getType().equals(NotificationTypes.PAYMENT_FAILED))
                .filteredOn(notification -> order.id().equals(notification.getRelatedOrderId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getEventId()).isEqualTo(order.eventId());
                    assertThat(notification.getType()).isEqualTo(NotificationTypes.PAYMENT_FAILED);
                    assertThat(notification.getTitle()).isEqualTo("付款确认失败");
                    assertThat(notification.getContent()).isEqualTo("户名不符");
                    assertThat(notification.getRelatedOrderId()).isEqualTo(order.id());
                });

        var otherPayment = paymentService.create(new CreatePaymentRequest(
                order.id(), new BigDecimal("1.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        assertThatThrownBy(() -> paymentService.fail(otherPayment.id(), DemoActors.DEMO_ADMIN_ID, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment failure reason must not be blank");
    }

    @Test
    void failRejectsFailureReasonLongerThanColumnAfterTrim() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var payment = paymentService.create(new CreatePaymentRequest(
                order.id(), new BigDecimal("21600.00"), "OFFLINE_TRANSFER"), DemoActors.DEMO_EXHIBITOR_ID);
        var tooLongReason = " " + "x".repeat(501) + " ";

        assertThatThrownBy(() -> paymentService.fail(payment.id(), DemoActors.DEMO_ADMIN_ID, tooLongReason))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment failure reason must not exceed 500 characters");
    }
}
