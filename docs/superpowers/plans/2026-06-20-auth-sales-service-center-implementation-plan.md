# Auth Sales Service Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real account login, role/data permissions, sales assignment/follow-up, and configurable service center with service orders and offline payment confirmation.

**Architecture:** Add a Spring Security/JWT identity layer, then move backend business access from demo IDs to `CurrentUser`. Keep data permissions in application services through `DataScopeService`, not PostgreSQL RLS. Build service center as configurable service types/fields plus EAV application values, service orders, and service payments.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, JPA, PostgreSQL, Redis-ready cache hooks, React, TypeScript, Ant Design, ProComponents, Vitest.

---

## File Structure

Backend files to create:

- `backend/src/main/resources/db/migrations/v6/V6_001__create_auth_tables.sql`
- `backend/src/main/resources/db/migrations/v6/V6_002__create_sales_tables.sql`
- `backend/src/main/resources/db/migrations/v6/V6_003__create_service_tables.sql`
- `backend/src/main/resources/db/migrations/v6/V6_004__add_indexes.sql`
- `backend/src/main/resources/db/migrations/v6/V6_005__seed_roles.sql`
- `backend/src/main/resources/db/migrations/v6/V6_006__seed_users.sql`
- `backend/src/main/resources/db/migrations/v6/V6_007__backfill_demo_data.sql`
- `backend/src/main/resources/db/migrations/v6/R6_rollback.sql`
- `backend/src/main/resources/db/migrations/v6/V6_verify.sql`
- `backend/src/main/java/com/sportexpo/exhibitor/auth/*`
- `backend/src/main/java/com/sportexpo/exhibitor/security/*`
- `backend/src/main/java/com/sportexpo/exhibitor/users/*`
- `backend/src/main/java/com/sportexpo/exhibitor/sales/*`
- `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/*`
- `backend/src/test/java/com/sportexpo/exhibitor/auth/*`
- `backend/src/test/java/com/sportexpo/exhibitor/security/*`
- `backend/src/test/java/com/sportexpo/exhibitor/sales/*`
- `backend/src/test/java/com/sportexpo/exhibitor/servicecenter/*`

Backend files to modify:

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`
- `backend/src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/common/GlobalExceptionHandler.java`
- Existing controllers/services that currently use `DemoActors`
- Existing repositories that need `exhibitorId` scoped queries

Frontend files to create:

- `frontend/src/app/auth.ts`
- `frontend/src/app/auth.test.ts`
- `frontend/src/app/AuthProvider.tsx`
- `frontend/src/app/ProtectedRoute.tsx`
- `frontend/src/app/roleRouting.ts`
- `frontend/src/app/roleRouting.test.ts`
- `frontend/src/pages/admin/users/UserListPage.tsx`
- `frontend/src/pages/admin/roles/RoleListPage.tsx`
- `frontend/src/pages/admin/sales/SalesAssignmentPage.tsx`
- `frontend/src/pages/admin/sales/SalesFollowUpPage.tsx`
- `frontend/src/pages/admin/service-types/ServiceTypeListPage.tsx`
- `frontend/src/pages/admin/service-applications/ServiceApplicationListPage.tsx`
- `frontend/src/pages/admin/service-applications/ServiceApplicationDetailPage.tsx`
- `frontend/src/pages/admin/service-orders/ServiceOrderListPage.tsx`
- `frontend/src/pages/admin/service-payments/ServicePaymentConfirmPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceCenterPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceApplyPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceApplicationListPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceApplicationDetailPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceOrderListPage.tsx`
- `frontend/src/pages/exhibitor/services/ServiceOrderPaymentPage.tsx`
- `frontend/src/pages/exhibitor/services/serviceForm.tsx`
- `frontend/src/pages/exhibitor/services/serviceViewModels.ts`
- `frontend/src/pages/exhibitor/services/serviceViewModels.test.ts`

Frontend files to modify:

- `frontend/src/app/request.ts`
- `frontend/src/app/routes.tsx`
- `frontend/src/app/layouts/AdminLayout.tsx`
- `frontend/src/app/layouts/ExhibitorLayout.tsx`
- `frontend/src/pages/LoginPage.tsx`
- Existing pages that still import `DEMO_EXHIBITOR_ID`

---

## Task 1: Database Migration Runner And V6 SQL

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`
- Create: `backend/src/main/resources/db/migrations/v6/*.sql`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`

- [ ] **Step 1: Write a failing schema migration test**

Add a test to `FoundationApplicationTest`:

```java
@Test
void initializesV6AuthSalesAndServiceTables() {
    var tables = jdbcTemplate.queryForList("""
            select table_name
            from information_schema.tables
            where table_schema = ?
            """, String.class, schema);

    assertThat(tables).contains(
            "users",
            "roles",
            "user_roles",
            "exhibitor_sales_assignments",
            "sales_follow_ups",
            "service_types",
            "service_fields",
            "service_applications",
            "service_application_booths",
            "service_application_values",
            "service_orders",
            "service_order_items",
            "service_payments"
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

Expected: FAIL because the V6 tables do not exist.

- [ ] **Step 3: Extend `SchemaInitializer` to execute V6 scripts**

Implementation requirements:

- Continue executing `db/schema-foundation.sql`.
- Add ordered execution for classpath resources under `db/migrations/v6/` whose names start with `V6_`.
- Replace `__schema__` in every SQL script before execution.
- Do not execute `R6_rollback.sql` or `V6_verify.sql` at startup.
- Use `ScriptUtils.executeSqlScript`, not string splitting.

Core code shape:

```java
private static final List<String> V6_SCRIPTS = List.of(
        "db/migrations/v6/V6_001__create_auth_tables.sql",
        "db/migrations/v6/V6_002__create_sales_tables.sql",
        "db/migrations/v6/V6_003__create_service_tables.sql",
        "db/migrations/v6/V6_004__add_indexes.sql",
        "db/migrations/v6/V6_005__seed_roles.sql",
        "db/migrations/v6/V6_006__seed_users.sql",
        "db/migrations/v6/V6_007__backfill_demo_data.sql"
);
```

- [ ] **Step 4: Create V6 auth SQL**

Create `V6_001__create_auth_tables.sql` with schema-qualified tables:

```sql
create table if not exists __schema__.roles (
    id uuid primary key,
    code varchar(80) not null unique,
    name varchar(120) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists __schema__.users (
    id uuid primary key,
    username varchar(120) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    phone varchar(40),
    email varchar(120),
    user_type varchar(40) not null,
    exhibitor_id uuid,
    status varchar(40) not null,
    default_role varchar(80),
    failed_login_count int not null default 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    jwt_version int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.user_roles (
    user_id uuid not null references __schema__.users(id),
    role_id uuid not null references __schema__.roles(id),
    created_at timestamptz not null,
    primary key (user_id, role_id)
);
```

- [ ] **Step 5: Create sales SQL**

Create `V6_002__create_sales_tables.sql`:

```sql
create table if not exists __schema__.exhibitor_sales_assignments (
    id uuid primary key,
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    sales_user_id uuid not null references __schema__.users(id),
    assignment_type varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique (exhibitor_id, sales_user_id, assignment_type)
);

create table if not exists __schema__.sales_follow_ups (
    id uuid primary key,
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    sales_user_id uuid not null references __schema__.users(id),
    method varchar(40) not null,
    content varchar(1000) not null,
    next_follow_up_at timestamptz,
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);
```

- [ ] **Step 6: Create service SQL**

Create `V6_003__create_service_tables.sql` with:

- `service_types`
- `service_fields`
- `service_applications`
- `service_application_booths`
- `service_application_values`
- `service_application_attachments`
- `service_orders`
- `service_order_items`
- `service_payments`

Use `numeric(14,2)` for money and quantity fields. Include `quote_expires_at` on `service_orders`.

- [ ] **Step 7: Create indexes SQL**

Create `V6_004__add_indexes.sql`:

```sql
create index if not exists idx_sales_assignments_sales_user
    on __schema__.exhibitor_sales_assignments(sales_user_id, assignment_type);

create index if not exists idx_sales_assignments_exhibitor
    on __schema__.exhibitor_sales_assignments(exhibitor_id, assignment_type);

create unique index if not exists uk_exhibitor_primary_sales
    on __schema__.exhibitor_sales_assignments(exhibitor_id)
    where assignment_type = 'PRIMARY';

create index if not exists idx_orders_exhibitor_status
    on __schema__.participation_orders(exhibitor_id, status);

create index if not exists idx_service_applications_exhibitor_status
    on __schema__.service_applications(exhibitor_id, status);

create index if not exists idx_service_orders_exhibitor_status
    on __schema__.service_orders(exhibitor_id, status);

create index if not exists idx_service_payments_order_status
    on __schema__.service_payments(service_order_id, status);

create index if not exists idx_app_values_field_text
    on __schema__.service_application_values(field_code, value_text);
```

- [ ] **Step 8: Create seed SQL**

Create role/user/backfill scripts:

- `V6_005__seed_roles.sql` inserts deterministic roles with `on conflict (code) do nothing`.
- `V6_006__seed_users.sql` inserts `admin`, `organizer`, `sales01`, `finance01`, `service01`, `exhibitor01`.
- `V6_006__seed_users.sql` must not overwrite `password_hash` on conflict.
- `V6_007__backfill_demo_data.sql` assigns `sales01` as `PRIMARY` for demo exhibitor.

Use a BCrypt hash generated during implementation. Use the same demo password for all demo accounts and document it in README only if the user approves before release.

- [ ] **Step 9: Create rollback and verify SQL**

Create `R6_rollback.sql` to drop V6 tables in dependency order. Create `V6_verify.sql` with the verification SQL from the spec.

- [ ] **Step 10: Run migration tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=FoundationApplicationTest test
```

Expected: PASS.

- [ ] **Step 11: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java backend/src/main/resources/db/migrations/v6 backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java
git commit -m "feat: add v6 schema migrations"
```

---

## Task 2: Auth Domain, JWT, And Login Hardening

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/users/*`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/auth/*`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/auth/AuthServiceTest.java`

- [ ] **Step 1: Add Spring Security and JWT dependencies**

In `backend/pom.xml`, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: Add auth config**

In `application.yml`, add:

```yaml
app:
  auth:
    jwt-secret: ${JWT_SECRET:dev-only-please-replace-with-32-byte-secret}
    jwt-expiration-seconds: ${JWT_EXPIRATION_SECONDS:28800}
    jwt-issuer: ${JWT_ISSUER:sport-expo-exhibitor-system}
    login-max-failures: ${LOGIN_MAX_FAILURES:5}
    login-lock-minutes: ${LOGIN_LOCK_MINUTES:15}
```

Implementation note: allow the dev default locally, but production profile or Render deployment must set `JWT_SECRET`.

- [ ] **Step 3: Extend `ApiResponse` with error code**

Change `ApiResponse` to:

```java
public record ApiResponse<T>(boolean success, T data, String message, String errorCode) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK", null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
}
```

- [ ] **Step 4: Create user and role entities**

Create:

- `User.java`
- `Role.java`
- `UserRole.java` if using explicit entity, or `@ManyToMany` if mapping remains simple
- `UserRepository.java`
- `RoleRepository.java`

Required repository methods:

```java
Optional<User> findByUsername(String username);
List<User> findByStatus(String status);
Optional<Role> findByCode(String code);
```

- [ ] **Step 5: Write failing auth tests**

Create `AuthServiceTest` with tests:

Required test methods:

- `loginReturnsTokenAndRolesForValidPassword`
- `badPasswordIncrementsFailureCount`
- `locksUserAfterFiveFailures`
- `disabledUserCannotLogin`

Expected initial run: FAIL because `AuthService` does not exist.

- [ ] **Step 6: Implement JWT service**

Create `JwtService`:

Required public methods:

```java
public String issueToken(User user, List<String> roles);
public JwtPrincipal parse(String token);
```

Claims:

- `sub` = user ID
- `username`
- `roles`
- `exhibitorId`
- `jti`
- `iat`
- `exp`
- issuer from config

- [ ] **Step 7: Implement `AuthService`**

Login behavior:

- Look up user by username.
- For missing user or wrong password, return `E_AUTH_BAD_CREDENTIALS`.
- For locked user, return `E_AUTH_ACCOUNT_LOCKED`.
- For disabled user, return `E_AUTH_USER_DISABLED`.
- On failed password, increment `failedLoginCount`; set `lockedUntil` when count reaches configured max.
- On success, reset failure count, clear lock, update `lastLoginAt`.
- Return `LoginResponse`.

- [ ] **Step 8: Create auth controller**

Create `AuthController`:

Required controller methods:

```java
@PostMapping("/api/auth/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request);

@GetMapping("/api/auth/me")
public ApiResponse<CurrentUserResponse> me(Authentication authentication);

@PostMapping("/api/auth/logout")
public ApiResponse<Void> logout();
```

Login response headers:

- `X-Token-Type: Bearer`
- `X-Expires-In: 28800`

- [ ] **Step 9: Run auth tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=AuthServiceTest test
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java backend/src/main/java/com/sportexpo/exhibitor/auth backend/src/main/java/com/sportexpo/exhibitor/users backend/src/test/java/com/sportexpo/exhibitor/auth
git commit -m "feat: add jwt login and user roles"
```

---

## Task 3: Spring Security, Current User, And Data Scope

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/security/*`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/common/GlobalExceptionHandler.java`
- Modify: Existing controllers/services using `DemoActors`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java`

- [ ] **Step 1: Write failing security tests**

Create tests:

Required test methods:

- `businessApiWithoutTokenReturns401`
- `tamperedTokenReturns401`
- `expiredTokenReturns401`
- `roleMismatchReturns403`

Use `MockMvc`.

- [ ] **Step 2: Add security filter chain**

Create `SecurityConfig`:

- Disable CSRF for Bearer token API.
- Permit `/api/auth/login`, `/api/health`.
- Require authentication for `/api/**`.
- Enable method security with `@EnableMethodSecurity`.

- [ ] **Step 3: Add JWT filter**

Create `JwtAuthenticationFilter` that:

- Reads `Authorization`.
- Validates `Bearer`.
- Parses token through `JwtService`.
- Loads current user.
- Rejects disabled users.
- Populates `Authentication`.

- [ ] **Step 4: Add current user model**

Create:

- `CurrentUser.java`
- `CurrentUserService.java`

`CurrentUser` exposes:

- `UUID userId`
- `String username`
- `Set<String> roles`
- `UUID exhibitorId`
- `boolean hasRole(String role)`

- [ ] **Step 5: Add `DataScopeService`**

Required methods:

```java
boolean canAccessExhibitor(Authentication authentication, UUID exhibitorId);
void requireCanAccessExhibitor(Authentication authentication, UUID exhibitorId);
Set<UUID> accessibleExhibitorIds(Authentication authentication);
```

Behavior:

- Admin roles return unrestricted marker or all IDs only when needed.
- Exhibitor returns its own `exhibitorId`.
- Sales returns assigned exhibitor IDs.
- Finance and service staff are allowed for their specific service methods, not broad customer ownership.

- [ ] **Step 6: Replace demo actor reads in write paths**

Search:

```powershell
rg -n "DemoActors|DEMO_EXHIBITOR_ID|DEMO_ADMIN_ID" backend/src/main/java frontend/src
```

Backend services/controllers must use current user:

- `RegistrationController`
- `ParticipationOrderController`
- `PaymentController`
- `BoothSelectionController`
- `ContractController`
- `ExhibitorController`
- `EventCategoryController`

Keep `DemoActors` only for seed constants or tests.

- [ ] **Step 7: Run security tests and existing backend tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java backend/src/test/java
git commit -m "feat: enforce authenticated api access"
```

---

## Task 4: Sales Assignments And Follow-Ups

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/sales/*`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/exhibitor/*`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/sales/SalesAssignmentServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/sales/SalesFollowUpServiceTest.java`

- [ ] **Step 1: Write failing sales assignment tests**

Tests:

Required test methods:

- `adminAssignsPrimaryAndCollaborators`
- `cannotAssignTwoPrimarySales`
- `cacheEvictsOldAndNewSalesAfterAssignmentChange`
- `salesCanOnlyAccessAssignedExhibitor`

- [ ] **Step 2: Implement sales assignment entity and repository**

Create:

- `ExhibitorSalesAssignment.java`
- `ExhibitorSalesAssignmentRepository.java`
- `SalesAssignmentRequest.java`
- `SalesAssignmentResponse.java`

Repository methods:

```java
List<ExhibitorSalesAssignment> findByExhibitorId(UUID exhibitorId);
List<ExhibitorSalesAssignment> findBySalesUserId(UUID salesUserId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<ExhibitorSalesAssignment> findLockedByExhibitorId(UUID exhibitorId);
```

- [ ] **Step 3: Implement `SalesAssignmentService`**

`updateAssignments(exhibitorId, request, actorId)` must:

- Lock current assignments for the exhibitor.
- Validate primary and collaborators are `SALES` users.
- Replace assignment set.
- Catch unique conflict and throw `E_SALES_ASSIGNMENT_CONFLICT`.
- Evict `sales:accessible_exhibitor_ids:{id}` for old and new sales IDs.
- Write audit log.

- [ ] **Step 4: Create controller**

Endpoints:

```http
GET /api/exhibitors/{id}/sales-assignments
PUT /api/exhibitors/{id}/sales-assignments
```

Permissions:

- `SYSTEM_ADMIN`, `ORGANIZER_ADMIN` can update.
- `SALES` can read assignments for accessible exhibitors.

- [ ] **Step 5: Implement follow-up backend**

Create:

- `SalesFollowUp.java`
- `SalesFollowUpRepository.java`
- `SalesFollowUpService.java`
- `SalesFollowUpController.java`

Endpoints:

```http
GET /api/sales-follow-ups?exhibitorId=&status=
POST /api/sales-follow-ups
PUT /api/sales-follow-ups/{id}
```

Rules:

- Sales can create only for accessible exhibitor.
- Sales can edit only own follow-ups.
- Admin can list all.
- Exhibitor cannot access follow-ups.

- [ ] **Step 6: Run sales tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=SalesAssignmentServiceTest,SalesFollowUpServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/sales backend/src/test/java/com/sportexpo/exhibitor/sales
git commit -m "feat: add sales assignments and follow ups"
```

---

## Task 5: Service Type And Field Configuration Backend

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/type/*`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/field/*`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/servicecenter/ServiceTypeServiceTest.java`

- [ ] **Step 1: Write failing service type tests**

Tests:

Required test methods:

- `adminCreatesServiceTypeWithOpenConditions`
- `singleSelectFieldRequiresOptionsBeforeEnabled`
- `duplicateFieldCodeInSameServiceTypeFails`
- `usedFieldCannotBeDeletedAndCanBeDisabled`

- [ ] **Step 2: Implement entities**

Create:

- `ServiceType`
- `ServiceField`

Enums:

- `ServiceCategory`
- `ServiceTargetScope`
- `ServiceOpenCondition`
- `ServiceChargeMode`
- `ServiceFieldType`

Store `openConditions` as JSON text or `varchar` array; choose JSON text if the project has no array converter.

- [ ] **Step 3: Implement service type APIs**

Endpoints:

```http
GET /api/service-types?eventId=&enabled=
POST /api/service-types
PUT /api/service-types/{id}
GET /api/service-types/{id}/fields
POST /api/service-types/{id}/fields
PUT /api/service-fields/{id}
```

Permissions:

- `SYSTEM_ADMIN`, `ORGANIZER_ADMIN`, `SERVICE_STAFF` can manage.
- `EXHIBITOR` can list enabled service types only through service center use cases.

- [ ] **Step 4: Add seed service types**

In V6 or a separate idempotent seed script, create demo service types:

- 标准展位楣板信息: `FASCIA`, `BOOTH`, `FREE`
- 展商证件申请: `BADGE`, `ORDER`, `FREE`
- 家具租赁: `FURNITURE`, `BOOTH`, `FIXED_PRICE`
- 会刊广告: `ADVERTISING`, `EXHIBITOR`, `MANUAL_QUOTE`

- [ ] **Step 5: Run tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=ServiceTypeServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/servicecenter backend/src/test/java/com/sportexpo/exhibitor/servicecenter backend/src/main/resources/db/migrations/v6
git commit -m "feat: add service type configuration"
```

---

## Task 6: Service Application Backend

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/application/*`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationTypes.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/servicecenter/ServiceApplicationServiceTest.java`

- [ ] **Step 1: Write failing service application tests**

Tests:

Required test methods:

- `exhibitorCannotSubmitWhenOpenConditionUnmet`
- `orderScopedServiceRequiresOrder`
- `boothScopedServiceRequiresConfirmedBooths`
- `submittedApplicationCanBeApprovedRejectedOrNeedSupplement`
- `rejectedApplicationCanBeCopiedToDraft`

- [ ] **Step 2: Implement entities**

Create:

- `ServiceApplication`
- `ServiceApplicationBooth`
- `ServiceApplicationValue`
- `ServiceApplicationAttachment`

Status enum:

```java
public final class ServiceApplicationStatuses {
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String NEED_SUPPLEMENT = "NEED_SUPPLEMENT";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
}
```

- [ ] **Step 3: Implement open condition evaluator**

Create `ServiceOpenConditionEvaluator`:

- `REGISTRATION_APPROVED`: approved registration exists.
- `ORDER_CREATED`: order exists for exhibitor/event.
- `CONTRACT_EFFECTIVE`: effective contract exists for target order.
- `PAYMENT_CONFIRMED`: target participation order is paid.
- `BOOTH_CONFIRMED`: confirmed booth selection exists for target order/booth.

- [ ] **Step 4: Implement field value validator**

Validate:

- Required values.
- Single/multi select options.
- Phone and email regex.
- Number min/max/scale.
- File/image metadata shape.

Throw `E_SERVICE_FIELD_INVALID`.

- [ ] **Step 5: Implement application service and controller**

Endpoints:

```http
GET /api/service-applications?eventId=&exhibitorId=&status=&serviceTypeId=
POST /api/service-applications
GET /api/service-applications/{id}
PUT /api/service-applications/{id}
POST /api/service-applications/{id}/submit
POST /api/service-applications/{id}/start-review
POST /api/service-applications/{id}/approve
POST /api/service-applications/{id}/reject
POST /api/service-applications/{id}/request-supplement
POST /api/service-applications/{id}/complete
POST /api/service-applications/{id}/copy-from-rejected
```

- [ ] **Step 6: Add notifications and audit**

Create notifications:

- `SERVICE_APPLICATION_SUBMITTED`
- `SERVICE_APPLICATION_APPROVED`
- `SERVICE_APPLICATION_REJECTED`
- `SERVICE_APPLICATION_NEED_SUPPLEMENT`

Audit all status transitions and value changes.

- [ ] **Step 7: Run service application tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=ServiceApplicationServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/servicecenter backend/src/test/java/com/sportexpo/exhibitor/servicecenter backend/src/main/java/com/sportexpo/exhibitor/notification
git commit -m "feat: add service application workflow"
```

---

## Task 7: Service Orders And Service Payments Backend

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/order/*`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/servicecenter/payment/*`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/servicecenter/ServiceOrderPaymentTest.java`

- [ ] **Step 1: Write failing service order tests**

Tests:

Required test methods:

- `fixedPriceApprovalCreatesServiceOrder`
- `manualQuoteCreatesItemsAndQuoteExpiry`
- `expiredQuoteCannotAcceptPayment`
- `serviceStaffCanExtendUnexpiredQuote`
- `confirmingPaymentsAggregatesToPaid`
- `concurrentServicePaymentConfirmationsAggregateCorrectly`

- [ ] **Step 2: Implement entities**

Create:

- `ServiceOrder`
- `ServiceOrderItem`
- `ServicePayment`

Statuses:

- Service order: `PENDING_PAYMENT`, `PARTIALLY_PAID`, `PAID`, `IN_PROGRESS`, `COMPLETED`, `REFUNDING`, `REFUNDED`, `VOIDED`
- Service payment: `PENDING_CONFIRM`, `CONFIRMED`, `FAILED`, `VOIDED`

- [ ] **Step 3: Implement quote generation**

`ServiceOrderService.quote(applicationId, request, actor)`:

- Requires service application `APPROVED`.
- Requires `MANUAL_QUOTE`.
- Requires item list not empty.
- Sets `quoteExpiresAt` between now + 1 day and now + 30 days.
- Creates order and items.
- Sends `SERVICE_ORDER_CREATED`.

- [ ] **Step 4: Implement fixed price order creation**

When a fixed price service application is approved:

- Create service order with one item.
- Set total amount = fixed price.
- Set status `PENDING_PAYMENT`.
- Set quote expiry if needed for payment deadline; use service deadline or null for fixed price.

- [ ] **Step 5: Implement service payments**

`ServicePaymentService.confirm(paymentId, actor)` must:

- Begin transaction.
- Lock service order row with `FOR UPDATE`.
- Lock payment row.
- Ensure payment is `PENDING_CONFIRM`.
- Mark payment `CONFIRMED`.
- Sum confirmed payments.
- Set service order `PARTIALLY_PAID` or `PAID`.
- If paid, set application `IN_PROGRESS`.
- Write audit and notification.

- [ ] **Step 6: Implement fail payment**

Failure requires reason max 500 characters, sets `FAILED`, writes `SERVICE_PAYMENT_FAILED`.

- [ ] **Step 7: Add controllers**

Endpoints:

```http
GET /api/service-orders
GET /api/service-orders/{id}
POST /api/service-orders/{id}/quote
POST /api/service-orders/{id}/extend-quote
POST /api/service-orders/{id}/void
GET /api/service-payments
POST /api/service-payments
POST /api/service-payments/{id}/confirm
POST /api/service-payments/{id}/fail
```

- [ ] **Step 8: Run tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=ServiceOrderPaymentTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/servicecenter backend/src/test/java/com/sportexpo/exhibitor/servicecenter
git commit -m "feat: add service orders and payments"
```

---

## Task 8: Frontend Auth, Request Layer, And Role Routing

**Files:**
- Modify: `frontend/src/app/request.ts`
- Modify: `frontend/src/pages/LoginPage.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Create: `frontend/src/app/auth.ts`
- Create: `frontend/src/app/AuthProvider.tsx`
- Create: `frontend/src/app/ProtectedRoute.tsx`
- Create: `frontend/src/app/roleRouting.ts`
- Test: `frontend/src/app/auth.test.ts`
- Test: `frontend/src/app/roleRouting.test.ts`

- [ ] **Step 1: Write failing frontend auth tests**

Test cases:

```ts
it('stores access token in localStorage', () => {});
it('adds Authorization header to api requests', () => {});
it('maps exhibitor role to /exhibitor', () => {});
it('maps sales role to /admin/dashboard', () => {});
it('keeps return path after 401', () => {});
```

- [ ] **Step 2: Implement auth storage**

Create `auth.ts`:

```ts
const TOKEN_KEY = 'access_token';

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_KEY);
}
```

- [ ] **Step 3: Update request layer**

`apiGet`, `apiPost`, `apiPut` must:

- Add `Authorization: Bearer <token>` when token exists.
- On 401, clear token and redirect to `/login?returnTo=<current path>`.
- On 403, show “无权限访问”.
- Parse `errorCode`.

- [ ] **Step 4: Implement current user provider**

`AuthProvider` loads `/api/auth/me`, exposes:

- `user`
- `roles`
- `activeRole`
- `switchRole(role)`
- `login`
- `logout`

- [ ] **Step 5: Implement role routing**

`getDefaultPath(user)`:

- `EXHIBITOR` only -> `/exhibitor`
- internal roles -> `/admin/dashboard`
- multi-role with default role -> role-specific path

- [ ] **Step 6: Update login page**

Login page:

- Uses `/api/auth/login`.
- Stores token.
- Redirects to `returnTo` if present and allowed, otherwise default role path.
- Shows backend error message.

- [ ] **Step 7: Wrap routes**

Use `ProtectedRoute` for `/admin/*` and `/exhibitor/*`.

- [ ] **Step 8: Run tests and build**

Run:

```powershell
npm test -- auth roleRouting
npm run build
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add frontend/src/app frontend/src/pages/LoginPage.tsx
git commit -m "feat: add frontend authentication"
```

---

## Task 9: Frontend Admin Users, Sales, And Role Menus

**Files:**
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Create: `frontend/src/pages/admin/users/UserListPage.tsx`
- Create: `frontend/src/pages/admin/roles/RoleListPage.tsx`
- Create: `frontend/src/pages/admin/sales/SalesAssignmentPage.tsx`
- Create: `frontend/src/pages/admin/sales/SalesFollowUpPage.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Test: `frontend/src/pages/admin/pageLogic.test.ts`

- [ ] **Step 1: Add role-filtered menu model tests**

Tests:

```ts
it('shows user management only for admins', () => {});
it('shows sales follow ups for sales and admins', () => {});
it('shows service payment menu for finance', () => {});
```

- [ ] **Step 2: Update admin layout menus**

Menu must include:

- 用户管理
- 角色管理
- 销售跟进
- 服务类型配置
- 服务申请审核
- 服务订单管理
- 服务付款确认

Filter by current user roles.

- [ ] **Step 3: Implement user list page**

Use `ProTable`:

- username
- displayName
- roles
- status
- bound exhibitor
- actions: create, disable, enable, reset password

- [ ] **Step 4: Implement role list page**

Read-only list of roles for this round.

- [ ] **Step 5: Implement sales assignment page**

For an exhibitor:

- Display current primary sales.
- Display collaborator sales.
- Use `Select` loaded from `/api/users?role=SALES`.
- Save through `PUT /api/exhibitors/{id}/sales-assignments`.

- [ ] **Step 6: Implement follow-up page**

Use `ProTable + ModalForm`:

- exhibitor selector
- method
- content
- nextFollowUpAt
- status

- [ ] **Step 7: Run frontend tests and build**

Run:

```powershell
npm test -- pageLogic
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/app/layouts/AdminLayout.tsx frontend/src/app/routes.tsx frontend/src/pages/admin
git commit -m "feat: add admin user and sales pages"
```

---

## Task 10: Frontend Service Center And Configurable Forms

**Files:**
- Create: `frontend/src/pages/exhibitor/services/*`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Test: `frontend/src/pages/exhibitor/services/serviceViewModels.test.ts`

- [ ] **Step 1: Write service view model tests**

Tests:

```ts
it('groups service types by category', () => {});
it('shows unmet condition reason', () => {});
it('maps field types to antd components', () => {});
it('keeps draft values when request fails', () => {});
```

- [ ] **Step 2: Add exhibitor service routes**

Routes:

```text
/exhibitor/services
/exhibitor/services/:serviceTypeId/apply
/exhibitor/service-applications
/exhibitor/service-applications/:id
/exhibitor/service-orders
/exhibitor/service-orders/:id/payment
```

- [ ] **Step 3: Add bottom nav service entry**

Exhibitor bottom nav becomes:

- 首页
- 报名
- 订单
- 服务
- 通知

- [ ] **Step 4: Implement `ServiceCenterPage`**

Mobile-first list grouped by category:

- service name
- charge mode
- target scope
- deadline
- available/unavailable state
- unavailable reason

- [ ] **Step 5: Implement dynamic form renderer**

`serviceForm.tsx` maps:

- `TEXT` -> `Input`
- `TEXTAREA` -> `Input.TextArea`
- `NUMBER` -> `InputNumber`
- `DATE` -> `DatePicker`
- `SINGLE_SELECT` -> `Select`
- `MULTI_SELECT` -> `Select mode="multiple"`
- `FILE` -> `Upload`
- `IMAGE` -> `Upload accept="image/*"`
- `PHONE` -> `Input`
- `EMAIL` -> `Input`

- [ ] **Step 6: Implement service apply page**

Behavior:

- Load service type and fields.
- Load target orders when scope is `ORDER`.
- Load confirmed booths when scope is `BOOTH`.
- Save draft through `POST /api/service-applications`.
- Submit through `/submit`.
- Preserve form state when network request fails.

- [ ] **Step 7: Implement service application detail**

Show:

- status
- values
- attachments
- review reason
- supplement reason
- copy rejected application button
- payment entry when service order exists

- [ ] **Step 8: Implement service order payment page**

Show:

- service name
- total amount
- confirmed amount
- quote expiry and remaining days
- offline payment form

- [ ] **Step 9: Run tests and build**

Run:

```powershell
npm test -- serviceViewModels
npm run build
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add frontend/src/pages/exhibitor/services frontend/src/app/layouts/ExhibitorLayout.tsx frontend/src/app/routes.tsx
git commit -m "feat: add exhibitor service center"
```

---

## Task 11: Frontend Admin Service Management

**Files:**
- Create: `frontend/src/pages/admin/service-types/ServiceTypeListPage.tsx`
- Create: `frontend/src/pages/admin/service-applications/ServiceApplicationListPage.tsx`
- Create: `frontend/src/pages/admin/service-applications/ServiceApplicationDetailPage.tsx`
- Create: `frontend/src/pages/admin/service-orders/ServiceOrderListPage.tsx`
- Create: `frontend/src/pages/admin/service-payments/ServicePaymentConfirmPage.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Test: `frontend/src/pages/admin/pageLogic.test.ts`

- [ ] **Step 1: Add routes and lazy imports**

Add:

```text
/admin/service-types
/admin/service-applications
/admin/service-applications/:id
/admin/service-orders
/admin/service-payments
```

- [ ] **Step 2: Implement service type config page**

Use `ProTable + ModalForm`:

- service type fields
- open conditions multi-select
- charge mode
- fixed price
- field configuration drawer

- [ ] **Step 3: Implement service application review**

List:

- event
- exhibitor
- service type
- target order/booths
- status
- submittedAt

Detail:

- values rendered read-only
- attachments
- approve
- reject with required reason
- request supplement with required reason
- complete

- [ ] **Step 4: Implement quote UI**

For manual quote:

- Add multiple item rows.
- Quantity, unit price, item name.
- Quote expiry date.
- Submit to `/api/service-orders/{id}/quote`.

- [ ] **Step 5: Implement service payment confirmation**

Use `ProTable`:

- service order
- exhibitor
- amount
- method
- status
- submittedAt
- confirm
- fail with reason

- [ ] **Step 6: Run build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/pages/admin/service-types frontend/src/pages/admin/service-applications frontend/src/pages/admin/service-orders frontend/src/pages/admin/service-payments frontend/src/app/routes.tsx
git commit -m "feat: add admin service management"
```

---

## Task 12: Cross-Cutting Verification And Browser Smoke

**Files:**
- Modify: `README.md`
- Modify: docs if implementation decisions differ from spec

- [ ] **Step 1: Run full backend tests**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: PASS.

- [ ] **Step 2: Run frontend tests**

Run:

```powershell
npm test -- auth roleRouting serviceViewModels mobileViewModels pageLogic dashboardLogic display
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS. Existing Ant Design ProComponents chunk warnings are acceptable if no runtime errors appear.

- [ ] **Step 4: Start local backend and frontend**

Backend:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys'
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm run dev
```

- [ ] **Step 5: Browser smoke for auth and roles**

Verify:

- `admin` logs into admin dashboard.
- `sales01` sees sales/customer related pages only.
- `finance01` sees payment related pages.
- `service01` sees service pages.
- `exhibitor01` enters `/exhibitor`.
- Anonymous request to an admin page redirects to login.

- [ ] **Step 6: Browser smoke for service center**

Verify mobile width:

- `/exhibitor/services` shows service categories.
- Unavailable services show reason.
- Exhibitor submits a free service application.
- Service staff approves it.
- Exhibitor submits a fixed-price service application.
- Service order appears.
- Exhibitor submits offline service payment.
- Finance confirms payment.
- Service order becomes paid.

- [ ] **Step 7: Update README smoke flow**

Add sixth-round credentials and smoke flow.

- [ ] **Step 8: Final git status**

Run:

```powershell
git status --short
```

Expected: only intentional files are modified; local temp files remain untracked unless user asks to remove them.

- [ ] **Step 9: Commit final docs**

```powershell
git add README.md docs
git commit -m "docs: add auth service center smoke flow"
```

---

## Self-Review Checklist

- Every spec requirement maps to a task:
  - Auth and JWT: Tasks 2, 3, 8.
  - Sales assignment and follow-up: Tasks 4, 9.
  - Service type and dynamic fields: Tasks 5, 10, 11.
  - Service applications: Tasks 6, 10, 11.
  - Service orders/payments: Tasks 7, 10, 11.
  - Migration scripts and compatibility: Task 1.
  - Browser verification: Task 12.
- No task depends on unlisted files.
- Every task includes a test command.
- Every task includes a commit point.
- Demo identity replacement is explicitly covered in Task 3 and Task 8.

## Execution Options

1. **Subagent-Driven (recommended)** - Dispatch one subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session with checkpoints.
