package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;
import com.sportexpo.exhibitor.registration.dto.RejectRegistrationRequest;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {
    private final RegistrationService service;
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;

    public RegistrationController(RegistrationService service, CurrentUserService currentUserService,
                                  DataScopeService dataScopeService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','EXHIBITOR')")
    public ApiResponse<List<RegistrationResponse>> list(@RequestParam(required = false) UUID eventId,
                                                        @RequestParam(required = false) UUID exhibitorId,
                                                        @RequestParam(required = false) String status,
                                                        Authentication authentication) {
        var registrations = service.list(eventId, exhibitorId, status);
        if (!dataScopeService.isExhibitorScoped(authentication)) {
            return ApiResponse.ok(registrations);
        }
        var allowedIds = dataScopeService.accessibleExhibitorIds(authentication);
        return ApiResponse.ok(registrations.stream()
                .filter(registration -> allowedIds.contains(registration.exhibitorId()))
                .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','EXHIBITOR')")
    public ApiResponse<RegistrationResponse> get(@PathVariable UUID id, Authentication authentication) {
        var registration = service.get(id);
        dataScopeService.requireCanAccessExhibitorIfScoped(authentication, registration.exhibitorId());
        return ApiResponse.ok(registration);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<RegistrationResponse> create(@RequestBody RegistrationRequest request,
                                                    Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        return ApiResponse.ok(service.create(scopedRequest(request, currentUser.exhibitorId()), currentUser.userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<RegistrationResponse> update(@PathVariable UUID id, @RequestBody RegistrationRequest request,
                                                    Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        return ApiResponse.ok(service.update(id, scopedRequest(request, currentUser.exhibitorId()), currentUser.userId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<RegistrationResponse> submit(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(service.submitOrResubmit(id, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<RegistrationResponse> approve(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(service.approve(id, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<RegistrationResponse> reject(@PathVariable UUID id, @RequestBody RejectRegistrationRequest request,
                                                    Authentication authentication) {
        return ApiResponse.ok(service.reject(id, currentUserService.getCurrentUser(authentication).userId(), request.reason()));
    }

    private RegistrationRequest scopedRequest(RegistrationRequest request, UUID exhibitorId) {
        if (exhibitorId == null) {
            currentUserService.requireCurrentExhibitorId();
        }
        return new RegistrationRequest(
                request.eventId(),
                exhibitorId,
                request.category(),
                request.zoneId(),
                request.intentAreaSqm(),
                request.contactName(),
                request.contactPhone(),
                request.remark()
        );
    }
}
