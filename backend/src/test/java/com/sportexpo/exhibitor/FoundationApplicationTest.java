package com.sportexpo.exhibitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

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
