package com.sportexpo.exhibitor.auth;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.users.RoleRepository;
import com.sportexpo.exhibitor.users.User;
import com.sportexpo.exhibitor.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceTest extends TestDatabaseSupport {
    @Autowired
    AuthService authService;
    @Autowired
    JwtService jwtService;
    @Autowired
    AuthProperties authProperties;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}")
    String schema;

    @BeforeEach
    @AfterEach
    void cleanupAuthTestUsers() {
        jdbcTemplate.update("""
                delete from %s.user_roles ur
                using %s.users u
                where ur.user_id = u.id
                  and u.username like 'auth-%%'
                """.formatted(schema, schema));
        jdbcTemplate.update("""
                delete from %s.users
                where username like 'auth-%%'
                """.formatted(schema));
    }

    @Test
    void loginReturnsTokenAndRolesForValidPassword() {
        var user = createUser("auth-valid", "ACTIVE");

        var response = authService.login(new LoginRequest(user.getUsername(), "demo123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(authProperties.getJwtExpirationSeconds());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.username()).isEqualTo(user.getUsername());
        assertThat(response.roles()).containsExactly("SYSTEM_ADMIN");

        var principal = jwtService.parse(response.accessToken());
        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.username()).isEqualTo(user.getUsername());
        assertThat(principal.roles()).containsExactly("SYSTEM_ADMIN");
    }

    @Test
    void badPasswordIncrementsFailureCount() {
        var user = createUser("auth-bad-password", "ACTIVE");

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "wrong")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.BAD_CREDENTIALS);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getFailedLoginCount()).isEqualTo(1);
        assertThat(updated.getLockedUntil()).isNull();
    }

    @Test
    void locksUserAfterFiveFailures() {
        var user = createUser("auth-lock", "ACTIVE");

        for (var attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "wrong")))
                    .isInstanceOf(AuthException.class)
                    .extracting("code")
                    .isEqualTo(AuthErrorCodes.BAD_CREDENTIALS);
        }

        var locked = userRepository.findById(user.getId()).orElseThrow();
        assertThat(locked.getFailedLoginCount()).isEqualTo(5);
        assertThat(locked.getLockedUntil()).isAfter(OffsetDateTime.now());

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "demo123")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.ACCOUNT_LOCKED);
    }

    @Test
    void disabledUserCannotLogin() {
        var user = createUser("auth-disabled", "DISABLED");

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "demo123")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.USER_DISABLED);
    }

    @Test
    void disabledUserReturnsDisabledEvenWhenLocked() {
        var user = createUser("auth-disabled-locked", "DISABLED", 5, OffsetDateTime.now().plusMinutes(5));

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "demo123")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.USER_DISABLED);
    }

    @Test
    void expiredLockIsClearedBeforeRecordingNewBadPassword() {
        var user = createUser("auth-expired-lock", "ACTIVE", 5, OffsetDateTime.now().minusMinutes(5));

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getUsername(), "wrong")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.BAD_CREDENTIALS);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getFailedLoginCount()).isEqualTo(1);
        assertThat(updated.getLockedUntil()).isNull();
    }

    private User createUser(String usernamePrefix, String status) {
        return createUser(usernamePrefix, status, 0, null);
    }

    private User createUser(String usernamePrefix, String status, int failedLoginCount, OffsetDateTime lockedUntil) {
        var now = OffsetDateTime.now();
        var username = usernamePrefix + "-" + UUID.randomUUID();
        var user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("demo123"),
                "Auth Test User",
                null,
                username + "@example.com",
                "ADMIN",
                null,
                status,
                "SYSTEM_ADMIN",
                failedLoginCount,
                lockedUntil,
                null,
                0,
                now,
                now,
                null,
                null
        );
        user.assignRole(roleRepository.findByCode("SYSTEM_ADMIN").orElseThrow());
        return userRepository.saveAndFlush(user);
    }
}
