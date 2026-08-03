package com.sportexpo.exhibitor.sales;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.exhibitor.ExhibitorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class SalesFollowUpService {
    private final SalesFollowUpRepository repository;
    private final ExhibitorRepository exhibitorRepository;
    private final AuditService auditService;

    public SalesFollowUpService(SalesFollowUpRepository repository,
                                ExhibitorRepository exhibitorRepository,
                                AuditService auditService) {
        this.repository = repository;
        this.exhibitorRepository = exhibitorRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SalesFollowUpResponse> list(UUID exhibitorId, String status) {
        var normalizedStatus = normalizeStatus(status);
        return repository.findByFilters(exhibitorId, normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesFollowUpResponse> listScoped(UUID exhibitorId, String status, Collection<UUID> allowedExhibitorIds) {
        if (allowedExhibitorIds == null || allowedExhibitorIds.isEmpty()) {
            return List.of();
        }
        var normalizedStatus = normalizeStatus(status);
        return repository.findByFiltersAndExhibitorIdIn(exhibitorId, normalizedStatus, allowedExhibitorIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SalesFollowUpResponse create(SalesFollowUpRequest request, UUID actorId) {
        exhibitorRepository.findById(request.exhibitorId())
                .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
        var now = OffsetDateTime.now();
        var followUp = repository.save(new SalesFollowUp(
                UUID.randomUUID(),
                request.exhibitorId(),
                actorId,
                request.method().trim(),
                request.content().trim(),
                request.nextFollowUpAt(),
                request.status().trim(),
                now,
                now,
                actorId,
                actorId
        ));
        auditService.record(null, actorId, "SALES_FOLLOW_UP_CREATE", "SalesFollowUp",
                followUp.getId(), null, followUp.getStatus(), "create sales follow up");
        return toResponse(followUp);
    }

    @Transactional
    public SalesFollowUpResponse update(UUID followUpId, SalesFollowUpRequest request, UUID actorId, boolean adminMode) {
        var followUp = repository.findByIdForUpdate(followUpId)
                .orElseThrow(() -> new BusinessException("Sales follow up does not exist"));
        var now = OffsetDateTime.now();
        var oldStatus = followUp.getStatus();

        if (adminMode) {
            followUp.updateStatusByAdmin(request.status().trim(), actorId, now);
            auditService.record(null, actorId, "SALES_FOLLOW_UP_ADMIN_UPDATE", "SalesFollowUp",
                    followUp.getId(), oldStatus, followUp.getStatus(), "admin update follow up status");
        } else {
            if (!followUp.getSalesUserId().equals(actorId)) {
                throw new AccessDeniedException("Access denied for sales follow up");
            }
            if (!followUp.getExhibitorId().equals(request.exhibitorId())) {
                throw new BusinessException("Sales follow up exhibitor cannot be changed");
            }
            followUp.updateBySales(
                    request.method().trim(),
                    request.content().trim(),
                    request.nextFollowUpAt(),
                    request.status().trim(),
                    actorId,
                    now
            );
            auditService.record(null, actorId, "SALES_FOLLOW_UP_UPDATE", "SalesFollowUp",
                    followUp.getId(), oldStatus, followUp.getStatus(), "sales update follow up");
        }
        return toResponse(followUp);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim();
    }

    private SalesFollowUpResponse toResponse(SalesFollowUp followUp) {
        return new SalesFollowUpResponse(
                followUp.getId(),
                followUp.getExhibitorId(),
                followUp.getSalesUserId(),
                followUp.getMethod(),
                followUp.getContent(),
                followUp.getNextFollowUpAt(),
                followUp.getStatus(),
                followUp.getCreatedAt(),
                followUp.getUpdatedAt()
        );
    }
}
