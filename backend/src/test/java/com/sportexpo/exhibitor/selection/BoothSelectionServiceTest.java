package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.booth.BoothService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BoothSelectionServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired BoothSelectionService selectionService;
    @Autowired BoothService boothService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

    @Test
    void rejectsSelectionWhenAreaIsNotExactlyEqual() {
        var order = fixture.createPaidOrder("18.00", "1200.00", "STANDARD");
        var booth = fixture.createBooth(order.eventId(), "A001", "9.00", "STANDARD");

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(booth.id()))))
                .hasMessageContaining("Selected booth area must equal order area");
    }

    @Test
    void submitsSelectionAndLocksBoothsWhenAreaMatches() {
        var order = fixture.createPaidOrder("18.00", "1200.00", "STANDARD");
        var b1 = fixture.createBooth(order.eventId(), "A001", "9.00", "STANDARD");
        var b2 = fixture.createBooth(order.eventId(), "A002", "9.00", "STANDARD");

        var selection = selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(b2.id(), b1.id())));

        assertThat(selection.status()).isEqualTo(BusinessStatuses.SELECTION_PENDING_REVIEW);
        assertThat(selection.totalAreaSqm()).isEqualByComparingTo("18.00");
        assertThat(boothService.get(b1.id()).status()).isEqualTo(BusinessStatuses.BOOTH_LOCKED);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_PAUSED);
    }

    @Test
    void rejectsSelectionWhenBoothTypeDoesNotMatchOrderBoothType() {
        var order = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
        var booth = fixture.createBooth(order.eventId(), "A001", "9.00", "CUSTOM");

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(booth.id()))))
                .hasMessageContaining("Selected booth type must match order booth type");
    }

    @Test
    void rejectsDuplicateBoothIdsBeforeQueryingBooths() {
        var order = fixture.createPaidOrder("18.00", "1200.00", "STANDARD");
        var boothId = UUID.randomUUID();

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(boothId, boothId))))
                .hasMessageContaining("Selected booths must not contain duplicates");
    }

    @Test
    void concurrentSameBoothAllowsExactlyOneSelection() throws Exception {
        var event = fixture.createEvent();
        var firstOrder = fixture.createPaidOrderForEvent(event.id(), "9.00", "1200.00", "STANDARD");
        var secondOrder = fixture.createPaidOrderForEvent(event.id(), "9.00", "1200.00", "STANDARD");
        var booth = fixture.createBooth(event.id(), "A001", "9.00", "STANDARD");

        var firstRequest = new CreateBoothSelectionRequest(firstOrder.id(), firstOrder.exhibitorId(), List.of(booth.id()));
        var secondRequest = new CreateBoothSelectionRequest(secondOrder.id(), secondOrder.exhibitorId(), List.of(booth.id()));

        var result = submitConcurrently(List.of(firstRequest, secondRequest));

        assertThat(result.completed()).isTrue();
        assertThat(result.successes()).hasSize(1);
        assertThat(result.businessFailures()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void concurrentDifferentOrderWithReversedBoothIdsDoesNotDeadlock() throws Exception {
        var event = fixture.createEvent();
        var firstOrder = fixture.createPaidOrderForEvent(event.id(), "18.00", "1200.00", "STANDARD");
        var secondOrder = fixture.createPaidOrderForEvent(event.id(), "18.00", "1200.00", "STANDARD");
        var x = fixture.createBooth(event.id(), "X", "9.00", "STANDARD");
        var y = fixture.createBooth(event.id(), "Y", "9.00", "STANDARD");

        var firstRequest = new CreateBoothSelectionRequest(firstOrder.id(), firstOrder.exhibitorId(), List.of(x.id(), y.id()));
        var secondRequest = new CreateBoothSelectionRequest(secondOrder.id(), secondOrder.exhibitorId(), List.of(y.id(), x.id()));

        var result = submitConcurrently(List.of(firstRequest, secondRequest));

        assertThat(result.completed()).isTrue();
        assertThat(result.successes()).hasSize(1);
        assertThat(result.businessFailures()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    private ConcurrentSelectionResult submitConcurrently(List<CreateBoothSelectionRequest> requests) throws InterruptedException {
        var pool = Executors.newFixedThreadPool(requests.size());
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(requests.size());
        var successes = new CopyOnWriteArrayList<>();
        var businessFailures = new CopyOnWriteArrayList<BusinessException>();
        var errors = new CopyOnWriteArrayList<Throwable>();

        for (var request : requests) {
            pool.submit(() -> {
                try {
                    start.await();
                    successes.add(selectionService.submit(request));
                } catch (BusinessException error) {
                    businessFailures.add(error);
                } catch (Throwable error) {
                    errors.add(error);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        var completed = done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();
        return new ConcurrentSelectionResult(completed, successes, businessFailures, errors);
    }

    private record ConcurrentSelectionResult(boolean completed, List<?> successes,
                                             List<BusinessException> businessFailures, List<Throwable> errors) {
    }
}
