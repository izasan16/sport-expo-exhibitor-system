package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.scene.dto.SaveSceneDraftRequest;
import com.sportexpo.exhibitor.scene.dto.SceneFeaturePayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SceneValidationServiceUnitTest {
    private final SceneValidationService service = new SceneValidationService();

    @Test
    void reportsInvalidGeometryInsteadOfThrowingForMalformedPayload() {
        var request = new SaveSceneDraftRequest(
                1200, 800, new BigDecimal("20"),
                List.of(new SceneFeaturePayload(
                        "hall", "HALL_BOUNDARY", "Polygon",
                        Map.of("type", "Polygon", "coordinates", List.of(List.of(List.of("bad")))),
                        "主馆", "HALL-1", 1, Map.of()
                ))
        );

        var result = service.validateDraft(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).extracting("code").contains("INVALID_GEOMETRY");
    }

    @Test
    void requiresExactlyOneClosedHallBoundaryAndRejectsDegenerateBooths() {
        var request = new SaveSceneDraftRequest(
                1200, 800, new BigDecimal("20"),
                List.of(
                        feature("hall-1", "HALL_BOUNDARY", polygon(0, 0, 100, 0, 100, 100, 0, 0)),
                        feature("hall-2", "HALL_BOUNDARY", polygon(0, 0, 50, 0, 50, 50, 0, 0)),
                        feature("booth", "BOOTH", polygon(10, 10, 10, 10, 10, 10, 10, 10))
                )
        );

        var result = service.validateDraft(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).extracting("code")
                .contains("HALL_BOUNDARY_MULTIPLE", "BOOTH_DEGENERATE");
    }

    private SceneFeaturePayload feature(String id, String type, Object geometry) {
        return new SceneFeaturePayload(id, type, "Polygon", geometry, id, id, 1, Map.of());
    }

    private Object polygon(int... values) {
        var ring = new java.util.ArrayList<List<Integer>>();
        for (var i = 0; i < values.length; i += 2) {
            ring.add(List.of(values[i], values[i + 1]));
        }
        return Map.of("type", "Polygon", "coordinates", List.of(ring));
    }
}
