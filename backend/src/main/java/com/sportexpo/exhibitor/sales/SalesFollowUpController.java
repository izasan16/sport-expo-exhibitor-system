package com.sportexpo.exhibitor.sales;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/sales-follow-ups")
public class SalesFollowUpController {
    private final SalesFollowUpService service;
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;

    public SalesFollowUpController(SalesFollowUpService service, CurrentUserService currentUserService,
                                   DataScopeService dataScopeService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<List<SalesFollowUpResponse>> list(@RequestParam(required = false) UUID exhibitorId,
                                                         @RequestParam(required = false) String status,
                                                         Authentication authentication) {
        if (!dataScopeService.isExhibitorScoped(authentication)) {
            return ApiResponse.ok(service.list(exhibitorId, status));
        }
        if (exhibitorId != null) {
            dataScopeService.requireCanAccessExhibitor(authentication, exhibitorId);
        }
        return ApiResponse.ok(service.listScoped(exhibitorId, status,
                dataScopeService.accessibleExhibitorIds(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALES')")
    public ApiResponse<SalesFollowUpResponse> create(@Valid @RequestBody SalesFollowUpRequest request,
                                                     Authentication authentication) {
        dataScopeService.requireCanAccessExhibitor(authentication, request.exhibitorId());
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<SalesFollowUpResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody SalesFollowUpRequest request,
                                                     Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        if (currentUser.hasRole("SALES")) {
            dataScopeService.requireCanAccessExhibitor(authentication, request.exhibitorId());
        }
        return ApiResponse.ok(service.update(id, request, currentUser.userId(),
                currentUser.hasRole("SYSTEM_ADMIN") || currentUser.hasRole("ORGANIZER_ADMIN")));
    }
}
