package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final CurrentUserService currentUserService;

    public FileUploadController(LocalFileStorageService storageService, CurrentUserService currentUserService) {
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<UploadedFileResponse> upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return ApiResponse.ok(storageService.store(file, currentUserService.getCurrentUser(authentication).userId()));
    }
}
