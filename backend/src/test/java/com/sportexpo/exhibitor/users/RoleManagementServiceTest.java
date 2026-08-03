package com.sportexpo.exhibitor.users;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.rbac.dto.RolePermissionUpdateRequest;
import com.sportexpo.exhibitor.users.dto.CopyRoleRequest;
import com.sportexpo.exhibitor.users.dto.CreateRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateRoleRequest;
import com.sportexpo.exhibitor.users.dto.UpdateRoleStatusRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RoleManagementServiceTest extends TestDatabaseSupport {
    private static final String ROLE_CODE_PREFIX = "rbac-role-";

    @Autowired
    RoleManagementService service;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}")
    String schema;

    @BeforeEach
    @AfterEach
    void cleanupRoles() {
        jdbcTemplate.update("""
                delete from %s.user_roles ur
                using %s.roles r
                where ur.role_id = r.id
                  and r.code like '%s%%'
                """.formatted(schema, schema, ROLE_CODE_PREFIX));
        jdbcTemplate.update("""
                delete from %s.role_permissions rp
                using %s.roles r
                where rp.role_id = r.id
                  and r.code like '%s%%'
                """.formatted(schema, schema, ROLE_CODE_PREFIX));
        jdbcTemplate.update("""
                delete from %s.roles
                where code like '%s%%'
                """.formatted(schema, ROLE_CODE_PREFIX));
    }

    @Test
    void listRolesIncludesSeededSystemAdminRole() {
        var roles = service.listRoles();

        assertThat(roles)
                .extracting(role -> role.code() + ":" + role.roleType() + ":" + role.enabled())
                .contains("SYSTEM_ADMIN:SYSTEM:true");
    }

    @Test
    void createRoleCreatesEnabledCustomRole() {
        var request = new CreateRoleRequest(
                ROLE_CODE_PREFIX + "create-" + UUID.randomUUID().toString().substring(0, 8),
                "自定义角色",
                "用于测试创建角色"
        );

        var created = service.createRole(request, DemoActors.DEMO_ADMIN_ID);

        assertThat(created.code()).isEqualTo(request.code());
        assertThat(created.name()).isEqualTo(request.name());
        assertThat(created.roleType()).isEqualTo("CUSTOM");
        assertThat(created.enabled()).isTrue();
        assertThat(created.description()).isEqualTo(request.description());
    }

    @Test
    void updateRoleChangesEditableFields() {
        var created = service.createRole(new CreateRoleRequest(
                ROLE_CODE_PREFIX + "update-" + UUID.randomUUID().toString().substring(0, 8),
                "初始角色",
                "初始说明"
        ), DemoActors.DEMO_ADMIN_ID);

        var updated = service.updateRole(created.id(), new UpdateRoleRequest(
                created.code(),
                "更新后的角色",
                "更新后的说明"
        ), DemoActors.DEMO_ADMIN_ID);

        assertThat(updated.name()).isEqualTo("更新后的角色");
        assertThat(updated.description()).isEqualTo("更新后的说明");
        assertThat(updated.code()).isEqualTo(created.code());
    }

    @Test
    void copyRoleCopiesSourcePermissions() {
        var source = roleRepository.findByCode("ORGANIZER_ADMIN").orElseThrow();
        var sourcePermissions = service.getRolePermissions(source.getId()).permissionCodes();

        var copied = service.copyRole(source.getId(), new CopyRoleRequest(
                ROLE_CODE_PREFIX + "copy-" + UUID.randomUUID().toString().substring(0, 8),
                "复制角色",
                "复制说明"
        ), DemoActors.DEMO_ADMIN_ID);

        var copiedPermissions = service.getRolePermissions(copied.id()).permissionCodes();
        assertThat(copied.roleType()).isEqualTo("CUSTOM");
        assertThat(copiedPermissions).containsExactlyInAnyOrderElementsOf(sourcePermissions);
    }

    @Test
    void toggleRoleStatusUpdatesEnabledFlag() {
        var created = service.createRole(new CreateRoleRequest(
                ROLE_CODE_PREFIX + "toggle-" + UUID.randomUUID().toString().substring(0, 8),
                "开关角色",
                "用于测试启停"
        ), DemoActors.DEMO_ADMIN_ID);

        var updated = service.toggleRoleStatus(created.id(), new UpdateRoleStatusRequest(false), DemoActors.DEMO_ADMIN_ID);

        assertThat(updated.enabled()).isFalse();
        assertThat(service.getRole(created.id()).enabled()).isFalse();
    }

    @Test
    void updateRolePermissionsRejectsReducingSystemAdminPermissions() {
        var systemAdmin = roleRepository.findByCode("SYSTEM_ADMIN").orElseThrow();
        var currentPermissionCodes = service.getRolePermissions(systemAdmin.getId()).permissionCodes();

        assertThatThrownBy(() -> service.updateRolePermissions(systemAdmin.getId(), new RolePermissionUpdateRequest(
                currentPermissionCodes.subList(0, currentPermissionCodes.size() - 1)
        ), DemoActors.DEMO_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SYSTEM_ADMIN");
    }
}
