package com.sportexpo.exhibitor.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AreaTierRepository extends JpaRepository<AreaTier, UUID> {
    List<AreaTier> findByEventIdOrderByAreaSqmAsc(UUID eventId);

    @Query("""
            select count(areaTier) > 0
            from AreaTier areaTier
            where areaTier.eventId = :eventId
              and ((areaTier.zoneId is null and :zoneId is null) or areaTier.zoneId = :zoneId)
              and areaTier.areaSqm = :areaSqm
            """)
    boolean existsByEventIdAndZoneIdAndAreaSqm(@Param("eventId") UUID eventId,
                                               @Param("zoneId") UUID zoneId,
                                               @Param("areaSqm") BigDecimal areaSqm);
}
