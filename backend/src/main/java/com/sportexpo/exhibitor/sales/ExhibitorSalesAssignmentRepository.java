package com.sportexpo.exhibitor.sales;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExhibitorSalesAssignmentRepository extends JpaRepository<ExhibitorSalesAssignment, UUID> {
    List<ExhibitorSalesAssignment> findByExhibitorId(UUID exhibitorId);

    List<ExhibitorSalesAssignment> findBySalesUserId(UUID salesUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from ExhibitorSalesAssignment assignment where assignment.exhibitorId = :exhibitorId")
    List<ExhibitorSalesAssignment> findLockedByExhibitorId(@Param("exhibitorId") UUID exhibitorId);
}
