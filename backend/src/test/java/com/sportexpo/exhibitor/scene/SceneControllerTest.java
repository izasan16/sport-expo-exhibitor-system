package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.auth.AuthService;
import com.sportexpo.exhibitor.auth.LoginRequest;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.file.StoredFile;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class SceneControllerTest extends TestDatabaseSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventService eventService;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SceneEditorService sceneEditorService;

    @Autowired
    private TestBusinessFixture fixture;

    @Autowired
    private AuthService authService;

    @Test
    void adminCanSaveSceneBackgroundAndReadDraft() throws Exception {
        var event = eventService.create(new CreateEventRequest("场景测试展会", 2028, "SCENE_DRIVEN", true));
        var fileId = UUID.randomUUID();
        storedFileRepository.save(new StoredFile(
                fileId,
                "hall.png",
                "image/png",
                "2026/06/" + fileId + ".png",
                "/uploads/2026/06/" + fileId + ".png",
                2048L,
                1280,
                720,
                OffsetDateTime.now(),
                DemoActors.DEMO_ADMIN_ID
        ));

        mockMvc.perform(put("/api/events/{eventId}/scene/background", event.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fileId", fileId,
                                "fileName", "hall.png",
                                "mimeType", "image/png",
                                "offsetX", 0,
                                "offsetY", 0,
                                "scale", 1,
                                "opacity", 0.45,
                                "locked", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.fileName").value("hall.png"));

        mockMvc.perform(get("/api/events/{eventId}/scene", event.id())
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.fileName").value("hall.png"));
    }

    @Test
    void adminCanPublishDraftAndQueryPublishedBooths() throws Exception {
        var event = eventService.create(new CreateEventRequest("已发布馆图测试展会", 2030, "SCENE_DRIVEN", true));
        sceneEditorService.saveDraft(event.id(), fixture.sceneDraftWithTwoBooths(), DemoActors.DEMO_ADMIN_ID);

        mockMvc.perform(post("/api/events/{eventId}/scene/draft/publish", event.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .content(objectMapper.writeValueAsString(Map.of("changeSummary", "首次发布"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/events/{eventId}/scene/revisions", event.id())
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));

        mockMvc.perform(get("/api/events/{eventId}/scene/published", event.id())
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.features.length()").value(3));

        mockMvc.perform(get("/api/events/{eventId}/scene/published/booths", event.id())
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].boothNo").value("A01"))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }

    private String tokenFor(String username) {
        return authService.login(new LoginRequest(username, "demo123")).accessToken();
    }
}
