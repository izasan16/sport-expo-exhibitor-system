package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.file.LocalFileStorageService;
import com.sportexpo.exhibitor.scene.dto.PublishSceneRequest;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneBoothResponse;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneResponse;
import com.sportexpo.exhibitor.scene.dto.SaveSceneBackgroundRequest;
import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneConversionJobResponse;
import com.sportexpo.exhibitor.scene.dto.SceneDraftResponse;
import com.sportexpo.exhibitor.scene.dto.SceneRevisionResponse;
import com.sportexpo.exhibitor.scene.dto.SceneSourceFileResponse;
import com.sportexpo.exhibitor.scene.dto.SceneValidationResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/scene")
public class SceneController {
    private final SceneQueryService queryService;
    private final SceneEditorService editorService;
    private final ScenePublishService publishService;
    private final SceneValidationService validationService;
    private final LocalFileStorageService fileStorageService;
    private final SceneCadConversionService cadConversionService;

    public SceneController(SceneQueryService queryService, SceneEditorService editorService,
                           ScenePublishService publishService,
                           SceneValidationService validationService,
                           LocalFileStorageService fileStorageService,
                           SceneCadConversionService cadConversionService) {
        this.queryService = queryService;
        this.editorService = editorService;
        this.publishService = publishService;
        this.validationService = validationService;
        this.fileStorageService = fileStorageService;
        this.cadConversionService = cadConversionService;
    }

    @GetMapping
    public ApiResponse<SceneDraftResponse> getDraft(@PathVariable UUID eventId) {
        return ApiResponse.ok(queryService.getDraft(eventId, DemoActors.DEMO_ADMIN_ID));
    }

    @PutMapping("/background")
    public ApiResponse<SceneDraftResponse> saveBackground(@PathVariable UUID eventId,
                                                          @Valid @RequestBody SaveSceneBackgroundRequest request) {
        return ApiResponse.ok(editorService.saveBackground(eventId, request, DemoActors.DEMO_ADMIN_ID));
    }

    @DeleteMapping("/background")
    public ApiResponse<SceneDraftResponse> clearBackground(@PathVariable UUID eventId) {
        return ApiResponse.ok(editorService.clearBackground(eventId, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/draft")
    public ApiResponse<SceneDraftResponse> saveDraft(@PathVariable UUID eventId,
                                                     @Valid @RequestBody SaveSceneDraftRequest request) {
        return ApiResponse.ok(editorService.saveDraft(eventId, request, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/draft/validate")
    public ApiResponse<SceneValidationResponse> validateDraft(@PathVariable UUID eventId,
                                                              @Valid @RequestBody SaveSceneDraftRequest request) {
        return ApiResponse.ok(validationService.validateDraft(request));
    }

    @PostMapping("/draft/publish")
    public ApiResponse<SceneDraftResponse> publishDraft(@PathVariable UUID eventId,
                                                        @Valid @RequestBody PublishSceneRequest request) {
        return ApiResponse.ok(publishService.publishDraft(eventId, request, DemoActors.DEMO_ADMIN_ID));
    }

    @GetMapping("/revisions")
    public ApiResponse<List<SceneRevisionResponse>> getRevisions(@PathVariable UUID eventId) {
        return ApiResponse.ok(queryService.getRevisions(eventId));
    }

    @PostMapping(value = "/source-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SceneSourceFileResponse> uploadSourceFile(@PathVariable UUID eventId,
                                                                 @RequestPart("file") MultipartFile file) {
        cadConversionService.requireSupportedSourceFormat(file.getOriginalFilename());
        var draftTarget = editorService.getDraftTarget(eventId, DemoActors.DEMO_ADMIN_ID);
        var storedFile = fileStorageService.store(file, DemoActors.DEMO_ADMIN_ID);
        try {
            return ApiResponse.ok(cadConversionService.registerCadSource(
                    eventId,
                    draftTarget.sceneId(),
                    draftTarget.revisionId(),
                    storedFile.originalName(),
                    storedFile.mimeType(),
                    storedFile.fileSize(),
                    storedFile.fileId(),
                    DemoActors.DEMO_ADMIN_ID
            ));
        } catch (RuntimeException exception) {
            cleanupStoredFile(storedFile.fileId(), exception);
            throw exception;
        } catch (Error error) {
            cleanupStoredFile(storedFile.fileId(), error);
            throw error;
        }
    }

    @GetMapping("/conversion-jobs/{jobId}")
    public ApiResponse<SceneConversionJobResponse> getConversionJob(@PathVariable UUID eventId,
                                                                    @PathVariable UUID jobId) {
        return ApiResponse.ok(cadConversionService.getJob(eventId, jobId));
    }

    @GetMapping("/revisions/{revisionId}")
    public ApiResponse<SceneDraftResponse> getRevision(@PathVariable UUID eventId, @PathVariable UUID revisionId) {
        return ApiResponse.ok(queryService.getRevision(eventId, revisionId));
    }

    private void cleanupStoredFile(UUID storedFileId, Throwable originalFailure) {
        try {
            fileStorageService.delete(storedFileId);
        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }
    }

    @GetMapping("/published")
    public ApiResponse<PublishedSceneResponse> getPublished(@PathVariable UUID eventId) {
        return ApiResponse.ok(queryService.getPublished(eventId));
    }

    @GetMapping("/published/booths")
    public ApiResponse<List<PublishedSceneBoothResponse>> getPublishedBooths(@PathVariable UUID eventId) {
        return ApiResponse.ok(queryService.getPublishedBooths(eventId));
    }
}
