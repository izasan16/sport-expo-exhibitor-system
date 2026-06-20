package com.sportexpo.exhibitor;

import com.sportexpo.exhibitor.config.SchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FoundationApplicationTest extends TestDatabaseSupport {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${app.db.schema}")
    String schema;

    @Test
    void createsFoundationTablesInsideDedicatedSchema() {
        var tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = ?
                  and table_name in ('events', 'booths', 'contract_versions', 'refunds', 'booth_release_tasks')
                """, Integer.class, schema);

        assertThat(tableCount).isEqualTo(5);
    }

    @Test
    void createsSecondRoundBaselineColumns() {
        var orderColumns = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'participation_orders'
                  and column_name in ('area_sqm', 'booth_type')
                """, Integer.class, schema);

        var contractOrderColumn = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'contracts'
                  and column_name = 'order_id'
                """, Integer.class, schema);

        var pricingIndexes = jdbcTemplate.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = ?
                  and indexname in ('uidx_area_tiers_event_zone_area', 'uidx_price_rules_event_zone_booth_type_enabled')
                """, Integer.class, schema);

        assertThat(orderColumns).isEqualTo(2);
        assertThat(contractOrderColumn).isEqualTo(1);
        assertThat(pricingIndexes).isEqualTo(2);
    }

    @Test
    void initializesV6AuthSalesAndServiceTables() {
        var tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = ?
                """, String.class, schema);

        assertThat(tables).contains(
                "users",
                "roles",
                "user_roles",
                "exhibitor_sales_assignments",
                "sales_follow_ups",
                "service_types",
                "service_fields",
                "service_applications",
                "service_application_booths",
                "service_application_values",
                "service_application_attachments",
                "service_orders",
                "service_order_items",
                "service_payments"
        );
    }

    @Test
    void initializesV6IndexesAndSeeds() {
        var indexCount = jdbcTemplate.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = ?
                  and indexname in ('uk_exhibitor_primary_sales', 'idx_app_values_field_code')
                """, Integer.class, schema);

        assertThat(indexCount).isEqualTo(2);
        assertThat(countRows(schema, "roles")).isEqualTo(6);
        assertThat(countRows(schema, "users")).isEqualTo(6);
        assertThat(countRows(schema, "user_roles")).isEqualTo(6);
        assertThat(primaryDemoSalesAssignments(schema)).isEqualTo(1);
    }

    @Test
    void skipsDemoSeedScriptsWhenDemoDataIsDisabled() throws Exception {
        var isolatedSchema = uniqueSchema("foundation_no_demo");
        try {
            new SchemaInitializer(jdbcTemplate, isolatedSchema, environmentFor(isolatedSchema, false)).run(null);

            assertThat(countRows(isolatedSchema, "roles")).isEqualTo(6);
            assertThat(countRows(isolatedSchema, "users")).isZero();
            assertThat(countRows(isolatedSchema, "user_roles")).isZero();
            assertThat(primaryDemoSalesAssignments(isolatedSchema)).isZero();
        } finally {
            dropSchema(isolatedSchema);
        }
    }

    @Test
    void demoSeedScriptsAreIdempotentAndDoNotReactivateDisabledUsers() throws Exception {
        var isolatedSchema = uniqueSchema("foundation_demo");
        try {
            var initializer = new SchemaInitializer(jdbcTemplate, isolatedSchema, environmentFor(isolatedSchema, true));
            initializer.run(null);

            jdbcTemplate.update("""
                    update %s.users
                    set password_hash = 'custom-hash',
                        status = 'DISABLED'
                    where username = 'sales01'
                    """.formatted(isolatedSchema));

            initializer.run(null);

            var salesUser = jdbcTemplate.queryForMap("""
                    select password_hash, status
                    from %s.users
                    where username = 'sales01'
                    """.formatted(isolatedSchema));

            assertThat(salesUser)
                    .containsEntry("password_hash", "custom-hash")
                    .containsEntry("status", "DISABLED");
            assertThat(countRows(isolatedSchema, "roles")).isEqualTo(6);
            assertThat(countRows(isolatedSchema, "users")).isEqualTo(6);
            assertThat(countRows(isolatedSchema, "user_roles")).isEqualTo(6);
            assertThat(primaryDemoSalesAssignments(isolatedSchema)).isEqualTo(1);
        } finally {
            dropSchema(isolatedSchema);
        }
    }

    private MockEnvironment environmentFor(String schema, boolean seedDemoData) {
        return new MockEnvironment()
                .withProperty("spring.jpa.properties.hibernate.default_schema", schema)
                .withProperty("app.db.seed-demo-data", Boolean.toString(seedDemoData));
    }

    private String uniqueSchema(String prefix) {
        return prefix + "_" + Long.toUnsignedString(System.nanoTime(), 36);
    }

    private int countRows(String schema, String table) {
        return jdbcTemplate.queryForObject("select count(*) from %s.%s".formatted(schema, table), Integer.class);
    }

    private int primaryDemoSalesAssignments(String schema) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from %s.exhibitor_sales_assignments a
                join %s.users u on u.id = a.sales_user_id
                where a.exhibitor_id = '00000000-0000-0000-0000-000000000001'
                  and a.assignment_type = 'PRIMARY'
                  and u.username = 'sales01'
                """.formatted(schema, schema), Integer.class);
    }

    private void dropSchema(String schema) {
        jdbcTemplate.execute("drop schema if exists " + schema + " cascade");
    }
}
