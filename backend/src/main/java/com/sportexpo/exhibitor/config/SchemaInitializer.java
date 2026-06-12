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

@Component
public class SchemaInitializer implements ApplicationRunner {
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
        var resource = new ClassPathResource("db/schema-foundation.sql");
        var sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        var schemaQualifiedSql = sql.replace("__schema__", schema);
        var scriptResource = new ByteArrayResource(schemaQualifiedSql.getBytes(StandardCharsets.UTF_8));
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(scriptResource, StandardCharsets.UTF_8));
            return null;
        });
    }
}
