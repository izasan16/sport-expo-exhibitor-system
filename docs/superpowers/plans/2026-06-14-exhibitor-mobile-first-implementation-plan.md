# Exhibitor Mobile First Full UX Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full UX feedback set in one round: mobile-first exhibitor portal, readable business UI, admin workbench improvements, payment failure reasons, selection remarks/recommendations, notifications, and automatic timeout release.

**Architecture:** Extend the existing Spring Boot modules and schema baseline first so frontend pages can depend on stable API contracts. Then refactor exhibitor pages into mobile-first Ant Design views while keeping admin pages desktop ProComponents. All work stays in the existing React/Spring/PostgreSQL application and uses the current schema initializer; do not introduce Flyway/Liquibase, `antd-mobile`, or a second frontend app.

**Tech Stack:** React, TypeScript, Ant Design, ProComponents, Vitest, Java Spring Boot, PostgreSQL, Spring scheduled task.

---

## Hard Requirements From Spec

- This is a single implementation round. Do not move listed requirements into “later”.
- Payment method enum must stay `OFFLINE_TRANSFER`, `OFFLINE_CASH`, `OFFLINE_CHECK`; do not introduce `OFFLINE_BANK`.
- Timeline uses existing `created_at` / `updated_at` / `confirmed_at` / `reviewed_at` / `signed_at` as approximate business progress, not audit truth.
- Exhibitor mobile pages must work at 375px without `MobileUnsupported`.
- All selectable business objects use dropdowns, lists, cards, or checkboxes; no user-facing manual UUID entry.
- Admin pages must show business names and short IDs, not raw UUIDs as primary content.
- New UI must tolerate legacy/null data. Missing text fields display `未填写`, missing optional sections display `暂无`, missing timestamps display `时间未记录`, and missing numeric amounts display `-`; pages must not render raw `null`, `undefined`, or crash.

---

## Task 1: Backend Schema, Notifications, and Payment/Selection Fields

**Files:**
- Modify: `backend/src/main/resources/db/schema-foundation.sql`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/notification/Notification.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationRepository.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/notification/dto/NotificationResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/payment/Payment.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentController.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/payment/dto/PaymentResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/payment/dto/FailPaymentRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelection.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionController.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/dto/CreateBoothSelectionRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/dto/SubmitBoothSelectionRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/dto/RejectBoothSelectionRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/dto/BoothSelectionResponse.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/notification/NotificationServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/payment/PaymentServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java`

- [ ] **Step 1: Write failing notification test**

Add `NotificationServiceTest` that creates one notification, lists by `DEMO_EXHIBITOR_ID`, marks it read, and asserts `readAt` becomes non-null.

- [ ] **Step 2: Write failing payment failure reason test**

In `PaymentServiceTest`, add a test that calls:

```java
paymentService.fail(payment.id(), DemoActors.DEMO_ADMIN_ID, "户名不符");
```

Assert status is `CONFIRM_FAILED`, response `failureReason()` is `户名不符`, and repository entity `getFailureReason()` is also `户名不符`. Also assert blank reason throws `BusinessException("Payment failure reason must not be blank")`.

- [ ] **Step 3: Write failing selection remark/recommendation test**

In `BoothSelectionServiceTest`, submit with:

```java
new CreateBoothSelectionRequest(order.id(), order.exhibitorId(), List.of(b1.id(), b2.id()), "希望靠近主通道")
```

Assert response `exhibitorRemark()` is persisted. Reject with:

```java
new RejectBoothSelectionRequest("booth not suitable", "A101 / A102")
```

Assert `reviewComment()` and `recommendedBooths()` are both returned.

- [ ] **Step 4: Run backend tests and verify RED**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: compilation/test failures for missing `NotificationService`, `failureReason`, `remark`, and `recommendedBooths`.

Expected error examples include `cannot find symbol class NotificationService`, `method fail cannot be applied to given types`, `cannot find symbol method failureReason()`, or constructor mismatch for selection request/reject request DTOs. If the test fails for database connectivity or syntax errors instead, fix the test setup before implementing production code.

- [ ] **Step 5: Add schema columns and notifications table**

Update `schema-foundation.sql`:

```sql
alter table __schema__.payments add column if not exists failure_reason varchar(500);
alter table __schema__.booth_selections add column if not exists exhibitor_remark varchar(500);
alter table __schema__.booth_selections add column if not exists recommended_booths varchar(500);

create table if not exists __schema__.notifications (
    id uuid primary key,
    event_id uuid references __schema__.events(id),
    recipient_type varchar(40) not null,
    recipient_id uuid not null,
    title varchar(200) not null,
    content varchar(1000) not null,
    type varchar(80) not null,
    read_at timestamptz,
    created_at timestamptz not null
);

create index if not exists idx_notifications_recipient
    on __schema__.notifications(recipient_id, created_at desc);
```

- [ ] **Step 6: Implement notification module**

Create `Notification`, `NotificationRepository`, `NotificationResponse`, `NotificationService`, and `NotificationController`.

`NotificationService` must expose:

```java
NotificationResponse create(UUID eventId, String recipientType, UUID recipientId,
                            String title, String content, String type);
List<NotificationResponse> list(UUID recipientId);
NotificationResponse markRead(UUID id, UUID recipientId);
int cleanupReadNotificationsOlderThan(OffsetDateTime cutoff);
```

Use `DemoActors.DEMO_EXHIBITOR_ID` as default recipient in controller when `recipientId` is absent.

- [ ] **Step 7: Centralize notification type constants**

Create `backend/src/main/java/com/sportexpo/exhibitor/notification/NotificationTypes.java` with constants:

```java
ORDER_SALES_CONFIRMED
CONTRACT_SIGNED
PAYMENT_CONFIRMED
PAYMENT_FAILED
SELECTION_SUBMITTED
SELECTION_APPROVED
SELECTION_REJECTED
SELECTION_TIMEOUT_RELEASED
```

- [ ] **Step 8: Implement payment failure reason**

Add `failureReason` column mapping, response field, `FailPaymentRequest(@NotBlank @Size(max = 500) String reason)`, and change `PaymentService.fail` to require a trimmed reason. On failure, create `PAYMENT_FAILED` notification for the order exhibitor.

- [ ] **Step 9: Implement selection remark and recommended booths**

Add `exhibitorRemark` and `recommendedBooths` mapping and response fields. `SubmitBoothSelectionRequest` accepts `remark`. `RejectBoothSelectionRequest` accepts `reason` and `recommendedBooths`. Reject response must include both fields.

- [ ] **Step 10: Run backend task tests and verify GREEN**

Run:

```powershell
mvn test -Dtest=NotificationServiceTest,PaymentServiceTest,BoothSelectionServiceTest
```

Expected: all selected tests pass.

- [ ] **Step 11: Commit**

```powershell
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/notification backend/src/main/java/com/sportexpo/exhibitor/payment backend/src/main/java/com/sportexpo/exhibitor/selection backend/src/test/java/com/sportexpo/exhibitor/notification backend/src/test/java/com/sportexpo/exhibitor/payment backend/src/test/java/com/sportexpo/exhibitor/selection
git commit -m "feat: add notifications and review metadata"
```

## Task 2: Backend Admin Dashboard, Batch Confirm, Notification Triggers, and Timeout Job

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/admin/dto/AdminDashboardResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/order/dto/BatchConfirmOrdersRequest.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/order/dto/BatchConfirmOrdersResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/contract/ContractService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionTimeoutJob.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/ExhibitorApplication.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/admin/AdminDashboardServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java`

- [ ] **Step 1: Write failing dashboard aggregation test**

Create `AdminDashboardServiceTest` that prepares one pending sales order, one pending finance payment, and one pending selection. Assert counts are at least one, lists include expected items, and `generatedAt()` is non-null.

- [ ] **Step 2: Write failing batch confirm test**

In `ParticipationOrderServiceTest`, call:

```java
orderService.batchConfirmSales(List.of(ready.id(), alreadyConfirmed.id(), UUID.randomUUID()), DemoActors.DEMO_ADMIN_ID)
```

Assert one success, two failures, and successful order becomes `PENDING_CONTRACT`.

- [ ] **Step 3: Write failing timeout notification test**

In `BoothSelectionServiceTest`, create expired pending selection, call timeout release method used by the job, and assert one `SELECTION_TIMEOUT_RELEASED` notification exists for the exhibitor.

- [ ] **Step 4: Run tests and verify RED**

Run:

```powershell
mvn test -Dtest=AdminDashboardServiceTest,ParticipationOrderServiceTest,BoothSelectionServiceTest
```

Expected: failures for missing admin service, batch API, and timeout notification.

Expected error examples include `cannot find symbol class AdminDashboardService`, `cannot find symbol method batchConfirmSales`, or an assertion failure that no `SELECTION_TIMEOUT_RELEASED` notification was created. If a failure rolls back all batch items together, the implementation is violating the independent-transaction requirement.

- [ ] **Step 5: Implement `/api/admin/dashboard`**

`AdminDashboardResponse` must include:

```java
OffsetDateTime generatedAt;
long pendingSalesConfirmCount;
long pendingFinanceConfirmCount;
long pendingSelectionReviewCount;
long todayNewOrderCount;
List<ParticipationOrderResponse> pendingSalesConfirmOrders;
List<PaymentResponse> pendingFinanceConfirmPayments;
List<BoothSelectionResponse> pendingSelectionReviews;
```

Use repository queries or service lists, then limit each top list to 5.

- [ ] **Step 6: Implement batch sales confirm**

Add endpoint:

```http
POST /api/participation-orders/batch-confirm
```

Request:

```json
{ "orderIds": ["uuid"] }
```

Response:

```java
record BatchConfirmOrdersResponse(int successCount, int failureCount, List<BatchFailure> failures)
record BatchFailure(UUID orderId, String reason)
```

Process each order in its own independent transaction. Do not wrap the whole batch in one `@Transactional` method that rolls back every order together. Use either `TransactionTemplate` inside the loop or a separate public service method annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)` for each order. A failed order must not rollback successful order confirmations.

The backend failure response returns the full `orderId` and `reason`; it does not calculate `shortId`. Frontend computes display short IDs with the shared `formatShortId(orderId)` helper so every page uses the same short-id rule.

- [ ] **Step 7: Add notification triggers**

Create notifications at these exact moments:

| Type | Method |
| --- | --- |
| `ORDER_SALES_CONFIRMED` | `ParticipationOrderService.confirmSales` |
| `CONTRACT_SIGNED` | `ContractService.markOfflineSigned` |
| `PAYMENT_CONFIRMED` | `PaymentService.confirm` |
| `PAYMENT_FAILED` | `PaymentService.fail` |
| `SELECTION_SUBMITTED` | `BoothSelectionService.submit` |
| `SELECTION_APPROVED` | `BoothSelectionService.approve` |
| `SELECTION_REJECTED` | `BoothSelectionService.reject` |
| `SELECTION_TIMEOUT_RELEASED` | timeout release service method |

- [ ] **Step 8: Implement timeout job**

Enable scheduling on `ExhibitorApplication` with `@EnableScheduling`. Add `BoothSelectionTimeoutJob`:

```java
// fixedRate runs every 10 minutes based on scheduled start time; the job is short and idempotent.
// initialDelay gives the application 60 seconds to finish schema initialization and warm up.
@Scheduled(initialDelay = 60_000, fixedRate = 600_000)
public void releaseExpiredSelections() {
    service.releaseExpiredSelections(OffsetDateTime.now(), DemoActors.SYSTEM_ACTOR_ID);
}
```

If no system actor constant exists, add one in `DemoActors`.

- [ ] **Step 9: Run backend task tests and verify GREEN**

Run:

```powershell
mvn test -Dtest=AdminDashboardServiceTest,ParticipationOrderServiceTest,BoothSelectionServiceTest
```

Expected: all selected tests pass.

- [ ] **Step 10: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/admin backend/src/main/java/com/sportexpo/exhibitor/order backend/src/main/java/com/sportexpo/exhibitor/contract backend/src/main/java/com/sportexpo/exhibitor/payment backend/src/main/java/com/sportexpo/exhibitor/selection backend/src/main/java/com/sportexpo/exhibitor/ExhibitorApplication.java backend/src/test/java/com/sportexpo/exhibitor/admin backend/src/test/java/com/sportexpo/exhibitor/order backend/src/test/java/com/sportexpo/exhibitor/selection
git commit -m "feat: add admin aggregation and timeout release"
```

## Task 3: Frontend Responsive Foundation and Display Models

**Files:**
- Create: `frontend/src/app/responsive.ts`
- Create: `frontend/src/app/responsive.test.ts`
- Modify: `frontend/src/app/App.tsx`
- Delete: `frontend/src/app/MobileUnsupported.tsx`
- Modify: `frontend/src/app/display.ts`
- Modify: `frontend/src/app/display.test.ts`
- Create: `frontend/src/pages/exhibitor/mobileViewModels.ts`
- Create: `frontend/src/pages/exhibitor/mobileViewModels.test.ts`

- [ ] **Step 1: Write failing responsive tests**

`responsive.test.ts` must assert:

```ts
expect(isMobileWidth(375)).toBe(true)
expect(isMobileWidth(767)).toBe(true)
expect(isMobileWidth(768)).toBe(false)
```

- [ ] **Step 2: Write failing display tests**

Add assertions:

```ts
expect(formatPaymentMethod('OFFLINE_TRANSFER')).toBe('银行转账')
expect(formatPaymentMethod('OFFLINE_BANK')).toBe('OFFLINE_BANK')
```

The second assertion prevents accidentally introducing `OFFLINE_BANK` as a supported value.

- [ ] **Step 3: Write failing mobile view model tests**

Test:

- empty home flow returns six steps
- selected booth summary removes a booth and recalculates selected area
- timeline event builder uses `updatedAt` fallback when exact event time is absent
- notification type labels include all eight notification types
- `formatShortId(undefined)` and `formatShortId(null)` return `-`, UUID strings return the first 8 chars, and strings shorter than 8 chars return the original string
- null-safe text helpers return `未填写`, `暂无`, `时间未记录`, or `-` according to field type

- [ ] **Step 4: Run frontend tests and verify RED**

Run:

```powershell
cd frontend
cmd /c npm test -- responsive display mobileViewModels
```

Expected: missing `responsive.ts` and mobile view model failures.

Expected error examples include module resolution failure for `responsive`, missing `mobileViewModels`, or assertions failing because `OFFLINE_TRANSFER` still renders as a raw enum. If the tests pass before implementation, rewrite the test so it proves the missing behavior.

- [ ] **Step 5: Implement responsive helper**

Create:

```ts
export const MOBILE_BREAKPOINT = 768
export const isMobileWidth = (width: number) => width < MOBILE_BREAKPOINT
export const useIsMobile = () => { /* matchMedia('(max-width: 767px)') hook */ }
```

- [ ] **Step 6: Remove mobile blocker**

Delete `MobileUnsupported.tsx`. Remove its import and wrapper from `App.tsx`. Search must return no results:

```powershell
rg -n "MobileUnsupported|请在电脑端访问展商系统" frontend/src
```

- [ ] **Step 7: Implement display and view model helpers**

Add helpers for:

- payment methods: `OFFLINE_TRANSFER` -> `银行转账`, `OFFLINE_CASH` -> `现金`, `OFFLINE_CHECK` -> `支票`
- notification labels for all eight types
- selected booth add/remove summary
- order progress Steps model
- approximate timeline model from `createdAt`, `updatedAt`, `confirmedAt`, `reviewedAt`, `signedAt`
- null-safe display helpers: text -> `未填写`, optional section/body -> `暂无`, time -> `时间未记录`, amount -> `-`
- `formatShortId` boundary handling for null, undefined, normal UUID, and strings shorter than 8 chars

- [ ] **Step 8: Run frontend task tests and verify GREEN**

Run:

```powershell
cmd /c npm test -- responsive display mobileViewModels
```

Expected: all selected tests pass.

- [ ] **Step 9: Commit**

```powershell
git add frontend/src/app frontend/src/pages/exhibitor/mobileViewModels.ts frontend/src/pages/exhibitor/mobileViewModels.test.ts
git commit -m "feat: add mobile responsive foundation"
```

## Task 4: Mobile Exhibitor Home, Orders, Detail, Payment, Selection, and Notifications

**Files:**
- Modify: `frontend/src/pages/exhibitor/HomePage.tsx`
- Modify: `frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx`
- Modify: `frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx`
- Modify: `frontend/src/pages/exhibitor/orders/ExhibitorOrderDetailPage.tsx`
- Modify: `frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx`
- Modify: `frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx`
- Create: `frontend/src/pages/exhibitor/notifications/NotificationCenterPage.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`

- [ ] **Step 1: Home uses Ant Design Steps**

Empty state shows `Steps` with six items: 提交订单、销售确认、合同确认、付款确认、选择展位、审核完成. Primary button navigates to `/exhibitor/orders/new`.

All home cards and summaries must use null-safe helpers. If a legacy order lacks event display data, show `未填写` for event name and keep the page interactive.

- [ ] **Step 2: Orders mobile view uses card list**

At mobile width, render `List` of `Card` items with event name, status tag, amount, area, booth type, short order id, next action, and details action. Desktop may keep `ProTable`.

Null display rules: missing event name/exhibitor name/status label uses `未填写`; missing created/updated time uses `时间未记录`; missing optional remarks or empty recent records use `暂无`; missing amount uses `-`.

- [ ] **Step 3: Order create page uses vertical mobile form**

Use event and area tier dropdowns. Do not show manual UUID inputs. Submit button is block on mobile.

- [ ] **Step 4: Order detail uses Collapse and approximate timeline**

Mobile view renders `Collapse` sections: 基本信息、合同信息、付款记录、选位记录、操作时间线. Default expanded panels are `basic` and `timeline`, so users immediately see core order facts and current progress. Timeline is built from existing timestamps and labelled “业务进度”.

Old-data compatibility: if contract, payment, selection, remark, recommended booths, failure reason, or timestamps are absent, render `暂无` / `未填写` / `时间未记录` rather than blank content or raw `null`.

- [ ] **Step 5: Payment page uses readable methods**

Payment method select uses only `OFFLINE_TRANSFER`, `OFFLINE_CASH`, `OFFLINE_CHECK` with Chinese labels. Existing payment cards show failure reason when present.

If `failureReason` is null because the payment was created before this field existed, show `暂无失败原因` only for failed payments and hide the failure reason row for other statuses.

- [ ] **Step 6: Selection mobile view uses List + Checkbox**

Use `List + Checkbox` for booths. Selected list has cancel buttons. Cancel must uncheck the same booth and recalculate area immediately. Desktop `ProTable` and mobile `List` must share the exact same `selectedKeys` state, for example:

```ts
const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
```

Pass `selectedKeys` into `ProTable` `rowSelection.selectedRowKeys`, and use `selectedKeys.includes(booth.id)` for mobile `Checkbox.checked`. Both desktop and mobile handlers must call the same `setSelectedKeys` path.

- [ ] **Step 7: Selection submit includes remark**

Submit payload:

```ts
{
  orderId,
  boothIds,
  remark
}
```

Area must be exactly equal before enabling submit.

- [ ] **Step 8: Add notification center**

Add `/exhibitor/notifications` route and menu entry. Page lists title, content, type label, read/unread state, created time, and mark-read action. Supports all/unread segmented control.

Null display rules: missing title/content/type label uses `未填写`; missing read time shows `未读`; missing created time uses `时间未记录`.

- [ ] **Step 9: Run frontend build**

Run:

```powershell
cmd /c npm run build
```

Expected: build passes.

- [ ] **Step 10: Commit**

```powershell
git add frontend/src/pages/exhibitor frontend/src/app/routes.tsx frontend/src/app/layouts/ExhibitorLayout.tsx
git commit -m "feat: mobile optimize exhibitor portal"
```

## Task 5: Admin Workbench, Orders, Payments, and Selection Review UI

**Files:**
- Modify: `frontend/src/pages/admin/DashboardPage.tsx`
- Modify: `frontend/src/pages/admin/dashboard/dashboardLogic.ts`
- Modify: `frontend/src/pages/admin/dashboard/dashboardLogic.test.ts`
- Modify: `frontend/src/pages/admin/orders/AdminOrderListPage.tsx`
- Modify: `frontend/src/pages/admin/payments/PaymentConfirmPage.tsx`
- Modify: `frontend/src/pages/admin/selections/SelectionReviewPage.tsx`
- Modify: `frontend/src/app/apiLoaders.ts`
- Modify: `frontend/src/app/domain.ts`

- [ ] **Step 1: Dashboard uses aggregation endpoint**

Replace client-side full-list aggregation with `GET /api/admin/dashboard`. Page shows counts, top 5 lists, and service `generatedAt`.

If `generatedAt` is missing, display `时间未记录`. Empty top lists display `暂无待处理事项`.

- [ ] **Step 2: Dashboard refresh rules**

Refresh on page mount, route re-entry/focus, and after admin actions: order confirm/batch confirm, payment confirm/fail, selection approve/reject/timeout release. Show loading state while refreshing.

- [ ] **Step 3: Order list supports batch confirm with failure Alert**

Allow row selection only for `PENDING_SALES_CONFIRM`. On response with `failureCount > 0`, show `Alert` and list each failed short order id plus reason. Backend returns full `orderId`; frontend computes short display value with `formatShortId(orderId)`. Clear selection after request.

- [ ] **Step 4: Payment failure uses ModalForm**

Replace simple confirm with ProComponents `ModalForm`. Reason is required, max 500, show count. Submit:

```ts
apiPost(`/api/payments/${payment.id}/fail`, { reason })
```

Payment list shows event name, exhibitor display name, short order id, method label, status, amount, and failure reason.

Null display rules: missing event/exhibitor names use `未填写`; missing failure reason on failed payment uses `暂无失败原因`; missing failure reason on non-failed payment hides the row; missing amount uses `-`.

- [ ] **Step 5: Selection review shows remark and recommendation**

Review page shows selected booth numbers, total area, exhibitor remark, and lock expiry. Reject modal has required reason, 500 char count, and recommended booths field. Submit:

```ts
apiPost(`/api/booth-selections/${selection.id}/reject`, { reason, recommendedBooths })
```

Null display rules: missing exhibitor remark or recommended booths uses `暂无`; missing lock expiry uses `时间未记录`; missing booth list displays `暂无展位信息`.

- [ ] **Step 6: Tests**

Update dashboard logic tests to assert `generatedAt` formatting and dashboard response handling. Add page logic tests for batch failure summary and reject reason length validation.

- [ ] **Step 7: Run frontend selected tests**

Run:

```powershell
cmd /c npm test -- dashboardLogic pageLogic display
```

Expected: selected tests pass.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/pages/admin frontend/src/app/apiLoaders.ts frontend/src/app/domain.ts
git commit -m "feat: complete admin workbench ux"
```

## Task 6: Full Verification and Browser Acceptance

**Files:**
- Modify only if verification reveals issues in files changed by Tasks 1-5.

- [ ] **Step 1: Backend full test**

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

- [ ] **Step 2: Frontend tests and build**

Run:

```powershell
cd frontend
cmd /c npm test -- display responsive mobileViewModels pageLogic dashboardLogic
cmd /c npm run build
```

Expected: tests and build pass.

- [ ] **Step 3: Search checks**

Run:

```powershell
rg -n "MobileUnsupported|请在电脑端访问展商系统" frontend/src
rg -n "OFFLINE_BANK" frontend/src backend/src/main/java backend/src/test/java
rg -n "null|undefined" frontend/src/pages/exhibitor frontend/src/pages/admin
rg -n "idx_notifications_recipient|initialDelay = 60_000|fixedRate = 600_000|generatedAt|业务进度|selectedKeys|OFFLINE_TRANSFER" backend/src/main/resources backend/src/main/java frontend/src
```

Expected: first command returns no matches. Second command may match only the display test that asserts `OFFLINE_BANK` is unsupported. Third command is a review aid: no user-facing fallback string should literally render `null` or `undefined`; code-level null checks are allowed when paired with the null-safe display helpers. Fourth command is a positive search check and must find the required implementation markers for notification index, scheduled timeout cadence, dashboard timestamp, timeline label, shared selection state, and payment enum.

- [ ] **Step 4: Browser mobile checks**

Use the in-app browser at 375px width:

- `/exhibitor` shows Steps flow and no mobile blocker.
- `/exhibitor/orders` shows mobile cards.
- `/exhibitor/orders/new` uses dropdowns, not UUID inputs.
- order detail shows Collapse, payment progress, and “业务进度” timeline.
- payment page shows 银行转账/现金/支票.
- selection page uses checkbox list, selected list cancel unchecks the source item, and area is recalculated.
- `/exhibitor/notifications` lists notifications and mark-read works.

- [ ] **Step 5: Browser desktop checks**

Use desktop width:

- `/admin/dashboard` uses aggregation endpoint and refreshes after actions.
- `/admin/orders` supports batch confirm and partial failure Alert.
- `/admin/payments` requires failure reason and displays it.
- `/admin/selections` shows booth list, exhibitor remark, reject reason count, and recommended booths.

- [ ] **Step 6: Commit final verification fixes**

```powershell
git add .
git commit -m "test: verify mobile first remediation"
```
