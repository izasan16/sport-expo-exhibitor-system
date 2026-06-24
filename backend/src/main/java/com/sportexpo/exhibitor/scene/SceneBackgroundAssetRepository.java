package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SceneBackgroundAssetRepository extends JpaRepository<SceneBackgroundAsset, UUID> {
    Optional<SceneBackgroundAsset> findBySceneRevisionId(UUID sceneRevisionId);

    void deleteBySceneRevisionId(UUID sceneRevisionId);
}
