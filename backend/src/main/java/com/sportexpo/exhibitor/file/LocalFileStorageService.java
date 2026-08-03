package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.file.dto.UploadedFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LocalFileStorageService {
    private final StoredFileRepository repository;
    private final Path rootPath;
    private final String publicPrefix;
    private final FileUploadValidator validator;

    public LocalFileStorageService(StoredFileRepository repository,
                                   @Value("${app.file-storage.root-path}") String rootPath,
                                   @Value("${app.file-storage.public-prefix}") String publicPrefix) {
        this.repository = repository;
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
        this.publicPrefix = publicPrefix;
        this.validator = new FileUploadValidator(10 * 1024 * 1024);
    }

    public UploadedFileResponse store(MultipartFile file, UUID actorId) {
        Path absolutePath = null;
        try {
            var content = file.getBytes();
            var fileId = UUID.randomUUID();
            var originalName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                    ? "file"
                    : file.getOriginalFilename();
            validator.validate(originalName, file.getContentType(), content);
            var extension = StringUtils.getFilenameExtension(originalName);
            var datePath = LocalDate.now().toString();
            var filename = extension == null || extension.isBlank() ? fileId.toString() : fileId + "." + extension;
            var relativePath = Path.of(datePath, filename);
            absolutePath = rootPath.resolve(relativePath).normalize();

            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, content);

            var stored = repository.save(new StoredFile(
                    fileId,
                    originalName,
                    file.getContentType() == null || file.getContentType().isBlank()
                            ? "application/octet-stream"
                            : file.getContentType(),
                    relativePath.toString().replace('\\', '/'),
                    publicPrefix + "/" + relativePath.toString().replace('\\', '/'),
                    file.getSize(),
                    null,
                    null,
                    OffsetDateTime.now(),
                    actorId
            ));
            return UploadedFileResponse.from(stored);
        } catch (IOException exception) {
            if (absolutePath != null) {
                try {
                    Files.deleteIfExists(absolutePath);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw new IllegalStateException("Failed to store file", exception);
        } catch (RuntimeException exception) {
            if (absolutePath != null) {
                try {
                    Files.deleteIfExists(absolutePath);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw exception;
        }
    }

    public UploadedFileResponse storeBytes(byte[] content, String originalName, String mimeType, UUID actorId) {
        try {
            var fileId = UUID.randomUUID();
            var extension = StringUtils.getFilenameExtension(originalName);
            var datePath = LocalDate.now().toString();
            var filename = extension == null || extension.isBlank() ? fileId.toString() : fileId + "." + extension;
            var relativePath = Path.of(datePath, filename);
            var absolutePath = rootPath.resolve(relativePath).normalize();
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, content);
            var stored = repository.save(new StoredFile(
                    fileId, originalName, mimeType, relativePath.toString().replace('\\', '/'),
                    publicPrefix + "/" + relativePath.toString().replace('\\', '/'), content.length,
                    null, null, OffsetDateTime.now(), actorId));
            return UploadedFileResponse.from(stored);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store generated file", exception);
        }
    }

    public void discardContent(UUID storedFileId) {
        var storedFile = repository.findById(storedFileId)
                .orElseThrow(() -> new IllegalStateException("Stored file does not exist"));
        var absolutePath = rootPath.resolve(storedFile.getStoragePath()).normalize();
        try {
            Files.deleteIfExists(absolutePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to discard file content", exception);
        }
    }

    public byte[] readContent(UUID storedFileId) {
        var storedFile = repository.findById(storedFileId)
                .orElseThrow(() -> new IllegalStateException("Stored file does not exist"));
        try {
            return Files.readAllBytes(rootPath.resolve(storedFile.getStoragePath()).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read stored file", exception);
        }
    }

    public void delete(UUID storedFileId) {
        var storedFile = repository.findById(storedFileId)
                .orElseThrow(() -> new IllegalStateException("Stored file does not exist"));
        var absolutePath = rootPath.resolve(storedFile.getStoragePath()).normalize();
        try {
            Files.deleteIfExists(absolutePath);
            repository.delete(storedFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete file", exception);
        }
    }
}
