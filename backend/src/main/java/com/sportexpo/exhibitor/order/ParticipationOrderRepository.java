package com.sportexpo.exhibitor.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ParticipationOrderRepository extends JpaRepository<ParticipationOrder, UUID>,
        JpaSpecificationExecutor<ParticipationOrder> {
}
