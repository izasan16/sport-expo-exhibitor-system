package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SceneSourceFileRepository extends JpaRepository<SceneSourceFile, UUID> {
}
