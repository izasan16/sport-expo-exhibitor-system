package com.sportexpo.exhibitor.auth;

import com.sportexpo.exhibitor.common.ApiResponse;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token");
        }
        if (authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return ApiResponse.ok(new CurrentUserResponse(
                    principal.userId(),
                    principal.username(),
                    principal.roles(),
                    principal.exhibitorId()
            ));
        }
        throw new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token");
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }
}
