package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.event.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> list() {
        return ApiResponse.ok(service.list());
    }
}
