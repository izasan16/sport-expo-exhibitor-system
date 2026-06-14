package com.sportexpo.exhibitor.order;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.contract.Contract;
import com.sportexpo.exhibitor.contract.ContractRepository;
import com.sportexpo.exhibitor.contract.ContractVersion;
import com.sportexpo.exhibitor.contract.ContractVersionRepository;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.notification.NotificationService;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.order.dto.BatchConfirmOrdersResponse;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.pricing.AreaTier;
import com.sportexpo.exhibitor.pricing.AreaTierRepository;
import com.sportexpo.exhibitor.pricing.PriceRule;
import com.sportexpo.exhibitor.pricing.PriceRuleRepository;
import com.sportexpo.exhibitor.selection.SelectionEligibility;
import com.sportexpo.exhibitor.selection.SelectionEligibilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ParticipationOrderService {
    private static final Logger logger = LoggerFactory.getLogger(ParticipationOrderService.class);
    private static final String GENERIC_BATCH_CONFIRM_FAILURE_REASON = "Order confirmation failed";
    private static final BigDecimal ZERO_AMOUNT = money(BigDecimal.ZERO);
    private static final DateTimeFormatter CONTRACT_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Map<String, Object> SALES_CONFIRM_SNAPSHOT = Map.of(
            "version", 1,
            "reason", "sales_confirm",
            "source", "offline_contract",
            "signMode", "offline_mark"
    );

    private final ParticipationOrderRepository repository;
    private final EventRepository eventRepository;
    private final AreaTierRepository areaTierRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final SelectionEligibilityRepository eligibilityRepository;
    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TransactionTemplate batchTransactionTemplate;

    public ParticipationOrderService(ParticipationOrderRepository repository,
                                     EventRepository eventRepository,
                                     AreaTierRepository areaTierRepository,
                                     PriceRuleRepository priceRuleRepository,
                                     SelectionEligibilityRepository eligibilityRepository,
                                     ContractRepository contractRepository,
                                     ContractVersionRepository contractVersionRepository,
                                     AuditService auditService,
                                     NotificationService notificationService,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.areaTierRepository = areaTierRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.contractRepository = contractRepository;
        this.contractVersionRepository = contractVersionRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.batchTransactionTemplate = new TransactionTemplate(transactionManager);
        this.batchTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public ParticipationOrderResponse create(CreateParticipationOrderRequest request) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        var areaTier = areaTierRepository.findById(request.areaTierId())
                .filter(tier -> tier.isEnabled() && tier.getEventId().equals(request.eventId()))
                .orElseThrow(() -> new BusinessException("Enabled area tier does not exist"));
        if (!Objects.equals(areaTier.getZoneId(), request.zoneId())) {
            throw new BusinessException("Area tier does not match order zone");
        }
        var priceRule = findEnabledPriceRule(request)
                .orElseThrow(() -> new BusinessException("Enabled price rule does not exist"));

        var now = OffsetDateTime.now();
        var areaFee = money(areaTier.getAreaSqm().multiply(priceRule.getUnitPrice()));
        var order = repository.save(new ParticipationOrder(
                UUID.randomUUID(),
                request.eventId(),
                request.exhibitorId(),
                null,
                request.zoneId(),
                areaTier.getId(),
                areaTier.getAreaSqm(),
                request.boothType(),
                BusinessStatuses.ORDER_PENDING_SALES_CONFIRM,
                areaFee,
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                areaFee,
                now,
                now,
                request.exhibitorId(),
                request.exhibitorId()
        ));
        eligibilityRepository.save(new SelectionEligibility(
                UUID.randomUUID(),
                order.getEventId(),
                order.getId(),
                BusinessStatuses.ELIGIBILITY_NOT_OPEN,
                null,
                now,
                now,
                request.exhibitorId(),
                request.exhibitorId()
        ));
        auditService.record(order.getEventId(), request.exhibitorId(), "ORDER_CREATE", "ParticipationOrder",
                order.getId(), null, order.getStatus(), "create participation order");
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<ParticipationOrderResponse> list(UUID eventId, UUID exhibitorId, String status) {
        return repository.findAll(orderFilters(eventId, exhibitorId, status), Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipationOrderResponse get(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
    }

    @Transactional
    public ParticipationOrderResponse confirmSales(UUID orderId, UUID actorId) {
        var order = repository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
        if (!BusinessStatuses.ORDER_PENDING_SALES_CONFIRM.equals(order.getStatus())) {
            throw new BusinessException("Order is not pending sales confirmation");
        }

        var now = OffsetDateTime.now();
        var oldOrderStatus = order.getStatus();
        order.changeStatus(BusinessStatuses.ORDER_PENDING_CONTRACT, now, actorId);
        repository.save(order);

        var contract = contractRepository.save(new Contract(
                UUID.randomUUID(),
                order.getEventId(),
                order.getId(),
                null,
                contractNo(order.getId(), now),
                BusinessStatuses.CONTRACT_GENERATED,
                now,
                now,
                actorId,
                actorId
        ));
        var version = contractVersionRepository.save(new ContractVersion(
                UUID.randomUUID(),
                order.getEventId(),
                contract.getId(),
                1,
                "sales_confirm",
                SALES_CONFIRM_SNAPSHOT,
                order.getTotalAmount(),
                order.getAreaSqm(),
                BusinessStatuses.CONTRACT_VERSION_UNSIGNED,
                null,
                null,
                null,
                now,
                actorId
        ));
        contract.assignCurrentVersion(version.getId(), now, actorId);
        contractRepository.save(contract);

        auditService.record(order.getEventId(), actorId, "ORDER_SALES_CONFIRM", "ParticipationOrder",
                order.getId(), oldOrderStatus, order.getStatus(), "sales confirm order");
        auditService.record(contract.getEventId(), actorId, "CONTRACT_GENERATE", "Contract",
                contract.getId(), null, contract.getStatus(), "generate contract");
        notificationService.create(order.getEventId(), "EXHIBITOR", order.getExhibitorId(),
                "Order sales confirmed", "Your participation order has been confirmed by sales.",
                NotificationTypes.ORDER_SALES_CONFIRMED);
        return toResponse(order);
    }

    public BatchConfirmOrdersResponse batchConfirmSales(List<UUID> orderIds, UUID actorId) {
        var failures = new ArrayList<BatchConfirmOrdersResponse.BatchFailure>();
        var successCount = 0;
        for (var orderId : orderIds == null ? List.<UUID>of() : orderIds) {
            try {
                batchTransactionTemplate.executeWithoutResult(
                        ignored -> confirmSales(orderId, actorId));
                successCount++;
            } catch (BusinessException exception) {
                failures.add(new BatchConfirmOrdersResponse.BatchFailure(orderId, exception.getMessage()));
            } catch (RuntimeException exception) {
                logger.warn("Unexpected error while batch confirming order {}", orderId, exception);
                failures.add(new BatchConfirmOrdersResponse.BatchFailure(
                        orderId, GENERIC_BATCH_CONFIRM_FAILURE_REASON));
            }
        }
        return new BatchConfirmOrdersResponse(successCount, failures.size(), failures);
    }

    private Optional<PriceRule> findEnabledPriceRule(CreateParticipationOrderRequest request) {
        if (request.zoneId() != null) {
            var zoneRule = priceRuleRepository.findFirstByEventIdAndZoneIdAndBoothTypeAndEnabledOrderByUpdatedAtDesc(
                    request.eventId(), request.zoneId(), request.boothType(), true);
            if (zoneRule.isPresent()) {
                return zoneRule;
            }
        }
        return priceRuleRepository.findFirstByEventIdAndZoneIdAndBoothTypeAndEnabledOrderByUpdatedAtDesc(
                request.eventId(), null, request.boothType(), true);
    }

    private Specification<ParticipationOrder> orderFilters(UUID eventId, UUID exhibitorId, String status) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (eventId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("eventId"), eventId));
            }
            if (exhibitorId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("exhibitorId"), exhibitorId));
            }
            if (status != null && !status.isBlank()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            return predicate;
        };
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String contractNo(UUID orderId, OffsetDateTime now) {
        return "CON-" + now.format(CONTRACT_NO_TIME) + "-" + orderId.toString().substring(0, 8);
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
}
