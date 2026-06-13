package com.sportexpo.exhibitor.pricing;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {
    List<PriceRule> findByEventIdOrderByUpdatedAtDesc(UUID eventId);

    @Query("""
            select count(priceRule) > 0
            from PriceRule priceRule
            where priceRule.eventId = :eventId
              and ((priceRule.zoneId is null and :zoneId is null) or priceRule.zoneId = :zoneId)
              and priceRule.boothType = :boothType
              and priceRule.enabled = :enabled
            """)
    boolean existsByEventIdAndZoneIdAndBoothTypeAndEnabled(@Param("eventId") UUID eventId,
                                                           @Param("zoneId") UUID zoneId,
                                                           @Param("boothType") String boothType,
                                                           @Param("enabled") boolean enabled);

    default Optional<PriceRule> findFirstByEventIdAndZoneIdAndBoothTypeAndEnabledOrderByUpdatedAtDesc(
            UUID eventId,
            UUID zoneId,
            String boothType,
            boolean enabled) {
        return findMatchingRules(eventId, zoneId, boothType, enabled, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
            select priceRule
            from PriceRule priceRule
            where priceRule.eventId = :eventId
              and ((priceRule.zoneId is null and :zoneId is null) or priceRule.zoneId = :zoneId)
              and priceRule.boothType = :boothType
              and priceRule.enabled = :enabled
            order by priceRule.updatedAt desc
            """)
    List<PriceRule> findMatchingRules(
            @Param("eventId") UUID eventId,
            @Param("zoneId") UUID zoneId,
            @Param("boothType") String boothType,
            @Param("enabled") boolean enabled,
            Pageable pageable);
}
