package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.auth.AuthService;
import com.sportexpo.exhibitor.auth.LoginRequest;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = "app.file-storage.root-path=target/test-uploads-cad")
class SceneCadUploadControllerTest extends TestDatabaseSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private VenueSceneRepository venueSceneRepository;

    @Autowired
    private AuthService authService;

    @SpyBean
    private SceneCadConversionService cadConversionService;

    @Value("${app.file-storage.root-path}")
    private String rootPath;

    @Test
    void adminCanUploadCadFileAndPollPendingConversionJob() throws Exception {
        var event = eventService.create(new CreateEventRequest("CAD 控制器测试展会", 2034, "SCENE_DRIVEN", true));

        var uploadResponse = mockMvc.perform(multipart("/api/events/{eventId}/scene/source-files", event.id())
                        .file(new MockMultipartFile("file", "hall-plan.dxf", "application/dxf", "0\nSECTION\n2\nHEADER\n0\nENDSEC\n0\nEOF".getBytes()))
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .contentType(MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceFileId").isNotEmpty())
                .andExpect(jsonPath("$.data.sourceFormat").value("DXF"))
                .andExpect(jsonPath("$.data.originalName").value("hall-plan.dxf"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.job.jobId").isNotEmpty())
                .andExpect(jsonPath("$.data.job.jobStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.job.id").doesNotExist())
                .andReturn();

        var jobId = objectMapper.readTree(uploadResponse.getResponse().getContentAsString())
                .path("data")
                .path("job")
                .path("jobId")
                .asText();

        mockMvc.perform(get("/api/events/{eventId}/scene/conversion-jobs/{jobId}", event.id(), jobId)
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(jobId))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.jobStatus").value("PENDING"));
    }

    @Test
    void rejectsUnsupportedCadUploadBeforePersistingStoredFile() throws Exception {
        var event = eventService.create(new CreateEventRequest("CAD 错误格式测试展会", 2037, "SCENE_DRIVEN", true));
        var storedFileCountBefore = storedFileRepository.count();
        var diskFileCountBefore = storedFileCountOnDisk();
        var sceneExistsBefore = venueSceneRepository.findByEventId(event.id()).isPresent();

        mockMvc.perform(multipart("/api/events/{eventId}/scene/source-files", event.id())
                        .file(new MockMultipartFile("file", "hall-plan.svg", "image/svg+xml", "<svg/>".getBytes()))
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .contentType(MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Only DWG and DXF files are supported")));

        org.junit.jupiter.api.Assertions.assertEquals(sceneExistsBefore, venueSceneRepository.findByEventId(event.id()).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(storedFileCountBefore, storedFileRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(diskFileCountBefore, storedFileCountOnDisk());
    }

    @Test
    void cleansUpStoredFileWhenCadRegistrationFailsAfterUpload() throws Exception {
        var event = eventService.create(new CreateEventRequest("CAD 补偿清理测试展会", 2038, "SCENE_DRIVEN", true));
        var storedFileCountBefore = storedFileRepository.count();
        var diskFileCountBefore = storedFileCountOnDisk();
        doThrow(new BusinessException("Scene revision is not the current draft"))
                .when(cadConversionService)
                .registerCadSource(any(), any(), any(), any(), any(), anyLong(), any(), any());

        mockMvc.perform(multipart("/api/events/{eventId}/scene/source-files", event.id())
                        .file(new MockMultipartFile("file", "hall-plan.dxf", "application/dxf", "0\nSECTION\n2\nHEADER\n0\nENDSEC\n0\nEOF".getBytes()))
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .contentType(MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Scene revision is not the current draft")));

        org.junit.jupiter.api.Assertions.assertEquals(storedFileCountBefore, storedFileRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(diskFileCountBefore, storedFileCountOnDisk());
    }

    private long storedFileCountOnDisk() throws IOException {
        var uploadRoot = Path.of(rootPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadRoot)) {
            return 0L;
        }
        try (Stream<Path> paths = Files.walk(uploadRoot)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private String tokenFor(String username) {
        return authService.login(new LoginRequest(username, "demo123")).accessToken();
    }
}
