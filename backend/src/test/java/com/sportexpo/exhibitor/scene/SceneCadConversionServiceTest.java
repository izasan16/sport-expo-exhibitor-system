package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.file.StoredFile;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import com.sportexpo.exhibitor.scene.dto.PublishSceneRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SceneCadConversionServiceTest extends TestDatabaseSupport {
    @Autowired
    private EventService eventService;

    @Autowired
    private SceneQueryService queryService;

    @Autowired
    private SceneEditorService sceneEditorService;

    @Autowired
    private ScenePublishService scenePublishService;

    @Autowired
    private SceneCadConversionService sceneCadConversionService;

    @Autowired
    private SceneSourceFileRepository sceneSourceFileRepository;

    @Autowired
    private SceneConversionJobRepository sceneConversionJobRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private TestBusinessFixture fixture;

    @Test
    void registersDxfSourceFileAndCreatesPendingConversionJob() {
        var event = eventService.create(new CreateEventRequest("CAD 上传测试展会", 2031, "SCENE_DRIVEN", true));
        var draft = queryService.getDraft(event.id(), DemoActors.DEMO_ADMIN_ID);
        var storedFile = createStoredFile("hall-plan.DXF", "application/dxf", 4096L);

        var response = sceneCadConversionService.registerCadSource(
                event.id(),
                draft.sceneId(),
                draft.revisionId(),
                storedFile.getOriginalName(),
                storedFile.getMimeType(),
                storedFile.getFileSize(),
                storedFile.getId(),
                DemoActors.DEMO_ADMIN_ID
        );

        var sourceFile = sceneSourceFileRepository.findById(response.sourceFileId()).orElseThrow();
        var job = sceneConversionJobRepository.findById(response.job().jobId()).orElseThrow();

        assertThat(response.sourceFormat()).isEqualTo("DXF");
        assertThat(response.sourceFileId()).isEqualTo(sourceFile.getId());
        assertThat(response.job().jobStatus()).isEqualTo("PENDING");
        assertThat(response.job().jobId()).isEqualTo(job.getId());
        assertThat(sourceFile.getSceneId()).isEqualTo(draft.sceneId());
        assertThat(sourceFile.getSceneRevisionId()).isEqualTo(draft.revisionId());
        assertThat(sourceFile.getStoredFileId()).isEqualTo(storedFile.getId());
        assertThat(sourceFile.getSourceFormat()).isEqualTo("DXF");
        assertThat(job.getSceneId()).isEqualTo(draft.sceneId());
        assertThat(job.getSceneRevisionId()).isEqualTo(draft.revisionId());
        assertThat(job.getSourceFileId()).isEqualTo(sourceFile.getId());
        assertThat(job.getJobStatus()).isEqualTo("PENDING");
    }

    @Test
    void normalizesDwgSourceFormat() {
        var event = eventService.create(new CreateEventRequest("DWG 上传测试展会", 2032, "SCENE_DRIVEN", true));
        var draft = queryService.getDraft(event.id(), DemoActors.DEMO_ADMIN_ID);
        var storedFile = createStoredFile("hall-plan.dwg", "application/acad", 2048L);

        var response = sceneCadConversionService.registerCadSource(
                event.id(),
                draft.sceneId(),
                draft.revisionId(),
                storedFile.getOriginalName(),
                storedFile.getMimeType(),
                storedFile.getFileSize(),
                storedFile.getId(),
                DemoActors.DEMO_ADMIN_ID
        );

        assertThat(response.sourceFormat()).isEqualTo("DWG");
        assertThat(sceneSourceFileRepository.findById(response.sourceFileId()).orElseThrow().getSourceFormat()).isEqualTo("DWG");
    }

    @Test
    void rejectsUnsupportedSourceFormat() {
        var event = eventService.create(new CreateEventRequest("错误格式测试展会", 2033, "SCENE_DRIVEN", true));
        var draft = queryService.getDraft(event.id(), DemoActors.DEMO_ADMIN_ID);
        var storedFile = createStoredFile("hall-plan.svg", "image/svg+xml", 1024L);

        assertThatThrownBy(() -> sceneCadConversionService.registerCadSource(
                event.id(),
                draft.sceneId(),
                draft.revisionId(),
                storedFile.getOriginalName(),
                storedFile.getMimeType(),
                storedFile.getFileSize(),
                storedFile.getId(),
                DemoActors.DEMO_ADMIN_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only DWG and DXF files are supported");
    }

    @Test
    void draftTargetCreatesAndReturnsCurrentDraftIdentifiers() {
        var event = eventService.create(new CreateEventRequest("草稿目标测试展会", 2035, "SCENE_DRIVEN", true));

        var draftTarget = sceneEditorService.getDraftTarget(event.id(), DemoActors.DEMO_ADMIN_ID);
        var draft = queryService.getDraft(event.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(draftTarget.sceneId()).isEqualTo(draft.sceneId());
        assertThat(draftTarget.revisionId()).isEqualTo(draft.revisionId());
    }

    @Test
    void rejectsRegistrationForPublishedOrStaleRevision() {
        var event = eventService.create(new CreateEventRequest("过期修订测试展会", 2036, "SCENE_DRIVEN", true));
        var originalDraft = sceneEditorService.saveDraft(event.id(), fixture.sceneDraftWithTwoBooths(), DemoActors.DEMO_ADMIN_ID);
        scenePublishService.publishDraft(event.id(), new PublishSceneRequest("首次发布"), DemoActors.DEMO_ADMIN_ID);
        var currentDraft = queryService.getDraft(event.id(), DemoActors.DEMO_ADMIN_ID);
        var storedFile = createStoredFile("hall-plan.dxf", "application/dxf", 1024L);

        assertThat(currentDraft.revisionId()).isNotEqualTo(originalDraft.revisionId());
        assertThatThrownBy(() -> sceneCadConversionService.registerCadSource(
                event.id(),
                originalDraft.sceneId(),
                originalDraft.revisionId(),
                storedFile.getOriginalName(),
                storedFile.getMimeType(),
                storedFile.getFileSize(),
                storedFile.getId(),
                DemoActors.DEMO_ADMIN_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Scene revision is not the current draft");
    }

    private StoredFile createStoredFile(String originalName, String mimeType, long fileSize) {
        var fileId = UUID.randomUUID();
        return storedFileRepository.save(new StoredFile(
                fileId,
                originalName,
                mimeType,
                "2026/06/" + fileId,
                "/uploads/2026/06/" + fileId,
                fileSize,
                null,
                null,
                OffsetDateTime.now(),
                DemoActors.DEMO_ADMIN_ID
        ));
    }
}
