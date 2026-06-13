package com.sportexpo.exhibitor.booth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BoothRepository extends JpaRepository<Booth, UUID> {
    List<Booth> findByEventId(UUID eventId);

    List<Booth> findByEventIdAndStatus(UUID eventId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booth b where b.id in :ids order by b.id asc")
    List<Booth> findAllByIdInForUpdateOrderById(@Param("ids") List<UUID> ids);
}
