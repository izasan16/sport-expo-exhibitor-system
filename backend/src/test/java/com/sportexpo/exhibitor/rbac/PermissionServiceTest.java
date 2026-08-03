package com.sportexpo.exhibitor.rbac;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.rbac.dto.PermissionRequest;
import com.sportexpo.exhibitor.users.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PermissionServiceTest extends TestDatabaseSupport {
    private static final String PERMISSION_CODE_PREFIX = "rbac-perm-";

    @Autowired
    PermissionService service;
    @Autowired
    PermissionRepository permissionRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}")
    String schema;

    @BeforeEach
    @AfterEach
    void cleanupPermissions() {
        jdbcTemplate.update("""
                delete from %s.role_permissions rp
                using %s.permissions p
                where rp.permission_id = p.id
                  and p.code like '%s%%'
                """.formatted(schema, schema, PERMISSION_CODE_PREFIX));
        jdbcTemplate.update("""
                delete from %s.permissions
                where code like '%s%%'
                """.formatted(schema, PERMISSION_CODE_PREFIX));
    }

    @Test
    void listPermissionsIncludesSeededUserListPage() {
        assertThat(service.listPermissions())
                .extracting(permission -> permission.code())
                .contains("page.admin.users.list");
    }

    @Test
    void treePermissionsGroupsChildrenUnderParent() {
        var tree = service.treePermissions();
        var userMenu = tree.stream()
                .filter(node -> "menu.admin.users".equals(node.code()))
                .findFirst()
                .orElseThrow();

        assertThat(userMenu.children())
                .extracting(child -> child.code())
                .contains("page.admin.users.list", "action.admin.users.create");
    }

    @Test
    void createPermissionPersistsNewPermission() {
        var parentId = permissionRepository.findAll().stream()
                .filter(permission -> "menu.admin.permissions".equals(permission.getCode()))
                .map(Permission::getId)
                .findFirst()
                .orElseThrow();
        var code = PERMISSION_CODE_PREFIX + "create-" + UUID.randomUUID().toString().substring(0, 8);

        var created = service.createPermission(new PermissionRequest(
                code,
                "测试权限",
                "ACTION",
                "admin",
                parentId,
                null,
                "test-action",
                999,
                true,
                "创建权限测试"
        ), DemoActors.DEMO_ADMIN_ID);

        assertThat(created.code()).isEqualTo(code);
        assertThat(created.parentId()).isEqualTo(parentId);
        assertThat(created.enabled()).isTrue();
    }

    @Test
    void updatePermissionChangesEditableFields() {
        var created = createPermission();

        var updated = service.updatePermission(created.id(), new PermissionRequest(
                created.code(),
                "更新后的权限",
                "ACTION",
                "admin",
                created.parentId(),
                "/admin/permissions/updated",
                "updated-action",
                1001,
                true,
                "更新说明"
        ), DemoActors.DEMO_ADMIN_ID);

        assertThat(updated.name()).isEqualTo("更新后的权限");
        assertThat(updated.routePath()).isEqualTo("/admin/permissions/updated");
        assertThat(updated.actionKey()).isEqualTo("updated-action");
        assertThat(updated.sortOrder()).isEqualTo(1001);
        assertThat(updated.description()).isEqualTo("更新说明");
    }

    @Test
    void togglePermissionStatusDisablesPermission() {
        var created = createPermission();

        var updated = service.togglePermissionStatus(created.id(), false, DemoActors.DEMO_ADMIN_ID);

        assertThat(updated.enabled()).isFalse();
        assertThat(service.listPermissions())
                .filteredOn(permission -> permission.id().equals(created.id()))
                .singleElement()
                .extracting(permission -> permission.enabled())
                .isEqualTo(false);
    }

    private com.sportexpo.exhibitor.rbac.dto.PermissionResourceResponse createPermission() {
        var parentId = permissionRepository.findAll().stream()
                .filter(permission -> "menu.admin.permissions".equals(permission.getCode()))
                .map(Permission::getId)
                .findFirst()
                .orElseThrow();
        return service.createPermission(new PermissionRequest(
                PERMISSION_CODE_PREFIX + UUID.randomUUID().toString().substring(0, 8),
                "原始权限",
                "ACTION",
                "admin",
                parentId,
                null,
                "initial-action",
                1000,
                true,
                "原始说明"
        ), DemoActors.DEMO_ADMIN_ID);
    }
}
