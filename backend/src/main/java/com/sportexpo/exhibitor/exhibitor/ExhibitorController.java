package com.sportexpo.exhibitor.exhibitor;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exhibitors")
public class ExhibitorController {
    private final ExhibitorService service;
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;

    public ExhibitorController(ExhibitorService service, CurrentUserService currentUserService,
                               DataScopeService dataScopeService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<List<ExhibitorResponse>> list(Authentication authentication) {
        if (dataScopeService.isUnrestricted(authentication)) {
            return ApiResponse.ok(service.list());
        }
        return ApiResponse.ok(service.listByIds(dataScopeService.accessibleExhibitorIds(authentication)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<ExhibitorResponse> current(Authentication authentication) {
        return ApiResponse.ok(service.get(currentUserService.requireCurrentExhibitorId(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN') or @dataScopeService.canAccessExhibitor(authentication, #id)")
    public ApiResponse<ExhibitorResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<ExhibitorResponse> create(@RequestBody ExhibitorRequest request, Authentication authentication) {
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<ExhibitorResponse> update(@PathVariable UUID id, @RequestBody ExhibitorRequest request,
                                                 Authentication authentication) {
        return ApiResponse.ok(service.update(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/current")
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<ExhibitorResponse> updateCurrent(@RequestBody ExhibitorRequest request,
                                                        Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        return ApiResponse.ok(service.update(currentUserService.requireCurrentExhibitorId(authentication), request, currentUser.userId()));
    }
}
