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
        return ApiResponse.ok(new CurrentUserResponse(
                currentUser.userId(),
                currentUser.username(),
                currentUser.roles().stream().sorted().toList(),
                currentUser.exhibitorId()
        ));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }
}
