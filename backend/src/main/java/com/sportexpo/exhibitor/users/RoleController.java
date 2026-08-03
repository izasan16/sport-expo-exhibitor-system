package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.rbac.dto.RolePermissionUpdateRequest;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.users.dto.CopyRoleRequest;
import com.sportexpo.exhibitor.users.dto.CreateRoleRequest;
import com.sportexpo.exhibitor.users.dto.RoleDetailResponse;
import com.sportexpo.exhibitor.users.dto.RoleSummaryResponse;
import com.sportexpo.exhibitor.users.dto.UpdateRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateRoleStatusRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleManagementService service;
    private final CurrentUserService currentUserService;

    public RoleController(RoleManagementService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('page.admin.roles.list')")
    public ApiResponse<List<RoleSummaryResponse>> listRoles() {
        return ApiResponse.ok(service.listRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('page.admin.roles.list')")
    public ApiResponse<RoleDetailResponse> getRole(@PathVariable UUID id) {
        return ApiResponse.ok(service.getRole(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('action.admin.roles.create')")
    public ApiResponse<RoleDetailResponse> createRole(@Valid @RequestBody CreateRoleRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(service.createRole(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('action.admin.roles.edit')")
    public ApiResponse<RoleDetailResponse> updateRole(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateRoleRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(service.updateRole(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasAuthority('action.admin.roles.copy')")
    public ApiResponse<RoleDetailResponse> copyRole(@PathVariable UUID id, @Valid @RequestBody CopyRoleRequest request,
                                                    Authentication authentication) {
        return ApiResponse.ok(service.copyRole(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('action.admin.roles.toggle-status')")
    public ApiResponse<RoleDetailResponse> toggleRoleStatus(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateRoleStatusRequest request,
                                                            Authentication authentication) {
        return ApiResponse.ok(service.toggleRoleStatus(id, request,
                currentUserService.getCurrentUser(authentication).userId()));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('page.admin.roles.list')")
    public ApiResponse<RoleDetailResponse> getRolePermissions(@PathVariable UUID id) {
        return ApiResponse.ok(service.getRolePermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('action.admin.roles.assign-permissions')")
    public ApiResponse<RoleDetailResponse> updateRolePermissions(@PathVariable UUID id,
                                                                 @Valid @RequestBody RolePermissionUpdateRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.ok(service.updateRolePermissions(id, request,
                currentUserService.getCurrentUser(authentication).userId()));
    }
}
