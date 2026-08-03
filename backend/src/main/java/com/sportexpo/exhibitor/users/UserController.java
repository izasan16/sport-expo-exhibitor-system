package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.users.dto.CreateUserRequest;
import com.sportexpo.exhibitor.users.dto.ResetPasswordRequest;
import com.sportexpo.exhibitor.users.dto.UpdateDefaultRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserRolesRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserStatusRequest;
import com.sportexpo.exhibitor.users.dto.UserDetailResponse;
import com.sportexpo.exhibitor.users.dto.UserSummaryResponse;
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
@RequestMapping("/api/users")
public class UserController {
    private final UserManagementService service;
    private final CurrentUserService currentUserService;

    public UserController(UserManagementService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('page.admin.users.list')")
    public ApiResponse<List<UserSummaryResponse>> list(@RequestParam(required = false) String username,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String roleCode) {
        return ApiResponse.ok(service.list(username, status, roleCode));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('page.admin.users.list')")
    public ApiResponse<UserDetailResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('action.admin.users.create')")
    public ApiResponse<UserDetailResponse> create(@Valid @RequestBody CreateUserRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('action.admin.users.edit')")
    public ApiResponse<UserDetailResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok(service.update(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("#request.enabled() ? hasAuthority('action.admin.users.enable') : hasAuthority('action.admin.users.disable')")
    public ApiResponse<UserDetailResponse> updateStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateUserStatusRequest request,
                                                        Authentication authentication) {
        return ApiResponse.ok(service.updateStatus(id, request.enabled(),
                currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('action.admin.users.assign-roles')")
    public ApiResponse<UserDetailResponse> updateRoles(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateUserRolesRequest request,
                                                       Authentication authentication) {
        return ApiResponse.ok(service.updateRoles(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/default-role")
    @PreAuthorize("hasAuthority('action.admin.users.set-default-role')")
    public ApiResponse<UserDetailResponse> updateDefaultRole(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateDefaultRoleRequest request,
                                                             Authentication authentication) {
        return ApiResponse.ok(service.updateDefaultRole(id, request,
                currentUserService.getCurrentUser(authentication).userId()));
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('action.admin.users.reset-password')")
    public ApiResponse<UserDetailResponse> resetPassword(@PathVariable UUID id,
                                                         @Valid @RequestBody ResetPasswordRequest request,
                                                         Authentication authentication) {
        return ApiResponse.ok(service.resetPassword(id, request, currentUserService.getCurrentUser(authentication).userId()));
    }
}
