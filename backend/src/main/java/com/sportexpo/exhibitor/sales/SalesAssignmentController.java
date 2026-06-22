package com.sportexpo.exhibitor.sales;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/exhibitors/{exhibitorId}/sales-assignments")
public class SalesAssignmentController {
    private final SalesAssignmentService service;
    private final CurrentUserService currentUserService;

    public SalesAssignmentController(SalesAssignmentService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN') or @dataScopeService.canAccessExhibitor(authentication, #exhibitorId)")
    public ApiResponse<SalesAssignmentResponse> get(@PathVariable UUID exhibitorId) {
        return ApiResponse.ok(service.getAssignments(exhibitorId));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<SalesAssignmentResponse> update(@PathVariable UUID exhibitorId,
                                                       @Valid @RequestBody SalesAssignmentRequest request,
                                                       Authentication authentication) {
        return ApiResponse.ok(service.updateAssignments(
                exhibitorId,
                request,
                currentUserService.getCurrentUser(authentication).userId()
        ));
    }
}
