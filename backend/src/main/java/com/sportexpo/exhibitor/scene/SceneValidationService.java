package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneFeaturePayload;
import com.sportexpo.exhibitor.scene.dto.SceneValidationIssueResponse;
import com.sportexpo.exhibitor.scene.dto.SceneValidationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SceneValidationService {
    public SceneValidationResponse validateDraft(SaveSceneDraftRequest request) {
        var issues = new ArrayList<SceneValidationIssueResponse>();
        var hallBoundary = findSingleHallBoundary(request.safeFeatures(), issues);
        var boothFeatures = request.safeFeatures().stream()
                .filter(feature -> "BOOTH".equals(feature.featureType()))
                .toList();

        if (!boothFeatures.isEmpty() && request.pixelToMeterRatio().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add(issue(null, "SCENE_SCALE_REQUIRED", "Pixel-to-meter ratio must be calibrated before publish"));
        }

        for (var feature : boothFeatures) {
            var geometry = geometry(feature.geometry());
            if (!isClosedPolygon(geometry)) {
                issues.add(issue(feature.code(), "BOOTH_NOT_CLOSED", "Booth polygon must be closed"));
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
        return geometry(hallBoundaries.get(0).geometry());
    }

    private SceneValidationIssueResponse issue(String featureCode, String code, String message) {
        return new SceneValidationIssueResponse(featureCode, code, message);
    }

    @SuppressWarnings("unchecked")
    SceneGeometry geometry(Object rawGeometry) {
        var geometryMap = (Map<String, Object>) rawGeometry;
        var coordinateSets = (List<Object>) geometryMap.get("coordinates");
        var firstRing = (List<Object>) coordinateSets.get(0);
        var points = new ArrayList<List<BigDecimal>>();
        for (var item : firstRing) {
            var pair = (List<Object>) item;
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
        return new BigDecimal(String.valueOf(value));
    }
}
