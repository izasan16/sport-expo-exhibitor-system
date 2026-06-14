package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.booth.Booth;
import com.sportexpo.exhibitor.booth.BoothRepository;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.order.ParticipationOrder;
import com.sportexpo.exhibitor.order.ParticipationOrderRepository;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.RejectBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.SelectionEligibilityResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class BoothSelectionService {
    private static final int MAX_REJECT_REASON_LENGTH = 500;
    private static final int MAX_OPTIONAL_TEXT_LENGTH = 500;

    private final ParticipationOrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final SelectionEligibilityRepository eligibilityRepository;
    private final BoothRepository boothRepository;
    private final BoothSelectionRepository selectionRepository;
    private final BoothLockRepository lockRepository;
    private final AuditService auditService;

    public BoothSelectionService(ParticipationOrderRepository orderRepository,
                                 EventRepository eventRepository,
                                 SelectionEligibilityRepository eligibilityRepository,
                                 BoothRepository boothRepository,
                                 BoothSelectionRepository selectionRepository,
                                 BoothLockRepository lockRepository,
                                 AuditService auditService) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.boothRepository = boothRepository;
        this.selectionRepository = selectionRepository;
        this.lockRepository = lockRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BoothSelectionResponse submit(CreateBoothSelectionRequest request) {
        var sortedBoothIds = validateAndSortBoothIds(request.boothIds());
        var exhibitorRemark = normalizeOptionalText(request.exhibitorRemark(),
                "Exhibitor remark must not exceed 500 characters");
        var order = orderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
        if (!order.getExhibitorId().equals(request.exhibitorId())) {
            throw new BusinessException("Order exhibitor does not match request exhibitor");
        }
        if (!BusinessStatuses.ORDER_PAID.equals(order.getStatus())) {
            throw new BusinessException("Order is not paid");
        }
        var eligibility = eligibilityRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
        if (!BusinessStatuses.ELIGIBILITY_AVAILABLE.equals(eligibility.getStatus())) {
            throw new BusinessException("Selection eligibility is not available");
        }
        if (selectionRepository.existsByOrderIdAndStatus(order.getId(), BusinessStatuses.SELECTION_PENDING_REVIEW)) {
            throw new BusinessException("Order already has pending booth selection");
        }

        var booths = boothRepository.findAllByIdInForUpdateOrderById(sortedBoothIds);
        if (booths.size() != sortedBoothIds.size()) {
            throw new BusinessException("Selected booths do not exist");
        }
        validateBooths(order, booths);

        var totalArea = booths.stream()
                .map(Booth::getAreaSqm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalArea.compareTo(order.getAreaSqm()) != 0) {
            throw new BusinessException("Selected booth area must equal order area");
        }

        var event = eventRepository.findById(order.getEventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        var now = OffsetDateTime.now();
        var lockExpiresAt = now.plusMinutes(event.getDefaultLockMinutes());
        var selection = selectionRepository.save(new BoothSelection(
                UUID.randomUUID(),
                order.getEventId(),
                order.getId(),
                order.getExhibitorId(),
                BusinessStatuses.SELECTION_PENDING_REVIEW,
                totalArea,
                now,
                lockExpiresAt,
                null,
                null,
                null,
                null,
                exhibitorRemark,
                null,
                now,
                now,
                request.exhibitorId(),
                request.exhibitorId()
        ));

        var locks = booths.stream()
                .map(booth -> new BoothLock(
                        UUID.randomUUID(),
                        order.getEventId(),
                        order.getId(),
                        selection.getId(),
                        booth.getId(),
                        BusinessStatuses.LOCK_ACTIVE,
                        lockExpiresAt,
                        now,
                        now,
                        request.exhibitorId(),
                        request.exhibitorId()
                ))
                .toList();
        lockRepository.saveAll(locks);

        booths.forEach(booth -> booth.markLocked(now, request.exhibitorId()));
        boothRepository.saveAll(booths);

        eligibility.markPaused(BusinessStatuses.PAUSE_SELECTION_PENDING, now);
        eligibilityRepository.save(eligibility);

        auditService.record(order.getEventId(), request.exhibitorId(), "BOOTH_SELECTION_SUBMIT",
                "BoothSelection", selection.getId(), null, selection.getStatus(), "submit booth selection");
        return toResponse(selection, sortedBoothIds);
    }

    @Transactional
    public BoothSelectionResponse approve(UUID selectionId, UUID actorId) {
        var selection = findPendingSelectionForUpdate(selectionId);
        var locks = lockRepository.findBySelectionId(selection.getId());
        var booths = lockBoothsForLocks(locks);
        booths.forEach(booth -> {
            if (!BusinessStatuses.BOOTH_LOCKED.equals(booth.getStatus())) {
                throw new BusinessException("Selected booth is not locked");
            }
        });

        var now = OffsetDateTime.now();
        selection.markConfirmed(actorId, now);
        locks.forEach(lock -> lock.markConfirmed(now, actorId));
        booths.forEach(booth -> booth.markConfirmed(now, actorId));
        var eligibility = eligibilityRepository.findByOrderId(selection.getOrderId())
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
        eligibility.markClosed(now);

        selectionRepository.save(selection);
        lockRepository.saveAll(locks);
        boothRepository.saveAll(booths);
        eligibilityRepository.save(eligibility);
        auditService.record(selection.getEventId(), actorId, "BOOTH_SELECTION_APPROVE",
                "BoothSelection", selection.getId(), BusinessStatuses.SELECTION_PENDING_REVIEW,
                selection.getStatus(), "approve booth selection");
        return toResponse(selection, locks.stream().map(BoothLock::getBoothId).sorted().toList());
    }

    @Transactional
    public BoothSelectionResponse reject(UUID selectionId, RejectBoothSelectionRequest request, UUID actorId) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException("Reject reason must not be blank");
        }
        var reason = request.reason().trim();
        if (reason.length() > MAX_REJECT_REASON_LENGTH) {
            throw new BusinessException("Reject reason must not exceed 500 characters");
        }
        var recommendedBooths = normalizeOptionalText(request.recommendedBooths(),
                "Recommended booths must not exceed 500 characters");
        var selection = findPendingSelectionForUpdate(selectionId);
        var locks = lockRepository.findBySelectionId(selection.getId());
        var booths = lockBoothsForLocks(locks);

        var now = OffsetDateTime.now();
        selection.markRejected(actorId, reason, recommendedBooths, now);
        locks.forEach(lock -> lock.markRejected(now, actorId));
        booths.forEach(booth -> booth.markAvailable(now, actorId));
        var eligibility = eligibilityRepository.findByOrderId(selection.getOrderId())
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
        eligibility.markAvailable(now);

        selectionRepository.save(selection);
        lockRepository.saveAll(locks);
        boothRepository.saveAll(booths);
        eligibilityRepository.save(eligibility);
        auditService.record(selection.getEventId(), actorId, "BOOTH_SELECTION_REJECT",
                "BoothSelection", selection.getId(), BusinessStatuses.SELECTION_PENDING_REVIEW,
                selection.getStatus(), reason);
        return toResponse(selection, locks.stream().map(BoothLock::getBoothId).sorted().toList());
    }

    @Transactional
    public BoothSelectionResponse timeoutRelease(UUID selectionId, UUID actorId) {
        var selection = findPendingSelectionForUpdate(selectionId);
        var now = OffsetDateTime.now();
        if (!now.isAfter(selection.getLockExpiresAt())) {
            throw new BusinessException("Selection lock has not expired");
        }
        var locks = lockRepository.findBySelectionId(selection.getId());
        var booths = lockBoothsForLocks(locks);

        var comment = "lock timeout released";
        selection.markTimeoutReleased(comment, actorId, now);
        locks.forEach(lock -> lock.markTimeoutReleased(now, actorId));
        booths.forEach(booth -> booth.markAvailable(now, actorId));
        var eligibility = eligibilityRepository.findByOrderId(selection.getOrderId())
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
        eligibility.markAvailable(now);

        selectionRepository.save(selection);
        lockRepository.saveAll(locks);
        boothRepository.saveAll(booths);
        eligibilityRepository.save(eligibility);
        auditService.record(selection.getEventId(), actorId, "BOOTH_SELECTION_TIMEOUT_RELEASE",
                "BoothSelection", selection.getId(), BusinessStatuses.SELECTION_PENDING_REVIEW,
                selection.getStatus(), comment);
        return toResponse(selection, locks.stream().map(BoothLock::getBoothId).sorted().toList());
    }

    @Transactional(readOnly = true)
    public SelectionEligibilityResponse getEligibility(UUID orderId) {
        return eligibilityRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
    }

    @Transactional(readOnly = true)
    public List<BoothSelectionResponse> list(UUID orderId, String status) {
        var selections = orderId == null
                ? selectionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : selectionRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return selections.stream()
                .filter(selection -> status == null || status.isBlank() || selection.getStatus().equals(status))
                .map(this::toResponse)
                .toList();
    }

    private List<UUID> validateAndSortBoothIds(List<UUID> boothIds) {
        if (boothIds == null || boothIds.isEmpty()) {
            throw new BusinessException("Selected booths must not be empty");
        }
        var uniqueIds = new HashSet<>(boothIds);
        if (uniqueIds.size() != boothIds.size()) {
            throw new BusinessException("Selected booths must not contain duplicates");
        }
        var sortedBoothIds = new ArrayList<>(boothIds);
        sortedBoothIds.sort(Comparator.naturalOrder());
        return sortedBoothIds;
    }

    private void validateBooths(ParticipationOrder order, List<Booth> booths) {
        for (var booth : booths) {
            if (!booth.getEventId().equals(order.getEventId())) {
                throw new BusinessException("Selected booth must belong to order event");
            }
            if (!BusinessStatuses.BOOTH_AVAILABLE.equals(booth.getStatus())) {
                throw new BusinessException("Selected booth is not available");
            }
            if (!booth.getBoothType().equals(order.getBoothType())) {
                throw new BusinessException("Selected booth type must match order booth type");
            }
        }
    }

    private BoothSelection findPendingSelectionForUpdate(UUID selectionId) {
        var selection = selectionRepository.findByIdForUpdate(selectionId)
                .orElseThrow(() -> new BusinessException("Booth selection does not exist"));
        if (!BusinessStatuses.SELECTION_PENDING_REVIEW.equals(selection.getStatus())) {
            throw new BusinessException("Booth selection is not pending review");
        }
        return selection;
    }

    private List<Booth> lockBoothsForLocks(List<BoothLock> locks) {
        var boothIds = locks.stream()
                .map(BoothLock::getBoothId)
                .sorted()
                .toList();
        var booths = boothRepository.findAllByIdInForUpdateOrderById(boothIds);
        if (booths.size() != boothIds.size()) {
            throw new BusinessException("Selected booths do not exist");
        }
        return booths;
    }

    private BoothSelectionResponse toResponse(BoothSelection selection) {
        var boothIds = lockRepository.findBySelectionId(selection.getId()).stream()
                .map(BoothLock::getBoothId)
                .sorted()
                .toList();
        return toResponse(selection, boothIds);
    }

    private BoothSelectionResponse toResponse(BoothSelection selection, List<UUID> boothIds) {
        return new BoothSelectionResponse(
                selection.getId(),
                selection.getEventId(),
                selection.getOrderId(),
                selection.getExhibitorId(),
                selection.getStatus(),
                selection.getTotalAreaSqm(),
                selection.getLockStartedAt(),
                selection.getLockExpiresAt(),
                selection.getHelperUserId(),
                selection.getReviewerUserId(),
                selection.getReviewedAt(),
                selection.getReviewComment(),
                selection.getExhibitorRemark(),
                selection.getRecommendedBooths(),
                boothIds,
                selection.getCreatedAt(),
                selection.getUpdatedAt(),
                selection.getCreatedBy(),
                selection.getUpdatedBy()
        );
    }

    private String normalizeOptionalText(String value, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var trimmed = value.trim();
        if (trimmed.length() > MAX_OPTIONAL_TEXT_LENGTH) {
            throw new BusinessException(tooLongMessage);
        }
        return trimmed;
    }

    private SelectionEligibilityResponse toResponse(SelectionEligibility eligibility) {
        return new SelectionEligibilityResponse(
                eligibility.getId(),
                eligibility.getEventId(),
                eligibility.getOrderId(),
                eligibility.getStatus(),
                eligibility.getPauseReason(),
                eligibility.getCreatedAt(),
                eligibility.getUpdatedAt(),
                eligibility.getCreatedBy(),
                eligibility.getUpdatedBy()
        );
    }
}
