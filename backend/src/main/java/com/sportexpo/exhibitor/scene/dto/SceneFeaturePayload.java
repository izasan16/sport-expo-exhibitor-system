package com.sportexpo.exhibitor.scene.dto;

import java.util.Map;

public record SceneFeaturePayload(
        String id,
        String featureType,
        String geometryType,
        Object geometry,
        String label,
        String code,
        Integer layerOrder,
        Map<String, Object> meta
) {
}
