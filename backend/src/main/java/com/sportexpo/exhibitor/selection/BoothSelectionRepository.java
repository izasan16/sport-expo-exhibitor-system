package com.sportexpo.exhibitor.selection;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoothSelectionRepository extends JpaRepository<BoothSelection, UUID> {
    long countByStatus(String status);

    List<BoothSelection> findTop5ByStatusOrderByCreatedAtDesc(String status);

    boolean existsByOrderIdAndStatus(UUID orderId, String status);

    List<BoothSelection> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<BoothSelection> findByStatusAndLockExpiresAtBefore(String status, OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BoothSelection s where s.id = :id")
    Optional<BoothSelection> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select selection
            from BoothSelection selection
            where (:orderId is null or selection.orderId = :orderId)
              and (:status is null or selection.status = :status)
              and selection.exhibitorId in :exhibitorIds
            order by selection.createdAt desc
            """)
    List<BoothSelection> findByFiltersAndExhibitorIdIn(@Param("orderId") UUID orderId,
                                                       @Param("status") String status,
                                                       @Param("exhibitorIds") Collection<UUID> exhibitorIds);
}
