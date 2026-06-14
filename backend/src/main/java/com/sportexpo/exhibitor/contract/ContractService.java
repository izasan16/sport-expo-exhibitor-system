package com.sportexpo.exhibitor.contract;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.contract.dto.ContractResponse;
import com.sportexpo.exhibitor.contract.dto.ContractVersionResponse;
import com.sportexpo.exhibitor.notification.NotificationService;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.order.ParticipationOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {
    private final ContractRepository contractRepository;
    private final ContractVersionRepository versionRepository;
    private final ParticipationOrderRepository orderRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ContractService(ContractRepository contractRepository, ContractVersionRepository versionRepository,
                           ParticipationOrderRepository orderRepository, AuditService auditService,
                           NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.versionRepository = versionRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> list(UUID orderId) {
        var contracts = orderId == null
                ? contractRepository.findAllByOrderByCreatedAtDesc()
                : contractRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return contracts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ContractVersionResponse> listVersions(UUID contractId) {
        return versionRepository.findByContractIdOrderByVersionNoAsc(contractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ContractResponse markOfflineSigned(UUID contractId, UUID actorId) {
        var contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Contract does not exist"));
        if (!BusinessStatuses.CONTRACT_GENERATED.equals(contract.getStatus())) {
            throw new BusinessException("Contract is not generated");
        }

        var order = orderRepository.findByIdForUpdate(contract.getOrderId())
                .orElseThrow(() -> new BusinessException("Participation order does not exist"));
        if (!BusinessStatuses.ORDER_PENDING_CONTRACT.equals(order.getStatus())) {
            throw new BusinessException("Order is not pending contract");
        }

        var version = versionRepository.findById(contract.getCurrentVersionId())
                .orElseThrow(() -> new BusinessException("Current contract version does not exist"));
        var now = OffsetDateTime.now();
        contract.changeStatus(BusinessStatuses.CONTRACT_EFFECTIVE, now, actorId);
        version.markSigned(actorId, now, BusinessStatuses.CONTRACT_VERSION_MARKED_SIGNED);
        order.changeStatus(BusinessStatuses.ORDER_PENDING_PAYMENT, now, actorId);

        contractRepository.save(contract);
        versionRepository.save(version);
        orderRepository.save(order);
        auditService.record(contract.getEventId(), actorId, "CONTRACT_MARK_SIGNED", "Contract",
                contract.getId(), BusinessStatuses.CONTRACT_GENERATED, contract.getStatus(), "mark offline signed");
        auditService.record(order.getEventId(), actorId, "ORDER_CONTRACT_EFFECTIVE", "ParticipationOrder",
                order.getId(), BusinessStatuses.ORDER_PENDING_CONTRACT, order.getStatus(), "contract effective");
        notificationService.create(contract.getEventId(), "EXHIBITOR", order.getExhibitorId(),
                "Contract signed", "Your offline contract has been marked as signed.",
                NotificationTypes.CONTRACT_SIGNED);
        return toResponse(contract);
    }

    private ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getEventId(),
                contract.getOrderId(),
                contract.getCurrentVersionId(),
                contract.getContractNo(),
                contract.getStatus(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getCreatedBy(),
                contract.getUpdatedBy()
        );
    }

    private ContractVersionResponse toResponse(ContractVersion version) {
        return new ContractVersionResponse(
                version.getId(),
                version.getEventId(),
                version.getContractId(),
                version.getVersionNo(),
                version.getReason(),
                version.getContentSnapshot(),
                version.getAmount(),
                version.getAreaSqm(),
                version.getSignStatus(),
                version.getSignerId(),
                version.getSignedAt(),
                version.getFileId(),
                version.getCreatedAt(),
                version.getCreatedBy()
        );
    }
}
