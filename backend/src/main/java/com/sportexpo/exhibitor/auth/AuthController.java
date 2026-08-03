package com.sportexpo.exhibitor.auth;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var response = authService.login(request);
        return ResponseEntity.ok()
                .header("X-Token-Type", response.tokenType())
                .header("X-Expires-In", Long.toString(response.expiresIn()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/api/auth/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        var sortedRoles = currentUser.roles().stream().sorted().toList();
        return ApiResponse.ok(new CurrentUserResponse(
                currentUser.userId(),
                currentUser.username(),
                currentUser.displayName(),
                sortedRoles,
                currentUser.defaultRole(),
                sortedRoles,
                currentUser.exhibitorId(),
                currentUser.permissions().stream().sorted().toList()
        ));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        authService.logout(currentUserService.getCurrentUser(authentication).userId());
        return ApiResponse.ok(null);
    }
}
