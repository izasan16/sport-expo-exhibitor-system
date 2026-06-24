package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.file.StoredFile;
import com.sportexpo.exhibitor.file.StoredFileRepository;
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
    private SceneCadConversionService sceneCadConversionService;

    @Autowired
    private SceneSourceFileRepository sceneSourceFileRepository;

    @Autowired
    private SceneConversionJobRepository sceneConversionJobRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

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

        var sourceFile = sceneSourceFileRepository.findById(response.id()).orElseThrow();
        var job = sceneConversionJobRepository.findById(response.job().id()).orElseThrow();

        assertThat(response.sourceFormat()).isEqualTo("DXF");
        assertThat(response.job().jobStatus()).isEqualTo("PENDING");
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
        assertThat(sceneSourceFileRepository.findById(response.id()).orElseThrow().getSourceFormat()).isEqualTo("DWG");
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
