package com.sportexpo.exhibitor.scene.dto;

public record SceneValidationIssueResponse(
        String featureCode,
        String code,
        String message
) {
}
