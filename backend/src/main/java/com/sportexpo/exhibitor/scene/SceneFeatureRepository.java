package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SceneFeatureRepository extends JpaRepository<SceneFeature, UUID> {
    List<SceneFeature> findBySceneRevisionIdOrderByLayerOrderAsc(UUID sceneRevisionId);

    void deleteBySceneRevisionId(UUID sceneRevisionId);
}
