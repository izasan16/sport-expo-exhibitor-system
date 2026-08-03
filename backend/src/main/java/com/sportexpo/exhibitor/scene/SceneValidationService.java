package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneFeaturePayload;
import com.sportexpo.exhibitor.scene.dto.SceneValidationIssueResponse;
import com.sportexpo.exhibitor.scene.dto.SceneValidationResponse;
import com.sportexpo.exhibitor.common.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SceneValidationService {
    private static final Set<String> SUPPORTED_FEATURE_TYPES = Set.of("HALL_BOUNDARY", "BOOTH", "OBSTACLE", "TEXT", "IMAGE");

    public void validatePayloadShape(List<SceneFeaturePayload> features) {
        for (var feature : features) {
            if (feature == null || !SUPPORTED_FEATURE_TYPES.contains(feature.featureType())) {
                throw new BusinessException("Unsupported scene feature type");
            }
            if (!"Polygon".equals(feature.geometryType())) {
                throw new BusinessException("Scene feature geometry type must be Polygon");
            }
            geometry(feature.geometry());
            if ("BOOTH".equals(feature.featureType()) && boothIdentifier(feature) == null) {
                throw new BusinessException("Booth feature requires boothNo or code");
            }
        }
    }

    public SceneValidationResponse validateDraft(SaveSceneDraftRequest request) {
        var issues = new ArrayList<SceneValidationIssueResponse>();
        var features = request.safeFeatures().stream().filter(java.util.Objects::nonNull).toList();
        for (var feature : features) {
            try {
                validateFeatureShape(feature);
            } catch (RuntimeException exception) {
                issues.add(issue(feature.code(), "INVALID_FEATURE_PAYLOAD", "Feature payload is invalid"));
            }
        }
        var hallBoundary = findSingleHallBoundary(features, issues);
        var boothFeatures = features.stream()
                .filter(feature -> "BOOTH".equals(feature.featureType()))
                .toList();
        var boothCodes = new java.util.HashSet<String>();

        if (!boothFeatures.isEmpty() && request.pixelToMeterRatio().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add(issue(null, "SCENE_SCALE_REQUIRED", "Pixel-to-meter ratio must be calibrated before publish"));
        }

        for (var feature : boothFeatures) {
            var boothCode = boothIdentifier(feature);
            if (boothCode == null) {
                issues.add(issue(feature.code(), "BOOTH_NUMBER_REQUIRED", "Booth feature requires boothNo or code"));
            } else if (!boothCodes.add(boothCode)) {
                issues.add(issue(boothCode, "BOOTH_NUMBER_DUPLICATE", "Booth number must be unique"));
            }
            SceneGeometry geometry;
            try {
                geometry = geometry(feature.geometry());
            } catch (RuntimeException exception) {
                issues.add(issue(feature.code(), "INVALID_GEOMETRY", "Feature geometry is invalid"));
                continue;
            }
            if (!isClosedPolygon(geometry)) {
                issues.add(issue(feature.code(), "BOOTH_NOT_CLOSED", "Booth polygon must be closed"));
                continue;
            }
            if (polygonArea(geometry) <= 0) {
                issues.add(issue(feature.code(), "BOOTH_DEGENERATE", "Booth polygon must enclose an area"));
                continue;
            }
            if (hallBoundary != null && !isInsideHall(geometry, hallBoundary)) {
                issues.add(issue(feature.code(), "BOOTH_OUTSIDE_HALL", "Booth must stay inside hall boundary"));
            }
        }

        return new SceneValidationResponse(issues.isEmpty(), issues);
    }

    private SceneGeometry findSingleHallBoundary(List<SceneFeaturePayload> features,
                                                 List<SceneValidationIssueResponse> issues) {
        var hallBoundaries = features.stream()
                .filter(feature -> "HALL_BOUNDARY".equals(feature.featureType()))
                .toList();
        if (hallBoundaries.isEmpty()) {
            issues.add(issue(null, "HALL_BOUNDARY_REQUIRED", "Hall boundary is required"));
            return null;
        }
        if (hallBoundaries.size() > 1) {
            issues.add(issue(null, "HALL_BOUNDARY_MULTIPLE", "Only one hall boundary is allowed"));
        }
        try {
            var geometry = geometry(hallBoundaries.get(0).geometry());
            if (!isClosedPolygon(geometry) || polygonArea(geometry) <= 0) {
                issues.add(issue(null, "HALL_BOUNDARY_INVALID", "Hall boundary must be a closed non-empty polygon"));
            }
            return geometry;
        } catch (RuntimeException exception) {
            issues.add(issue(null, "INVALID_GEOMETRY", "Hall boundary geometry is invalid"));
            return null;
        }
    }

    private SceneValidationIssueResponse issue(String featureCode, String code, String message) {
        return new SceneValidationIssueResponse(featureCode, code, message);
    }

    @SuppressWarnings("unchecked")
    SceneGeometry geometry(Object rawGeometry) {
        if (!(rawGeometry instanceof Map<?, ?> rawMap)
                || !"Polygon".equals(String.valueOf(rawMap.get("type")))
                || !(rawMap.get("coordinates") instanceof List<?> coordinateSets)
                || coordinateSets.isEmpty()
                || !(coordinateSets.get(0) instanceof List<?> firstRing)) {
            throw new BusinessException("Invalid scene geometry");
        }
        var points = new ArrayList<List<BigDecimal>>();
        for (var item : firstRing) {
            if (!(item instanceof List<?> pair) || pair.size() != 2) {
                throw new BusinessException("Invalid scene geometry");
            }
            points.add(List.of(decimal(pair.get(0)), decimal(pair.get(1))));
        }
        return SceneGeometry.polygon(points);
    }

    private boolean isClosedPolygon(SceneGeometry geometry) {
        if (geometry.coordinates().size() < 4) {
            return false;
        }
        var first = geometry.coordinates().get(0);
        var last = geometry.coordinates().get(geometry.coordinates().size() - 1);
        return first.get(0).compareTo(last.get(0)) == 0 && first.get(1).compareTo(last.get(1)) == 0;
    }

    private boolean isInsideHall(SceneGeometry booth, SceneGeometry hallBoundary) {
        return booth.coordinates().stream()
                .limit(Math.max(0, booth.coordinates().size() - 1L))
                .allMatch(point -> pointInPolygon(point, hallBoundary.coordinates()));
    }

    private boolean pointInPolygon(List<BigDecimal> point, List<List<BigDecimal>> polygon) {
        double px = point.get(0).doubleValue();
        double py = point.get(1).doubleValue();
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i).get(0).doubleValue();
            double yi = polygon.get(i).get(1).doubleValue();
            double xj = polygon.get(j).get(0).doubleValue();
            double yj = polygon.get(j).get(1).doubleValue();

            boolean intersects = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / ((yj - yi) == 0 ? 1 : (yj - yi)) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            throw new BusinessException("Invalid scene coordinate");
        }
        return new BigDecimal(String.valueOf(value));
    }

    private void validateFeatureShape(SceneFeaturePayload feature) {
        if (feature == null || !SUPPORTED_FEATURE_TYPES.contains(feature.featureType())) {
            throw new BusinessException("Unsupported scene feature type");
        }
        if (!"Polygon".equals(feature.geometryType())) {
            throw new BusinessException("Scene feature geometry type must be Polygon");
        }
        geometry(feature.geometry());
    }

    private String boothIdentifier(SceneFeaturePayload feature) {
        if (feature.code() != null && !feature.code().isBlank()) {
            return feature.code().trim();
        }
        if (feature.meta() == null) {
            return null;
        }
        var rawBoothNo = feature.meta().get("boothNo");
        return rawBoothNo == null || String.valueOf(rawBoothNo).isBlank()
                ? null : String.valueOf(rawBoothNo).trim();
    }

    private double polygonArea(SceneGeometry geometry) {
        double twiceArea = 0;
        for (var i = 0; i < geometry.coordinates().size() - 1; i++) {
            var current = geometry.coordinates().get(i);
            var next = geometry.coordinates().get(i + 1);
            twiceArea += current.get(0).doubleValue() * next.get(1).doubleValue()
                    - next.get(0).doubleValue() * current.get(1).doubleValue();
        }
        return Math.abs(twiceArea / 2);
    }
}
