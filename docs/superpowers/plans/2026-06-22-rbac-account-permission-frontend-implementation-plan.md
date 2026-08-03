# RBAC Account Permission Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-style login flow, RBAC permission model, dynamic admin navigation, multi-role workbench switching, and a usable permission center across backend and frontend.

**Architecture:** Extend the existing V6 auth foundation with V7 role-management fields, permission resources, and role-permission mappings, then expose current-user permissions plus user/role/permission management APIs. On the frontend, replace the demo entry with a real login flow, add an auth provider and permission guards, persist active workbench role in local storage, move admin navigation to permission-driven registries, and implement the permission center pages with Ant Design + ProComponents.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, JPA, PostgreSQL, React 18, TypeScript, Ant Design, ProComponents, React Router 6, Vitest.

---

## File Structure

Backend files to create:

- `backend/src/main/resources/db/migrations/v7/V7_001__create_permission_tables.sql`
- `backend/src/main/resources/db/migrations/v7/V7_002__seed_permission_resources.sql`
- `backend/src/main/resources/db/migrations/v7/V7_003__seed_role_permissions.sql`
- `backend/src/main/resources/db/migrations/v7/R7_rollback.sql`
- `backend/src/main/resources/db/migrations/v7/V7_verify.sql`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/Permission.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermission.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermissionId.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermissionRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionResourceResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionTreeNodeResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermissionUpdateRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UserSummaryResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UserDetailResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/CreateUserRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateUserRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateUserStatusRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateUserRolesRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateDefaultRoleRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/ResetPasswordRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UserManagementService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UserManagementController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/RoleSummaryResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/RoleDetailResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/CreateRoleRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateRoleRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/CopyRoleRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UpdateRoleStatusRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/RoleManagementService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/RoleManagementController.java`
- `backend/src/test/java/com/sportexpo/exhibitor/rbac/PermissionServiceTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/users/UserManagementServiceTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/users/RoleManagementServiceTest.java`

Backend files to modify:

- `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`
- `backend/src/main/java/com/sportexpo/exhibitor/auth/CurrentUserResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/auth/AuthController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/security/CurrentUser.java`
- `backend/src/main/java/com/sportexpo/exhibitor/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/User.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/Role.java`
- `backend/src/main/java/com/sportexpo/exhibitor/users/UserRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/common/GlobalExceptionHandler.java`
- `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java`

Frontend files to create:

- `frontend/src/app/types/auth.ts`
- `frontend/src/app/types/rbac.ts`
- `frontend/src/app/auth.ts`
- `frontend/src/app/auth.test.ts`
- `frontend/src/app/AuthProvider.tsx`
- `frontend/src/app/ProtectedRoute.tsx`
- `frontend/src/app/AccessGuard.tsx`
- `frontend/src/app/roleRouting.ts`
- `frontend/src/app/roleRouting.test.ts`
- `frontend/src/app/permissionRegistry.ts`
- `frontend/src/app/menuRegistry.tsx`
- `frontend/src/app/menuRegistry.test.tsx`
- `frontend/src/app/access.ts`
- `frontend/src/pages/errors/Unauthorized401Page.tsx`
- `frontend/src/pages/errors/Forbidden403Page.tsx`
- `frontend/src/pages/admin/users/UserListPage.tsx`
- `frontend/src/pages/admin/roles/RoleListPage.tsx`
- `frontend/src/pages/admin/permissions/PermissionResourcePage.tsx`
- `frontend/src/pages/admin/role-permissions/RolePermissionMatrixPage.tsx`
- `frontend/src/pages/admin/rbac/rbacViewModels.ts`
- `frontend/src/pages/admin/rbac/rbacViewModels.test.ts`

Frontend files to modify:

- `frontend/src/app/request.ts`
- `frontend/src/app/routes.tsx`
- `frontend/src/app/layouts/AdminLayout.tsx`
- `frontend/src/app/layouts/ExhibitorLayout.tsx`
- `frontend/src/app/App.tsx`
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/admin/pageLogic.ts`
- `frontend/src/pages/admin/pageLogic.test.ts`
- `frontend/src/styles/global.css`

---

## Task 1: Add V7 RBAC Schema And Seed Data

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`
- Create: `backend/src/main/resources/db/migrations/v7/V7_001__create_permission_tables.sql`
- Create: `backend/src/main/resources/db/migrations/v7/V7_002__seed_permission_resources.sql`
- Create: `backend/src/main/resources/db/migrations/v7/V7_003__seed_role_permissions.sql`
- Create: `backend/src/main/resources/db/migrations/v7/R7_rollback.sql`
- Create: `backend/src/main/resources/db/migrations/v7/V7_verify.sql`

- [ ] **Step 1: Write the failing foundation test for V7 tables and seeds**

Add a test like this to `FoundationApplicationTest`:

```java
@Test
void initializesV7RbacTablesAndSeeds() {
    var tables = jdbcTemplate.queryForList("""
            select table_name
            from information_schema.tables
            where table_schema = ?
            """, String.class, schema);

    assertThat(tables).contains("permissions", "role_permissions");

    var roleColumns = jdbcTemplate.queryForList("""
            select column_name
            from information_schema.columns
            where table_schema = ?
              and table_name = 'roles'
            """, String.class, schema);

    assertThat(roleColumns).contains("role_type", "enabled", "description");

    var permissionCodes = jdbcTemplate.queryForList("""
            select code
            from %s.permissions
            order by code
            """.formatted(schema), String.class);

    assertThat(permissionCodes).contains(
            "menu.admin.users",
            "page.admin.users.list",
            "action.admin.users.create",
            "menu.admin.roles",
            "page.admin.permissions.list",
            "page.admin.role-permissions.matrix"
    );
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=FoundationApplicationTest test
```

Expected: FAIL because the V7 tables do not exist yet.

- [ ] **Step 3: Extend `SchemaInitializer` to execute V7 scripts**

Update `SchemaInitializer` to add ordered V7 execution after V6:

```java
private static final List<String> V7_BASELINE_SCRIPTS = List.of(
        "db/migrations/v7/V7_001__create_permission_tables.sql",
        "db/migrations/v7/V7_002__seed_permission_resources.sql",
        "db/migrations/v7/V7_003__seed_role_permissions.sql"
);

for (var script : V7_BASELINE_SCRIPTS) {
    executeSqlResource(script);
}
```

- [ ] **Step 4: Create the permission tables**

Create `V7_001__create_permission_tables.sql`:

```sql
alter table __schema__.roles
    add column if not exists role_type varchar(20) not null default 'SYSTEM',
    add column if not exists enabled boolean not null default true,
    add column if not exists description varchar(500),
    add column if not exists created_by uuid,
    add column if not exists updated_by uuid;

create table if not exists __schema__.permissions (
    id uuid primary key,
    code varchar(200) not null unique,
    name varchar(160) not null,
    resource_type varchar(20) not null,
    module_key varchar(80) not null,
    parent_id uuid references __schema__.permissions(id),
    route_path varchar(255),
    action_key varchar(160),
    sort_order int not null default 0,
    enabled boolean not null default true,
    description varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.role_permissions (
    role_id uuid not null references __schema__.roles(id),
    permission_id uuid not null references __schema__.permissions(id),
    created_at timestamptz not null,
    created_by uuid,
    primary key (role_id, permission_id)
);

create index if not exists idx_permissions_parent_sort
    on __schema__.permissions(parent_id, sort_order);

create index if not exists idx_permissions_type_module
    on __schema__.permissions(resource_type, module_key);
```

- [ ] **Step 5: Seed permission resources**

Create `V7_002__seed_permission_resources.sql` with deterministic UUIDs and at least these resources:

```sql
insert into __schema__.permissions (
    id, code, name, resource_type, module_key, parent_id, route_path, action_key,
    sort_order, enabled, description, created_at, updated_at, created_by, updated_by
) values
    ('00000000-0000-0000-0000-000000000690', 'menu.admin.dashboard', '工作台', 'MENU', 'admin', null, '/admin/dashboard', null, 10, true, 'Admin dashboard menu', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000691', 'page.admin.dashboard', '工作台页面', 'PAGE', 'admin', '00000000-0000-0000-0000-000000000690', '/admin/dashboard', null, 20, true, 'Admin dashboard page', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000701', 'menu.admin.users', '用户管理', 'MENU', 'admin', null, '/admin/users', null, 100, true, 'Admin user menu', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000702', 'page.admin.users.list', '用户列表页', 'PAGE', 'admin', '00000000-0000-0000-0000-000000000701', '/admin/users', null, 110, true, 'Admin user list page', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000703', 'action.admin.users.create', '创建用户', 'ACTION', 'admin', '00000000-0000-0000-0000-000000000701', null, 'create-user', 120, true, 'Create user action', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000711', 'menu.admin.roles', '角色管理', 'MENU', 'admin', null, '/admin/roles', null, 200, true, 'Admin role menu', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000712', 'page.admin.roles.list', '角色列表页', 'PAGE', 'admin', '00000000-0000-0000-0000-000000000711', '/admin/roles', null, 210, true, 'Admin role list page', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000721', 'menu.admin.permissions', '权限资源', 'MENU', 'admin', null, '/admin/permissions', null, 300, true, 'Permission resources menu', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000722', 'page.admin.permissions.list', '权限资源页', 'PAGE', 'admin', '00000000-0000-0000-0000-000000000721', '/admin/permissions', null, 310, true, 'Permission resource page', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000731', 'menu.admin.role-permissions', '授权矩阵', 'MENU', 'admin', null, '/admin/role-permissions', null, 400, true, 'Role permission matrix menu', now(), now(), null, null),
    ('00000000-0000-0000-0000-000000000732', 'page.admin.role-permissions.matrix', '授权矩阵页', 'PAGE', 'admin', '00000000-0000-0000-0000-000000000731', '/admin/role-permissions', null, 410, true, 'Role permission matrix page', now(), now(), null, null)
on conflict (code) do nothing;
```

Also seed action permissions used in this round:

- `action.admin.users.edit`
- `action.admin.users.enable`
- `action.admin.users.disable`
- `action.admin.users.batch-enable`
- `action.admin.users.batch-disable`
- `action.admin.users.reset-password`
- `action.admin.users.assign-roles`
- `action.admin.users.set-default-role`
- `action.admin.roles.create`
- `action.admin.roles.edit`
- `action.admin.roles.copy`
- `action.admin.roles.toggle-status`
- `action.admin.roles.assign-permissions`
- `action.admin.permissions.create`
- `action.admin.permissions.edit`
- `action.admin.permissions.toggle-status`

Also seed menu/page permissions for the existing admin shell so Task 8 can guard them without inventing permissions ad hoc:

- `menu.admin.exhibitors` / `page.admin.exhibitors.list`
- `menu.admin.registrations` / `page.admin.registrations.list`
- `menu.admin.orders` / `page.admin.orders.list`
- `menu.admin.contracts` / `page.admin.contracts.list`
- `menu.admin.payments` / `page.admin.payments.list`
- `menu.admin.selections` / `page.admin.selections.list`
- `menu.admin.events` / `page.admin.events.list`
- `menu.admin.booths` / `page.admin.booths.list`
- `menu.admin.area-tiers` / `page.admin.area-tiers.list`
- `menu.admin.price-rules` / `page.admin.price-rules.list`

- [ ] **Step 6: Seed role-permission assignments**

Create `V7_003__seed_role_permissions.sql`:

```sql
update __schema__.roles
set role_type = 'SYSTEM',
    enabled = true
where code in ('SYSTEM_ADMIN', 'ORGANIZER_ADMIN', 'SALES', 'FINANCE', 'SERVICE_STAFF', 'EXHIBITOR');

insert into __schema__.role_permissions (role_id, permission_id, created_at, created_by)
select r.id, p.id, now(), null
from __schema__.roles r
join __schema__.permissions p on 1 = 1
where r.code = 'SYSTEM_ADMIN'
on conflict do nothing;

insert into __schema__.role_permissions (role_id, permission_id, created_at, created_by)
select r.id, p.id, now(), null
from __schema__.roles r
join __schema__.permissions p on p.code in (
    'menu.admin.users',
    'page.admin.users.list',
    'menu.admin.roles',
    'page.admin.roles.list',
    'menu.admin.permissions',
    'page.admin.permissions.list',
    'menu.admin.role-permissions',
    'page.admin.role-permissions.matrix',
    'action.admin.users.create',
    'action.admin.users.edit',
    'action.admin.users.enable',
    'action.admin.users.disable',
    'action.admin.users.reset-password',
    'action.admin.users.assign-roles',
    'action.admin.users.set-default-role',
    'action.admin.roles.create',
    'action.admin.roles.edit',
    'action.admin.roles.copy',
    'action.admin.roles.toggle-status',
    'action.admin.roles.assign-permissions',
    'action.admin.permissions.create',
    'action.admin.permissions.edit',
    'action.admin.permissions.toggle-status'
)
where r.code = 'ORGANIZER_ADMIN'
on conflict do nothing;
```

- [ ] **Step 7: Add verify and rollback SQL**

Create `V7_verify.sql`:

```sql
select count(*) from __schema__.permissions;
select count(*) from __schema__.role_permissions;
select code from __schema__.permissions
where code in (
    'menu.admin.users',
    'page.admin.users.list',
    'menu.admin.roles',
    'page.admin.permissions.list',
    'page.admin.role-permissions.matrix'
)
order by code;
```

Create `R7_rollback.sql`:

```sql
drop table if exists __schema__.role_permissions;
drop table if exists __schema__.permissions;
```

- [ ] **Step 8: Run the foundation test again**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=FoundationApplicationTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java backend/src/main/resources/db/migrations/v7 backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java
git commit -m "feat: add v7 rbac schema"
```

---

## Task 2: Build Backend Permission Aggregation And Current User Contract

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/auth/CurrentUserResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/auth/AuthController.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/security/CurrentUser.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/Permission.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionRepository.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermission.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermissionId.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/RolePermissionRepository.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java`

- [ ] **Step 1: Write a failing `/api/auth/me` permission test**

Add a test like:

```java
@Test
void currentUserResponseIncludesDefaultRoleAndPermissions() {
    var response = restTemplate.exchange(
            "/api/auth/me",
            HttpMethod.GET,
            authorized("admin"),
            new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<String>) response.getBody().data().get("permissions"))
            .contains("menu.admin.users", "page.admin.users.list");
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=SecurityIntegrationTest test
```

Expected: FAIL because the response does not include permissions yet.

- [ ] **Step 3: Implement JPA entities and repositories**

Create `Permission.java` shape:

```java
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    private UUID id;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "resource_type")
    private String resourceType;
    @Column(name = "module_key")
    private String moduleKey;
    @Column(name = "parent_id")
    private UUID parentId;
    @Column(name = "route_path")
    private String routePath;
    @Column(name = "action_key")
    private String actionKey;
    @Column(name = "sort_order")
    private int sortOrder;
    @Column(name = "enabled")
    private boolean enabled;
    @Column(name = "description")
    private String description;
}
```

Create repository query:

```java
@Query("""
        select distinct p
        from Permission p
        join RolePermission rp on rp.permission.id = p.id
        join Role r on r.id = rp.role.id
        where r.code in :roleCodes
          and p.enabled = true
        order by p.sortOrder asc, p.code asc
        """)
List<Permission> findEnabledByRoleCodes(@Param("roleCodes") Collection<String> roleCodes);
```

- [ ] **Step 4: Expand `CurrentUser` and `/api/auth/me`**

Change `CurrentUser`:

```java
public record CurrentUser(
        UUID userId,
        String username,
        String displayName,
        Set<String> roles,
        String defaultRole,
        UUID exhibitorId,
        Set<String> permissions
) { ... }
```

Change `CurrentUserResponse`:

```java
public record CurrentUserResponse(
        UUID userId,
        String username,
        String displayName,
        List<String> roles,
        String defaultRole,
        List<String> activeAvailableRoles,
        UUID exhibitorId,
        List<String> permissions
) { }
```

- [ ] **Step 5: Populate permissions in `JwtAuthenticationFilter`**

Fetch the authenticated user, then attach permissions:

```java
var permissions = permissionRepository.findEnabledByRoleCodes(user.roleCodes()).stream()
        .map(Permission::getCode)
        .collect(Collectors.toCollection(LinkedHashSet::new));

var currentUser = new CurrentUser(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        Set.copyOf(user.roleCodes()),
        user.getDefaultRole(),
        user.getExhibitorId(),
        permissions
);
```

- [ ] **Step 6: Return the richer `/api/auth/me` response**

In `AuthController.me`:

```java
return ApiResponse.ok(new CurrentUserResponse(
        currentUser.userId(),
        currentUser.username(),
        currentUser.displayName(),
        currentUser.roles().stream().sorted().toList(),
        currentUser.defaultRole(),
        currentUser.roles().stream().sorted().toList(),
        currentUser.exhibitorId(),
        currentUser.permissions().stream().sorted().toList()
));
```

- [ ] **Step 7: Re-run the security test**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=SecurityIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/auth backend/src/main/java/com/sportexpo/exhibitor/security backend/src/main/java/com/sportexpo/exhibitor/rbac backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java
git commit -m "feat: expose current user permissions"
```

---

## Task 3: Add Backend User, Role, And Permission Management APIs

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/users/UserManagementService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/users/UserManagementController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/users/RoleManagementService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/users/RoleManagementController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/rbac/PermissionController.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/users/User.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/users/Role.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/users/UserRepository.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/common/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/users/UserManagementServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/users/RoleManagementServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/rbac/PermissionServiceTest.java`

- [ ] **Step 1: Write failing user management tests**

Add tests covering:

```java
@Test
void createExhibitorUserRequiresBoundExhibitor() { ... }

@Test
void updateUserRolesRejectsDefaultRoleOutsideAssignedRoles() { ... }

@Test
void resetPasswordChangesHashWithoutChangingUsername() { ... }
```

- [ ] **Step 2: Run the failing user management tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=UserManagementServiceTest test
```

Expected: FAIL because the service does not exist yet.

- [ ] **Step 3: Add minimal domain mutators on `User` and `Role`**

Add methods on `User`:

```java
public void updateProfile(String displayName, String phone, String email, String userType,
                          UUID exhibitorId, String status, String defaultRole, UUID actorId,
                          OffsetDateTime now) {
    this.displayName = displayName;
    this.phone = phone;
    this.email = email;
    this.userType = userType;
    this.exhibitorId = exhibitorId;
    this.status = status;
    this.defaultRole = defaultRole;
    this.updatedAt = now;
    this.updatedBy = actorId;
}

public void replaceRoles(Collection<Role> roles) {
    userRoles.clear();
    roles.forEach(this::assignRole);
}

public void resetPassword(String passwordHash, UUID actorId, OffsetDateTime now) {
    this.passwordHash = passwordHash;
    this.updatedAt = now;
    this.updatedBy = actorId;
}
```

Add role management fields on `Role`:

```java
@Column(name = "role_type")
private String roleType;

@Column(name = "enabled")
private boolean enabled;

@Column(name = "description")
private String description;
```

- [ ] **Step 4: Implement `UserManagementService`**

Core behavior:

```java
public UserDetailResponse create(CreateUserRequest request, UUID actorId) {
    validateUserTypeAndBinding(request.userType(), request.exhibitorId());
    var roles = roleRepository.findByCodeIn(request.roleCodes());
    validateDefaultRole(request.defaultRole(), roles);
    var now = OffsetDateTime.now();
    var user = new User(
            UUID.randomUUID(),
            request.username().trim(),
            passwordEncoder.encode(request.initialPassword()),
            request.displayName().trim(),
            request.phone(),
            request.email(),
            request.userType(),
            request.exhibitorId(),
            request.enabled() ? "ACTIVE" : "DISABLED",
            request.defaultRole(),
            0,
            null,
            null,
            0,
            now,
            now,
            actorId,
            actorId
    );
    roles.forEach(user::assignRole);
    userRepository.save(user);
    return UserDetailResponse.from(user);
}
```

- [ ] **Step 5: Implement `RoleManagementService` and `PermissionService`**

Minimum methods:

- `listRoles()`
- `createRole()`
- `updateRole()`
- `copyRole()`
- `toggleRoleStatus()`
- `getRolePermissions(roleId)`
- `updateRolePermissions(roleId, permissionCodes, actorId)`
- `listPermissions()`
- `treePermissions()`
- `createPermission()`
- `updatePermission()`
- `togglePermissionStatus()`

For role copy:

```java
var copied = new Role(
        UUID.randomUUID(),
        request.newCode().trim(),
        request.newName().trim(),
        "CUSTOM",
        true,
        request.description(),
        now,
        now
);
```

Then copy the source role's `role_permissions`.

- [ ] **Step 6: Add controllers and route guards**

Controller route requirements:

```java
@PreAuthorize("hasAuthority('page.admin.users.list')")
@GetMapping("/api/users")

@PreAuthorize("hasAuthority('action.admin.users.create')")
@PostMapping("/api/users")

@PreAuthorize("hasAuthority('page.admin.roles.list')")
@GetMapping("/api/roles")

@PreAuthorize("hasAuthority('action.admin.roles.assign-permissions')")
@PutMapping("/api/roles/{id}/permissions")

@PreAuthorize("hasAuthority('page.admin.permissions.list')")
@GetMapping("/api/permissions")
```

- [ ] **Step 7: Re-run service tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=UserManagementServiceTest,RoleManagementServiceTest,PermissionServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/users backend/src/main/java/com/sportexpo/exhibitor/rbac backend/src/main/java/com/sportexpo/exhibitor/common/GlobalExceptionHandler.java backend/src/test/java/com/sportexpo/exhibitor/users backend/src/test/java/com/sportexpo/exhibitor/rbac
git commit -m "feat: add rbac management apis"
```

---

## Task 4: Replace The Demo Entry With Real Frontend Auth

**Files:**
- Create: `frontend/src/app/types/auth.ts`
- Create: `frontend/src/app/auth.ts`
- Create: `frontend/src/app/AuthProvider.tsx`
- Create: `frontend/src/app/ProtectedRoute.tsx`
- Create: `frontend/src/app/AccessGuard.tsx`
- Create: `frontend/src/app/roleRouting.ts`
- Create: `frontend/src/app/auth.test.ts`
- Create: `frontend/src/app/roleRouting.test.ts`
- Create: `frontend/src/pages/errors/Unauthorized401Page.tsx`
- Create: `frontend/src/pages/errors/Forbidden403Page.tsx`
- Modify: `frontend/src/app/request.ts`
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/pages/LoginPage.tsx`

- [ ] **Step 1: Write failing auth tests**

Create tests:

```ts
it('stores access token after login', () => {
  setAccessToken('abc');
  expect(getAccessToken()).toBe('abc');
});

it('maps exhibitor-only users to /exhibitor', () => {
  expect(getDefaultPath({
    id: 'u1',
    username: 'exhibitor01',
    displayName: 'Exhibitor',
    roles: ['EXHIBITOR'],
    defaultRole: 'EXHIBITOR',
    activeAvailableRoles: ['EXHIBITOR'],
    exhibitorId: 'e1',
    permissions: [],
  })).toBe('/exhibitor');
});

it('restores the previous active role only when the user still owns it', () => {
  expect(resolveRestoredActiveRole({
    id: 'u1',
    username: 'admin',
    displayName: 'Admin',
    roles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
    defaultRole: 'SYSTEM_ADMIN',
    activeAvailableRoles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
    exhibitorId: 'e1',
    permissions: [],
  }, 'EXHIBITOR')).toBe('EXHIBITOR');
});
```

- [ ] **Step 2: Run the failing tests**

Run:

```powershell
npm test -- auth roleRouting
```

Expected: FAIL because the modules do not exist yet.

- [ ] **Step 3: Implement auth types and storage**

Create `types/auth.ts`:

```ts
export type RoleCode =
  | 'SYSTEM_ADMIN'
  | 'ORGANIZER_ADMIN'
  | 'SALES'
  | 'FINANCE'
  | 'SERVICE_STAFF'
  | 'EXHIBITOR';

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
  defaultRole?: RoleCode | null;
  activeAvailableRoles: RoleCode[];
  exhibitorId?: string | null;
  permissions: string[];
};
```

Create `auth.ts`:

```ts
const TOKEN_KEY = 'access_token';
const ACTIVE_ROLE_KEY = 'active_role';

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getStoredActiveRole() {
  return localStorage.getItem(ACTIVE_ROLE_KEY);
}

export function setStoredActiveRole(role: string) {
  localStorage.setItem(ACTIVE_ROLE_KEY, role);
}

export function clearStoredActiveRole() {
  localStorage.removeItem(ACTIVE_ROLE_KEY);
}
```

- [ ] **Step 4: Implement role routing and provider**

`roleRouting.ts`:

```ts
const INTERNAL_ROLE_PRIORITY: RoleCode[] = [
  'SYSTEM_ADMIN',
  'ORGANIZER_ADMIN',
  'FINANCE',
  'SERVICE_STAFF',
  'SALES',
];

export function resolveActiveRole(user: CurrentUser): RoleCode {
  if (user.defaultRole && user.roles.includes(user.defaultRole)) {
    return user.defaultRole;
  }
  return INTERNAL_ROLE_PRIORITY.find((role) => user.roles.includes(role))
    ?? 'EXHIBITOR';
}

export function getDefaultPath(user: CurrentUser, activeRole = resolveActiveRole(user)) {
  return activeRole === 'EXHIBITOR' ? '/exhibitor' : '/admin/dashboard';
}

export function resolveRestoredActiveRole(user: CurrentUser, storedRole?: string | null): RoleCode {
  if (storedRole && user.roles.includes(storedRole as RoleCode)) {
    return storedRole as RoleCode;
  }
  return resolveActiveRole(user);
}
```

`AuthProvider.tsx` should expose:

- `user`
- `permissions`
- `activeRole`
- `login`
- `logout`
- `switchRole`
- `hasPermission`

Provider behavior:

- Login success always starts from `resolveActiveRole(user)` based on `defaultRole` or role priority.
- App refresh may restore `activeRole` from local storage only when the stored role still belongs to the current user.
- Logout clears both the token and stored active role.

- [ ] **Step 5: Update the request layer**

Change `request.ts` to attach bearer tokens and preserve return paths:

```ts
const token = getAccessToken();
const response = await fetch(withApiBaseUrl(url), {
  ...options,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  },
});

if (response.status === 401) {
  clearAccessToken();
  clearStoredActiveRole();
  const returnTo = `${window.location.pathname}${window.location.search}`;
  window.location.assign(`/login?returnTo=${encodeURIComponent(returnTo)}`);
  throw new Error('未登录或登录已过期');
}
```

For `403`, keep a friendly error route instead of only showing a toast:

```ts
if (response.status === 403) {
  const body = await parseBody<unknown>(response);
  const requiredPermission = typeof body?.errorCode === 'string' ? body.errorCode : '';
  window.location.assign(`/403?requiredPermission=${encodeURIComponent(requiredPermission)}`);
  throw new Error(body?.message || '无权限访问');
}
```

- [ ] **Step 6: Replace `LoginPage`**

The new page should use `Form`, `Input`, and `Button`, then call `/api/auth/login` and `/api/auth/me`:

```tsx
<Form layout="vertical" onFinish={handleSubmit}>
  <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
    <Input autoComplete="username" />
  </Form.Item>
  <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
    <Input.Password autoComplete="current-password" />
  </Form.Item>
  <Button type="primary" htmlType="submit" loading={submitting} block>
    登录
  </Button>
</Form>
```

Also create:

- `Unauthorized401Page.tsx` with a relogin CTA
- `Forbidden403Page.tsx` with:
  - title
  - description
  - current roles
  - required permission query parameter
  - back button
  - workbench switch button when multiple roles are available

- [ ] **Step 7: Wrap the app in `AuthProvider` and run tests**

Run:

```powershell
npm test -- auth roleRouting
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/app frontend/src/pages/LoginPage.tsx frontend/src/app/App.tsx
git commit -m "feat: add frontend auth flow"
```

---

## Task 5: Add Permission-Driven Routing, Admin Menu Filtering, And Role Switching

**Files:**
- Create: `frontend/src/app/types/rbac.ts`
- Create: `frontend/src/app/permissionRegistry.ts`
- Create: `frontend/src/app/menuRegistry.tsx`
- Create: `frontend/src/app/menuRegistry.test.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Write a failing menu registry test**

Create:

```ts
it('shows permission center menu only when the current user has matching menu permissions', () => {
  const menu = buildAdminMenu([
    'menu.admin.users',
    'menu.admin.roles',
    'menu.admin.permissions',
  ]);

  expect(menu.map((item) => item.key)).toContain('/admin/permissions');
  expect(menu.map((item) => item.key)).not.toContain('/admin/role-permissions');
});
```

- [ ] **Step 2: Run the failing frontend test**

Run:

```powershell
npm test -- menuRegistry
```

Expected: FAIL because the registry does not exist.

- [ ] **Step 3: Implement permission registries**

Create a route/permission registry like:

```ts
export const PAGE_PERMISSIONS = {
  adminDashboard: 'page.admin.dashboard',
  adminUsers: 'page.admin.users.list',
  adminRoles: 'page.admin.roles.list',
  adminPermissions: 'page.admin.permissions.list',
  adminRolePermissions: 'page.admin.role-permissions.matrix',
} as const;

export const ACTION_PERMISSIONS = {
  createUser: 'action.admin.users.create',
  batchEnableUsers: 'action.admin.users.batch-enable',
  batchDisableUsers: 'action.admin.users.batch-disable',
  resetPassword: 'action.admin.users.reset-password',
  assignRolePermissions: 'action.admin.roles.assign-permissions',
} as const;
```

- [ ] **Step 4: Implement `buildAdminMenu`**

The menu builder should only include routes whose menu permission exists:

```ts
export function buildAdminMenu(permissionCodes: string[]) {
  const permissions = new Set(permissionCodes);
  return ADMIN_MENU_REGISTRY
    .filter((item) => permissions.has(item.permission))
    .map((item) => ({
      path: item.path,
      name: item.name,
      icon: item.icon,
    }));
}
```

- [ ] **Step 5: Update `ProtectedRoute` and `routes.tsx`**

Wrap admin pages with required page permission:

```tsx
{
  path: 'users',
  element: (
    <ProtectedRoute requiredPermission="page.admin.users.list">
      {lazyPage(<UserListPage />)}
    </ProtectedRoute>
  ),
}
```

Also switch `/` to:

```tsx
{ path: '/', element: <Navigate to="/login" replace /> }
```

And add friendly auth error routes:

```tsx
{ path: '/401', element: lazyPage(<Unauthorized401Page />) }
{ path: '/403', element: lazyPage(<Forbidden403Page />) }
```

- [ ] **Step 6: Add role switcher to `AdminLayout`**

Use `Dropdown` or `Select` in `ProLayout.actionsRender` or `avatarProps.render`:

```tsx
<Select
  value={activeRole}
  options={availableRoles.map((role) => ({ label: ROLE_LABELS[role], value: role }))}
  onChange={(value) => switchRole(value)}
  variant="borderless"
/>
```

- [ ] **Step 7: Re-run tests and build**

Run:

```powershell
npm test -- menuRegistry roleRouting
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/app frontend/src/app/layouts frontend/src/app/routes.tsx frontend/src/styles/global.css
git commit -m "feat: add permission driven routing and menus"
```

---

## Task 6: Implement Admin User Management Page

**Files:**
- Create: `frontend/src/pages/admin/users/UserListPage.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Test: `frontend/src/pages/admin/pageLogic.test.ts`

- [ ] **Step 1: Add a failing page logic test for user status display and role tags**

Append tests:

```ts
it('formats disabled user status as a danger tag label', () => {
  expect(formatNullableAdminText('DISABLED')).toBe('DISABLED');
});
```

And add a new helper test for role tag composition:

```ts
it('joins multiple role labels in stable order', () => {
  expect(formatRoleLabels(['SALES', 'ORGANIZER_ADMIN'])).toBe('主办方管理员 / 销售');
});
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
npm test -- pageLogic
```

Expected: FAIL because `formatRoleLabels` does not exist.

- [ ] **Step 3: Add the helper in `pageLogic.ts`**

```ts
const ROLE_LABELS: Record<string, string> = {
  SYSTEM_ADMIN: '系统管理员',
  ORGANIZER_ADMIN: '主办方管理员',
  SALES: '销售',
  FINANCE: '财务',
  SERVICE_STAFF: '服务人员',
  EXHIBITOR: '展商',
};

export function formatRoleLabels(roleCodes: string[]) {
  return [...roleCodes]
    .sort((left, right) => left.localeCompare(right))
    .map((code) => ROLE_LABELS[code] ?? code)
    .join(' / ');
}
```

- [ ] **Step 4: Implement `UserListPage`**

Use `ProTable` with:

```tsx
<ProTable<UserSummary>
  rowKey="id"
  search={{ labelWidth: 88 }}
  request={async (params) => {
    const data = await apiGet<UserSummary[]>(`/api/users?username=${params.username ?? ''}&status=${params.status ?? ''}`);
    return { data, success: true };
  }}
  columns={[
    { title: '账号', dataIndex: 'username' },
    { title: '姓名', dataIndex: 'displayName' },
    { title: '用户类型', dataIndex: 'userType' },
    { title: '角色', render: (_, record) => formatRoleLabels(record.roles) },
    { title: '默认角色', dataIndex: 'defaultRole' },
    { title: '状态', dataIndex: 'status', render: (_, record) => <Tag color={record.status === 'ACTIVE' ? 'success' : 'error'}>{record.status === 'ACTIVE' ? '启用' : '禁用'}</Tag> },
  ]}
/>
```

- [ ] **Step 5: Add row actions under action permissions**

Wrap action buttons:

```tsx
<AccessGuard permission="action.admin.users.reset-password">
  <Button type="link">重置密码</Button>
</AccessGuard>
```

Also add:

- 新建用户
- 编辑
- 启用 / 禁用
- 分配角色
- 设置默认角色

If batch actions are added to the first version of the page, they must use dedicated permission codes:

```tsx
<AccessGuard permission="action.admin.users.batch-enable">
  <Button disabled={selectedRows.length === 0}>批量启用</Button>
</AccessGuard>
```

All edit forms should use `ModalForm`.

- [ ] **Step 6: Re-run tests and build**

Run:

```powershell
npm test -- pageLogic
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/pages/admin/users frontend/src/pages/admin/pageLogic.ts frontend/src/pages/admin/pageLogic.test.ts frontend/src/app/routes.tsx
git commit -m "feat: add admin user management page"
```

---

## Task 7: Implement Role Management, Permission Resources, And Authorization Matrix

**Files:**
- Create: `frontend/src/pages/admin/roles/RoleListPage.tsx`
- Create: `frontend/src/pages/admin/permissions/PermissionResourcePage.tsx`
- Create: `frontend/src/pages/admin/role-permissions/RolePermissionMatrixPage.tsx`
- Create: `frontend/src/pages/admin/rbac/rbacViewModels.ts`
- Create: `frontend/src/pages/admin/rbac/rbacViewModels.test.ts`
- Modify: `frontend/src/app/routes.tsx`

- [ ] **Step 1: Write failing RBAC view-model tests**

Create tests for tree flattening and diff preview:

```ts
it('builds checked keys for a selected role from permission codes', () => {
  expect(buildCheckedPermissionKeys(['menu.admin.users', 'action.admin.users.create']))
    .toEqual(['menu.admin.users', 'action.admin.users.create']);
});

it('computes added and removed permission labels before save', () => {
  expect(diffPermissionSelection(
    ['menu.admin.users'],
    ['menu.admin.users', 'menu.admin.roles'],
    { 'menu.admin.roles': '角色管理' },
  )).toEqual({
    added: ['角色管理'],
    removed: [],
  });
});
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
npm test -- rbacViewModels
```

Expected: FAIL because the module does not exist.

- [ ] **Step 3: Implement view-model helpers**

Create:

```ts
export function buildCheckedPermissionKeys(permissionCodes: string[]) {
  return [...new Set(permissionCodes)].sort();
}

export function diffPermissionSelection(
  oldCodes: string[],
  newCodes: string[],
  labels: Record<string, string>,
) {
  const oldSet = new Set(oldCodes);
  const newSet = new Set(newCodes);
  return {
    added: [...newSet].filter((code) => !oldSet.has(code)).map((code) => labels[code] ?? code),
    removed: [...oldSet].filter((code) => !newSet.has(code)).map((code) => labels[code] ?? code),
  };
}
```

- [ ] **Step 4: Implement `RoleListPage`**

Use `ProTable` and `ModalForm`:

```tsx
columns={[
  { title: '角色名称', dataIndex: 'name' },
  { title: '角色编码', dataIndex: 'code' },
  { title: '类型', render: (_, record) => <Tag>{record.builtIn ? '内置' : '自定义'}</Tag> },
  { title: '状态', render: (_, record) => <Tag color={record.enabled ? 'success' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag> },
  { title: '成员数', dataIndex: 'memberCount' },
]}
```

Actions:

- 新建角色
- 编辑
- 复制
- 启停
- 分配权限

- [ ] **Step 5: Implement `PermissionResourcePage`**

Recommended layout:

```tsx
<Row gutter={16}>
  <Col span={8}>
    <Card title="资源树">
      <Tree treeData={treeData} selectedKeys={selectedKeys} onSelect={handleSelect} />
    </Card>
  </Col>
  <Col span={16}>
    <ProTable ... />
  </Col>
</Row>
```

This page must support:

- filtering by `resourceType`
- creating resources
- editing code/name/path/action key
- enabling/disabling resources

- [ ] **Step 6: Implement `RolePermissionMatrixPage`**

Use a left-right layout:

```tsx
<Row gutter={16}>
  <Col span={6}>
    <List dataSource={roles} renderItem={(role) => (
      <List.Item onClick={() => setSelectedRole(role)}>{role.name}</List.Item>
    )} />
  </Col>
  <Col span={18}>
    <Segmented
      value={resourceType}
      options={[
        { label: '菜单', value: 'MENU' },
        { label: '页面', value: 'PAGE' },
        { label: '操作', value: 'ACTION' },
      ]}
    />
    <Tree checkable checkedKeys={checkedKeys} onCheck={handleCheck} treeData={filteredTree} />
  </Col>
</Row>
```

Saving should show a confirmation modal with added/removed labels.

- [ ] **Step 7: Re-run tests and build**

Run:

```powershell
npm test -- rbacViewModels
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/pages/admin/roles frontend/src/pages/admin/permissions frontend/src/pages/admin/role-permissions frontend/src/pages/admin/rbac frontend/src/app/routes.tsx
git commit -m "feat: add rbac permission center pages"
```

---

## Task 8: Retrofit Existing Shells And Pages To Use Permission Guards

**Files:**
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Modify: `frontend/src/pages/admin/DashboardPage.tsx`
- Modify: `frontend/src/pages/admin/exhibitors/AdminExhibitorListPage.tsx`
- Modify: `frontend/src/pages/admin/orders/AdminOrderListPage.tsx`
- Modify: `frontend/src/pages/admin/orders/AdminOrderDetailPage.tsx`
- Modify: `frontend/src/pages/admin/payments/PaymentConfirmPage.tsx`
- Modify: `frontend/src/pages/admin/selections/SelectionReviewPage.tsx`

- [ ] **Step 1: Add a failing smoke-style menu assertion test**

Extend `menuRegistry.test.tsx`:

```ts
it('does not render hidden admin business menus when menu permissions are absent', () => {
  const menu = buildAdminMenu(['menu.admin.users']);
  expect(menu.map((item) => item.key)).not.toContain('/admin/orders');
});
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
npm test -- menuRegistry
```

Expected: FAIL until the registry covers legacy menus too.

- [ ] **Step 3: Add permission entries for legacy admin pages**

Add menu and page permission mappings for:

- `/admin/dashboard`
- `/admin/exhibitors`
- `/admin/registrations`
- `/admin/orders`
- `/admin/contracts`
- `/admin/payments`
- `/admin/selections`
- `/admin/events`
- `/admin/booths`
- `/admin/area-tiers`
- `/admin/price-rules`

Use existing backend seed resources if already present; otherwise seed them in V7 scripts before implementing frontend guards.

- [ ] **Step 4: Wrap sensitive page actions with `AccessGuard`**

Examples:

```tsx
<AccessGuard permission="action.admin.orders.confirm-sales">
  <Button type="primary">确认订单</Button>
</AccessGuard>
```

```tsx
<AccessGuard permission="action.admin.payments.confirm">
  <Button type="primary">财务确认</Button>
</AccessGuard>
```

- [ ] **Step 5: Keep exhibitor shell simple**

Do not add permission-center concepts to the exhibitor shell. Only ensure:

- logout is available
- current user name is shown
- role switch entry appears only if the user also has internal roles

- [ ] **Step 6: Re-run tests and build**

Run:

```powershell
npm test -- menuRegistry pageLogic
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/app/layouts frontend/src/app/routes.tsx frontend/src/pages/admin
git commit -m "feat: guard existing pages with rbac permissions"
```

---

## Task 9: Full Verification, Smoke Flow, And Docs Sync

**Files:**
- Modify: `README.md`
- Modify: docs if implementation diverges from spec

- [ ] **Step 1: Run the backend test suite**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```powershell
cd frontend
npm test -- auth roleRouting menuRegistry pageLogic rbacViewModels
```

Expected: PASS.

- [ ] **Step 3: Run the frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 4: Start backend locally**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys'
mvn spring-boot:run
```

Expected: backend starts with V6 + V7 schema initialized.

- [ ] **Step 5: Start frontend locally**

Run:

```powershell
cd frontend
npm run dev
```

Expected: local Vite URL is available.

- [ ] **Step 6: Browser smoke**

Verify:

1. `admin` logs in through the real login page and lands on `/admin/dashboard`.
2. `admin` sees `用户管理 / 角色管理 / 权限资源 / 授权矩阵`.
3. `organizer` sees the permission center but not the system-admin-only actions if those were restricted in seed data.
4. `sales01` lands on `/admin/dashboard` and sees a reduced menu without permission-center entries.
5. `exhibitor01` lands on `/exhibitor`.
6. A multi-role account can switch workbenches in the top-right user area.
7. Disabling a user blocks the next login attempt.
8. Removing a role menu permission hides that menu on next login.
9. Visiting a page without page permission opens the friendly `/403` page with the current roles and required permission displayed.

- [ ] **Step 7: Update README**

Add a short section:

```markdown
## RBAC smoke flow

### Demo accounts

| Username | Password | Expected landing |
| --- | --- | --- |
| admin | demo123 | /admin/dashboard |
| organizer | demo123 | /admin/dashboard |
| sales01 | demo123 | /admin/dashboard |
| exhibitor01 | demo123 | /exhibitor |

### What to verify

1. Real login page replaces the old entry selection card.
2. Admin menus are permission-driven.
3. Permission center pages are visible only to authorized roles.
4. Multi-role users can switch workbenches without logging out.
5. Unauthorized page access lands on a friendly 403 page instead of a raw error.
```

- [ ] **Step 8: Final git status**

Run:

```powershell
git status --short
```

Expected: only intended files are modified.

- [ ] **Step 9: Commit docs and verification updates**

```powershell
git add README.md docs
git commit -m "docs: add rbac frontend smoke flow"
```

---

## Self-Review Checklist

- Spec coverage:
  - Login page and JWT frontend flow: Tasks 2, 4, 5, 9.
  - Dynamic menu, route, and button permissions: Tasks 1, 2, 3, 5, 8.
  - Permission center pages: Tasks 6, 7.
  - Multi-role workbench switching: Tasks 4, 5, 9.
  - Backend RBAC persistence: Tasks 1, 2, 3.
- Placeholder scan:
  - No `TODO`, `TBD`, or “similar to above” steps remain.
  - Every task includes a concrete test command and a commit point.
- Type consistency:
  - `CurrentUser` / `CurrentUserResponse` use the same field names across backend and frontend.
  - Permission codes use a single string-code model in migration, backend service, and frontend registry.

## Execution Options

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints
