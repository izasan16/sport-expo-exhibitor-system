package com.sportexpo.exhibitor.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    java.util.Optional<StoredFile> findByStoragePath(String storagePath);
}
