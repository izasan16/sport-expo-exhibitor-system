package com.sportexpo.exhibitor.file;

import com.sportexpo.exhibitor.common.BusinessException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FileUploadValidator {
    private static final Map<String, String> MIME_BY_EXTENSION = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "svg", "image/svg+xml", "pdf", "application/pdf",
            "dwg", "application/acad", "dxf", "application/dxf"
    );
    private static final Map<String, Set<String>> COMPATIBLE_MIMES = Map.of(
            "dwg", Set.of("application/acad", "application/dwg", "application/x-dwg", "image/vnd.dwg", "application/octet-stream"),
            "dxf", Set.of("application/dxf", "application/vnd.dxf", "image/vnd.dxf", "text/plain", "application/octet-stream")
    );

    private final long maxBytes;

    public FileUploadValidator(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public void validate(String originalName, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException("Uploaded file must not be empty");
        }
        if (content.length > maxBytes) {
            throw new BusinessException("Uploaded file exceeds the maximum size");
        }
        var extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || extension.isBlank()) {
            throw new BusinessException("Uploaded file extension is required");
        }
        var normalizedExtension = extension.toLowerCase(Locale.ROOT);
        var expectedMime = MIME_BY_EXTENSION.get(normalizedExtension);
        if (expectedMime == null) {
            throw new BusinessException("Uploaded file extension is not supported");
        }
        var normalizedMime = mimeType == null || mimeType.isBlank()
                ? "application/octet-stream" : mimeType.toLowerCase(Locale.ROOT);
        normalizedMime = normalizedMime.split(";", 2)[0].trim();
        if (!expectedMime.equals(normalizedMime)
                && !COMPATIBLE_MIMES.getOrDefault(normalizedExtension, Set.of()).contains(normalizedMime)) {
            throw new BusinessException("Uploaded file MIME type does not match its extension");
        }
        if (!hasValidContent(normalizedExtension, content)) {
            throw new BusinessException("Uploaded file content is invalid");
        }
    }

    private boolean hasValidContent(String extension, byte[] content) {
        return switch (extension) {
            case "png" -> content.length >= 8 && content[0] == (byte) 0x89 && content[1] == 0x50
                    && content[2] == 0x4E && content[3] == 0x47;
            case "jpg", "jpeg" -> content.length >= 3 && (content[0] & 0xFF) == 0xFF
                    && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
            case "pdf" -> new String(content, 0, Math.min(content.length, 5), StandardCharsets.US_ASCII).equals("%PDF-");
            case "svg" -> new String(content, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT).contains("<svg");
            case "dxf" -> new String(content, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT).contains("SECTION")
                    || new String(content, 0, Math.min(content.length, 22), StandardCharsets.US_ASCII)
                    .startsWith("AUTO\u0000CAD BINARY DXF");
            case "dwg" -> content.length >= 6 && new String(content, 0, 6, StandardCharsets.US_ASCII).startsWith("AC10");
            default -> false;
        };
    }
}
