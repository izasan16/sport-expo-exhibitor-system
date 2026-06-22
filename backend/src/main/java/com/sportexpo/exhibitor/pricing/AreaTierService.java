package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.pricing.dto.AreaTierResponse;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AreaTierService {
    private final AreaTierRepository repository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public AreaTierService(AreaTierRepository repository, EventRepository eventRepository, AuditService auditService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AreaTierResponse create(CreateAreaTierRequest request, UUID actorId) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        if (repository.existsByEventIdAndZoneIdAndAreaSqm(request.eventId(), request.zoneId(), request.areaSqm())) {
            throw new BusinessException("Area tier already exists");
        }
        var areaTier = repository.save(new AreaTier(
                UUID.randomUUID(),
                request.eventId(),
                request.zoneId(),
                request.areaSqm(),
                request.enabled()
        ));
        auditService.record(areaTier.getEventId(), actorId, "AREA_TIER_CREATE", "AreaTier",
                areaTier.getId(), null, null, "create area tier");
        return toResponse(areaTier);
    }

    @Transactional(readOnly = true)
    public List<AreaTierResponse> list(UUID eventId) {
        return repository.findByEventIdOrderByAreaSqmAsc(eventId).stream().map(this::toResponse).toList();
    }

    private AreaTierResponse toResponse(AreaTier areaTier) {
        return new AreaTierResponse(
                areaTier.getId(),
                areaTier.getEventId(),
                areaTier.getZoneId(),
                areaTier.getAreaSqm(),
                areaTier.isEnabled()
        );
    }
}
