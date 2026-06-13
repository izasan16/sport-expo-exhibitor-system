package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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

    @Test
    void returnsBusinessErrorWhenEventDoesNotExist() throws Exception {
        var body = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "boothNo", "B001",
                "areaSqm", "9.00",
                "boothType", "STANDARD"
        );

        mockMvc.perform(post("/api/booths")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Event does not exist"));
    }
}
