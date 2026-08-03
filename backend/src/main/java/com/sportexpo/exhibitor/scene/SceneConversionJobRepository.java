package com.sportexpo.exhibitor.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SceneConversionJobRepository extends JpaRepository<SceneConversionJob, UUID> {
}
