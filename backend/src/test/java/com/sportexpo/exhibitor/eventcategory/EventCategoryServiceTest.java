package com.sportexpo.exhibitor.eventcategory;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventCategoryServiceTest extends TestDatabaseSupport {
    @Autowired
    EventService eventService;
    @Autowired
    EventCategoryService eventCategoryService;

    @Test
    void listsEnabledEventCategoriesByEvent() {
        var event = eventService.create(new CreateEventRequest("报名品类测试展", 2026));
        eventCategoryService.create(new EventCategoryRequest(event.id(), "FITNESS", "健身器材", true, 20),
                DemoActors.DEMO_ADMIN_ID);
        eventCategoryService.create(new EventCategoryRequest(event.id(), "SPORTS_EQUIPMENT", "体育用品", true, 10),
                DemoActors.DEMO_ADMIN_ID);
        eventCategoryService.create(new EventCategoryRequest(event.id(), "DISABLED", "禁用品类", false, 0),
                DemoActors.DEMO_ADMIN_ID);

        var enabled = eventCategoryService.list(event.id(), true);

        assertThat(enabled).extracting("name").containsExactly("体育用品", "健身器材");
        assertThat(enabled).allMatch(EventCategoryResponse -> EventCategoryResponse.enabled());
    }
}
