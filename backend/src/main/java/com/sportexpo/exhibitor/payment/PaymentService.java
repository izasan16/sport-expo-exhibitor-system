package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.notification.NotificationService;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.order.ParticipationOrder;
import com.sportexpo.exhibitor.order.ParticipationOrderRepository;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
import com.sportexpo.exhibitor.selection.SelectionEligibility;
import com.sportexpo.exhibitor.selection.SelectionEligibilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private static final BigDecimal ZERO_AMOUNT = money(BigDecimal.ZERO);
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final PaymentRepository paymentRepository;
    private final ParticipationOrderRepository orderRepository;
    private final SelectionEligibilityRepository eligibilityRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository, ParticipationOrderRepository orderRepository,
                          SelectionEligibilityRepository eligibilityRepository, AuditService auditService,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, UUID actorId) {
        var order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
        if (!BusinessStatuses.ORDER_PENDING_PAYMENT.equals(order.getStatus())
                && !BusinessStatuses.ORDER_PARTIALLY_PAID.equals(order.getStatus())) {
            throw new BusinessException("Order is not pending payment");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (request.amount().stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Payment amount must have at most 2 decimal places");
        }

        var now = OffsetDateTime.now();
        var payment = paymentRepository.save(new Payment(
                UUID.randomUUID(),
                order.getEventId(),
                order.getId(),
                money(request.amount()),
                BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM,
                request.method(),
                null,
                null,
                now,
                now,
                actorId,
                actorId
        ));
        auditService.record(payment.getEventId(), actorId, "PAYMENT_CREATE", "Payment",
                payment.getId(), null, payment.getStatus(), "create payment");
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID orderId, String status) {
        var payments = findPayments(orderId, status);
        return payments.stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse confirm(UUID paymentId, UUID actorId) {
        var orderId = paymentRepository.findOrderIdByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException("Payment does not exist"));
        var order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
        var payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException("Payment does not exist"));

        if (!payment.getOrderId().equals(order.getId())) {
            throw new BusinessException("Payment does not belong to locked order");
        }
        if (!BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM.equals(payment.getStatus())) {
            throw new BusinessException("Payment is not pending finance confirmation");
        }

        var now = OffsetDateTime.now();
        var oldPaymentStatus = payment.getStatus();
        payment.markConfirmed(actorId, now);
        paymentRepository.save(payment);

        var confirmedAmount = paymentRepository.findByOrderIdAndStatus(order.getId(), BusinessStatuses.PAYMENT_CONFIRMED)
                .stream()
                .map(Payment::getAmount)
                .reduce(ZERO_AMOUNT, BigDecimal::add);
        updateOrderAndEligibility(order, confirmedAmount, actorId, now);

        auditService.record(payment.getEventId(), actorId, "PAYMENT_CONFIRM", "Payment",
                payment.getId(), oldPaymentStatus, payment.getStatus(), "confirm payment");
        notificationService.create(payment.getEventId(), "EXHIBITOR", order.getExhibitorId(),
                "付款已确认", "您的付款已由财务确认，可以继续办理后续参展流程。",
                NotificationTypes.PAYMENT_CONFIRMED, null, null, order.getId());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse fail(UUID paymentId, UUID actorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Payment failure reason must not be blank");
        }
        var trimmedReason = reason.trim();
        if (trimmedReason.length() > MAX_FAILURE_REASON_LENGTH) {
            throw new BusinessException("Payment failure reason must not exceed 500 characters");
        }
        var payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException("Payment does not exist"));
        if (!BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM.equals(payment.getStatus())) {
            throw new BusinessException("Payment is not pending finance confirmation");
        }
        var order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));

        var now = OffsetDateTime.now();
        var oldPaymentStatus = payment.getStatus();
        payment.markFailed(actorId, trimmedReason, now);
        paymentRepository.save(payment);
        notificationService.create(payment.getEventId(), "EXHIBITOR", order.getExhibitorId(),
                "付款确认失败", trimmedReason, NotificationTypes.PAYMENT_FAILED, null, null, order.getId());
        auditService.record(payment.getEventId(), actorId, "PAYMENT_FAIL", "Payment",
                payment.getId(), oldPaymentStatus, payment.getStatus(), trimmedReason);
        return toResponse(payment);
    }

    private List<Payment> findPayments(UUID orderId, String status) {
        var normalizedStatus = status == null || status.isBlank() ? null : status;
        if (orderId != null && normalizedStatus != null) {
            return paymentRepository.findByOrderIdAndStatus(orderId, normalizedStatus);
        }
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId);
        }
        if (normalizedStatus != null) {
            return paymentRepository.findByStatus(normalizedStatus);
        }
        return paymentRepository.findAll();
    }

    private void updateOrderAndEligibility(ParticipationOrder order, BigDecimal confirmedAmount,
                                           UUID actorId, OffsetDateTime now) {
        var eligibility = eligibilityRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));
        if (confirmedAmount.compareTo(order.getTotalAmount()) >= 0) {
            order.changeStatus(BusinessStatuses.ORDER_PAID, now, actorId);
            if (BusinessStatuses.ELIGIBILITY_NOT_OPEN.equals(eligibility.getStatus())) {
                eligibility.markAvailable(now);
            }
        } else if (confirmedAmount.compareTo(ZERO_AMOUNT) > 0) {
            order.changeStatus(BusinessStatuses.ORDER_PARTIALLY_PAID, now, actorId);
            if (BusinessStatuses.ELIGIBILITY_NOT_OPEN.equals(eligibility.getStatus())) {
                eligibility.markNotOpen(now);
            }
        } else {
            order.changeStatus(BusinessStatuses.ORDER_PENDING_PAYMENT, now, actorId);
            if (BusinessStatuses.ELIGIBILITY_NOT_OPEN.equals(eligibility.getStatus())) {
                eligibility.markNotOpen(now);
            }
        }
        orderRepository.save(order);
        eligibilityRepository.save(eligibility);
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

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
