package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import com.sportexpo.exhibitor.scene.dto.SaveSceneBackgroundRequest;
import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneBackgroundResponse;
import com.sportexpo.exhibitor.scene.dto.SceneDraftResponse;
import com.sportexpo.exhibitor.scene.dto.SceneFeaturePayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SceneEditorService {
    private static final int DEFAULT_CANVAS_WIDTH = 1200;
    private static final int DEFAULT_CANVAS_HEIGHT = 800;
    private static final BigDecimal DEFAULT_RATIO = new BigDecimal("20.0000");

    private final EventRepository eventRepository;
    private final VenueSceneRepository sceneRepository;
    private final SceneRevisionRepository revisionRepository;
    private final SceneFeatureRepository featureRepository;
    private final SceneBackgroundAssetRepository backgroundAssetRepository;
    private final StoredFileRepository storedFileRepository;
    private final SceneValidationService validationService;
    private final SceneGeometryCalculator geometryCalculator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SceneEditorService(EventRepository eventRepository, VenueSceneRepository sceneRepository,
                              SceneRevisionRepository revisionRepository, SceneFeatureRepository featureRepository,
                              SceneBackgroundAssetRepository backgroundAssetRepository,
                              StoredFileRepository storedFileRepository, SceneValidationService validationService,
                              SceneGeometryCalculator geometryCalculator, AuditService auditService,
                              ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.sceneRepository = sceneRepository;
        this.revisionRepository = revisionRepository;
        this.featureRepository = featureRepository;
        this.backgroundAssetRepository = backgroundAssetRepository;
        this.storedFileRepository = storedFileRepository;
        this.validationService = validationService;
        this.geometryCalculator = geometryCalculator;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SceneDraftResponse getDraft(UUID eventId, UUID actorId) {
        return buildDraftResponse(loadOrCreateDraft(eventId, actorId, null));
    }

    @Transactional
    public DraftTarget getDraftTarget(UUID eventId, UUID actorId) {
        var draftContext = loadOrCreateDraft(eventId, actorId, null);
        return new DraftTarget(draftContext.scene().getId(), draftContext.revision().getId());
    }

    @Transactional
    public SceneDraftResponse saveDraft(UUID eventId, SaveSceneDraftRequest request, UUID actorId) {
        validationService.validatePayloadShape(request.safeFeatures());
        var draftContext = loadOrCreateDraft(eventId, actorId, request);
        var now = OffsetDateTime.now();
        draftContext.scene().updateDraftMetrics(request.canvasWidth(), request.canvasHeight(), request.pixelToMeterRatio(), actorId, now);
        sceneRepository.save(draftContext.scene());
        draftContext.revision().updateSnapshot(objectMapper.valueToTree(request), actorId, now);
        revisionRepository.save(draftContext.revision());

        backgroundAssetRepository.findBySceneRevisionId(draftContext.revision().getId()).ifPresent(existing -> {
            // keep current background for the new draft snapshot
        });
        featureRepository.deleteBySceneRevisionId(draftContext.revision().getId());
        for (var feature : request.safeFeatures()) {
            featureRepository.save(toFeatureEntity(
                    draftContext.scene().getEventId(),
                    draftContext.revision().getId(),
                    request.pixelToMeterRatio(),
                    feature,
                    now
            ));
        }
        auditService.record(eventId, actorId, "SCENE_DRAFT_SAVE", "VenueScene", draftContext.scene().getId(), null, "DRAFT", "save draft");
        return buildDraftResponse(draftContext);
    }

    @Transactional
    public SceneDraftResponse saveBackground(UUID eventId, SaveSceneBackgroundRequest request, UUID actorId) {
        var draftContext = loadOrCreateDraft(eventId, actorId, null);
        var storedFile = storedFileRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException("Scene background file does not exist"));
        if (!Set.of("image/png", "image/jpeg", "image/svg+xml", "application/pdf")
                .contains(storedFile.getMimeType().toLowerCase())) {
            throw new BusinessException("Scene background file type is not supported");
        }
        if (!storedFile.getMimeType().equalsIgnoreCase(request.mimeType())) {
            throw new BusinessException("Scene background MIME type does not match stored file");
        }
        var now = OffsetDateTime.now();
        backgroundAssetRepository.deleteBySceneRevisionId(draftContext.revision().getId());
        backgroundAssetRepository.save(new SceneBackgroundAsset(
                UUID.randomUUID(),
                draftContext.scene().getId(),
                draftContext.revision().getId(),
                storedFile.getId(),
                request.fileName(),
                request.mimeType(),
                "CAD_EXPORT",
                storedFile.getWidth(),
                storedFile.getHeight(),
                defaultDecimal(request.offsetX()),
                defaultDecimal(request.offsetY()),
                request.scale(),
                request.opacity(),
                request.locked(),
                now,
                now,
                actorId,
                actorId
        ));
        auditService.record(eventId, actorId, "SCENE_BACKGROUND_SAVE", "VenueScene", draftContext.scene().getId(), null, "DRAFT", request.fileName());
        return buildDraftResponse(draftContext);
    }

    @Transactional
    public SceneDraftResponse clearBackground(UUID eventId, UUID actorId) {
        var draftContext = loadOrCreateDraft(eventId, actorId, null);
        backgroundAssetRepository.deleteBySceneRevisionId(draftContext.revision().getId());
        auditService.record(eventId, actorId, "SCENE_BACKGROUND_CLEAR", "VenueScene", draftContext.scene().getId(), null, "DRAFT", "clear background");
        return buildDraftResponse(draftContext);
    }

    private DraftContext loadOrCreateDraft(UUID eventId, UUID actorId, SaveSceneDraftRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        if (!"SCENE_DRIVEN".equals(event.getBoothSourceMode())) {
            throw new BusinessException("Event scene editing is not enabled");
        }

        var now = OffsetDateTime.now();
        var scene = sceneRepository.findByEventId(eventId)
                .orElseGet(() -> sceneRepository.save(new VenueScene(
                        UUID.randomUUID(),
                        eventId,
                        "DRAFT",
                        request != null ? request.canvasWidth() : DEFAULT_CANVAS_WIDTH,
                        request != null ? request.canvasHeight() : DEFAULT_CANVAS_HEIGHT,
                        request != null ? request.pixelToMeterRatio() : DEFAULT_RATIO,
                        null,
                        now,
                        now,
                        actorId,
                        actorId
                )));

        var revision = revisionRepository.findFirstBySceneIdAndStatusOrderByRevisionNoDesc(scene.getId(), "DRAFT")
                .orElseGet(() -> revisionRepository.save(new SceneRevision(
                        UUID.randomUUID(),
                        scene.getId(),
                        nextRevisionNo(scene.getId()),
                        "DRAFT",
                        null,
                        objectMapper.valueToTree(Map.of()),
                        null,
                        null,
                        now,
                        now,
                        actorId,
                        actorId
                )));
        return new DraftContext(scene, revision);
    }

    private int nextRevisionNo(UUID sceneId) {
        return revisionRepository.findBySceneIdOrderByRevisionNoDesc(sceneId).stream()
                .findFirst()
                .map(item -> item.getRevisionNo() + 1)
                .orElse(1);
    }

    private SceneFeature toFeatureEntity(UUID eventId, UUID revisionId, BigDecimal pixelToMeterRatio,
                                         SceneFeaturePayload feature, OffsetDateTime now) {
        JsonNode geometryJson = objectMapper.valueToTree(feature.geometry());
        JsonNode metaJson = objectMapper.valueToTree(feature.meta() == null ? Map.of() : feature.meta());
        var geometry = validationService.geometry(feature.geometry());
        var areaSqm = "BOOTH".equals(feature.featureType())
                ? geometryCalculator.calculateAreaSqm(geometry, pixelToMeterRatio)
                : null;
        var areaPx2 = "BOOTH".equals(feature.featureType()) && areaSqm != null
                ? areaSqm.multiply(pixelToMeterRatio.multiply(pixelToMeterRatio)).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new SceneFeature(
                featureId(feature.id()),
                revisionId,
                eventId,
                feature.featureType(),
                feature.geometryType(),
                geometryJson,
                feature.label(),
                feature.code(),
                feature.layerOrder() == null ? 0 : feature.layerOrder(),
                objectMapper.valueToTree(Map.of()),
                metaJson,
                areaPx2,
                areaSqm,
                false,
                now,
                now
        );
    }

    SceneDraftResponse buildDraftResponse(DraftContext context) {
        return buildSceneResponse(context.scene(), context.revision());
    }

    SceneDraftResponse buildSceneResponse(VenueScene scene, SceneRevision revision) {
        var background = backgroundAssetRepository.findBySceneRevisionId(revision.getId())
                .map(asset -> {
                    var file = storedFileRepository.findById(asset.getFileId()).orElse(null);
                    return new SceneBackgroundResponse(
                            asset.getFileId(),
                            asset.getFileName(),
                            asset.getMimeType(),
                            file != null ? file.getPublicUrl() : null,
                            asset.getWidth(),
                            asset.getHeight(),
                            asset.getOffsetX(),
                            asset.getOffsetY(),
                            asset.getScale(),
                            asset.getOpacity(),
                            asset.isLocked()
                    );
                })
                .orElse(null);

        var features = featureRepository.findBySceneRevisionIdOrderByLayerOrderAsc(revision.getId()).stream()
                .map(this::toFeaturePayload)
                .toList();

        return new SceneDraftResponse(
                scene.getId(),
                revision.getId(),
                revision.getStatus(),
                scene.getCanvasWidth(),
                scene.getCanvasHeight(),
                scene.getPixelToMeterRatio(),
                background,
                features
        );
    }

    SceneFeaturePayload toFeaturePayload(SceneFeature feature) {
        return new SceneFeaturePayload(
                feature.getId().toString(),
                feature.getFeatureType(),
                feature.getGeometryType(),
                rawGeometry(feature),
                feature.getLabel(),
                feature.getCode(),
                feature.getLayerOrder(),
                rawMeta(feature)
        );
    }

    Object rawGeometry(SceneFeature feature) {
        return objectMapper.convertValue(feature.getGeometryJson(), Object.class);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> rawMeta(SceneFeature feature) {
        return objectMapper.convertValue(feature.getMetaJson(), Map.class);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private UUID featureId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }

    record DraftContext(VenueScene scene, SceneRevision revision) {
    }

    public record DraftTarget(UUID sceneId, UUID revisionId) {
    }
}
