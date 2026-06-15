# Exhibitor Profile Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add exhibitor business profiles, event category configuration, registration submission/review, and registration-gated order creation without introducing real login.

**Architecture:** Add focused Spring Boot modules for `exhibitor`, `eventcategory`, and `registration`, then wire `ParticipationOrderService` to approved registrations. Extend notifications with related object fields so registration notices can deep-link. Frontend adds exhibitor mobile registration/profile pages and admin exhibitor/registration pages while still using `DEMO_EXHIBITOR_ID` as the simulated current user.

**Tech Stack:** Java Spring Boot, Spring Data JPA, PostgreSQL schema initializer, React, TypeScript, Ant Design, ProComponents, Vitest, JUnit 5.

---

## File Structure

Backend create:

```text
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/Exhibitor.java
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/ExhibitorRepository.java
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/ExhibitorService.java
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/ExhibitorController.java
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/dto/ExhibitorRequest.java
backend/src/main/java/com/sportexpo/exhibitor/exhibitor/dto/ExhibitorResponse.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/EventCategory.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/EventCategoryRepository.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/EventCategoryService.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/EventCategoryController.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/dto/EventCategoryRequest.java
backend/src/main/java/com/sportexpo/exhibitor/eventcategory/dto/EventCategoryResponse.java
backend/src/main/java/com/sportexpo/exhibitor/registration/Registration.java
backend/src/main/java/com/sportexpo/exhibitor/registration/RegistrationRepository.java
backend/src/main/java/com/sportexpo/exhibitor/registration/RegistrationService.java
backend/src/main/java/com/sportexpo/exhibitor/registration/RegistrationController.java
backend/src/main/java/com/sportexpo/exhibitor/registration/dto/RegistrationRequest.java
backend/src/main/java/com/sportexpo/exhibitor/registration/dto/RegistrationResponse.java
backend/src/main/java/com/sportexpo/exhibitor/registration/dto/RejectRegistrationRequest.java
```

Backend modify:

```text
backend/src/main/resources/db/schema-foundation.sql
backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java
backend/src/main/java/com/sportexpo/exhibitor/notification/Notification.java
backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationService.java
backend/src/main/java/com/sportexpo/exhibitor/notification/dto/NotificationResponse.java
backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationTypes.java
backend/src/main/java/com/sportexpo/exhibitor/order/dto/CreateParticipationOrderRequest.java
backend/src/main/java/com/sportexpo/exhibitor/order/dto/ParticipationOrderResponse.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java
backend/src/main/java/com/sportexpo/exhibitor/admin/dto/AdminDashboardResponse.java
backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java
```

Frontend create:

```text
frontend/src/pages/exhibitor/registrations/RegistrationListPage.tsx
frontend/src/pages/exhibitor/registrations/RegistrationCreatePage.tsx
frontend/src/pages/exhibitor/registrations/RegistrationDetailPage.tsx
frontend/src/pages/exhibitor/profile/ExhibitorProfilePage.tsx
frontend/src/pages/admin/exhibitors/AdminExhibitorListPage.tsx
frontend/src/pages/admin/registrations/AdminRegistrationListPage.tsx
frontend/src/pages/admin/registrations/AdminRegistrationDetailPage.tsx
frontend/src/pages/exhibitor/registrationViewModels.ts
frontend/src/pages/exhibitor/registrationViewModels.test.ts
```

Frontend modify:

```text
frontend/src/app/domain.ts
frontend/src/app/display.ts
frontend/src/app/display.test.ts
frontend/src/app/layouts/ExhibitorLayout.tsx
frontend/src/app/layouts/ExhibitorLayout.test.ts
frontend/src/app/layouts/AdminLayout.tsx
frontend/src/app/routes.tsx
frontend/src/pages/exhibitor/HomePage.tsx
frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx
frontend/src/pages/exhibitor/notifications/NotificationCenterPage.tsx
frontend/src/pages/admin/DashboardPage.tsx
frontend/src/pages/admin/dashboard/dashboardLogic.ts
frontend/src/pages/admin/dashboard/dashboardLogic.test.ts
```

---

## Task 1: Schema and Backend Domain Foundation

**Files:**
- Modify: `backend/src/main/resources/db/schema-foundation.sql`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java`
- Create backend domain/repository/controller/service/dto files listed under `exhibitor`, `eventcategory`, and `registration`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/exhibitor/ExhibitorServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/eventcategory/EventCategoryServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/registration/RegistrationServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create tests with these exact behavior names:

```java
@Test
void createsAndUpdatesExhibitorProfile() {}

@Test
void listsEnabledEventCategoriesByEvent() {}

@Test
void rejectsRegistrationCategoryOutsideEventConfiguration() {}

@Test
void preventsSecondDraftOrSubmittedRegistrationForSameExhibitorAndEvent() {}

@Test
void rejectedRegistrationDoesNotBlockNewRegistrationForSameExhibitorAndEvent() {}
```

Expected assertions:

```java
assertThat(exhibitor.companyName()).isEqualTo("演示展商");
assertThat(category.name()).isEqualTo("体育用品");
assertThatThrownBy(() -> registrationService.create(requestWithBadCategory(), DEMO_EXHIBITOR_ID))
    .isInstanceOf(BusinessException.class)
    .hasMessage("Registration category is not enabled for this event");
assertThatThrownBy(() -> registrationService.create(secondDraftForSameEvent(), DEMO_EXHIBITOR_ID))
    .isInstanceOf(BusinessException.class)
    .hasMessage("Active registration already exists for this exhibitor and event");
assertThat(registrationService.create(newDraftAfterRejected(), DEMO_EXHIBITOR_ID).status()).isEqualTo("DRAFT");
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test -Dtest=ExhibitorServiceTest,EventCategoryServiceTest,RegistrationServiceTest
```

Expected: compilation fails because `ExhibitorService`, `EventCategoryService`, and `RegistrationService` do not exist.

- [ ] **Step 3: Extend schema baseline**

Add tables before `participation_orders`:

```sql
create table if not exists __schema__.exhibitors (
    id uuid primary key,
    company_name varchar(200) not null,
    credit_code varchar(80),
    contact_name varchar(100),
    contact_phone varchar(40),
    contact_email varchar(120),
    address varchar(300),
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create unique index if not exists ux_exhibitors_credit_code
    on __schema__.exhibitors(credit_code)
    where credit_code is not null;

create table if not exists __schema__.event_categories (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    code varchar(80) not null,
    name varchar(120) not null,
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique(event_id, code)
);

create table if not exists __schema__.registrations (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    status varchar(40) not null,
    category varchar(120),
    zone_id uuid references __schema__.zones(id),
    intent_area_sqm numeric(14,2),
    contact_name varchar(100),
    contact_phone varchar(40),
    remark varchar(500),
    review_comment varchar(500),
    reviewed_by uuid,
    reviewed_at timestamptz,
    submitted_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create unique index if not exists ux_registrations_in_progress_event_exhibitor
    on __schema__.registrations(event_id, exhibitor_id)
    where status in ('DRAFT', 'SUBMITTED');

create index if not exists idx_registrations_event_exhibitor_status
    on __schema__.registrations(event_id, exhibitor_id, status);

create index if not exists idx_registrations_status_submitted
    on __schema__.registrations(status, submitted_at desc);
```

Add FK when safe:

```sql
alter table __schema__.participation_orders
    add constraint fk_participation_orders_registration
    foreign key (registration_id) references __schema__.registrations(id);
```

If existing local rows have `registration_id is null`, keep the column nullable but enforce non-null in service for new orders.

Add demo seed logic after core event tables exist:

```sql
insert into __schema__.exhibitors(id, company_name, status, created_at, updated_at, created_by, updated_by)
values ('00000000-0000-0000-0000-000000000001', '演示展商', 'ACTIVE', now(), now(),
        '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002')
on conflict (id) do update set company_name = excluded.company_name, status = excluded.status, updated_at = now();

with demo_event as (
    select id
    from __schema__.events
    where year = 2026 and status <> 'ARCHIVED'
    order by created_at asc
    limit 1
)
insert into __schema__.event_categories(id, event_id, code, name, enabled, sort_order, created_at, updated_at)
select gen_random_uuid(), id, 'SPORTS_EQUIPMENT', '体育用品', true, 10, now(), now()
from demo_event
on conflict (event_id, code) do update set name = excluded.name, enabled = true, updated_at = now();

with demo_event as (
    select id
    from __schema__.events
    where year = 2026 and status <> 'ARCHIVED'
    order by created_at asc
    limit 1
)
insert into __schema__.registrations(id, event_id, exhibitor_id, status, category, contact_name, contact_phone,
                                     submitted_at, reviewed_by, reviewed_at, created_at, updated_at, created_by, updated_by)
select gen_random_uuid(), id, '00000000-0000-0000-0000-000000000001', 'APPROVED', 'SPORTS_EQUIPMENT',
       '王老板', '13800000000', now(), '00000000-0000-0000-0000-000000000002', now(), now(), now(),
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'
from demo_event
where not exists (
    select 1 from __schema__.registrations r
    where r.event_id = demo_event.id
      and r.exhibitor_id = '00000000-0000-0000-0000-000000000001'
      and r.status = 'APPROVED'
);
```

If no `events` row exists yet, the `demo_event` CTE returns no rows and the initializer must skip demo category/registration creation instead of inserting a dangling `event_id`.

- [ ] **Step 4: Add status constants**

In `BusinessStatuses.java`, add:

```java
public static final String EXHIBITOR_ACTIVE = "ACTIVE";
public static final String EXHIBITOR_SUSPENDED = "SUSPENDED";
public static final String EXHIBITOR_ARCHIVED = "ARCHIVED";

public static final String REGISTRATION_DRAFT = "DRAFT";
public static final String REGISTRATION_SUBMITTED = "SUBMITTED";
public static final String REGISTRATION_APPROVED = "APPROVED";
public static final String REGISTRATION_REJECTED = "REJECTED";
public static final String REGISTRATION_CANCELLED = "CANCELLED";
```

- [ ] **Step 5: Implement domain modules**

Implement:

```java
record ExhibitorRequest(String companyName, String creditCode, String contactName,
                        String contactPhone, String contactEmail, String address, String status) {}

record EventCategoryRequest(UUID eventId, String code, String name, boolean enabled, int sortOrder) {}

record RegistrationRequest(UUID eventId, UUID exhibitorId, String category, UUID zoneId,
                           BigDecimal intentAreaSqm, String contactName, String contactPhone, String remark) {}
```

Service rules:

```java
// EventCategoryService
list(UUID eventId, Boolean enabled) sorts by sortOrder asc then name asc.

// RegistrationService.create
assert exhibitor exists and status ACTIVE.
assert event exists.
assert category exists for event and enabled.
assert no registration exists for same event/exhibitor with status DRAFT or SUBMITTED.
do not block existing REJECTED, APPROVED, or CANCELLED registrations.
save status DRAFT.
```

- [ ] **Step 6: Run tests and verify GREEN**

Run:

```powershell
mvn test -Dtest=ExhibitorServiceTest,EventCategoryServiceTest,RegistrationServiceTest
```

Expected: selected tests pass.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/common backend/src/main/java/com/sportexpo/exhibitor/exhibitor backend/src/main/java/com/sportexpo/exhibitor/eventcategory backend/src/main/java/com/sportexpo/exhibitor/registration backend/src/test/java/com/sportexpo/exhibitor/exhibitor backend/src/test/java/com/sportexpo/exhibitor/eventcategory backend/src/test/java/com/sportexpo/exhibitor/registration
git commit -m "feat: add exhibitor profiles and registration domain"
```

## Task 2: Registration Workflow, Notifications, and Dashboard

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/registration/RegistrationService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/notification/Notification.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationTypes.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/notification/dto/NotificationResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditService.java`
- Modify: `backend/src/main/resources/db/schema-foundation.sql`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/admin/dto/AdminDashboardResponse.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/registration/RegistrationServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/notification/NotificationServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/admin/AdminDashboardServiceTest.java`

- [ ] **Step 1: Write failing workflow tests**

Add tests:

```java
@Test
void submitApproveAndRejectRegistrationCreateNotifications() {}

@Test
void rejectedRegistrationCanBeEditedThenResubmittedAndReviewFieldsAreCleared() {}

@Test
void dashboardIncludesPendingRegistrationReviewCountAndTopItems() {}
```

Expected assertions:

```java
assertThat(submitted.status()).isEqualTo("SUBMITTED");
assertThat(approved.status()).isEqualTo("APPROVED");
assertThat(rejected.reviewComment()).isEqualTo("资质材料不完整");
assertThat(resubmitted.reviewComment()).isNull();
assertThat(resubmitted.reviewedBy()).isNull();
assertThat(resubmitted.reviewedAt()).isNull();
assertThat(notification.relatedType()).isEqualTo("Registration");
assertThat(notification.relatedId()).isEqualTo(registration.id());
assertThat(dashboard.pendingRegistrationReviewCount()).isGreaterThanOrEqualTo(1);
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
mvn test -Dtest=RegistrationServiceTest,NotificationServiceTest,AdminDashboardServiceTest
```

Expected: failures for missing notification related fields and dashboard registration fields.

- [ ] **Step 3: Extend notifications schema and DTO**

Add columns:

```sql
alter table __schema__.notifications add column if not exists related_type varchar(80);
alter table __schema__.notifications add column if not exists related_id uuid;
alter table __schema__.notifications add column if not exists related_order_id uuid;
create index if not exists idx_notifications_related
    on __schema__.notifications(related_type, related_id);
```

Add notification constants:

```java
public static final String REGISTRATION_SUBMITTED = "REGISTRATION_SUBMITTED";
public static final String REGISTRATION_APPROVED = "REGISTRATION_APPROVED";
public static final String REGISTRATION_REJECTED = "REGISTRATION_REJECTED";
```

Extend `NotificationService.create` with overload:

```java
NotificationResponse create(UUID eventId, String recipientType, UUID recipientId,
                            String title, String content, String type,
                            String relatedType, UUID relatedId, UUID relatedOrderId);
```

Existing callers can keep the old overload, which delegates with null related fields.

- [ ] **Step 4: Implement registration state transitions**

Rules:

```java
submit(id, actorId):
  allowed from DRAFT only
  set status SUBMITTED
  set submittedAt now
  clear reviewComment, reviewedBy, reviewedAt for defensive consistency
  notify ADMIN recipient DEMO_ADMIN_ID with REGISTRATION_SUBMITTED
  audit REGISTRATION_SUBMIT with target Registration and targetId id

resubmit(id, actorId):
  allowed from REJECTED only
  update editable fields must already have been saved by PUT /api/registrations/{id}
  set status SUBMITTED
  set submittedAt now
  set reviewComment null
  set reviewedBy null
  set reviewedAt null
  notify ADMIN recipient DEMO_ADMIN_ID with REGISTRATION_SUBMITTED
  audit REGISTRATION_RESUBMIT with target Registration and targetId id, including that previous review fields were cleared

approve(id, actorId):
  allowed from SUBMITTED
  set status APPROVED
  set reviewedBy actorId, reviewedAt now
  notify EXHIBITOR recipient registration.exhibitorId with REGISTRATION_APPROVED
  audit REGISTRATION_APPROVE with target Registration and targetId id

reject(id, actorId, reason):
  allowed from SUBMITTED
  reason nonblank and <= 500
  set status REJECTED
  set reviewComment reason, reviewedBy actorId, reviewedAt now
  notify EXHIBITOR recipient registration.exhibitorId with REGISTRATION_REJECTED
  audit REGISTRATION_REJECT with target Registration and targetId id and reason
```

`RegistrationController.postSubmit(id)` must route by current state: `DRAFT` calls `submit(id, DEMO_EXHIBITOR_ID)`; `REJECTED` calls `resubmit(id, DEMO_EXHIBITOR_ID)`; any other state returns `BusinessException("Registration cannot be submitted from current status")`.

- [ ] **Step 5: Extend admin dashboard**

Add to `AdminDashboardResponse`:

```java
long pendingRegistrationReviewCount;
List<RegistrationResponse> pendingRegistrationReviews;
```

`AdminDashboardService` must include `SUBMITTED` registrations, sorted by `submittedAt desc`, limited to 5.

- [ ] **Step 6: Run tests and verify GREEN**

Run:

```powershell
mvn test -Dtest=RegistrationServiceTest,NotificationServiceTest,AdminDashboardServiceTest
```

Expected: selected tests pass.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/registration backend/src/main/java/com/sportexpo/exhibitor/notification backend/src/main/java/com/sportexpo/exhibitor/admin backend/src/test/java/com/sportexpo/exhibitor/registration backend/src/test/java/com/sportexpo/exhibitor/notification backend/src/test/java/com/sportexpo/exhibitor/admin
git commit -m "feat: add registration review workflow"
```

## Task 3: Registration-Gated Order Creation

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/dto/CreateParticipationOrderRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/dto/ParticipationOrderResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrder.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java`

- [ ] **Step 1: Write failing order gating tests**

Add tests:

```java
@Test
void createOrderFailsWithoutApprovedRegistration() {}

@Test
void createOrderBindsApprovedRegistration() {}

@Test
void createOrderRejectsRegistrationFromDifferentEventOrExhibitor() {}

@Test
void createOrderRejectsZoneOutsideRegistrationScope() {}
```

Expected assertions:

```java
assertThatThrownBy(() -> orderService.create(requestWithoutRegistration()))
    .isInstanceOf(BusinessException.class)
    .hasMessage("Registration does not exist");
assertThat(created.registrationId()).isEqualTo(approvedRegistration.id());
assertThatThrownBy(() -> orderService.create(requestWithOtherEventRegistration()))
    .hasMessage("Registration does not belong to this event");
assertThatThrownBy(() -> orderService.create(requestWithOtherExhibitorRegistration()))
    .hasMessage("Registration does not belong to this exhibitor");
assertThatThrownBy(() -> orderService.create(requestWithZoneOutsideRegistration()))
    .hasMessage("Order zone is outside approved registration scope");
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
mvn test -Dtest=ParticipationOrderServiceTest
```

Expected: compilation or assertion failure because request/response do not expose `registrationId` and service saves null.

- [ ] **Step 3: Extend DTOs**

Change request:

```java
public record CreateParticipationOrderRequest(
        @NotNull UUID eventId,
        @NotNull UUID exhibitorId,
        UUID registrationId,
        UUID zoneId,
        @NotNull UUID areaTierId,
        @NotBlank String boothType
) {}
```

Change response to include `UUID registrationId` immediately after `UUID exhibitorId`.

- [ ] **Step 4: Implement service validation**

In `ParticipationOrderService.create` before area tier lookup:

```java
var exhibitor = exhibitorRepository.findById(request.exhibitorId())
    .filter(item -> BusinessStatuses.EXHIBITOR_ACTIVE.equals(item.getStatus()))
    .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));

var registration = resolveRegistration(request)
    .orElseThrow(() -> new BusinessException("Registration does not exist"));

if (!registration.getExhibitorId().equals(request.exhibitorId())) {
    throw new BusinessException("Registration does not belong to this exhibitor");
}
if (!registration.getEventId().equals(request.eventId())) {
    throw new BusinessException("Registration does not belong to this event");
}
if (!BusinessStatuses.REGISTRATION_APPROVED.equals(registration.getStatus())) {
    throw new BusinessException("Registration is not approved");
}
if (registration.getZoneId() != null && !Objects.equals(registration.getZoneId(), request.zoneId())) {
    throw new BusinessException("Order zone is outside approved registration scope");
}
```

`resolveRegistration` behavior:

```java
if request.registrationId != null: find by id
else: find approved registrations by eventId and exhibitorId
  if exactly one: use it
  if zero: empty
  if more than one: throw BusinessException("Multiple approved registrations exist; choose one")
```

Save `registration.getId()` into `ParticipationOrder`.

- [ ] **Step 5: Run tests and verify GREEN**

Run:

```powershell
mvn test -Dtest=ParticipationOrderServiceTest
```

Expected: selected tests pass.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/order backend/src/test/java/com/sportexpo/exhibitor/order
git commit -m "feat: require approved registration for orders"
```

## Task 4: Frontend Domain, Routes, and Exhibitor Registration UI

**Files:**
- Modify: `frontend/src/app/domain.ts`
- Modify: `frontend/src/app/display.ts`
- Modify: `frontend/src/app/display.test.ts`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.test.ts`
- Modify: `frontend/src/app/routes.tsx`
- Create: `frontend/src/pages/exhibitor/registrationViewModels.ts`
- Create: `frontend/src/pages/exhibitor/registrationViewModels.test.ts`
- Create: `frontend/src/pages/exhibitor/profile/ExhibitorProfilePage.tsx`
- Create: `frontend/src/pages/exhibitor/registrations/RegistrationListPage.tsx`
- Create: `frontend/src/pages/exhibitor/registrations/RegistrationCreatePage.tsx`
- Create: `frontend/src/pages/exhibitor/registrations/RegistrationDetailPage.tsx`
- Modify: `frontend/src/pages/exhibitor/HomePage.tsx`
- Modify: `frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx`

- [ ] **Step 1: Write failing view model tests**

Create `registrationViewModels.test.ts`:

```ts
it('labels registration statuses in Chinese', () => {
  expect(formatRegistrationStatus('DRAFT')).toBe('草稿');
  expect(formatRegistrationStatus('SUBMITTED')).toBe('待审核');
  expect(formatRegistrationStatus('APPROVED')).toBe('审核通过');
  expect(formatRegistrationStatus('REJECTED')).toBe('已驳回');
  expect(formatRegistrationStatus('CANCELLED')).toBe('已取消');
});

it('derives exhibitor home registration actions', () => {
  expect(getRegistrationHomeAction(undefined)).toMatchObject({ label: '提交报名', to: '/exhibitor/registrations/new' });
  expect(getRegistrationHomeAction({ id: 'r1', status: 'SUBMITTED' })).toMatchObject({ label: '查看报名进度', disabled: false });
  expect(getRegistrationHomeAction({ id: 'r2', status: 'REJECTED' })).toMatchObject({ label: '修改并重新提交' });
  expect(getRegistrationHomeAction({ id: 'r3', status: 'APPROVED' })).toMatchObject({ label: '去下单' });
});
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
cd frontend
cmd /c npm test -- registrationViewModels ExhibitorLayout display
```

Expected: module missing for `registrationViewModels`.

- [ ] **Step 3: Add frontend types and display helpers**

Add to `domain.ts`:

```ts
export type ExhibitorStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';
export type RegistrationStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type Exhibitor = { id: string; companyName: string; creditCode?: string | null; contactName?: string | null; contactPhone?: string | null; contactEmail?: string | null; address?: string | null; status: ExhibitorStatus; createdAt: string; updatedAt: string };
export type EventCategory = { id: string; eventId: string; code: string; name: string; enabled: boolean; sortOrder: number; createdAt: string; updatedAt: string };
export type Registration = { id: string; eventId: string; exhibitorId: string; status: RegistrationStatus; category?: string | null; categoryName?: string | null; zoneId?: string | null; intentAreaSqm?: string | number | null; contactName?: string | null; contactPhone?: string | null; remark?: string | null; reviewComment?: string | null; reviewedBy?: string | null; reviewedAt?: string | null; submittedAt?: string | null; createdAt: string; updatedAt: string };
```

Add display helpers:

```ts
formatRegistrationStatus(status)
formatExhibitorStatus(status)
formatEventCategoryDisplay(category)
```

- [ ] **Step 4: Update exhibitor navigation and routes**

Bottom nav labels become:

```text
首页
报名
订单
通知
```

Routes:

```tsx
{ path: 'profile', element: lazyPage(<ExhibitorProfilePage />) }
{ path: 'registrations', element: lazyPage(<RegistrationListPage />) }
{ path: 'registrations/new', element: lazyPage(<RegistrationCreatePage />) }
{ path: 'registrations/:registrationId', element: lazyPage(<RegistrationDetailPage />) }
```

Header config:

```ts
/exhibitor/registrations -> 报名进度
/exhibitor/registrations/new -> 提交报名, backTo /exhibitor/registrations
/exhibitor/registrations/:id -> 报名详情, backTo /exhibitor/registrations
/exhibitor/profile -> 企业资料, backTo /exhibitor
```

- [ ] **Step 5: Implement exhibitor pages**

`RegistrationCreatePage`:

- loads `/api/events`
- loads `/api/event-categories?eventId=...`
- category uses `Select`, no manual input
- posts `/api/registrations`
- then posts `/api/registrations/{id}/submit`
- navigates to `/exhibitor/registrations/{id}`

`RegistrationDetailPage`:

- shows status, event, category, intent area, contact info, remark, review comment
- if status `REJECTED`, shows editable form
- save button calls `PUT /api/registrations/{id}`
- resubmit button calls `POST /api/registrations/{id}/submit`

`OrderCreatePage`:

- before showing order form, loads `/api/registrations?exhibitorId=DEMO_EXHIBITOR_ID&status=APPROVED`
- if none, shows `Empty` with button to `/exhibitor/registrations/new`
- if one, uses its `id` as `registrationId`
- if multiple, requires selecting registration from dropdown
- includes `registrationId` in `buildOrderCreateBody`

- [ ] **Step 6: Run frontend tests and verify GREEN**

Run:

```powershell
cmd /c npm test -- registrationViewModels ExhibitorLayout display pageLogic
```

Expected: selected tests pass.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/app frontend/src/pages/exhibitor
git commit -m "feat: add exhibitor registration mobile flow"
```

## Task 5: Admin Exhibitor and Registration Review UI

**Files:**
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Create: `frontend/src/pages/admin/exhibitors/AdminExhibitorListPage.tsx`
- Create: `frontend/src/pages/admin/registrations/AdminRegistrationListPage.tsx`
- Create: `frontend/src/pages/admin/registrations/AdminRegistrationDetailPage.tsx`
- Modify: `frontend/src/pages/admin/DashboardPage.tsx`
- Modify: `frontend/src/pages/admin/dashboard/dashboardLogic.ts`
- Modify: `frontend/src/pages/admin/dashboard/dashboardLogic.test.ts`

- [ ] **Step 1: Write failing dashboard logic tests**

Add:

```ts
it('shows pending registration review count and route target', () => {
  expect(buildAdminDashboardKpis({
    pendingRegistrationReviewCount: 3,
    pendingSalesConfirmCount: 0,
    pendingFinanceConfirmCount: 0,
    pendingSelectionReviewCount: 0,
  })).toEqual(expect.arrayContaining([
    expect.objectContaining({ title: '待审核报名', value: 3, to: '/admin/registrations?status=SUBMITTED' }),
  ]));
});
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
cmd /c npm test -- dashboardLogic
```

Expected: fails because dashboard model lacks registration KPI.

- [ ] **Step 3: Add admin menu and routes**

Routes:

```tsx
{ path: 'exhibitors', element: lazyPage(<AdminExhibitorListPage />) }
{ path: 'registrations', element: lazyPage(<AdminRegistrationListPage />) }
{ path: 'registrations/:registrationId', element: lazyPage(<AdminRegistrationDetailPage />) }
```

Menu under business group:

```text
展商管理 -> /admin/exhibitors
报名审核 -> /admin/registrations
```

- [ ] **Step 4: Implement admin pages**

`AdminExhibitorListPage`:

- `ProTable`
- loads `/api/exhibitors`
- columns: companyName, creditCode, contactName, contactPhone, status
- create/edit uses `ModalForm`

`AdminRegistrationListPage`:

- `ProTable`
- loads `/api/registrations`
- filters: eventId, status, exhibitorId
- columns: event, exhibitor company name, category name/code, status, submittedAt
- row click navigates to `/admin/registrations/{id}`

`AdminRegistrationDetailPage`:

- loads `/api/registrations/{id}`
- approve button calls `/api/registrations/{id}/approve`
- reject button opens `ModalForm` with required `reason`, max 500
- reject calls `/api/registrations/{id}/reject`

- [ ] **Step 5: Extend dashboard UI**

Dashboard shows registration KPI and top 5 pending registrations. Clicking KPI navigates to `/admin/registrations?status=SUBMITTED`.

- [ ] **Step 6: Run tests and verify GREEN**

Run:

```powershell
cmd /c npm test -- dashboardLogic display
```

Expected: selected tests pass.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/app/layouts/AdminLayout.tsx frontend/src/app/routes.tsx frontend/src/pages/admin
git commit -m "feat: add admin exhibitor registration review"
```

## Task 6: Notifications Deep Links and Full Verification

**Files:**
- Modify: `frontend/src/pages/exhibitor/notifications/NotificationCenterPage.tsx`
- Modify: `frontend/src/pages/exhibitor/mobileViewModels.ts`
- Modify: `frontend/src/pages/exhibitor/mobileViewModels.test.ts`
- Modify as needed: backend/frontend files changed by Tasks 1-5

- [ ] **Step 1: Write failing notification link test**

Add to `mobileViewModels.test.ts`:

```ts
it('maps registration notifications to registration detail links', () => {
  expect(getNotificationTarget({
    type: 'REGISTRATION_APPROVED',
    relatedType: 'Registration',
    relatedId: 'registration-1',
  })).toBe('/exhibitor/registrations/registration-1');
  expect(getNotificationTarget({
    type: 'PAYMENT_CONFIRMED',
    relatedOrderId: 'order-1',
  })).toBe('/exhibitor/orders/order-1');
});
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
cmd /c npm test -- mobileViewModels
```

Expected: `getNotificationTarget` missing.

- [ ] **Step 3: Implement notification target helper and page click**

Helper behavior:

```ts
Registration relatedType + relatedId -> /exhibitor/registrations/{relatedId}
relatedOrderId -> /exhibitor/orders/{relatedOrderId}
otherwise undefined
```

`NotificationCenterPage`:

- card click navigates when target exists
- still supports mark read
- does not navigate for notifications without related object

- [ ] **Step 4: Backend full tests**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 5: Frontend full verification**

Run:

```powershell
cd frontend
cmd /c npm test -- registrationViewModels mobileViewModels ExhibitorLayout dashboardLogic display pageLogic
cmd /c npm run build
```

Expected: selected tests and build pass.

- [ ] **Step 6: Search checks**

Run:

```powershell
rg -n "registrationId" backend/src/main/java frontend/src
rg -n "REGISTRATION_SUBMITTED|REGISTRATION_APPROVED|REGISTRATION_REJECTED|relatedType|relatedId|relatedOrderId" backend/src/main/java frontend/src
rg -n "手输|手动输入.*品类|category.*Input" frontend/src/pages/exhibitor
```

Expected:

- first command shows order request/response and order create page usage.
- second command shows notification constants, DTO fields, and frontend target mapping.
- third command returns no user-facing category free-input implementation.

- [ ] **Step 7: Browser acceptance**

Manual browser checks:

```text
/exhibitor
  shows enterprise/profile and registration status before order actions

/exhibitor/registrations/new
  event dropdown works
  category dropdown loads from /api/event-categories?eventId=
  no category ID manual input

/exhibitor/orders/new
  no approved registration -> guides to registration
  approved registration -> submits order with registrationId

/admin/registrations
  submitted registration visible
  approve works
  reject requires reason

/exhibitor/notifications
  registration notification click opens registration detail
```

- [ ] **Step 8: Commit final verification fixes**

```powershell
git add .
git commit -m "test: verify exhibitor registration foundation"
```

## Self-Review

- Spec coverage: exhibitors, event categories, registrations, rejected edit/resubmit, no duplicate in-progress registrations, order gating, admin review, frontend registration pages, notification related fields, and demo compatibility are each covered by a task.
- Placeholder scan: this plan contains no TBD/TODO/fill-later placeholders.
- Type consistency: `registrationId`, `relatedType`, `relatedId`, `relatedOrderId`, `Registration.status`, and `EventCategory.code/name` are named consistently across backend and frontend tasks.
