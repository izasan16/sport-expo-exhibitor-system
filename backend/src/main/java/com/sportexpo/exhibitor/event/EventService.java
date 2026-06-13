package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.event.dto.EventResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository repository;
    private final AuditService auditService;

    public EventService(EventRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        var now = OffsetDateTime.now();
        var event = repository.save(new Event(UUID.randomUUID(), request.name(), request.year(), "DRAFT", 1440, now, now));
        auditService.record(event.getId(), null, "EVENT_CREATE", "Event", event.getId(), null, "DRAFT", "create event");
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getYear(), event.getStatus());
    }
}
