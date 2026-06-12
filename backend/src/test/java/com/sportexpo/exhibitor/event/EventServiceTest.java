package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class EventServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exhibitor")
            .withUsername("exhibitor")
            .withPassword("exhibitor");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.db.schema", () -> "exhibitor_sys_test");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "exhibitor_sys_test");
    }

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
}
