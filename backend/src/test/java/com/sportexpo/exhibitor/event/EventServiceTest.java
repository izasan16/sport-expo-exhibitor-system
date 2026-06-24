package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventServiceTest extends TestDatabaseSupport {
    @Autowired
    EventService eventService;

    @Test
    void createsEvent() {
        var response = eventService.create(new CreateEventRequest("中国国际体育用品博览会", 2026));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("中国国际体育用品博览会");
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    @Test
    void createsFormOnlyEventByDefault() {
        var response = eventService.create(new CreateEventRequest("2027体博会", 2027));

        assertThat(response.boothSourceMode()).isEqualTo("FORM_ONLY");
        assertThat(response.visualSceneEnabled()).isFalse();
    }
}
