package com.sportexpo.exhibitor.admin;

import com.sportexpo.exhibitor.admin.dto.AdminDashboardResponse;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.order.ParticipationOrder;
import com.sportexpo.exhibitor.order.ParticipationOrderRepository;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.Payment;
import com.sportexpo.exhibitor.payment.PaymentRepository;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
import com.sportexpo.exhibitor.selection.BoothLock;
import com.sportexpo.exhibitor.selection.BoothLockRepository;
import com.sportexpo.exhibitor.selection.BoothSelection;
import com.sportexpo.exhibitor.selection.BoothSelectionRepository;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminDashboardService {
    private final ParticipationOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BoothSelectionRepository selectionRepository;
    private final BoothLockRepository lockRepository;

    public AdminDashboardService(ParticipationOrderRepository orderRepository, PaymentRepository paymentRepository,
                                 BoothSelectionRepository selectionRepository, BoothLockRepository lockRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.selectionRepository = selectionRepository;
        this.lockRepository = lockRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        var generatedAt = OffsetDateTime.now();
        var todayStart = generatedAt.toLocalDate().atStartOfDay().atOffset(generatedAt.getOffset());
        var tomorrowStart = todayStart.plusDays(1);

        return new AdminDashboardResponse(
                generatedAt,
                orderRepository.countByStatus(BusinessStatuses.ORDER_PENDING_SALES_CONFIRM),
                paymentRepository.countByStatus(BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM),
                selectionRepository.countByStatus(BusinessStatuses.SELECTION_PENDING_REVIEW),
                orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(todayStart, tomorrowStart),
                orderRepository.findTop5ByStatusOrderByCreatedAtDesc(BusinessStatuses.ORDER_PENDING_SALES_CONFIRM)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                paymentRepository.findTop5ByStatusOrderByCreatedAtDesc(BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                selectionRepository.findTop5ByStatusOrderByCreatedAtDesc(BusinessStatuses.SELECTION_PENDING_REVIEW)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private ParticipationOrderResponse toResponse(ParticipationOrder order) {
        return new ParticipationOrderResponse(
                order.getId(),
                order.getEventId(),
                order.getExhibitorId(),
                order.getZoneId(),
                order.getAreaTierId(),
                order.getAreaSqm(),
                order.getBoothType(),
                order.getStatus(),
                order.getAreaFee(),
                order.getServiceFee(),
                order.getDiscountAmount(),
                order.getAdjustmentAmount(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getEventId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getFailureReason(),
                payment.getConfirmedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCreatedBy(),
                payment.getUpdatedBy()
        );
    }

    private BoothSelectionResponse toResponse(BoothSelection selection) {
        var boothIds = lockRepository.findBySelectionId(selection.getId()).stream()
                .map(BoothLock::getBoothId)
                .sorted()
                .toList();
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
}
