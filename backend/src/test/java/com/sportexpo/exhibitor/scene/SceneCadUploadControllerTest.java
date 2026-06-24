package com.sportexpo.exhibitor.scene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    void adminCanUploadCadFileAndPollPendingConversionJob() throws Exception {
        var event = eventService.create(new CreateEventRequest("CAD 控制器测试展会", 2034, "SCENE_DRIVEN", true));

        var uploadResponse = mockMvc.perform(multipart("/api/events/{eventId}/scene/source-files", event.id())
                        .file(new MockMultipartFile("file", "hall-plan.dxf", "application/dxf", "dxf-content".getBytes()))
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

        mockMvc.perform(get("/api/events/{eventId}/scene/conversion-jobs/{jobId}", event.id(), jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(jobId))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.jobStatus").value("PENDING"));
    }
}
