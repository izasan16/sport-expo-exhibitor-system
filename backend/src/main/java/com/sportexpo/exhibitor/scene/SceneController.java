package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.scene.dto.PublishSceneRequest;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneBoothResponse;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneResponse;
import com.sportexpo.exhibitor.scene.dto.SaveSceneBackgroundRequest;
import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneDraftResponse;
import com.sportexpo.exhibitor.scene.dto.SceneRevisionResponse;
import com.sportexpo.exhibitor.scene.dto.SceneValidationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/scene")
public class SceneController {
    private final SceneQueryService queryService;
    private final SceneEditorService editorService;
    private final ScenePublishService publishService;
    private final SceneValidationService validationService;

    public SceneController(SceneQueryService queryService, SceneEditorService editorService,
                           ScenePublishService publishService,
                           SceneValidationService validationService) {
        this.queryService = queryService;
        this.editorService = editorService;
        this.publishService = publishService;
        this.validationService = validationService;
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

    @GetMapping("/revisions/{revisionId}")
    public ApiResponse<SceneDraftResponse> getRevision(@PathVariable UUID eventId, @PathVariable UUID revisionId) {
        return ApiResponse.ok(queryService.getRevision(eventId, revisionId));
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
