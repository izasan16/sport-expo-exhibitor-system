package com.sportexpo.exhibitor.selection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoothLockRepository extends JpaRepository<BoothLock, UUID> {
    List<BoothLock> findBySelectionId(UUID selectionId);
}
