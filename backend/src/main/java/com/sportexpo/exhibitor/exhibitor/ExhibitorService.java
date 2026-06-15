package com.sportexpo.exhibitor.exhibitor;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExhibitorService {
    private final ExhibitorRepository repository;
    private final AuditService auditService;

    public ExhibitorService(ExhibitorRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public ExhibitorResponse create(ExhibitorRequest request, UUID actorId) {
        var status = request.status() == null || request.status().isBlank()
                ? BusinessStatuses.EXHIBITOR_ACTIVE
                : request.status();
        var exhibitor = repository.save(new Exhibitor(UUID.randomUUID(), request.companyName(), request.creditCode(),
                request.contactName(), request.contactPhone(), request.contactEmail(), request.address(), status, actorId));
        auditService.record(null, actorId, "EXHIBITOR_CREATE", "Exhibitor", exhibitor.getId(),
                null, status, "create exhibitor");
        return toResponse(exhibitor);
    }

    @Transactional
    public ExhibitorResponse update(UUID id, ExhibitorRequest request, UUID actorId) {
        var exhibitor = repository.findById(id).orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
        var oldStatus = exhibitor.getStatus();
        var status = request.status() == null || request.status().isBlank() ? oldStatus : request.status();
        exhibitor.update(request.companyName(), request.creditCode(), request.contactName(), request.contactPhone(),
                request.contactEmail(), request.address(), status, actorId);
        auditService.record(null, actorId, "EXHIBITOR_UPDATE", "Exhibitor", exhibitor.getId(),
                oldStatus, status, "update exhibitor");
        return toResponse(exhibitor);
    }

    @Transactional(readOnly = true)
    public ExhibitorResponse get(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
    }

    @Transactional(readOnly = true)
    public List<ExhibitorResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private ExhibitorResponse toResponse(Exhibitor exhibitor) {
        return new ExhibitorResponse(exhibitor.getId(), exhibitor.getCompanyName(), exhibitor.getCreditCode(),
                exhibitor.getContactName(), exhibitor.getContactPhone(), exhibitor.getContactEmail(),
                exhibitor.getAddress(), exhibitor.getStatus(), exhibitor.getCreatedAt(), exhibitor.getUpdatedAt());
    }
}
