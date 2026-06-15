package com.sportexpo.exhibitor.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select registration from Registration registration where registration.id = :id")
    Optional<Registration> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByEventIdAndExhibitorIdAndStatusIn(UUID eventId, UUID exhibitorId, Collection<String> statuses);
    @Query("""
            select registration
            from Registration registration
            where (:eventId is null or registration.eventId = :eventId)
              and (:exhibitorId is null or registration.exhibitorId = :exhibitorId)
              and (:status is null or registration.status = :status)
            order by registration.updatedAt desc
            """)
    List<Registration> findByFilters(@Param("eventId") UUID eventId,
                                      @Param("exhibitorId") UUID exhibitorId,
                                      @Param("status") String status);
    List<Registration> findByEventIdAndExhibitorIdAndStatus(UUID eventId, UUID exhibitorId, String status);
    List<Registration> findByEventIdAndExhibitorId(UUID eventId, UUID exhibitorId);
    List<Registration> findByStatusOrderBySubmittedAtDesc(String status);
    long countByStatus(String status);
    List<Registration> findTop5ByStatusOrderBySubmittedAtDesc(String status);
}
