package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.booth.Booth;
import com.sportexpo.exhibitor.booth.BoothRepository;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.scene.dto.PublishSceneRequest;
import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneFeaturePayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScenePublishService {
    private final EventRepository eventRepository;
    private final VenueSceneRepository sceneRepository;
    private final SceneRevisionRepository revisionRepository;
    private final SceneFeatureRepository featureRepository;
    private final SceneBackgroundAssetRepository backgroundAssetRepository;
    private final BoothRepository boothRepository;
    private final BoothSceneBindingRepository boothSceneBindingRepository;
    private final SceneValidationService validationService;
    private final SceneEditorService editorService;
    private final AuditService auditService;

    public ScenePublishService(EventRepository eventRepository,
                               VenueSceneRepository sceneRepository,
                               SceneRevisionRepository revisionRepository,
                               SceneFeatureRepository featureRepository,
                               SceneBackgroundAssetRepository backgroundAssetRepository,
                               BoothRepository boothRepository,
                               BoothSceneBindingRepository boothSceneBindingRepository,
                               SceneValidationService validationService,
                               SceneEditorService editorService,
                               AuditService auditService) {
        this.eventRepository = eventRepository;
        this.sceneRepository = sceneRepository;
        this.revisionRepository = revisionRepository;
        this.featureRepository = featureRepository;
        this.backgroundAssetRepository = backgroundAssetRepository;
        this.boothRepository = boothRepository;
        this.boothSceneBindingRepository = boothSceneBindingRepository;
        this.validationService = validationService;
        this.editorService = editorService;
        this.auditService = auditService;
    }

    @Transactional
    public com.sportexpo.exhibitor.scene.dto.SceneDraftResponse publishDraft(UUID eventId,
                                                                             PublishSceneRequest request,
                                                                             UUID actorId) {
        eventRepository.findById(eventId)
                .filter(event -> "SCENE_DRIVEN".equals(event.getBoothSourceMode()))
                .orElseThrow(() -> new BusinessException("Event scene publishing is not enabled"));

        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene draft does not exist"));
        var draftRevision = revisionRepository.findFirstBySceneIdAndStatusOrderByRevisionNoDesc(scene.getId(), "DRAFT")
                .orElseThrow(() -> new BusinessException("Scene draft does not exist"));

        var draftRequest = toSaveRequest(scene, draftRevision);
        var validation = validationService.validateDraft(draftRequest);
        if (!validation.valid()) {
            throw new BusinessException("Scene draft validation failed");
        }

        var now = OffsetDateTime.now();
        syncBooths(scene, draftRevision, actorId, now);
        draftRevision.markPublished(request.changeSummary(), actorId, now);
        revisionRepository.save(draftRevision);
        scene.markPublished(draftRevision.getId(), actorId, now);
        sceneRepository.save(scene);
        clonePublishedRevisionAsDraft(scene, draftRevision, actorId, now);
        auditService.record(eventId, actorId, "SCENE_PUBLISH", "VenueScene", scene.getId(), "DRAFT", "PUBLISHED",
                request.changeSummary());
        return editorService.buildSceneResponse(scene, draftRevision);
    }

    private void syncBooths(VenueScene scene, SceneRevision revision, UUID actorId, OffsetDateTime now) {
        var boothFeatures = featureRepository.findBySceneRevisionIdOrderByLayerOrderAsc(revision.getId()).stream()
                .filter(feature -> "BOOTH".equals(feature.getFeatureType()))
                .toList();

        var existingByBoothNo = new HashMap<String, Booth>();
        boothRepository.findByEventId(scene.getEventId()).forEach(booth -> existingByBoothNo.put(booth.getBoothNo(), booth));

        var publishedBoothNos = new java.util.HashSet<String>();
        var boundBoothIds = new ArrayList<UUID>();
        var existingBindings = boothSceneBindingRepository.findByEventId(scene.getEventId());
        var bindingsByBoothId = new HashMap<UUID, BoothSceneBinding>();
        existingBindings.forEach(binding -> bindingsByBoothId.put(binding.getBoothId(), binding));

        for (var feature : boothFeatures) {
            var boothNo = boothNo(feature);
            publishedBoothNos.add(boothNo);
            var areaSqm = feature.getAreaSqm() == null ? BigDecimal.ZERO : feature.getAreaSqm();
            var bounds = polygonBounds(validationService.geometry(editorService.rawGeometry(feature)));
            var boothType = boothType(feature);
            var existing = existingByBoothNo.get(boothNo);
            Booth persisted;
            if (existing == null) {
                persisted = boothRepository.save(new Booth(
                        UUID.randomUUID(),
                        scene.getEventId(),
                        null,
                        null,
                        boothNo,
                        areaSqm,
                        boothType,
                        BusinessStatuses.BOOTH_AVAILABLE,
                        bounds.minX(),
                        bounds.minY(),
                        bounds.width(),
                        bounds.height(),
                        now,
                        now,
                        actorId
                ));
            } else {
                existing.applySceneSnapshot(areaSqm, boothType, bounds.minX(), bounds.minY(), bounds.width(), bounds.height(),
                        actorId, now);
                persisted = boothRepository.save(existing);
            }

            boundBoothIds.add(persisted.getId());
            var currentBinding = bindingsByBoothId.get(persisted.getId());
            if (currentBinding == null) {
                boothSceneBindingRepository.save(new BoothSceneBinding(
                        UUID.randomUUID(),
                        scene.getEventId(),
                        revision.getId(),
                        feature.getId(),
                        persisted.getId(),
                        "ACTIVE",
                        now,
                        now
                ));
            } else {
                currentBinding.rebind(revision.getId(), feature.getId(), now);
                boothSceneBindingRepository.save(currentBinding);
            }
        }

        boothRepository.findByEventId(scene.getEventId()).stream()
                .filter(booth -> !publishedBoothNos.contains(booth.getBoothNo()))
                .forEach(booth -> {
                    if (!BusinessStatuses.BOOTH_AVAILABLE.equals(booth.getStatus())) {
                        throw new BusinessException("Published scene cannot remove non-available booth");
                    }
                    boothSceneBindingRepository.deleteByBoothId(booth.getId());
                    boothRepository.delete(booth);
                });
    }

    private void clonePublishedRevisionAsDraft(VenueScene scene, SceneRevision publishedRevision, UUID actorId, OffsetDateTime now) {
        var draftRevision = new SceneRevision(
                UUID.randomUUID(),
                scene.getId(),
                publishedRevision.getRevisionNo() + 1,
                "DRAFT",
                null,
                publishedRevision.getSnapshotJson(),
                null,
                null,
                now,
                now,
                actorId,
                actorId
        );
        revisionRepository.save(draftRevision);

        featureRepository.findBySceneRevisionIdOrderByLayerOrderAsc(publishedRevision.getId())
                .forEach(feature -> featureRepository.save(new SceneFeature(
                        UUID.randomUUID(),
                        draftRevision.getId(),
                        feature.getEventId(),
                        feature.getFeatureType(),
                        feature.getGeometryType(),
                        feature.getGeometryJson(),
                        feature.getLabel(),
                        feature.getCode(),
                        feature.getLayerOrder(),
                        feature.getStyleJson(),
                        feature.getMetaJson(),
                        feature.getAreaPx2(),
                        feature.getAreaSqm(),
                        feature.isLocked(),
                        now,
                        now
                )));

        backgroundAssetRepository.findBySceneRevisionId(publishedRevision.getId())
                .ifPresent(asset -> backgroundAssetRepository.save(new SceneBackgroundAsset(
                        UUID.randomUUID(),
                        scene.getId(),
                        draftRevision.getId(),
                        asset.getFileId(),
                        asset.getFileName(),
                        asset.getMimeType(),
                        asset.getSourceType(),
                        asset.getWidth(),
                        asset.getHeight(),
                        asset.getOffsetX(),
                        asset.getOffsetY(),
                        asset.getScale(),
                        asset.getOpacity(),
                        asset.isLocked(),
                        now,
                        now,
                        actorId,
                        actorId
                )));
    }

    private SaveSceneDraftRequest toSaveRequest(VenueScene scene, SceneRevision revision) {
        var features = featureRepository.findBySceneRevisionIdOrderByLayerOrderAsc(revision.getId()).stream()
                .map(editorService::toFeaturePayload)
                .toList();
        return new SaveSceneDraftRequest(
                scene.getCanvasWidth(),
                scene.getCanvasHeight(),
                scene.getPixelToMeterRatio(),
                features
        );
    }

    private String boothNo(SceneFeature feature) {
        if (feature.getCode() != null && !feature.getCode().isBlank()) {
            return feature.getCode();
        }
        var meta = editorService.rawMeta(feature);
        var rawValue = meta.get("boothNo");
        if (rawValue == null || String.valueOf(rawValue).isBlank()) {
            throw new BusinessException("Scene booth feature requires boothNo");
        }
        return String.valueOf(rawValue);
    }

    private String boothType(SceneFeature feature) {
        var meta = editorService.rawMeta(feature);
        var rawValue = meta.get("boothType");
        return rawValue == null || String.valueOf(rawValue).isBlank()
                ? "STANDARD"
                : String.valueOf(rawValue);
    }

    private PolygonBounds polygonBounds(SceneGeometry geometry) {
        var minX = geometry.coordinates().stream().map(point -> point.get(0)).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        var maxX = geometry.coordinates().stream().map(point -> point.get(0)).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        var minY = geometry.coordinates().stream().map(point -> point.get(1)).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        var maxY = geometry.coordinates().stream().map(point -> point.get(1)).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return new PolygonBounds(
                minX,
                minY,
                maxX.subtract(minX),
                maxY.subtract(minY)
        );
    }

    record PolygonBounds(BigDecimal minX, BigDecimal minY, BigDecimal width, BigDecimal height) {
    }
}
