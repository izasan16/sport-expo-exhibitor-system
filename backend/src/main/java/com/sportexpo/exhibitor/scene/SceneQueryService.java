package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.booth.BoothRepository;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneBoothResponse;
import com.sportexpo.exhibitor.scene.dto.PublishedSceneResponse;
import com.sportexpo.exhibitor.scene.dto.SceneDraftResponse;
import com.sportexpo.exhibitor.scene.dto.SceneRevisionResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SceneQueryService {
    private final SceneEditorService editorService;
    private final VenueSceneRepository sceneRepository;
    private final SceneRevisionRepository revisionRepository;
    private final BoothSceneBindingRepository boothSceneBindingRepository;
    private final SceneFeatureRepository featureRepository;
    private final BoothRepository boothRepository;

    public SceneQueryService(SceneEditorService editorService,
                             VenueSceneRepository sceneRepository,
                             SceneRevisionRepository revisionRepository,
                             BoothSceneBindingRepository boothSceneBindingRepository,
                             SceneFeatureRepository featureRepository,
                             BoothRepository boothRepository) {
        this.editorService = editorService;
        this.sceneRepository = sceneRepository;
        this.revisionRepository = revisionRepository;
        this.boothSceneBindingRepository = boothSceneBindingRepository;
        this.featureRepository = featureRepository;
        this.boothRepository = boothRepository;
    }

    public SceneDraftResponse getDraft(UUID eventId, UUID actorId) {
        return editorService.getDraft(eventId, actorId);
    }

    public List<SceneRevisionResponse> getRevisions(UUID eventId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        return revisionRepository.findBySceneIdOrderByRevisionNoDesc(scene.getId()).stream()
                .filter(revision -> "PUBLISHED".equals(revision.getStatus()))
                .map(revision -> new SceneRevisionResponse(
                        revision.getId(),
                        revision.getRevisionNo(),
                        revision.getStatus(),
                        revision.getChangeSummary(),
                        revision.getPublishedAt(),
                        revision.getPublishedBy(),
                        revision.getCreatedAt()
                ))
                .toList();
    }

    public SceneDraftResponse getRevision(UUID eventId, UUID revisionId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        var revision = revisionRepository.findById(revisionId)
                .filter(item -> scene.getId().equals(item.getSceneId()))
                .orElseThrow(() -> new BusinessException("Scene revision does not exist"));
        return editorService.buildSceneResponse(scene, revision);
    }

    public PublishedSceneResponse getPublished(UUID eventId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        var revision = publishedRevision(scene);
        var response = editorService.buildSceneResponse(scene, revision);
        return new PublishedSceneResponse(
                response.sceneId(),
                response.revisionId(),
                response.status(),
                response.canvasWidth(),
                response.canvasHeight(),
                response.pixelToMeterRatio(),
                response.background(),
                response.features()
        );
    }

    public List<PublishedSceneBoothResponse> getPublishedBooths(UUID eventId) {
        var scene = sceneRepository.findByEventId(eventId)
                .orElseThrow(() -> new BusinessException("Scene does not exist"));
        var revision = publishedRevision(scene);
        var featuresById = featureRepository.findBySceneRevisionIdOrderByLayerOrderAsc(revision.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(SceneFeature::getId, feature -> feature));

        return boothSceneBindingRepository.findBySceneRevisionIdAndBindingStatus(revision.getId(), "ACTIVE").stream()
                .map(binding -> {
                    var booth = boothRepository.findById(binding.getBoothId())
                            .orElseThrow(() -> new BusinessException("Scene booth does not exist"));
                    var feature = featuresById.get(binding.getSceneFeatureId());
                    return new PublishedSceneBoothResponse(
                            booth.getId(),
                            booth.getBoothNo(),
                            booth.getBoothType(),
                            booth.getStatus(),
                            booth.getAreaSqm(),
                            editorService.rawGeometry(feature)
                    );
                })
                .sorted(Comparator.comparing(PublishedSceneBoothResponse::boothNo))
                .toList();
    }

    private SceneRevision publishedRevision(VenueScene scene) {
        return revisionRepository.findById(scene.getPublishedRevisionId())
                .orElseThrow(() -> new BusinessException("Published scene does not exist"));
    }
}
