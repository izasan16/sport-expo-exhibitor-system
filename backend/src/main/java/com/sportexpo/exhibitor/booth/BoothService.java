package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.booth.dto.BoothResponse;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.event.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BoothService {
    private final BoothRepository repository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public BoothService(BoothRepository repository, EventRepository eventRepository, AuditService auditService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BoothResponse create(CreateBoothRequest request) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        var now = OffsetDateTime.now();
        var booth = repository.save(new Booth(
                UUID.randomUUID(),
                request.eventId(),
                request.boothNo(),
                request.areaSqm(),
                request.boothType(),
                BusinessStatuses.BOOTH_AVAILABLE,
                now,
                now
        ));
        auditService.record(booth.getEventId(), null, "BOOTH_CREATE", "Booth", booth.getId(), null,
                BusinessStatuses.BOOTH_AVAILABLE, "create booth");
        return toResponse(booth);
    }

    @Transactional(readOnly = true)
    public List<BoothResponse> list(UUID eventId) {
        return list(eventId, null);
    }

    @Transactional(readOnly = true)
    public List<BoothResponse> list(UUID eventId, String status) {
        var booths = (status == null || status.isBlank())
                ? repository.findByEventId(eventId)
                : repository.findByEventIdAndStatus(eventId, status);
        return booths.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BoothResponse get(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Booth does not exist"));
    }

    private BoothResponse toResponse(Booth booth) {
        return new BoothResponse(
                booth.getId(),
                booth.getEventId(),
                booth.getBoothNo(),
                booth.getAreaSqm(),
                booth.getBoothType(),
                booth.getStatus()
        );
    }
}
