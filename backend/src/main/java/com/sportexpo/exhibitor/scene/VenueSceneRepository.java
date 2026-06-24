package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VenueSceneRepository extends JpaRepository<VenueScene, UUID> {
    Optional<VenueScene> findByEventId(UUID eventId);
}
