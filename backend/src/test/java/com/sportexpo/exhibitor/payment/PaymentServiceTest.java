package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
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

@SpringBootTest
class PaymentServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired PaymentService paymentService;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

    @Test
    void partialPaymentKeepsEligibilityClosedAndFullPaymentOpensSelection() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");

        var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"));
        paymentService.confirm(p1.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PARTIALLY_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_NOT_OPEN);

        var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"));
        paymentService.confirm(p2.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
    }

    @Test
    void concurrentFinanceConfirmationsConvergeToPaid() throws Exception {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
        var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"));
        var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"));

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
    }
}
