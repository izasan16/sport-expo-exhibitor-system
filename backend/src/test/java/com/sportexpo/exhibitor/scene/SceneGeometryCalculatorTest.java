package com.sportexpo.exhibitor.scene;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SceneGeometryCalculatorTest {
    private final SceneGeometryCalculator calculator = new SceneGeometryCalculator();

    @Test
    void calculatesClosedPolygonAreaInSquareMeters() {
        var geometry = SceneGeometry.polygon(List.of(
                point("0", "0"),
                point("20", "0"),
                point("20", "20"),
                point("0", "20"),
                point("0", "0")
        ));

        var area = calculator.calculateAreaSqm(geometry, new BigDecimal("20"));

        assertThat(area).isEqualByComparingTo("1.00");
    }

    private static List<BigDecimal> point(String x, String y) {
        return List.of(new BigDecimal(x), new BigDecimal(y));
    }
}
