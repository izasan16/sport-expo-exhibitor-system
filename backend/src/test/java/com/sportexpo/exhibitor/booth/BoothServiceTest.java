package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.auth.AuthService;
import com.sportexpo.exhibitor.auth.LoginRequest;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class BoothServiceTest extends TestDatabaseSupport {
    @Autowired
    EventService eventService;

    @Autowired
    BoothService boothService;

    @Autowired
    BoothRepository boothRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuthService authService;

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
        assertThat(booth.status()).isEqualTo(BusinessStatuses.BOOTH_AVAILABLE);
    }

    @Test
    void listsBoothsByEventAndStatus() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var stillAvailable = boothService.create(new CreateBoothRequest(
                event.id(), "A001", new BigDecimal("9.00"), "STANDARD"));
        var locked = boothService.create(new CreateBoothRequest(
                event.id(), "A002", new BigDecimal("9.00"), "CUSTOM"));

        var lockedBooth = boothRepository.findById(locked.id()).orElseThrow();
        lockedBooth.markLocked(OffsetDateTime.now(), DemoActors.DEMO_ADMIN_ID);
        boothRepository.saveAndFlush(lockedBooth);

        var available = boothService.list(event.id(), BusinessStatuses.BOOTH_AVAILABLE);

        assertThat(available).extracting(booth -> booth.id()).containsExactly(stillAvailable.id());
        assertThat(available).extracting(booth -> booth.id()).doesNotContain(locked.id());
        assertThat(available).allMatch(booth -> booth.status().equals(BusinessStatuses.BOOTH_AVAILABLE));
    }

    @Test
    void returnsBusinessErrorWhenEventDoesNotExist() throws Exception {
        var body = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "boothNo", "B001",
                "areaSqm", "9.00",
                "boothType", "STANDARD"
        );

        mockMvc.perform(post("/api/booths")
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Event does not exist"));
    }

    private String tokenFor(String username) {
        return authService.login(new LoginRequest(username, "demo123")).accessToken();
    }
}
