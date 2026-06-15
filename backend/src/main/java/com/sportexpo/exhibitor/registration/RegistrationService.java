package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.eventcategory.EventCategoryRepository;
import com.sportexpo.exhibitor.exhibitor.ExhibitorRepository;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RegistrationService {
    private static final Set<String> IN_PROGRESS_STATUSES = Set.of(
            BusinessStatuses.REGISTRATION_DRAFT,
            BusinessStatuses.REGISTRATION_SUBMITTED
    );

    private final RegistrationRepository repository;
    private final ExhibitorRepository exhibitorRepository;
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final AuditService auditService;

    public RegistrationService(RegistrationRepository repository, ExhibitorRepository exhibitorRepository,
                               EventRepository eventRepository, EventCategoryRepository eventCategoryRepository,
                               AuditService auditService) {
        this.repository = repository;
        this.exhibitorRepository = exhibitorRepository;
        this.eventRepository = eventRepository;
        this.eventCategoryRepository = eventCategoryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RegistrationResponse create(RegistrationRequest request, UUID actorId) {
        var exhibitor = exhibitorRepository.findById(request.exhibitorId())
                .filter(item -> BusinessStatuses.EXHIBITOR_ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        eventCategoryRepository.findByEventIdAndCodeAndEnabled(request.eventId(), request.category(), true)
                .orElseThrow(() -> new BusinessException("Registration category is not enabled for this event"));
        if (repository.existsByEventIdAndExhibitorIdAndStatusIn(request.eventId(), exhibitor.getId(), IN_PROGRESS_STATUSES)) {
            throw new BusinessException("Active registration already exists for this exhibitor and event");
        }
        var registration = repository.save(new Registration(UUID.randomUUID(), request.eventId(), exhibitor.getId(),
                BusinessStatuses.REGISTRATION_DRAFT, request.category(), request.zoneId(), request.intentAreaSqm(),
                request.contactName(), request.contactPhone(), request.remark(), actorId));
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_CREATE", "Registration",
                registration.getId(), null, registration.getStatus(), "create registration");
        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public RegistrationResponse get(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Registration does not exist"));
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> list(UUID eventId, UUID exhibitorId, String status) {
        var registrations = repository.findByFilters(eventId, exhibitorId, status);
        var categoryNames = loadCategoryNames(registrations);
        return registrations.stream().map(registration -> toResponse(registration, categoryNames)).toList();
    }

    private RegistrationResponse toResponse(Registration registration) {
        return toResponse(registration, loadCategoryNames(List.of(registration)));
    }

    private RegistrationResponse toResponse(Registration registration, Map<CategoryKey, String> categoryNames) {
        var categoryName = categoryNames.get(new CategoryKey(registration.getEventId(), registration.getCategory()));
        return new RegistrationResponse(registration.getId(), registration.getEventId(), registration.getExhibitorId(),
                registration.getStatus(), registration.getCategory(), categoryName, registration.getZoneId(),
                registration.getIntentAreaSqm(), registration.getContactName(), registration.getContactPhone(),
                registration.getRemark(), registration.getReviewComment(), registration.getReviewedBy(),
                registration.getReviewedAt(), registration.getSubmittedAt(), registration.getCreatedAt(),
                registration.getUpdatedAt());
    }

    private Map<CategoryKey, String> loadCategoryNames(List<Registration> registrations) {
        var eventIds = registrations.stream().map(Registration::getEventId).collect(Collectors.toSet());
        var codes = registrations.stream().map(Registration::getCategory).filter(Objects::nonNull).collect(Collectors.toSet());
        if (eventIds.isEmpty() || codes.isEmpty()) {
            return Map.of();
        }
        return eventCategoryRepository.findByEventIdInAndCodeIn(eventIds, codes).stream()
                .collect(Collectors.toMap(
                        category -> new CategoryKey(category.getEventId(), category.getCode()),
                        Function.identity(),
                        (left, right) -> left
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getName()));
    }

    private record CategoryKey(UUID eventId, String code) {
    }
}
