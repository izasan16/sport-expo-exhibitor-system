package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.web.servlet.MockMvc;
import com.sportexpo.exhibitor.auth.AuthService;
import com.sportexpo.exhibitor.auth.LoginRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.file-storage.root-path=target/test-uploads",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
})
class FileUploadControllerTest extends TestDatabaseSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private AuthService authService;

    @Test
    void adminCanUploadSceneBackground() throws Exception {
        mockMvc.perform(multipart("/api/files/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "floorplan.png", "image/png",
                                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}))
                        .header("Authorization", "Bearer " + tokenFor("admin"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").isNotEmpty())
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andExpect(jsonPath("$.data.originalName").value("floorplan.png"));
    }

    private String tokenFor(String username) {
        return authService.login(new LoginRequest(username, "demo123")).accessToken();
    }

    @Test
    void rejectsFileLargerThanConfiguredMultipartLimit() throws Exception {
        byte[] oversized = new byte[2048];
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(new ByteArrayResource(oversized) {
            @Override
            public String getFilename() {
                return "oversized.bin";
            }
        }, partHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/files/upload",
                new HttpEntity<>(body, headers),
                String.class
        );

        org.junit.jupiter.api.Assertions.assertEquals(413, response.getStatusCode().value());
    }
}
