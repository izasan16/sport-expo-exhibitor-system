package com.sportexpo.exhibitor.selection;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoothSelectionRepository extends JpaRepository<BoothSelection, UUID> {
    boolean existsByOrderIdAndStatus(UUID orderId, String status);

    List<BoothSelection> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<BoothSelection> findByStatusAndLockExpiresAtBefore(String status, OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BoothSelection s where s.id = :id")
    Optional<BoothSelection> findByIdForUpdate(@Param("id") UUID id);
}
