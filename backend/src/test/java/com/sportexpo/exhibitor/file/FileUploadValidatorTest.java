package com.sportexpo.exhibitor.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadValidatorTest {
    private final FileUploadValidator validator = new FileUploadValidator(10 * 1024 * 1024);

    @Test
    void rejectsEmptyFilesAndMissingExtension() {
        assertThatThrownBy(() -> validator.validate("", "image/png", new byte[0]))
                .hasMessage("Uploaded file must not be empty");
        assertThatThrownBy(() -> validator.validate("floorplan", "image/png", new byte[] {1}))
                .hasMessage("Uploaded file extension is required");
    }

    @Test
    void rejectsMimeExtensionMismatchAndInvalidContent() {
        assertThatThrownBy(() -> validator.validate("floorplan.png", "application/pdf", new byte[] {1}))
                .hasMessage("Uploaded file MIME type does not match its extension");
        assertThatThrownBy(() -> validator.validate("floorplan.png", "image/png", "not-a-png".getBytes()))
                .hasMessage("Uploaded file content is invalid");
    }
}
