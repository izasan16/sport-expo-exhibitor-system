package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.booth.BoothService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.RejectBoothSelectionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class BoothSelectionServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired BoothSelectionService selectionService;
    @Autowired BoothService boothService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;
    @Autowired BoothLockRepository lockRepository;
    @Autowired EventRepository eventRepository;
    @Autowired MockMvc mockMvc;

    @Test
    void rejectsEmptyBoothIds() {
        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                UUID.randomUUID(), DemoActors.DEMO_EXHIBITOR_ID, List.of())))
                .hasMessageContaining("Selected booths must not be empty");
    }

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
        var defaultLockMinutes = eventRepository.findById(order.eventId()).orElseThrow().getDefaultLockMinutes();

        var beforeSubmit = java.time.OffsetDateTime.now();
        var selection = selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(b2.id(), b1.id())));
        var afterSubmit = java.time.OffsetDateTime.now();

        assertThat(selection.status()).isEqualTo(BusinessStatuses.SELECTION_PENDING_REVIEW);
        assertThat(selection.totalAreaSqm()).isEqualByComparingTo("18.00");
        assertThat(selection.boothIds()).containsExactlyElementsOf(
                List.of(b1.id(), b2.id()).stream().sorted().toList());
        assertThat(selection.lockExpiresAt())
                .isAfterOrEqualTo(beforeSubmit.plusMinutes(defaultLockMinutes))
                .isBeforeOrEqualTo(afterSubmit.plusMinutes(defaultLockMinutes));
        assertThat(boothService.get(b1.id()).status()).isEqualTo(BusinessStatuses.BOOTH_LOCKED);
        assertThat(boothService.get(b2.id()).status()).isEqualTo(BusinessStatuses.BOOTH_LOCKED);
        assertThat(lockRepository.findBySelectionId(selection.id()))
                .hasSize(2)
                .allSatisfy(lock -> {
                    assertThat(lock.getStatus()).isEqualTo(BusinessStatuses.LOCK_ACTIVE);
                    assertThat(lock.getLockExpiresAt().toInstant())
                            .isBetween(
                                    selection.lockExpiresAt().minusSeconds(1).toInstant(),
                                    selection.lockExpiresAt().plusSeconds(1).toInstant()
                            );
                })
                .extracting(BoothLock::getBoothId)
                .containsExactlyInAnyOrder(b1.id(), b2.id());
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
    void rejectsNotAvailableLockedBooth() {
        var firstOrder = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
        var secondOrder = fixture.createPaidOrderForEvent(firstOrder.eventId(), "9.00", "1200.00", "STANDARD");
        var booth = fixture.createBooth(firstOrder.eventId(), "A001", "9.00", "STANDARD");
        selectionService.submit(new CreateBoothSelectionRequest(
                firstOrder.id(), firstOrder.exhibitorId(), List.of(booth.id())));

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                secondOrder.id(), secondOrder.exhibitorId(), List.of(booth.id()))))
                .hasMessageContaining("Selected booth is not available");
    }

    @Test
    void rejectsBoothFromAnotherEvent() {
        var order = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
        var otherEvent = fixture.createEvent();
        var booth = fixture.createBooth(otherEvent.id(), "A001", "9.00", "STANDARD");

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(booth.id()))))
                .hasMessageContaining("Selected booth must belong to order event");
    }

    @Test
    void rejectsDuplicateBoothIdsBeforeQueryingBooths() {
        var boothId = UUID.randomUUID();

        assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
                UUID.randomUUID(), DemoActors.DEMO_EXHIBITOR_ID, List.of(boothId, boothId))))
                .hasMessageContaining("Selected booths must not contain duplicates");
    }

    @Test
    void publicSubmitUsesDemoExhibitorIdentity() throws Exception {
        var order = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
        var booth = fixture.createBooth(order.eventId(), "A001", "9.00", "STANDARD");
        var attackerExhibitorId = UUID.randomUUID();

        mockMvc.perform(post("/api/booth-selections")
                        .contentType("application/json")
                        .content("""
                                {
                                  "orderId": "%s",
                                  "exhibitorId": "%s",
                                  "boothIds": ["%s"]
                                }
                                """.formatted(order.id(), attackerExhibitorId, booth.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exhibitorId").value(DemoActors.DEMO_EXHIBITOR_ID.toString()))
                .andExpect(jsonPath("$.data.createdBy").value(DemoActors.DEMO_EXHIBITOR_ID.toString()));
    }

    @Test
    void approveSelectionConfirmsBoothsAndClosesEligibility() {
        var pending = fixture.createPendingSelection("9.00", "STANDARD");
        var boothId = pending.boothIds().get(0);

        var approved = selectionService.approve(pending.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(approved.status()).isEqualTo(BusinessStatuses.SELECTION_CONFIRMED);
        assertThat(approved.reviewerUserId()).isEqualTo(DemoActors.DEMO_ADMIN_ID);
        assertThat(approved.reviewedAt()).isNotNull();
        assertThat(approved.reviewComment()).isNull();
        assertThat(lockRepository.findBySelectionId(pending.id()))
                .hasSize(1)
                .allSatisfy(lock -> assertThat(lock.getStatus()).isEqualTo(BusinessStatuses.LOCK_CONFIRMED));
        assertThat(boothService.get(boothId).status()).isEqualTo(BusinessStatuses.BOOTH_CONFIRMED);
        var eligibility = eligibilityRepository.findByOrderId(pending.orderId()).orElseThrow();
        assertThat(eligibility.getStatus()).isEqualTo(BusinessStatuses.ELIGIBILITY_CLOSED);
        assertThat(eligibility.getPauseReason()).isNull();
    }

    @Test
    void rejectSelectionReleasesBoothsAndReopensEligibility() {
        var pending = fixture.createPendingSelection("9.00", "STANDARD");
        var boothId = pending.boothIds().get(0);

        var rejected = selectionService.reject(
                pending.id(), new RejectBoothSelectionRequest("booth not suitable"), DemoActors.DEMO_ADMIN_ID);

        assertThat(rejected.status()).isEqualTo(BusinessStatuses.SELECTION_REJECTED);
        assertThat(rejected.reviewerUserId()).isEqualTo(DemoActors.DEMO_ADMIN_ID);
        assertThat(rejected.reviewedAt()).isNotNull();
        assertThat(rejected.reviewComment()).isEqualTo("booth not suitable");
        assertThat(lockRepository.findBySelectionId(pending.id()))
                .hasSize(1)
                .allSatisfy(lock -> assertThat(lock.getStatus()).isEqualTo(BusinessStatuses.LOCK_REJECTED));
        assertThat(boothService.get(boothId).status()).isEqualTo(BusinessStatuses.BOOTH_AVAILABLE);
        var eligibility = eligibilityRepository.findByOrderId(pending.orderId()).orElseThrow();
        assertThat(eligibility.getStatus()).isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
        assertThat(eligibility.getPauseReason()).isNull();
    }

    @Test
    void rejectsRejectReasonLongerThanReviewCommentColumnAfterTrim() {
        var pending = fixture.createPendingSelection("9.00", "STANDARD");
        var tooLongReason = " " + "x".repeat(501) + " ";

        assertThatThrownBy(() -> selectionService.reject(
                pending.id(), new RejectBoothSelectionRequest(tooLongReason), DemoActors.DEMO_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reject reason must not exceed 500 characters");
    }

    @Test
    void timeoutReleaseReleasesBoothsAndReopensEligibility() {
        var pending = fixture.createExpiredPendingSelection("9.00", "STANDARD");
        var boothId = pending.boothIds().get(0);

        var released = selectionService.timeoutRelease(pending.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(released.status()).isEqualTo(BusinessStatuses.SELECTION_TIMEOUT);
        assertThat(released.reviewerUserId()).isNull();
        assertThat(released.reviewedAt()).isNotNull();
        assertThat(released.reviewComment()).isEqualTo("lock timeout released");
        assertThat(lockRepository.findBySelectionId(pending.id()))
                .hasSize(1)
                .allSatisfy(lock -> assertThat(lock.getStatus()).isEqualTo(BusinessStatuses.LOCK_TIMEOUT_RELEASED));
        assertThat(boothService.get(boothId).status()).isEqualTo(BusinessStatuses.BOOTH_AVAILABLE);
        var eligibility = eligibilityRepository.findByOrderId(pending.orderId()).orElseThrow();
        assertThat(eligibility.getStatus()).isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
        assertThat(eligibility.getPauseReason()).isNull();
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
        assertThat(result.businessFailures())
                .hasSize(1)
                .first()
                .extracting(Throwable::getMessage)
                .isEqualTo("Selected booth is not available");
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
