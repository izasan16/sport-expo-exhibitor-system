package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.file.LocalFileStorageService;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import com.sportexpo.exhibitor.scene.dto.SceneConversionJobResponse;
import com.sportexpo.exhibitor.scene.dto.SceneSourceFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class SceneCadConversionService {
    private final VenueSceneRepository sceneRepository;
    private final SceneRevisionRepository revisionRepository;
    private final StoredFileRepository storedFileRepository;
    private final LocalFileStorageService localFileStorageService;
    private final SceneSourceFileRepository sceneSourceFileRepository;
    private final SceneConversionJobRepository sceneConversionJobRepository;
    private final CadConversionGateway cadConversionGateway;
    private final String targetFormat;

    public SceneCadConversionService(VenueSceneRepository sceneRepository,
                                     SceneRevisionRepository revisionRepository,
                                     StoredFileRepository storedFileRepository,
                                     LocalFileStorageService localFileStorageService,
                                     SceneSourceFileRepository sceneSourceFileRepository,
                                     SceneConversionJobRepository sceneConversionJobRepository,
                                     CadConversionGateway cadConversionGateway,
                                     @Value("${app.scene.cad.converter-target-format}") String targetFormat) {
        this.sceneRepository = sceneRepository;
        this.revisionRepository = revisionRepository;
        this.storedFileRepository = storedFileRepository;
        this.localFileStorageService = localFileStorageService;
        this.sceneSourceFileRepository = sceneSourceFileRepository;
        this.sceneConversionJobRepository = sceneConversionJobRepository;
        this.cadConversionGateway = cadConversionGateway;
        this.targetFormat = targetFormat;
    }

    @Transactional
    public SceneSourceFileResponse registerCadSource(UUID eventId, UUID sceneId, UUID revisionId, String originalName,
                                                     String mimeType, long fileSize, UUID storedFileId,
                                                     UUID actorId) {
        var scene = sceneRepository.findById(sceneId)
                .filter(item -> eventId.equals(item.getEventId()))
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        var revision = revisionRepository.findById(revisionId)
                .filter(item -> scene.getId().equals(item.getSceneId()))
                .orElseThrow(() -> new BusinessException("Scene revision does not exist"));
        requireCurrentDraftRevision(scene.getId(), revision.getId());
        storedFileRepository.findById(storedFileId)
                .orElseThrow(() -> new BusinessException("Scene source file does not exist"));

        var sourceFormat = requireSupportedSourceFormat(originalName);
        var now = OffsetDateTime.now();
        var sourceFile = sceneSourceFileRepository.save(new SceneSourceFile(
                UUID.randomUUID(),
                scene.getId(),
                revision.getId(),
                storedFileId,
                sourceFormat,
                originalName,
                fileSize,
                "UPLOADED",
                now,
                actorId
        ));
        var job = sceneConversionJobRepository.save(new SceneConversionJob(
                UUID.randomUUID(),
                scene.getId(),
                revision.getId(),
                sourceFile.getId(),
                "PENDING",
                targetFormat,
                null,
                null,
                null,
                null,
                null,
                now
        ));
        cadConversionGateway.requestConversion(sourceFile, job);
        return toResponse(sourceFile, job);
    }

    @Transactional(readOnly = true)
    public SceneConversionJobResponse getJob(UUID eventId, UUID jobId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        var job = sceneConversionJobRepository.findById(jobId)
                .filter(item -> scene.getId().equals(item.getSceneId()))
                .orElseThrow(() -> new BusinessException("Scene conversion job does not exist"));
        return toResponse(job);
    }

    @Transactional
    public SceneConversionJobResponse markSucceeded(UUID eventId, UUID jobId, UUID resultFileId) {
        var job = findJob(eventId, jobId);
        if (!"PENDING".equals(job.getJobStatus())) {
            throw new BusinessException("Scene conversion job is not pending");
        }
        storedFileRepository.findById(resultFileId)
                .orElseThrow(() -> new BusinessException("Scene conversion result file does not exist"));
        var source = sceneSourceFileRepository.findById(job.getSourceFileId()).orElseThrow();
        job.markSucceeded(resultFileId, OffsetDateTime.now());
        source.markConverted();
        sceneSourceFileRepository.save(source);
        return toResponse(sceneConversionJobRepository.save(job));
    }

    public void markSucceededForGateway(SceneConversionJob job, UUID resultFileId) {
        markSucceededForJob(job, resultFileId);
    }

    @Transactional
    public SceneConversionJobResponse markFailed(UUID eventId, UUID jobId, String errorCode, String errorMessage) {
        var job = findJob(eventId, jobId);
        if (!"PENDING".equals(job.getJobStatus())) {
            throw new BusinessException("Scene conversion job is not pending");
        }
        var source = sceneSourceFileRepository.findById(job.getSourceFileId()).orElseThrow();
        job.markFailed(errorCode, errorMessage, OffsetDateTime.now());
        source.markConversionFailed();
        sceneSourceFileRepository.save(source);
        var result = sceneConversionJobRepository.save(job);
        localFileStorageService.discardContent(source.getStoredFileId());
        return toResponse(result);
    }

    public void markFailedForGateway(SceneConversionJob job, String errorCode, String errorMessage) {
        markFailedForJob(job, errorCode, errorMessage);
    }

    @Transactional
    protected void markSucceededForJob(SceneConversionJob job, UUID resultFileId) {
        storedFileRepository.findById(resultFileId)
                .orElseThrow(() -> new BusinessException("Scene conversion result file does not exist"));
        var source = sceneSourceFileRepository.findById(job.getSourceFileId()).orElseThrow();
        job.markSucceeded(resultFileId, OffsetDateTime.now());
        source.markConverted();
        sceneSourceFileRepository.save(source);
        sceneConversionJobRepository.save(job);
    }

    @Transactional
    protected void markFailedForJob(SceneConversionJob job, String errorCode, String errorMessage) {
        var source = sceneSourceFileRepository.findById(job.getSourceFileId()).orElseThrow();
        job.markFailed(errorCode, errorMessage, OffsetDateTime.now());
        source.markConversionFailed();
        sceneSourceFileRepository.save(source);
        sceneConversionJobRepository.save(job);
        localFileStorageService.discardContent(source.getStoredFileId());
    }

    private SceneConversionJob findJob(UUID eventId, UUID jobId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        return sceneConversionJobRepository.findById(jobId)
                .filter(item -> scene.getId().equals(item.getSceneId()))
                .orElseThrow(() -> new BusinessException("Scene conversion job does not exist"));
    }

    String requireSupportedSourceFormat(String originalName) {
        var extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null) {
            throw new BusinessException("Only DWG and DXF files are supported");
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "dxf" -> "DXF";
            case "dwg" -> "DWG";
            default -> throw new BusinessException("Only DWG and DXF files are supported");
        };
    }

    private void requireCurrentDraftRevision(UUID sceneId, UUID revisionId) {
        var currentDraft = revisionRepository.findFirstBySceneIdAndStatusOrderByRevisionNoDesc(sceneId, "DRAFT")
                .orElseThrow(() -> new BusinessException("Scene revision is not the current draft"));
        if (!currentDraft.getId().equals(revisionId)) {
            throw new BusinessException("Scene revision is not the current draft");
        }
    }

    private SceneSourceFileResponse toResponse(SceneSourceFile sourceFile, SceneConversionJob job) {
        return new SceneSourceFileResponse(
                sourceFile.getId(),
                sourceFile.getSourceFormat(),
                sourceFile.getOriginalName(),
                sourceFile.getCreatedAt(),
                toResponse(job)
        );
    }

    private SceneConversionJobResponse toResponse(SceneConversionJob job) {
        return new SceneConversionJobResponse(
                job.getId(),
                job.getSourceFileId(),
                job.getJobStatus(),
                job.getTargetFormat(),
                job.getResultFileId(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt()
        );
    }
}
