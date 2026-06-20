package com.sportexpo.exhibitor.eventcategory;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final CurrentUserService currentUserService;

    public EventCategoryController(EventCategoryService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','EXHIBITOR')")
    public ApiResponse<List<EventCategoryResponse>> list(@RequestParam UUID eventId,
                                                         @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.list(eventId, enabled));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<EventCategoryResponse> create(@RequestBody EventCategoryRequest request,
                                                     Authentication authentication) {
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<EventCategoryResponse> update(@PathVariable UUID id, @RequestBody EventCategoryRequest request,
                                                     Authentication authentication) {
        return ApiResponse.ok(service.update(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }
}
