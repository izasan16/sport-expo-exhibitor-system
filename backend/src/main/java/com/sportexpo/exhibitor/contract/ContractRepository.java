package com.sportexpo.exhibitor.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByOrderId(UUID orderId);

    List<Contract> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Contract> findAllByOrderByCreatedAtDesc();
}
