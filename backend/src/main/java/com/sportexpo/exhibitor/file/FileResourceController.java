package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.EventAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class FileResourceController {
    private final LocalFileStorageService storageService;
    private final CurrentUserService currentUserService;
    private final EventAccessService eventAccessService;
    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public FileResourceController(LocalFileStorageService storageService,
                                  CurrentUserService currentUserService,
                                  EventAccessService eventAccessService,
                                  JdbcTemplate jdbcTemplate,
                                  @Value("${app.db.schema}") String schema) {
        this.storageService = storageService;
        this.currentUserService = currentUserService;
        this.eventAccessService = eventAccessService;
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    @GetMapping("/uploads/**")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(HttpServletRequest request, Authentication authentication) {
        var path = request.getRequestURI().substring("/uploads/".length());
        var storedFile = storageService.findByStoragePath(path);
        var user = currentUserService.getCurrentUser(authentication);
        if (!user.hasRole("SYSTEM_ADMIN") && !user.hasRole("ORGANIZER_ADMIN")) {
            var eventId = eventIdForFile(storedFile.getId());
            if (eventId == null || !eventAccessService.canAccessEvent(authentication, eventId)) {
                throw new org.springframework.security.access.AccessDeniedException("File access denied");
            }
        }
        var mediaType = MediaType.parseMediaType(storedFile.getMimeType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName(storedFile.getOriginalName()) + "\"")
                .contentType(mediaType)
                .body(storageService.readContent(storedFile.getId()));
    }

    private UUID eventIdForFile(UUID fileId) {
        var sql = "select event_id from " + schema + ".venue_scenes where id in "
                + "(select scene_id from " + schema + ".scene_background_assets where file_id = ? "
                + "union select scene_id from " + schema + ".scene_source_files where file_id = ? "
                + "union select scene_id from " + schema + ".scene_conversion_jobs j "
                + "where j.result_file_id = ?) limit 1";
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getObject(1, UUID.class) : null, fileId, fileId, fileId);
    }

    private String safeFileName(String name) {
        return name == null ? "file" : name.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
