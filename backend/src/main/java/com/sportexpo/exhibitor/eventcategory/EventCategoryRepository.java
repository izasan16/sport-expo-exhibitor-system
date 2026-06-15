package com.sportexpo.exhibitor.eventcategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {
    List<EventCategory> findByEventIdOrderBySortOrderAscNameAsc(UUID eventId);
    List<EventCategory> findByEventIdAndEnabledOrderBySortOrderAscNameAsc(UUID eventId, boolean enabled);
    Optional<EventCategory> findByEventIdAndCodeAndEnabled(UUID eventId, String code, boolean enabled);
    List<EventCategory> findByEventIdInAndCodeIn(Collection<UUID> eventIds, Collection<String> codes);
    boolean existsByEventIdAndCode(UUID eventId, String code);
}
