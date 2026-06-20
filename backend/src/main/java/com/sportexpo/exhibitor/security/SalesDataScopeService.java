package com.sportexpo.exhibitor.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SalesDataScopeService {
    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public SalesDataScopeService(JdbcTemplate jdbcTemplate, @Value("${app.db.schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    @Cacheable(cacheNames = DataScopeService.SALES_ACCESSIBLE_EXHIBITOR_IDS_CACHE, key = "#salesUserId")
    public Set<UUID> accessibleExhibitorIds(UUID salesUserId) {
        return jdbcTemplate.queryForList("""
                        select exhibitor_id
                        from %s.exhibitor_sales_assignments
                        where sales_user_id = ?
                        """.formatted(schema), UUID.class, salesUserId)
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }
}
