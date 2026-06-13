package com.sportexpo.exhibitor.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, UUID> {
    List<ContractVersion> findByContractIdOrderByVersionNoAsc(UUID contractId);
}
