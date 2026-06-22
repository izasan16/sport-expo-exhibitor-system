package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.common.SystemActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.eventcategory.EventCategoryService;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.notification.NotificationRepository;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RegistrationServiceTest extends TestDatabaseSupport {
    @Autowired
    EventService eventService;
    @Autowired
    ExhibitorService exhibitorService;
    @Autowired
    EventCategoryService eventCategoryService;
    @Autowired
    RegistrationService registrationService;
    @Autowired
    RegistrationRepository registrationRepository;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @org.springframework.beans.factory.annotation.Value("${app.db.schema}")
    String schema;

    @Test
    void rejectsRegistrationCategoryOutsideEventConfiguration() {
        var setup = setupRegistrationFixture();

        assertThatThrownBy(() -> registrationService.create(request(setup.eventId(), setup.exhibitorId(), "OUTSIDE"),
                DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration category is not enabled for this event");
    }

    @Test
    void rejectsRegistrationZoneOutsideEvent() {
        var setup = setupRegistrationFixture();
        var otherSetup = setupRegistrationFixture();
        var pavilionId = UUID.randomUUID();
        var otherZoneId = UUID.randomUUID();
        var suffix = otherZoneId.toString().substring(0, 8);
        jdbcTemplate.update("""
                        insert into %s.pavilions (id, event_id, name, code, created_at, updated_at)
                        values (?, ?, ?, ?, now(), now())
                        """.formatted(schema),
                pavilionId, otherSetup.eventId(), "其它展馆-" + suffix, "P-" + suffix);
        jdbcTemplate.update("""
                        insert into %s.zones (id, event_id, pavilion_id, name, code, category_code, created_at, updated_at)
                        values (?, ?, ?, ?, ?, 'SPORTS_EQUIPMENT', now(), now())
                        """.formatted(schema),
                otherZoneId, otherSetup.eventId(), pavilionId, "其它展区-" + suffix, "Z-" + suffix);

        assertThatThrownBy(() -> registrationService.create(
                new RegistrationRequest(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT", otherZoneId,
                        new BigDecimal("18.00"), "王老板", "13800000000", "希望参加本届展会"),
                DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration zone is not available for this event");
    }

    @Test
    void preventsSecondDraftOrSubmittedRegistrationForSameExhibitorAndEvent() {
        var setup = setupRegistrationFixture();
        registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);

        assertThatThrownBy(() -> registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Active registration already exists for this exhibitor and event");
    }

    @Test
    void rejectedRegistrationDoesNotBlockNewRegistrationForSameExhibitorAndEvent() {
        var setup = setupRegistrationFixture();
        var rejected = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);
        var entity = registrationRepository.findById(rejected.id()).orElseThrow();
        entity.markRejected(DemoActors.DEMO_ADMIN_ID, "资料不完整");
        registrationRepository.save(entity);

        var newDraft = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);

        assertThat(newDraft.status()).isEqualTo("DRAFT");
    }

    @Test
    void listsRegistrationsByFiltersWithCategoryName() {
        var setup = setupRegistrationFixture();
        var registration = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);
        var otherSetup = setupRegistrationFixture();
        registrationService.create(request(otherSetup.eventId(), otherSetup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);

        var filtered = registrationService.list(setup.eventId(), setup.exhibitorId(), "DRAFT");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).id()).isEqualTo(registration.id());
        assertThat(filtered.get(0).categoryName()).isEqualTo("体育用品");
    }

    @Test
    void submitApproveAndRejectRegistrationCreateNotifications() {
        var setup = setupRegistrationFixture();
        var draft = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);

        var submitted = registrationService.submit(draft.id(), DemoActors.DEMO_EXHIBITOR_ID);

        assertThat(submitted.status()).isEqualTo(BusinessStatuses.REGISTRATION_SUBMITTED);
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(SystemActors.SYSTEM_ACTOR_ID))
                .anyMatch(notification -> notification.getType().equals(NotificationTypes.REGISTRATION_SUBMITTED)
                        && notification.getRelatedType().equals("Registration")
                        && notification.getRelatedId().equals(draft.id()));

        var approved = registrationService.approve(draft.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(approved.status()).isEqualTo(BusinessStatuses.REGISTRATION_APPROVED);
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(setup.exhibitorId()))
                .anyMatch(notification -> notification.getType().equals(NotificationTypes.REGISTRATION_APPROVED)
                        && notification.getRelatedType().equals("Registration")
                        && notification.getRelatedId().equals(draft.id()));

        var secondDraft = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.submit(secondDraft.id(), DemoActors.DEMO_EXHIBITOR_ID);
        var rejected = registrationService.reject(secondDraft.id(), DemoActors.DEMO_ADMIN_ID, "资质材料不完整");

        assertThat(rejected.status()).isEqualTo(BusinessStatuses.REGISTRATION_REJECTED);
        assertThat(rejected.reviewComment()).isEqualTo("资质材料不完整");
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(setup.exhibitorId()))
                .anyMatch(notification -> notification.getType().equals(NotificationTypes.REGISTRATION_REJECTED)
                        && notification.getRelatedType().equals("Registration")
                        && notification.getRelatedId().equals(secondDraft.id()));
    }

    @Test
    void rejectedRegistrationCanBeEditedThenResubmittedAndReviewFieldsAreCleared() {
        var setup = setupRegistrationFixture();
        var draft = registrationService.create(request(setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT"),
                DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.submit(draft.id(), DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.reject(draft.id(), DemoActors.DEMO_ADMIN_ID, "资质材料不完整");

        var saved = registrationService.update(draft.id(), new RegistrationRequest(
                setup.eventId(), setup.exhibitorId(), "SPORTS_EQUIPMENT", null, new BigDecimal("36.00"),
                "李经理", "13900000000", "补充资质后重新提交"
        ), DemoActors.DEMO_EXHIBITOR_ID);

        assertThat(saved.status()).isEqualTo(BusinessStatuses.REGISTRATION_REJECTED);
        assertThat(saved.intentAreaSqm()).isEqualByComparingTo("36.00");
        assertThat(saved.contactName()).isEqualTo("李经理");

        var resubmitted = registrationService.resubmit(draft.id(), DemoActors.DEMO_EXHIBITOR_ID);

        assertThat(resubmitted.status()).isEqualTo(BusinessStatuses.REGISTRATION_SUBMITTED);
        assertThat(resubmitted.reviewComment()).isNull();
        assertThat(resubmitted.reviewedBy()).isNull();
        assertThat(resubmitted.reviewedAt()).isNull();
        assertThat(resubmitted.submittedAt()).isNotNull();
    }

    private RegistrationFixture setupRegistrationFixture() {
        var event = eventService.create(new CreateEventRequest("报名审核测试展", 2026));
        var exhibitor = exhibitorService.create(new ExhibitorRequest(
                "测试展商-" + UUID.randomUUID(),
                "CREDIT-" + UUID.randomUUID(),
                "王老板",
                "13800000000",
                "demo@example.com",
                "上海市",
                "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID);
        eventCategoryService.create(new EventCategoryRequest(event.id(), "SPORTS_EQUIPMENT", "体育用品", true, 10),
                DemoActors.DEMO_ADMIN_ID);
        return new RegistrationFixture(event.id(), exhibitor.id());
    }

    private RegistrationRequest request(UUID eventId, UUID exhibitorId, String category) {
        return new RegistrationRequest(eventId, exhibitorId, category, null, new BigDecimal("18.00"),
                "王老板", "13800000000", "希望参加本届展会");
    }

    private record RegistrationFixture(UUID eventId, UUID exhibitorId) {
    }
}
