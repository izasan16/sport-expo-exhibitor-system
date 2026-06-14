package com.sportexpo.exhibitor.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationOrderRepository extends JpaRepository<ParticipationOrder, UUID>,
        JpaSpecificationExecutor<ParticipationOrder> {
    long countByStatus(String status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime startInclusive,
                                                              OffsetDateTime endExclusive);

    List<ParticipationOrder> findTop5ByStatusOrderByCreatedAtDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from ParticipationOrder po where po.id = :id")
    Optional<ParticipationOrder> findByIdForUpdate(@Param("id") UUID id);
}
