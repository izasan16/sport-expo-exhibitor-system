package com.sportexpo.exhibitor.sales;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesFollowUpRepository extends JpaRepository<SalesFollowUp, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select followUp from SalesFollowUp followUp where followUp.id = :id")
    Optional<SalesFollowUp> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select followUp
            from SalesFollowUp followUp
            where (:exhibitorId is null or followUp.exhibitorId = :exhibitorId)
              and (:status is null or followUp.status = :status)
            order by followUp.updatedAt desc
            """)
    List<SalesFollowUp> findByFilters(@Param("exhibitorId") UUID exhibitorId,
                                      @Param("status") String status);

    @Query("""
            select followUp
            from SalesFollowUp followUp
            where (:exhibitorId is null or followUp.exhibitorId = :exhibitorId)
              and (:status is null or followUp.status = :status)
              and followUp.exhibitorId in :exhibitorIds
            order by followUp.updatedAt desc
            """)
    List<SalesFollowUp> findByFiltersAndExhibitorIdIn(@Param("exhibitorId") UUID exhibitorId,
                                                      @Param("status") String status,
                                                      @Param("exhibitorIds") Collection<UUID> exhibitorIds);
}
