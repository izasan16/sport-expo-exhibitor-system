package com.sportexpo.exhibitor.event.dto;

import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        int year,
        String status,
        String boothSourceMode,
        boolean visualSceneEnabled
) {
}
