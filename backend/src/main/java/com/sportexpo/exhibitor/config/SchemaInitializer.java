package com.sportexpo.exhibitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class SchemaInitializer implements ApplicationRunner {
    private static final List<String> V6_BASELINE_SCRIPTS = List.of(
            "db/migrations/v6/V6_001__create_auth_tables.sql",
            "db/migrations/v6/V6_002__create_sales_tables.sql",
            "db/migrations/v6/V6_003__create_service_tables.sql",
            "db/migrations/v6/V6_004__add_indexes.sql",
            "db/migrations/v6/V6_005__seed_roles.sql"
    );

    private static final List<String> V6_DEMO_SCRIPTS = List.of(
            "db/migrations/v6/V6_006__seed_users.sql",
            "db/migrations/v6/V6_007__backfill_demo_data.sql"
    );

    private static final List<String> V7_BASELINE_SCRIPTS = List.of(
            "db/migrations/v7/V7_001__create_permission_tables.sql",
            "db/migrations/v7/V7_002__seed_permission_resources.sql",
            "db/migrations/v7/V7_003__seed_role_permissions.sql"
    );

    private final JdbcTemplate jdbcTemplate;
    private final String schema;
    private final Environment environment;

    public SchemaInitializer(JdbcTemplate jdbcTemplate, @Value("${app.db.schema}") String schema,
                             Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!schema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
        var hibernateSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema");
        if (hibernateSchema != null && !hibernateSchema.equals(schema)) {
            throw new IllegalStateException("Hibernate default schema must equal app.db.schema");
        }
        jdbcTemplate.execute("create schema if not exists " + schema);
        executeSqlResource("db/schema-foundation.sql");
        for (var script : V6_BASELINE_SCRIPTS) {
            executeSqlResource(script);
        }
        if (seedDemoData()) {
            for (var script : V6_DEMO_SCRIPTS) {
                executeSqlResource(script);
            }
        }
        for (var script : V7_BASELINE_SCRIPTS) {
            executeSqlResource(script);
        }
    }

    private boolean seedDemoData() {
        return environment.getProperty("app.db.seed-demo-data", Boolean.class, false);
    }

    private void executeSqlResource(String location) throws Exception {
        var resource = new ClassPathResource(location);
        var sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        var schemaQualifiedSql = sql.replace("__schema__", schema);
        var scriptResource = new ByteArrayResource(schemaQualifiedSql.getBytes(StandardCharsets.UTF_8));
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(scriptResource, StandardCharsets.UTF_8));
            return null;
        });
    }
}
