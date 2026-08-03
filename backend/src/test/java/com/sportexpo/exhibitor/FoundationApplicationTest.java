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
    void createsVenueSceneTablesAndEventModeColumns() {
        var sceneTables = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = ?
                  and table_name in ('venue_scenes', 'scene_revisions', 'scene_features', 'scene_background_assets', 'booth_scene_bindings', 'scene_edit_locks', 'stored_files')
                """, Integer.class, schema);

        var eventColumns = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'events'
                  and column_name in ('booth_source_mode', 'visual_scene_enabled')
                """, Integer.class, schema);

        assertThat(sceneTables).isEqualTo(7);
        assertThat(eventColumns).isEqualTo(2);
    }

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
        assertThat(demoUserCount(schema)).isEqualTo(6);
        assertThat(demoUserRoleCount(schema)).isEqualTo(6);
        assertThat(primaryDemoSalesAssignments(schema)).isEqualTo(1);
    }

    @Test
    void initializesV7RbacTablesAndSeeds() {
        var tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = ?
                """, String.class, schema);

        assertThat(tables).contains("permissions", "role_permissions");

        var roleColumns = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'roles'
                """, String.class, schema);

        assertThat(roleColumns).contains("role_type", "enabled", "description");

        var permissionCodes = jdbcTemplate.queryForList("""
                select code
                from %s.permissions
                order by code
                """.formatted(schema), String.class);

        assertThat(permissionCodes).contains(
                "menu.admin.users",
                "page.admin.users.list",
                "action.admin.users.create",
                "menu.admin.roles",
                "page.admin.permissions.list",
                "page.admin.role-permissions.matrix"
        );
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

    @Test
    void v7SeedScriptsDoNotReactivateDisabledBuiltInRoles() throws Exception {
        var isolatedSchema = uniqueSchema("foundation_v7_roles");
        try {
            var initializer = new SchemaInitializer(jdbcTemplate, isolatedSchema, environmentFor(isolatedSchema, true));
            initializer.run(null);

            jdbcTemplate.update("""
                    update %s.roles
                    set enabled = false
                    where code = 'SALES'
                    """.formatted(isolatedSchema));

            initializer.run(null);

            var salesRole = jdbcTemplate.queryForMap("""
                    select role_type, enabled
                    from %s.roles
                    where code = 'SALES'
                    """.formatted(isolatedSchema));

            assertThat(salesRole)
                    .containsEntry("role_type", "SYSTEM")
                    .containsEntry("enabled", false);
            assertThat(countRows(isolatedSchema, "permissions")).isPositive();
            assertThat(countRows(isolatedSchema, "role_permissions")).isPositive();
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

    private int demoUserCount(String schema) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from %s.users
                where username in ('admin', 'organizer', 'sales01', 'finance01', 'service01', 'exhibitor01')
                """.formatted(schema), Integer.class);
    }

    private int demoUserRoleCount(String schema) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from %s.user_roles ur
                join %s.users u on u.id = ur.user_id
                join %s.roles r on r.id = ur.role_id
                where (u.username, r.code) in (
                    ('admin', 'SYSTEM_ADMIN'),
                    ('organizer', 'ORGANIZER_ADMIN'),
                    ('sales01', 'SALES'),
                    ('finance01', 'FINANCE'),
                    ('service01', 'SERVICE_STAFF'),
                    ('exhibitor01', 'EXHIBITOR')
                )
                """.formatted(schema, schema, schema), Integer.class);
    }

    private void dropSchema(String schema) {
        jdbcTemplate.execute("drop schema if exists " + schema + " cascade");
    }

    @Test
    void createsCadConversionTablesAndReferenceLayerColumns() {
        var cadConversionTables = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = ?
                  and table_name in ('scene_source_files', 'scene_conversion_jobs')
                """, Integer.class, schema);

        var backgroundAssetColumns = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'scene_background_assets'
                  and column_name in ('render_format', 'rotation_deg', 'visible', 'z_index')
                """, Integer.class, schema);

        var sceneVisibilityColumns = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'venue_scenes'
                  and column_name in ('published_background_visible_to_sales',
                                      'published_background_visible_to_exhibitor')
                """, Integer.class, schema);

        var cadConversionIndexes = jdbcTemplate.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = ?
                  and indexname in ('idx_scene_source_files_scene_revision',
                                    'idx_scene_conversion_jobs_scene_revision')
                """, Integer.class, schema);

        var sourceFileForeignKeyColumns = jdbcTemplate.queryForObject("""
                select count(distinct kcu.column_name)
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_schema = kcu.constraint_schema
                 and tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                 and tc.table_name = kcu.table_name
                where tc.constraint_type = 'FOREIGN KEY'
                  and tc.table_schema = ?
                  and tc.table_name = 'scene_source_files'
                  and kcu.column_name in ('scene_id', 'scene_revision_id', 'file_id')
                """, Integer.class, schema);

        var conversionJobForeignKeyColumns = jdbcTemplate.queryForObject("""
                select count(distinct kcu.column_name)
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_schema = kcu.constraint_schema
                 and tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                 and tc.table_name = kcu.table_name
                where tc.constraint_type = 'FOREIGN KEY'
                  and tc.table_schema = ?
                  and tc.table_name = 'scene_conversion_jobs'
                  and kcu.column_name in ('scene_id', 'scene_revision_id', 'source_file_id', 'result_file_id')
                """, Integer.class, schema);

        var cadConversionConsistencyConstraints = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.table_constraints
                where table_schema = ?
                  and constraint_type = 'FOREIGN KEY'
                  and constraint_name in ('fk_scene_source_files_scene_revision_match',
                                          'fk_scene_conversion_jobs_scene_revision_match',
                                          'fk_scene_conversion_jobs_source_file_match')
                """, Integer.class, schema);

        assertThat(cadConversionTables).isEqualTo(2);
        assertThat(backgroundAssetColumns).isEqualTo(4);
        assertThat(sceneVisibilityColumns).isEqualTo(2);
        assertThat(cadConversionIndexes).isEqualTo(2);
        assertThat(sourceFileForeignKeyColumns).isEqualTo(3);
        assertThat(conversionJobForeignKeyColumns).isEqualTo(4);
        assertThat(cadConversionConsistencyConstraints).isEqualTo(3);
    }
}
