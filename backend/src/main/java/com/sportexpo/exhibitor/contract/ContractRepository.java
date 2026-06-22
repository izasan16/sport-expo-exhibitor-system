package com.sportexpo.exhibitor.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByOrderId(UUID orderId);

    List<Contract> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Contract> findAllByOrderByCreatedAtDesc();

    @Query("""
            select contract
            from Contract contract, ParticipationOrder participationOrder
            where contract.orderId = participationOrder.id
              and (:orderId is null or contract.orderId = :orderId)
              and participationOrder.exhibitorId in :exhibitorIds
            order by contract.createdAt desc
            """)
    List<Contract> findByOrderIdAndExhibitorIdIn(@Param("orderId") UUID orderId,
                                                 @Param("exhibitorIds") Collection<UUID> exhibitorIds);
}
