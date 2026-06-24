package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SceneRevisionRepository extends JpaRepository<SceneRevision, UUID> {
    List<SceneRevision> findBySceneIdOrderByRevisionNoDesc(UUID sceneId);

    Optional<SceneRevision> findFirstBySceneIdAndStatusOrderByRevisionNoDesc(UUID sceneId, String status);
}
