package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.users.dto.CreateUserRequest;
import com.sportexpo.exhibitor.users.dto.ResetPasswordRequest;
import com.sportexpo.exhibitor.users.dto.UpdateUserRolesRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserManagementServiceTest extends TestDatabaseSupport {
    private static final String USERNAME_PREFIX = "rbac-user-";

    @Autowired
    UserManagementService service;
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
    void cleanupUsers() {
        jdbcTemplate.update("""
                delete from %s.user_roles ur
                using %s.users u
                where ur.user_id = u.id
                  and u.username like '%s%%'
                """.formatted(schema, schema, USERNAME_PREFIX));
        jdbcTemplate.update("""
                delete from %s.users
                where username like '%s%%'
                """.formatted(schema, USERNAME_PREFIX));
    }

    @Test
    void createExhibitorUserRequiresBoundExhibitor() {
        var username = USERNAME_PREFIX + "create-exhibitor-" + UUID.randomUUID().toString().substring(0, 8);

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                username,
                "Temp123456",
                "展商用户",
                "13800000001",
                username + "@example.com",
                "EXHIBITOR",
                null,
                "ACTIVE",
                List.of("EXHIBITOR"),
                "EXHIBITOR"
        ), DemoActors.DEMO_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exhibitor");

        assertThat(userRepository.findByUsername(username)).isEmpty();
    }

    @Test
    void updateUserRolesRejectsDefaultRoleOutsideAssignedRoles() {
        var user = createUser("update-roles", "STAFF", null, "SALES");

        assertThatThrownBy(() -> service.updateRoles(user.getId(), new UpdateUserRolesRequest(
                List.of("SALES"),
                "FINANCE"
        ), DemoActors.DEMO_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("default role");

        var unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getDefaultRole()).isEqualTo("SALES");
        assertThat(unchanged.roleCodes()).containsExactly("SALES");
    }

    @Test
    void resetPasswordChangesHashWithoutChangingUsername() {
        var user = createUser("reset-password", "STAFF", null, "SALES");
        var oldHash = user.getPasswordHash();
        var oldJwtVersion = user.getJwtVersion();

        service.resetPassword(user.getId(), new ResetPasswordRequest("NewPass123!"), DemoActors.DEMO_ADMIN_ID);

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo(user.getUsername());
        assertThat(updated.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("NewPass123!", updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("demo123", updated.getPasswordHash())).isFalse();
        assertThat(updated.getJwtVersion()).isEqualTo(oldJwtVersion + 1);
    }

    private User createUser(String scenario, String userType, UUID exhibitorId, String defaultRoleCode) {
        var now = OffsetDateTime.now();
        var username = USERNAME_PREFIX + scenario + "-" + UUID.randomUUID().toString().substring(0, 8);
        var user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("demo123"),
                "测试用户-" + scenario,
                "13800000000",
                username + "@example.com",
                userType,
                exhibitorId,
                "ACTIVE",
                defaultRoleCode,
                0,
                null,
                null,
                0,
                now,
                now,
                DemoActors.DEMO_ADMIN_ID,
                DemoActors.DEMO_ADMIN_ID
        );
        user.assignRole(roleRepository.findByCode(defaultRoleCode).orElseThrow());
        return userRepository.saveAndFlush(user);
    }
}
