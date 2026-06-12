package com.sportexpo.exhibitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class FoundationApplicationTest {
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
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsFoundationTablesInsideDedicatedSchema() {
        var tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'exhibitor_sys_test'
                  and table_name in ('events', 'booths', 'contract_versions', 'refunds', 'booth_release_tasks')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(5);
    }
}
