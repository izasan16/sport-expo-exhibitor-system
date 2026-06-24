package com.sportexpo.exhibitor.scene;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SceneGeometryCalculator {
    public BigDecimal calculateAreaSqm(SceneGeometry geometry, BigDecimal pixelToMeterRatio) {
        BigDecimal twiceArea = BigDecimal.ZERO;
        var coordinates = geometry.coordinates();
        for (int index = 0; index < coordinates.size() - 1; index += 1) {
            var current = coordinates.get(index);
            var next = coordinates.get(index + 1);
            twiceArea = twiceArea.add(current.get(0).multiply(next.get(1)))
                    .subtract(next.get(0).multiply(current.get(1)));
        }

        var areaPx2 = twiceArea.abs().divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        var ratioSquared = pixelToMeterRatio.multiply(pixelToMeterRatio);
        return areaPx2.divide(ratioSquared, 2, RoundingMode.HALF_UP);
    }
}
