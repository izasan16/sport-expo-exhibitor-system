package com.sportexpo.exhibitor.rbac;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.rbac.dto.PermissionRequest;
import com.sportexpo.exhibitor.rbac.dto.PermissionResourceResponse;
import com.sportexpo.exhibitor.rbac.dto.PermissionTreeNodeResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
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
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionService service;
    private final CurrentUserService currentUserService;

    public PermissionController(PermissionService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('page.admin.permissions.list')")
    public ApiResponse<List<PermissionResourceResponse>> listPermissions() {
        return ApiResponse.ok(service.listPermissions());
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('page.admin.permissions.list')")
    public ApiResponse<List<PermissionTreeNodeResponse>> treePermissions() {
        return ApiResponse.ok(service.treePermissions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('action.admin.permissions.create')")
    public ApiResponse<PermissionResourceResponse> createPermission(@Valid @RequestBody PermissionRequest request,
                                                                    Authentication authentication) {
        return ApiResponse.ok(service.createPermission(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('action.admin.permissions.edit')")
    public ApiResponse<PermissionResourceResponse> updatePermission(@PathVariable UUID id,
                                                                    @Valid @RequestBody PermissionRequest request,
                                                                    Authentication authentication) {
        return ApiResponse.ok(service.updatePermission(id, request,
                currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('action.admin.permissions.toggle-status')")
    public ApiResponse<PermissionResourceResponse> togglePermissionStatus(@PathVariable UUID id,
                                                                          @RequestParam boolean enabled,
                                                                          Authentication authentication) {
        return ApiResponse.ok(service.togglePermissionStatus(id, enabled,
                currentUserService.getCurrentUser(authentication).userId()));
    }
}
