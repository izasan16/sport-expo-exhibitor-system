package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.eventcategory.EventCategoryRepository;
import com.sportexpo.exhibitor.exhibitor.ExhibitorRepository;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.notification.NotificationService;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

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
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public RegistrationService(RegistrationRepository repository, ExhibitorRepository exhibitorRepository,
                               EventRepository eventRepository, EventCategoryRepository eventCategoryRepository,
                               NotificationService notificationService, AuditService auditService,
                               JdbcTemplate jdbcTemplate, @Value("${app.db.schema}") String schema) {
        this.repository = repository;
        this.exhibitorRepository = exhibitorRepository;
        this.eventRepository = eventRepository;
        this.eventCategoryRepository = eventCategoryRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
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
        validateZoneBelongsToEvent(request.eventId(), request.zoneId());
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

    @Transactional
    public RegistrationResponse update(UUID id, RegistrationRequest request, UUID actorId) {
        var registration = findEntityForUpdate(id);
        if (!BusinessStatuses.REGISTRATION_DRAFT.equals(registration.getStatus())
                && !BusinessStatuses.REGISTRATION_REJECTED.equals(registration.getStatus())) {
            throw new BusinessException("Registration cannot be edited from current status");
        }
        if (!registration.getEventId().equals(request.eventId())) {
            throw new BusinessException("Registration event cannot be changed");
        }
        if (!registration.getExhibitorId().equals(request.exhibitorId())) {
            throw new BusinessException("Registration exhibitor cannot be changed");
        }
        eventCategoryRepository.findByEventIdAndCodeAndEnabled(request.eventId(), request.category(), true)
                .orElseThrow(() -> new BusinessException("Registration category is not enabled for this event"));
        validateZoneBelongsToEvent(request.eventId(), request.zoneId());
        registration.updateEditableFields(request.category(), request.zoneId(), request.intentAreaSqm(),
                request.contactName(), request.contactPhone(), request.remark(), actorId);
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_UPDATE", "Registration",
                registration.getId(), registration.getStatus(), registration.getStatus(), "update registration");
        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse submit(UUID id, UUID actorId) {
        var registration = findEntityForUpdate(id);
        return submitLocked(registration, actorId);
    }

    @Transactional
    public RegistrationResponse resubmit(UUID id, UUID actorId) {
        var registration = findEntityForUpdate(id);
        return resubmitLocked(registration, actorId);
    }

    @Transactional
    public RegistrationResponse submitOrResubmit(UUID id, UUID actorId) {
        var registration = findEntityForUpdate(id);
        if (BusinessStatuses.REGISTRATION_DRAFT.equals(registration.getStatus())) {
            return submitLocked(registration, actorId);
        }
        if (BusinessStatuses.REGISTRATION_REJECTED.equals(registration.getStatus())) {
            return resubmitLocked(registration, actorId);
        }
        throw new BusinessException("Registration cannot be submitted from current status");
    }

    @Transactional
    public RegistrationResponse approve(UUID id, UUID actorId) {
        var registration = findEntityForUpdate(id);
        if (!BusinessStatuses.REGISTRATION_SUBMITTED.equals(registration.getStatus())) {
            throw new BusinessException("Registration cannot be approved from current status");
        }
        var oldStatus = registration.getStatus();
        registration.approve(actorId);
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_APPROVE", "Registration",
                registration.getId(), oldStatus, registration.getStatus(), "approve registration");
        notifyExhibitor(registration, NotificationTypes.REGISTRATION_APPROVED, "报名审核通过", "您可以创建参展订单");
        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse reject(UUID id, UUID actorId, String reason) {
        var registration = findEntityForUpdate(id);
        if (!BusinessStatuses.REGISTRATION_SUBMITTED.equals(registration.getStatus())) {
            throw new BusinessException("Registration cannot be rejected from current status");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Reject reason is required");
        }
        if (reason.length() > 500) {
            throw new BusinessException("Reject reason is too long");
        }
        var oldStatus = registration.getStatus();
        registration.markRejected(actorId, reason);
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_REJECT", "Registration",
                registration.getId(), oldStatus, registration.getStatus(), reason);
        notifyExhibitor(registration, NotificationTypes.REGISTRATION_REJECTED, "报名被驳回", reason);
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

    private Registration findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Registration does not exist"));
    }

    private Registration findEntityForUpdate(UUID id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Registration does not exist"));
    }

    private void validateZoneBelongsToEvent(UUID eventId, UUID zoneId) {
        if (zoneId == null) {
            return;
        }
        var count = jdbcTemplate.queryForObject(
                "select count(*) from " + schema + ".zones where id = ? and event_id = ?",
                Integer.class,
                zoneId,
                eventId
        );
        if (count == null || count == 0) {
            throw new BusinessException("Registration zone is not available for this event");
        }
    }

    private void notifyAdmin(Registration registration, String type, String title, String content) {
        notificationService.create(registration.getEventId(), "ADMIN", DemoActors.DEMO_ADMIN_ID, title, content, type,
                "Registration", registration.getId(), null);
    }

    private void notifyExhibitor(Registration registration, String type, String title, String content) {
        notificationService.create(registration.getEventId(), "EXHIBITOR", registration.getExhibitorId(), title, content,
                type, "Registration", registration.getId(), null);
    }

    private RegistrationResponse submitLocked(Registration registration, UUID actorId) {
        if (!BusinessStatuses.REGISTRATION_DRAFT.equals(registration.getStatus())) {
            throw new BusinessException("Registration cannot be submitted from current status");
        }
        registration.submit(actorId);
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_SUBMIT", "Registration",
                registration.getId(), BusinessStatuses.REGISTRATION_DRAFT, registration.getStatus(), "submit registration");
        notifyAdmin(registration, NotificationTypes.REGISTRATION_SUBMITTED, "有新的报名待审核", "展商提交了参展报名");
        return toResponse(registration);
    }

    private RegistrationResponse resubmitLocked(Registration registration, UUID actorId) {
        if (!BusinessStatuses.REGISTRATION_REJECTED.equals(registration.getStatus())) {
            throw new BusinessException("Registration cannot be submitted from current status");
        }
        registration.submit(actorId);
        auditService.record(registration.getEventId(), actorId, "REGISTRATION_RESUBMIT", "Registration",
                registration.getId(), BusinessStatuses.REGISTRATION_REJECTED, registration.getStatus(),
                "resubmit registration and clear review fields");
        notifyAdmin(registration, NotificationTypes.REGISTRATION_SUBMITTED, "报名重新提交待审核", "展商重新提交了参展报名");
        return toResponse(registration);
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
