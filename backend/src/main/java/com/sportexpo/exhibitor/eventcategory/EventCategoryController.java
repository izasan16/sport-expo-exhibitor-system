package com.sportexpo.exhibitor.eventcategory;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/event-categories")
public class EventCategoryController {
    private final EventCategoryService service;

    public EventCategoryController(EventCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<EventCategoryResponse>> list(@RequestParam UUID eventId,
                                                         @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.list(eventId, enabled));
    }

    @PostMapping
    public ApiResponse<EventCategoryResponse> create(@RequestBody EventCategoryRequest request) {
        return ApiResponse.ok(service.create(request, DemoActors.DEMO_ADMIN_ID));
    }

    @PutMapping("/{id}")
    public ApiResponse<EventCategoryResponse> update(@PathVariable UUID id, @RequestBody EventCategoryRequest request) {
        return ApiResponse.ok(service.update(id, request, DemoActors.DEMO_ADMIN_ID));
    }
}
