package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SceneEditLockRepository extends JpaRepository<SceneEditLock, UUID> {
}
