package com.sportexpo.exhibitor.scene.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishSceneRequest(@NotBlank String changeSummary) {
}
