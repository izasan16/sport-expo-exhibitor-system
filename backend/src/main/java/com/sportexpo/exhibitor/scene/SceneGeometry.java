package com.sportexpo.exhibitor.scene;

import java.math.BigDecimal;
import java.util.List;

public record SceneGeometry(String type, List<List<BigDecimal>> coordinates) {
    public static SceneGeometry polygon(List<List<BigDecimal>> points) {
        return new SceneGeometry("Polygon", points);
    }
}
