package com.sportexpo.exhibitor.scene.dto;

import java.util.List;

public record SceneValidationResponse(
        boolean valid,
        List<SceneValidationIssueResponse> issues
) {
}
