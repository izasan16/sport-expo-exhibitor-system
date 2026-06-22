package com.sportexpo.exhibitor.auth;

import com.sportexpo.exhibitor.users.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = AuthException.class)
    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByUsernameForUpdate(request.username())
                .orElseThrow(() -> badCredentials());
        var now = OffsetDateTime.now();
        if (user.isDisabled()) {
            throw new AuthException(AuthErrorCodes.USER_DISABLED, "User disabled");
        }
        user.clearExpiredLockAt(now);
        if (user.isLockedAt(now)) {
            throw new AuthException(AuthErrorCodes.ACCOUNT_LOCKED, "Account locked");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(
                    properties.getLoginMaxFailures(),
                    Duration.ofMinutes(properties.getLoginLockMinutes()),
                    now
            );
            throw badCredentials();
        }

        user.recordSuccessfulLogin(now);
        var roles = user.roleCodes();
        var token = jwtService.issueToken(user, roles);
        return new LoginResponse(
                token,
                TOKEN_TYPE,
                jwtService.expirationSeconds(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getExhibitorId(),
                roles
        );
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token"));
        user.incrementJwtVersion(OffsetDateTime.now());
    }

    private AuthException badCredentials() {
        return new AuthException(AuthErrorCodes.BAD_CREDENTIALS, "Bad credentials");
    }
}
