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
}
