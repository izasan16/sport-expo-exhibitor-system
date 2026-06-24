package com.sportexpo.exhibitor.file.dto;

import com.sportexpo.exhibitor.file.StoredFile;

import java.util.UUID;

public record UploadedFileResponse(
        UUID fileId,
        String originalName,
        String mimeType,
        String url,
        long fileSize,
        Integer width,
        Integer height
) {
    public static UploadedFileResponse from(StoredFile file) {
        return new UploadedFileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getPublicUrl(),
                file.getFileSize(),
                file.getWidth(),
                file.getHeight()
        );
    }
}
