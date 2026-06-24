package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.file.dto.UploadedFileResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    private final LocalFileStorageService storageService;

    public FileUploadController(LocalFileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedFileResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(storageService.store(file, DemoActors.DEMO_ADMIN_ID));
    }
}
