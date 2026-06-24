package com.sportexpo.exhibitor.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEventRequest(
        @NotBlank String name,
        @Min(2000) int year,
        String boothSourceMode,
        Boolean visualSceneEnabled
) {
    public CreateEventRequest(String name, int year) {
        this(name, year, null, null);
    }
}
