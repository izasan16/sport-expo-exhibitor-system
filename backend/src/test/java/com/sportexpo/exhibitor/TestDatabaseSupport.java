package com.sportexpo.exhibitor;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class TestDatabaseSupport {
    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        var externalUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (externalUrl != null && !externalUrl.isBlank()) {
            var schema = envOrDefault("APP_DB_SCHEMA", "exhibitor_sys_test");
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> envOrDefault("SPRING_DATASOURCE_USERNAME", "exhibitor"));
            registry.add("spring.datasource.password", () -> envOrDefault("SPRING_DATASOURCE_PASSWORD", ""));
            registry.add("app.db.schema", () -> schema);
            registry.add("app.db.seed-demo-data", () -> "true");
            registry.add("spring.jpa.properties.hibernate.default_schema", () -> schema);
            return;
        }

        if (postgres == null) {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("exhibitor")
                    .withUsername("exhibitor")
                    .withPassword("exhibitor");
            postgres.start();
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.db.schema", () -> "exhibitor_sys_test");
        registry.add("app.db.seed-demo-data", () -> "true");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "exhibitor_sys_test");
    }

    private static String envOrDefault(String name, String defaultValue) {
        var value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
