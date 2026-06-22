package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.pricing.dto.AreaTierResponse;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/area-tiers")
public class AreaTierController {
    private final AreaTierService service;
    private final CurrentUserService currentUserService;

    public AreaTierController(AreaTierService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<AreaTierResponse> create(@Valid @RequestBody CreateAreaTierRequest request,
                                                Authentication authentication) {
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<List<AreaTierResponse>> list(@RequestParam UUID eventId) {
        return ApiResponse.ok(service.list(eventId));
    }
}
