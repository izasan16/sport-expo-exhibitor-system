package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoothServiceTest extends TestDatabaseSupport {
    @Autowired
    EventService eventService;

    @Autowired
    BoothService boothService;

    @Test
    void createsBoothInEvent() {
        var event = eventService.create(new CreateEventRequest("中国国际体育用品博览会", 2026));

        var booth = boothService.create(new CreateBoothRequest(
                event.id(),
                "A001",
                new BigDecimal("9.00"),
                "STANDARD"
        ));

        assertThat(booth.eventId()).isEqualTo(event.id());
        assertThat(booth.boothNo()).isEqualTo("A001");
        assertThat(booth.areaSqm()).isEqualByComparingTo("9.00");
        assertThat(booth.status()).isEqualTo("AVAILABLE");
    }
}
