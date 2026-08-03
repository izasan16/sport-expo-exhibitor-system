package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoothSceneBindingRepository extends JpaRepository<BoothSceneBinding, UUID> {
    List<BoothSceneBinding> findByEventId(UUID eventId);

    List<BoothSceneBinding> findBySceneRevisionId(UUID sceneRevisionId);

    List<BoothSceneBinding> findBySceneRevisionIdAndBindingStatus(UUID sceneRevisionId, String bindingStatus);

    void deleteByBoothId(UUID boothId);
}
