package com.sportexpo.exhibitor.eventcategory;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventCategoryService {
    private final EventCategoryRepository repository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public EventCategoryService(EventCategoryRepository repository, EventRepository eventRepository,
                                AuditService auditService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public EventCategoryResponse create(EventCategoryRequest request, UUID actorId) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        if (repository.existsByEventIdAndCode(request.eventId(), request.code())) {
            throw new BusinessException("Event category already exists");
        }
        var category = repository.save(new EventCategory(UUID.randomUUID(), request.eventId(), request.code(),
                request.name(), request.enabled(), request.sortOrder()));
        auditService.record(category.getEventId(), actorId, "EVENT_CATEGORY_CREATE", "EventCategory",
                category.getId(), null, null, "create event category");
        return toResponse(category);
    }

    @Transactional
    public EventCategoryResponse update(UUID id, EventCategoryRequest request, UUID actorId) {
        var category = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Event category does not exist"));
        category.update(request.code(), request.name(), request.enabled(), request.sortOrder());
        auditService.record(category.getEventId(), actorId, "EVENT_CATEGORY_UPDATE", "EventCategory",
                category.getId(), null, null, "update event category");
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<EventCategoryResponse> list(UUID eventId, Boolean enabled) {
        var categories = enabled == null
                ? repository.findByEventIdOrderBySortOrderAscNameAsc(eventId)
                : repository.findByEventIdAndEnabledOrderBySortOrderAscNameAsc(eventId, enabled);
        return categories.stream().map(this::toResponse).toList();
    }

    public boolean isEnabledForEvent(UUID eventId, String code) {
        return repository.findByEventIdAndCodeAndEnabled(eventId, code, true).isPresent();
    }

    private EventCategoryResponse toResponse(EventCategory category) {
        return new EventCategoryResponse(category.getId(), category.getEventId(), category.getCode(),
                category.getName(), category.isEnabled(), category.getSortOrder(), category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
