package com.sportexpo.exhibitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FoundationApplicationTest extends TestDatabaseSupport {
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
