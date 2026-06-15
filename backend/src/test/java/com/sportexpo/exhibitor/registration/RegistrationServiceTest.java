package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.eventcategory.EventCategoryService;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
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

    @Test
    void rejectsRegistrationCategoryOutsideEventConfiguration() {
        var setup = setupRegistrationFixture();

        assertThatThrownBy(() -> registrationService.create(request(setup.eventId(), setup.exhibitorId(), "OUTSIDE"),
                DemoActors.DEMO_EXHIBITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration category is not enabled for this event");
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
