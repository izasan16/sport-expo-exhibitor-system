package com.sportexpo.exhibitor.selection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SelectionEligibilityRepository extends JpaRepository<SelectionEligibility, UUID> {
    Optional<SelectionEligibility> findByOrderId(UUID orderId);
}
