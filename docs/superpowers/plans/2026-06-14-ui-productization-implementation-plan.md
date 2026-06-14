# UI Productization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Productize the current exhibitor and admin UI so the implemented order, contract, payment, and booth-selection loop feels like a usable V1 business system rather than an engineering demo.

**Architecture:** Keep the existing Spring Boot modular monolith and Vite React application. Add the minimal backend support required by the UI, then build shared frontend domain utilities, route structure, exhibitor order-centered pages, admin task-centered pages, and smoke verification. PostgreSQL remains the source of truth; frontend pages consume existing list APIs plus one new order detail API.

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit 5, MockMvc, React 18, TypeScript, Vite, Ant Design 5, ProComponents, Vitest.

---

## File Structure

Backend files to modify:

```text
backend/src/main/java/com/sportexpo/exhibitor/order/dto/ParticipationOrderResponse.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderController.java
backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java
```

Frontend files to create:

```text
frontend/src/app/domain.ts
frontend/src/app/formatters.ts
frontend/src/app/status.tsx
frontend/src/app/apiLoaders.ts
frontend/src/app/MobileUnsupported.tsx
frontend/src/app/routeHelpers.ts
frontend/src/pages/exhibitor/orders/ExhibitorOrderDetailPage.tsx
frontend/src/pages/admin/orders/AdminOrderDetailPage.tsx
frontend/src/pages/admin/dashboard/dashboardLogic.ts
frontend/src/pages/admin/dashboard/dashboardLogic.test.ts
```

Frontend files to modify:

```text
frontend/src/app/App.tsx
frontend/src/app/layouts/AdminLayout.tsx
frontend/src/app/layouts/ExhibitorLayout.tsx
frontend/src/app/routes.tsx
frontend/src/pages/exhibitor/HomePage.tsx
frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx
frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx
frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx
frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx
frontend/src/pages/exhibitor/selections/SelectionResultPage.tsx
frontend/src/pages/exhibitor/pageLogic.ts
frontend/src/pages/exhibitor/pageLogic.test.ts
frontend/src/pages/admin/DashboardPage.tsx
frontend/src/pages/admin/events/EventListPage.tsx
frontend/src/pages/admin/booths/BoothListPage.tsx
frontend/src/pages/admin/area-tiers/AreaTierListPage.tsx
frontend/src/pages/admin/price-rules/PriceRuleListPage.tsx
frontend/src/pages/admin/orders/AdminOrderListPage.tsx
frontend/src/pages/admin/contracts/ContractListPage.tsx
frontend/src/pages/admin/payments/PaymentConfirmPage.tsx
frontend/src/pages/admin/selections/SelectionReviewPage.tsx
frontend/src/pages/exhibitor/pageLogic.test.ts
```

---

## Shared Frontend Contracts

Use these shared frontend types in `frontend/src/app/domain.ts`:

```ts
export type OrderStatus =
  | 'PENDING_SALES_CONFIRM'
  | 'PENDING_CONTRACT'
  | 'PENDING_PAYMENT'
  | 'PARTIALLY_PAID'
  | 'PAID'
  | 'COMPLETED';

export type PaymentStatus = 'PENDING_FINANCE_CONFIRM' | 'CONFIRMED' | 'CONFIRM_FAILED';
export type ContractStatus = 'GENERATED' | 'EFFECTIVE';
export type ContractSignStatus = 'UNSIGNED' | 'MARKED_SIGNED';
export type SelectionStatus = 'PENDING_REVIEW' | 'CONFIRMED' | 'REJECTED' | 'TIMEOUT';
export type BoothStatus = 'AVAILABLE' | 'LOCKED' | 'CONFIRMED' | 'RESERVED' | 'DISABLED';
export type EligibilityStatus = 'NOT_OPEN' | 'AVAILABLE' | 'PAUSED' | 'CLOSED';

export type ParticipationOrder = {
  id: string;
  eventId: string;
  exhibitorId: string;
  zoneId?: string | null;
  areaTierId: string;
  areaSqm: number | string;
  boothType: string;
  status: OrderStatus;
  areaFee: number | string;
  serviceFee: number | string;
  discountAmount: number | string;
  adjustmentAmount: number | string;
  totalAmount: number | string;
  createdAt: string;
  updatedAt: string;
};

export type PaymentRecord = {
  id: string;
  eventId: string;
  orderId: string;
  amount: number | string;
  status: PaymentStatus;
  method: string;
  confirmedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ContractRecord = {
  id: string;
  eventId: string;
  orderId: string;
  currentVersionId?: string | null;
  contractNo: string;
  status: ContractStatus;
  createdAt: string;
  updatedAt: string;
};

export type ContractVersionRecord = {
  id: string;
  eventId: string;
  contractId: string;
  versionNo: number;
  reason: string;
  amount: number | string;
  areaSqm: number | string;
  signStatus: ContractSignStatus;
  signerId?: string | null;
  signedAt?: string | null;
  fileId?: string | null;
  createdAt: string;
  createdBy?: string | null;
};

export type SelectionEligibility = {
  id: string;
  eventId: string;
  orderId: string;
  status: EligibilityStatus;
  pauseReason?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BoothSelection = {
  id: string;
  eventId: string;
  orderId: string;
  exhibitorId: string;
  status: SelectionStatus;
  totalAreaSqm: number | string;
  lockStartedAt?: string | null;
  lockExpiresAt?: string | null;
  helperUserId?: string | null;
  reviewerUserId?: string | null;
  reviewedAt?: string | null;
  reviewComment?: string | null;
  boothIds: string[];
  createdAt: string;
  updatedAt: string;
};

export type Booth = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number | string;
  boothType: string;
  status: BoothStatus;
};
```

---

## Task 1: Backend Order Detail and Timestamps

**Files:**
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/dto/ParticipationOrderResponse.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderController.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java`

- [x] **Step 1: Add failing service test for response timestamps and detail lookup**

Append this test to `ParticipationOrderServiceTest`:

```java
@Test
void returnsOrderDetailWithCreatedAndUpdatedTimes() {
    var event = eventService.create(new CreateEventRequest("体博会", 2026));
    var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));
    priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

    var created = orderService.create(new CreateParticipationOrderRequest(
            event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
    ));

    var detail = orderService.get(created.id());

    assertThat(detail.id()).isEqualTo(created.id());
    assertThat(detail.createdAt()).isNotNull();
    assertThat(detail.updatedAt()).isNotNull();
}
```

Expected before implementation: compilation fails because `createdAt()` and `updatedAt()` do not exist on `ParticipationOrderResponse`.

- [x] **Step 2: Update `ParticipationOrderResponse`**

Add imports and fields:

```java
import java.time.OffsetDateTime;
```

Change the record to include these fields after `totalAmount`:

```java
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
```

- [x] **Step 3: Map timestamps in `ParticipationOrderService.toResponse`**

Change the final arguments of `new ParticipationOrderResponse(...)` to:

```java
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
```

- [x] **Step 3.5: Verify or add `ParticipationOrderService.get(UUID)`**

Ensure `ParticipationOrderService` contains this method before exposing the controller endpoint:

```java
@Transactional(readOnly = true)
public ParticipationOrderResponse get(UUID id) {
    return repository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException("Participation order does not exist"));
}
```

If the method already exists, leave it in place and only verify it maps through the updated `toResponse` method.

- [x] **Step 4: Expose order detail endpoint**

In `ParticipationOrderController`, add this method below `list`:

```java
@GetMapping("/{orderId}")
public ApiResponse<ParticipationOrderResponse> get(@PathVariable UUID orderId) {
    return ApiResponse.ok(service.get(orderId));
}
```

- [x] **Step 5: Run focused backend test**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test -Dtest=ParticipationOrderServiceTest
```

Expected: PASS.

- [x] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/order backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java
git commit -m "feat: expose order detail timestamps"
```

---

## Task 2: Shared Frontend Formatting, Status, and Page Logic

**Files:**
- Create: `frontend/src/app/domain.ts`
- Create: `frontend/src/app/formatters.ts`
- Create: `frontend/src/app/status.tsx`
- Create: `frontend/src/app/apiLoaders.ts`
- Modify: `frontend/src/pages/exhibitor/pageLogic.ts`
- Modify: `frontend/src/pages/exhibitor/pageLogic.test.ts`

- [x] **Step 1: Add failing page logic tests**

Extend `pageLogic.test.ts` imports from `./pageLogic` only:

```ts
import {
  getOrderStepIndex,
  getOrderNextAction,
  pickLatestActiveOrder,
} from './pageLogic';
import type { ParticipationOrder } from '../../app/domain';
```

Do not import `formatters.ts` or `status.tsx` in this step. This keeps the red test focused on `pageLogic` and avoids failing because shared UI modules are not created yet.

Append tests:

```ts
const baseOrder: ParticipationOrder = {
  id: 'e0e5589c-90aa-43b5-beb0-c42878beb4aa',
  eventId: 'event-1',
  exhibitorId: DEMO_EXHIBITOR_ID,
  zoneId: null,
  areaTierId: 'tier-1',
  areaSqm: '18.00',
  boothType: 'STANDARD',
  status: 'PENDING_PAYMENT',
  areaFee: '21600.00',
  serviceFee: '0.00',
  discountAmount: '0.00',
  adjustmentAmount: '0.00',
  totalAmount: '21600.00',
  createdAt: '2026-06-14T10:00:00+08:00',
  updatedAt: '2026-06-14T10:00:00+08:00',
};

it('picks latest active order before completed orders', () => {
  const completed = { ...baseOrder, id: 'completed-order', status: 'COMPLETED' as const, createdAt: '2026-06-15T10:00:00+08:00' };
  const pending = { ...baseOrder, id: 'pending-order', status: 'PAID' as const, createdAt: '2026-06-13T10:00:00+08:00' };

  expect(pickLatestActiveOrder([completed, pending])?.id).toBe('pending-order');
});

it('derives order steps and next actions', () => {
  expect(getOrderStepIndex('PENDING_SALES_CONFIRM')).toBe(1);
  expect(getOrderStepIndex('PENDING_PAYMENT')).toBe(3);
  expect(getOrderStepIndex('PAID')).toBe(4);
  expect(getOrderNextAction({ order: baseOrder }))
    .toMatchObject({ label: '提交付款', to: `/exhibitor/orders/${baseOrder.id}/payment` });
  expect(getOrderNextAction({ order: { ...baseOrder, status: 'PAID' }, eligibilityStatus: 'AVAILABLE' }))
    .toMatchObject({ label: '选择展位', to: `/exhibitor/orders/${baseOrder.id}/selection` });
});
```

Expected before implementation: FAIL because new modules and functions do not exist.

- [x] **Step 2: Create `domain.ts`**

Create the file using the exact shared frontend contracts from the "Shared Frontend Contracts" section of this plan.

- [x] **Step 3: Create `formatters.ts`**

```ts
export function formatShortId(id?: string | null) {
  return id ? id.slice(0, 8) : '-';
}

function toNumber(value: string | number | undefined | null): number {
  if (value === undefined || value === null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

export function formatMoney(value: string | number | undefined | null) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(toNumber(value));
}

export function formatArea(value: string | number | undefined | null) {
  return `${toNumber(value).toFixed(2)} ㎡`;
}

export function formatFriendlyTime(value?: string | null, now = new Date()) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfToday.getDate() - 1);
  const pad = (input: number) => String(input).padStart(2, '0');
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;

  if (date >= startOfToday) {
    return `今天 ${time}`;
  }
  if (date >= startOfYesterday && date < startOfToday) {
    return `昨天 ${time}`;
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${time}`;
}
```

- [x] **Step 4: Create `status.tsx`**

```tsx
import { Tag } from 'antd';
import type { BoothStatus, ContractSignStatus, ContractStatus, EligibilityStatus, OrderStatus, PaymentStatus, SelectionStatus } from './domain';

type StatusMeta = { label: string; color: string };

const orderStatusMap: Record<OrderStatus, StatusMeta> = {
  PENDING_SALES_CONFIRM: { label: '待销售确认', color: 'gold' },
  PENDING_CONTRACT: { label: '待生成合同', color: 'blue' },
  PENDING_PAYMENT: { label: '待付款', color: 'orange' },
  PARTIALLY_PAID: { label: '部分付款', color: 'orange' },
  PAID: { label: '已付款', color: 'green' },
  COMPLETED: { label: '已完成', color: 'default' },
};

const paymentStatusMap: Record<PaymentStatus, StatusMeta> = {
  PENDING_FINANCE_CONFIRM: { label: '待财务确认', color: 'orange' },
  CONFIRMED: { label: '已确认到账', color: 'green' },
  CONFIRM_FAILED: { label: '确认失败', color: 'red' },
};

const contractStatusMap: Record<ContractStatus | ContractSignStatus, StatusMeta> = {
  GENERATED: { label: '已生成', color: 'blue' },
  EFFECTIVE: { label: '已生效', color: 'green' },
  UNSIGNED: { label: '待线下签署', color: 'orange' },
  MARKED_SIGNED: { label: '已标记签署', color: 'green' },
};

const selectionStatusMap: Record<SelectionStatus, StatusMeta> = {
  PENDING_REVIEW: { label: '待审核', color: 'orange' },
  CONFIRMED: { label: '已确认', color: 'green' },
  REJECTED: { label: '已驳回', color: 'red' },
  TIMEOUT: { label: '已超时释放', color: 'default' },
};

const boothStatusMap: Record<BoothStatus, StatusMeta> = {
  AVAILABLE: { label: '可选', color: 'green' },
  LOCKED: { label: '锁定中', color: 'orange' },
  CONFIRMED: { label: '已确认', color: 'blue' },
  RESERVED: { label: '预留', color: 'purple' },
  DISABLED: { label: '禁用', color: 'default' },
};

const eligibilityStatusMap: Record<EligibilityStatus, StatusMeta> = {
  NOT_OPEN: { label: '未开放', color: 'default' },
  AVAILABLE: { label: '可选位', color: 'green' },
  PAUSED: { label: '暂停中', color: 'orange' },
  CLOSED: { label: '已关闭', color: 'blue' },
};

function fallbackMeta(status: string): StatusMeta {
  return { label: status, color: 'default' };
}

export function getOrderStatusMeta(status: OrderStatus | string) {
  return orderStatusMap[status as OrderStatus] ?? fallbackMeta(status);
}

export function getPaymentStatusMeta(status: PaymentStatus | string) {
  return paymentStatusMap[status as PaymentStatus] ?? fallbackMeta(status);
}

export function getContractStatusMeta(status: ContractStatus | ContractSignStatus | string) {
  return contractStatusMap[status as ContractStatus | ContractSignStatus] ?? fallbackMeta(status);
}

export function getSelectionStatusMeta(status: SelectionStatus | string) {
  return selectionStatusMap[status as SelectionStatus] ?? fallbackMeta(status);
}

export function getBoothStatusMeta(status: BoothStatus | string) {
  return boothStatusMap[status as BoothStatus] ?? fallbackMeta(status);
}

export function getEligibilityStatusMeta(status: EligibilityStatus | string) {
  return eligibilityStatusMap[status as EligibilityStatus] ?? fallbackMeta(status);
}

export function StatusTag({ meta }: { meta: StatusMeta }) {
  return <Tag color={meta.color}>{meta.label}</Tag>;
}
```

- [x] **Step 4.5: Create `apiLoaders.ts`**

Create a shared loader for the existing single-object eligibility endpoint so page code does not accidentally treat it as an array:

```ts
import type { SelectionEligibility } from './domain';
import { apiGet } from './request';

export async function loadSelectionEligibility(orderId: string): Promise<SelectionEligibility> {
  return apiGet<SelectionEligibility>(`/api/selection-eligibilities?orderId=${encodeURIComponent(orderId)}`);
}
```

This plan keeps the existing single-object API contract. Do not change page code to `apiGet<SelectionEligibility[]>` unless the backend endpoint is deliberately changed in the same task.

- [x] **Step 5: Extend `pageLogic.ts`**

Add imports:

```ts
import type { EligibilityStatus, OrderStatus, ParticipationOrder, SelectionStatus } from '../../app/domain';
```

Append:

```ts
export function getOrderStepIndex(status: OrderStatus | string) {
  if (status === 'PENDING_SALES_CONFIRM') return 1;
  if (status === 'PENDING_CONTRACT') return 2;
  if (status === 'PENDING_PAYMENT' || status === 'PARTIALLY_PAID') return 3;
  if (status === 'PAID') return 4;
  if (status === 'COMPLETED') return 5;
  return 0;
}

export function pickLatestActiveOrder(orders: ParticipationOrder[]) {
  const byCreatedDesc = [...orders].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
  return byCreatedDesc.find((order) => order.status !== 'COMPLETED') ?? byCreatedDesc[0];
}

export type OrderNextActionInput = {
  order?: ParticipationOrder;
  eligibilityStatus?: EligibilityStatus;
  latestSelectionStatus?: SelectionStatus;
};

export function getOrderNextAction(input: OrderNextActionInput) {
  const { order, eligibilityStatus, latestSelectionStatus } = input;
  if (!order) {
    return { label: '创建参展订单', to: '/exhibitor/orders/new', disabled: false, reason: '' };
  }
  if (order.status === 'PENDING_SALES_CONFIRM') {
    return { label: '等待销售确认', to: `/exhibitor/orders/${order.id}`, disabled: true, reason: '销售确认后才能继续' };
  }
  if (isOrderPayable(order.status)) {
    return { label: '提交付款', to: `/exhibitor/orders/${order.id}/payment`, disabled: false, reason: '' };
  }
  if (order.status === 'PAID' && eligibilityStatus === 'AVAILABLE') {
    return { label: '选择展位', to: `/exhibitor/orders/${order.id}/selection`, disabled: false, reason: '' };
  }
  if (latestSelectionStatus === 'PENDING_REVIEW') {
    return { label: '等待管理员审核', to: `/exhibitor/orders/${order.id}`, disabled: true, reason: '选位申请正在审核' };
  }
  if (latestSelectionStatus === 'CONFIRMED') {
    return { label: '查看选位结果', to: `/exhibitor/orders/${order.id}`, disabled: false, reason: '' };
  }
  return { label: '查看订单', to: `/exhibitor/orders/${order.id}`, disabled: false, reason: '' };
}
```

- [x] **Step 5.5: Add shared formatter and status tests**

After `formatters.ts` and `status.tsx` exist, extend `pageLogic.test.ts` imports:

```ts
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../app/formatters';
import { getOrderStatusMeta } from '../../app/status';
```

Append this test:

```ts
it('formats business values for product UI', () => {
  expect(formatShortId(baseOrder.id)).toBe('e0e5589c');
  expect(formatMoney('21600')).toBe('¥21,600.00');
  expect(formatArea('18')).toBe('18.00 ㎡');
  expect(formatFriendlyTime('2026-06-14T09:30:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('今天 09:30');
  expect(formatFriendlyTime('2026-06-13T20:15:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('昨天 20:15');
  expect(formatFriendlyTime('2026-06-10T08:05:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('2026-06-10 08:05');
  expect(getOrderStatusMeta('PENDING_PAYMENT')).toMatchObject({ label: '待付款' });
});
```

- [x] **Step 6: Run frontend logic tests**

Run:

```powershell
cd frontend
cmd /c npm test -- pageLogic
```

Expected: PASS.

- [x] **Step 7: Commit**

```powershell
git add frontend/src/app/domain.ts frontend/src/app/formatters.ts frontend/src/app/status.tsx frontend/src/app/apiLoaders.ts frontend/src/pages/exhibitor/pageLogic.ts frontend/src/pages/exhibitor/pageLogic.test.ts
git commit -m "feat: add shared product ui language"
```

---

## Task 3: Route Structure, Layout Navigation, and Desktop Guard

**Files:**
- Create: `frontend/src/app/MobileUnsupported.tsx`
- Create: `frontend/src/app/routeHelpers.ts`
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/app/routes.tsx`
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Modify: `frontend/src/app/layouts/ExhibitorLayout.tsx`

- [x] **Step 1: Create `MobileUnsupported.tsx`**

```tsx
import { Result } from 'antd';
import { useEffect, useState } from 'react';

function isMobileWidth() {
  return window.innerWidth < 768;
}

export default function MobileUnsupported({ children }: { children: React.ReactNode }) {
  const [mobile, setMobile] = useState(isMobileWidth);

  useEffect(() => {
    const onResize = () => setMobile(isMobileWidth());
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  if (mobile) {
    return (
      <Result
        status="info"
        title="请在电脑端访问展商系统"
        subTitle="当前版本面向展会运营和展商选位工作台，暂不支持手机端操作。"
      />
    );
  }

  return <>{children}</>;
}
```

- [x] **Step 2: Wrap the app with desktop guard**

In `App.tsx`, change the component to:

```tsx
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import MobileUnsupported from './MobileUnsupported';
import { routes } from './routes';

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <MobileUnsupported>
      <RouterProvider router={router} />
    </MobileUnsupported>
  );
}
```

- [x] **Step 3: Create route redirect helper**

Create `routeHelpers.ts`:

```tsx
import { Navigate, useSearchParams } from 'react-router-dom';

export function RedirectWithOrderId({ target }: { target: 'payment' | 'selection' | 'detail' }) {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId');

  if (!orderId) {
    return <Navigate to="/exhibitor/orders" replace state={{ message: '请先选择订单' }} />;
  }

  if (target === 'payment') {
    return <Navigate to={`/exhibitor/orders/${orderId}/payment`} replace />;
  }
  if (target === 'selection') {
    return <Navigate to={`/exhibitor/orders/${orderId}/selection`} replace />;
  }
  return <Navigate to={`/exhibitor/orders/${orderId}`} replace />;
}
```

- [x] **Step 4: Update routes**

In `routes.tsx`:

Add imports:

```tsx
import { Navigate } from 'react-router-dom';
import { RedirectWithOrderId } from './routeHelpers';
```

Add lazy pages:

```tsx
const ExhibitorOrderDetailPage = lazy(() => import('../pages/exhibitor/orders/ExhibitorOrderDetailPage'));
const AdminOrderDetailPage = lazy(() => import('../pages/admin/orders/AdminOrderDetailPage'));
```

Change admin index:

```tsx
{ index: true, element: <Navigate to="/admin/dashboard" replace /> },
{ path: 'dashboard', element: lazyPage(<DashboardPage />) },
```

Add admin order detail:

```tsx
{ path: 'orders/:orderId', element: lazyPage(<AdminOrderDetailPage />) },
```

Change exhibitor routes:

```tsx
{ index: true, element: lazyPage(<ExhibitorHomePage />) },
{ path: 'orders/new', element: lazyPage(<OrderCreatePage />) },
{ path: 'orders', element: lazyPage(<MyOrdersPage />) },
{ path: 'orders/:orderId', element: lazyPage(<ExhibitorOrderDetailPage />) },
{ path: 'orders/:orderId/payment', element: lazyPage(<PaymentSubmitPage />) },
{ path: 'orders/:orderId/selection', element: lazyPage(<BoothSelectionPage />) },
{ path: 'payments/new', element: <RedirectWithOrderId target="payment" /> },
{ path: 'selections/new', element: <RedirectWithOrderId target="selection" /> },
{ path: 'selections', element: <RedirectWithOrderId target="detail" /> },
```

- [x] **Step 5: Update admin layout menu grouping**

Replace `route.routes` in `AdminLayout.tsx` with:

```tsx
routes: [
  { path: '/admin/dashboard', name: '工作台' },
  {
    path: '/admin/business',
    name: '业务处理',
    routes: [
      { path: '/admin/orders', name: '订单管理' },
      { path: '/admin/contracts', name: '合同管理' },
      { path: '/admin/payments', name: '付款确认' },
      { path: '/admin/selections', name: '选位审核' },
    ],
  },
  {
    path: '/admin/config',
    name: '资源配置',
    routes: [
      { path: '/admin/events', name: '展会管理' },
      { path: '/admin/booths', name: '展位管理' },
      { path: '/admin/area-tiers', name: '面积档位' },
      { path: '/admin/price-rules', name: '价格规则' },
    ],
  },
],
```

Keep `menuItemRender`; guard it so only leaf items navigate:

```tsx
menuItemRender={(item, dom) => (
  <a onClick={() => item.path && !item.routes && navigate(item.path)}>{dom}</a>
)}
```

- [x] **Step 6: Update exhibitor layout menu**

Replace `menuItems` with:

```tsx
const menuItems = [
  { key: '/exhibitor', label: '首页' },
  { key: '/exhibitor/orders/new', label: '我要参展' },
  { key: '/exhibitor/orders', label: '我的订单' },
];
```

- [x] **Step 7: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [x] **Step 8: Commit**

```powershell
git add frontend/src/app
git commit -m "feat: restructure product navigation"
```

---

## Task 4: Exhibitor Home and My Orders Productization

**Files:**
- Modify: `frontend/src/pages/exhibitor/HomePage.tsx`
- Modify: `frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx`

- [x] **Step 1: Replace exhibitor home**

Implement `HomePage.tsx` with:

```tsx
import { PageContainer, ProCard, ProList } from '@ant-design/pro-components';
import { Button, Empty, Skeleton, Space, Statistic, Steps, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import type { BoothSelection, PaymentRecord, ParticipationOrder, SelectionEligibility } from '../../app/domain';
import { formatArea, formatMoney, formatShortId } from '../../app/formatters';
import { loadSelectionEligibility } from '../../app/apiLoaders';
import { apiGet } from '../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../app/status';
import { getOrderNextAction, getOrderStepIndex, pickLatestActiveOrder } from './pageLogic';

export default function HomePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);
  const [eligibility, setEligibility] = useState<SelectionEligibility>();

  const activeOrder = pickLatestActiveOrder(orders);
  const latestSelection = selections[0];
  const nextAction = getOrderNextAction({
    order: activeOrder,
    eligibilityStatus: eligibility?.status,
    latestSelectionStatus: latestSelection?.status,
  });

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      try {
        const loadedOrders = await apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
        if (!alive) return;
        setOrders(loadedOrders);
        const selectedOrder = pickLatestActiveOrder(loadedOrders);
        if (!selectedOrder) {
          setPayments([]);
          setSelections([]);
          setEligibility(undefined);
          return;
        }
        const [loadedPayments, loadedSelections, loadedEligibility] = await Promise.all([
          apiGet<PaymentRecord[]>(`/api/payments?orderId=${selectedOrder.id}`),
          apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${selectedOrder.id}`),
          loadSelectionEligibility(selectedOrder.id),
        ]);
        if (!alive) return;
        setPayments(loadedPayments);
        setSelections(loadedSelections);
        setEligibility(loadedEligibility);
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  if (loading) {
    return (
      <PageContainer title="首页">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (!activeOrder) {
    return (
      <PageContainer title="首页">
        <ProCard>
          <Empty description="您还没有参展订单">
            <Button type="primary" onClick={() => navigate('/exhibitor/orders/new')}>我要参展</Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="首页">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard split="vertical">
          <ProCard title={`订单 ${formatShortId(activeOrder.id)}`}>
            <Space direction="vertical" size={12}>
              <StatusTag meta={getOrderStatusMeta(activeOrder.status)} />
              <Typography.Text>面积 {formatArea(activeOrder.areaSqm)} · {activeOrder.boothType}</Typography.Text>
              <Button type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>
                {nextAction.label}
              </Button>
            </Space>
          </ProCard>
          <ProCard>
            <Statistic title="订单金额" value={formatMoney(activeOrder.totalAmount)} />
          </ProCard>
        </ProCard>
        <ProCard title="参展进度">
          <Steps
            current={getOrderStepIndex(activeOrder.status)}
            items={[
              { title: '下单' },
              { title: '销售确认' },
              { title: '合同' },
              { title: '付款' },
              { title: '选位' },
              { title: '完成' },
            ]}
          />
        </ProCard>
        <ProCard split="vertical">
          <ProCard title="最近付款">
            <ProList<PaymentRecord>
              rowKey="id"
              dataSource={payments.slice(0, 3)}
              locale={{ emptyText: '暂无付款记录' }}
              metas={{
                title: { render: (_, record) => formatMoney(record.amount) },
                description: { render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} /> },
              }}
            />
          </ProCard>
          <ProCard title="最近选位">
            <ProList<BoothSelection>
              rowKey="id"
              dataSource={selections.slice(0, 3)}
              locale={{ emptyText: '暂无选位记录' }}
              metas={{
                title: { render: (_, record) => `选位 ${formatShortId(record.id)}` },
                description: { render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} /> },
              }}
            />
          </ProCard>
        </ProCard>
      </Space>
    </PageContainer>
  );
}
```

Performance note for this V1:

```text
The home page intentionally loads the exhibitor order list first to choose the latest active order, then loads only that order's related data. Do not load related payments/selections/eligibility for all orders. If an exhibitor has many orders, the next iteration should replace this with a backend dashboard aggregation endpoint.
```

- [x] **Step 2: Productize My Orders**

In `MyOrdersPage.tsx`:

- Use `ParticipationOrder` from `domain.ts`.
- Add `useLocation` and show `location.state?.message` once via `message.info`.
- Replace raw status with `StatusTag`.
- Replace money and area display with formatters.
- Add `查看详情` action.
- Change payment and selection links to nested routes.
- Add table empty text.

The operation render must follow this shape:

```tsx
render: (_, record) => {
  const nextAction = getOrderNextAction({ order: record });
  return (
    <Space size={8}>
      <Button type="link" size="small" onClick={() => navigate(`/exhibitor/orders/${record.id}`)}>
        查看详情
      </Button>
      <Button
        type="link"
        size="small"
        disabled={nextAction.disabled}
        onClick={() => navigate(nextAction.to)}
      >
        {nextAction.label}
      </Button>
    </Space>
  );
}
```

This list-level action intentionally calls `getOrderNextAction({ order: record })` without eligibility or latest selection details. The list should stay fast and show a generic next action; the order detail page is responsible for precise selection eligibility and selection history decisions.

- [x] **Step 3: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [x] **Step 4: Commit**

```powershell
git add frontend/src/pages/exhibitor/HomePage.tsx frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx
git commit -m "feat: productize exhibitor home and orders"
```

---

## Task 5: Exhibitor Order Detail and Payment Child Flow

**Files:**
- Create: `frontend/src/pages/exhibitor/orders/ExhibitorOrderDetailPage.tsx`
- Modify: `frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx`

- [x] **Step 1: Create exhibitor order detail page**

Create `ExhibitorOrderDetailPage.tsx`:

```tsx
import { PageContainer, ProCard, ProDescriptions, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, Empty, Skeleton, Space, Steps, Tabs, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { BoothSelection, ContractRecord, PaymentRecord, ParticipationOrder, SelectionEligibility } from '../../../app/domain';
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { loadSelectionEligibility } from '../../../app/apiLoaders';
import { apiGet } from '../../../app/request';
import { getContractStatusMeta, getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../../app/status';
import { getOrderNextAction, getOrderStepIndex, isOrderPayable } from '../pageLogic';

export default function ExhibitorOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<ParticipationOrder>();
  const [contracts, setContracts] = useState<ContractRecord[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);
  const [eligibility, setEligibility] = useState<SelectionEligibility>();

  useEffect(() => {
    let alive = true;
    async function load() {
      if (!orderId) return;
      setLoading(true);
      try {
        const [loadedOrder, loadedContracts, loadedPayments, loadedSelections, loadedEligibility] = await Promise.all([
          apiGet<ParticipationOrder>(`/api/participation-orders/${orderId}`),
          apiGet<ContractRecord[]>(`/api/contracts?orderId=${orderId}`),
          apiGet<PaymentRecord[]>(`/api/payments?orderId=${orderId}`),
          apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${orderId}`),
          loadSelectionEligibility(orderId),
        ]);
        if (!alive) return;
        setOrder(loadedOrder);
        setContracts(loadedContracts);
        setPayments(loadedPayments);
        setSelections(loadedSelections);
        setEligibility(loadedEligibility);
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();
    return () => { alive = false; };
  }, [orderId]);

  if (loading) {
    return <PageContainer title="订单详情"><Skeleton active paragraph={{ rows: 10 }} /></PageContainer>;
  }

  if (!order) {
    return <PageContainer title="订单详情"><Empty description="未找到订单" /></PageContainer>;
  }

  const latestSelection = selections[0];
  const nextAction = getOrderNextAction({
    order,
    eligibilityStatus: eligibility?.status,
    latestSelectionStatus: latestSelection?.status,
  });

  const paymentColumns: ProColumns<PaymentRecord>[] = [
    { title: '金额', dataIndex: 'amount', renderText: (value) => formatMoney(value as string | number) },
    { title: '状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} /> },
    { title: '方式', dataIndex: 'method' },
    { title: '提交时间', dataIndex: 'createdAt', renderText: (value) => formatFriendlyTime(value as string) },
  ];

  const selectionColumns: ProColumns<BoothSelection>[] = [
    { title: '选位', dataIndex: 'id', renderText: (value) => formatShortId(String(value)) },
    { title: '状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} /> },
    { title: '面积', dataIndex: 'totalAreaSqm', renderText: (value) => formatArea(value as string | number) },
    { title: '提交时间', dataIndex: 'createdAt', renderText: (value) => formatFriendlyTime(value as string) },
  ];

  return (
    <PageContainer
      title={`订单 ${formatShortId(order.id)}`}
      extra={[
        <Button key="copy" onClick={() => navigator.clipboard.writeText(order.id).then(() => message.success('订单 ID 已复制'))}>复制完整 ID</Button>,
        <Button key="next" type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>{nextAction.label}</Button>,
      ]}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard>
          <Space size={16} wrap>
            <StatusTag meta={getOrderStatusMeta(order.status)} />
            <Typography.Text>{formatArea(order.areaSqm)}</Typography.Text>
            <Typography.Text>{order.boothType}</Typography.Text>
            <Typography.Text strong>{formatMoney(order.totalAmount)}</Typography.Text>
          </Space>
        </ProCard>
        <ProCard>
          <Steps current={getOrderStepIndex(order.status)} items={[
            { title: '下单' },
            { title: '销售确认' },
            { title: '合同' },
            { title: '付款' },
            { title: '选位' },
            { title: '完成' },
          ]} />
        </ProCard>
        <Tabs
          items={[
            {
              key: 'basic',
              label: '基本信息',
              children: (
                <ProDescriptions column={2} dataSource={order} columns={[
                  { title: '展会 ID', dataIndex: 'eventId', copyable: true },
                  { title: '展商 ID', dataIndex: 'exhibitorId', copyable: true },
                  { title: '面积', dataIndex: 'areaSqm', renderText: (value) => formatArea(value as string | number) },
                  { title: '总金额', dataIndex: 'totalAmount', renderText: (value) => formatMoney(value as string | number) },
                  { title: '创建时间', dataIndex: 'createdAt', renderText: (value) => formatFriendlyTime(value as string) },
                ]} />
              ),
            },
            {
              key: 'contract',
              label: '合同',
              children: contracts.length ? contracts.map((contract) => (
                <ProCard key={contract.id} title={contract.contractNo}>
                  <StatusTag meta={getContractStatusMeta(contract.status)} />
                </ProCard>
              )) : <Empty description="暂无合同记录" />,
            },
            {
              key: 'payment',
              label: '付款',
              children: (
                <ProTable<PaymentRecord>
                  rowKey="id"
                  search={false}
                  options={false}
                  columns={paymentColumns}
                  dataSource={payments}
                  locale={{ emptyText: '暂无付款记录' }}
                  toolBarRender={() => isOrderPayable(order.status) ? [
                    <Button key="pay" type="primary" onClick={() => navigate(`/exhibitor/orders/${order.id}/payment`)}>提交付款</Button>,
                  ] : []}
                />
              ),
            },
            {
              key: 'selection',
              label: '选位',
              children: (
                <ProTable<BoothSelection>
                  rowKey="id"
                  search={false}
                  options={false}
                  columns={selectionColumns}
                  dataSource={selections}
                  locale={{ emptyText: '暂无选位记录' }}
                  toolBarRender={() => order.status === 'PAID' && eligibility?.status === 'AVAILABLE' ? [
                    <Button key="select" type="primary" onClick={() => navigate(`/exhibitor/orders/${order.id}/selection`)}>选择展位</Button>,
                  ] : []}
                />
              ),
            },
          ]}
        />
      </Space>
    </PageContainer>
  );
}
```

- [x] **Step 2: Convert payment page to route-based child flow**

In `PaymentSubmitPage.tsx`:

- Read `orderId` with `useParams`.
- Load order detail with `/api/participation-orders/{orderId}`.
- Load payment records with `/api/payments?orderId={orderId}`.
- Remove editable order ID field.
- On submit, send `{ orderId, amount, method }`.
- On success, navigate to `/exhibitor/orders/${orderId}`.
- Keep `ProFormDigit` validation for two decimal places.

The submit body must be:

```ts
{
  orderId,
  amount: values.amount,
  method: values.method,
}
```

- [x] **Step 3: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [x] **Step 4: Commit**

```powershell
git add frontend/src/pages/exhibitor/orders/ExhibitorOrderDetailPage.tsx frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx
git commit -m "feat: add exhibitor order detail flow"
```

---

## Task 6: Exhibitor Booth Selection Map and Nested Selection Flow

**Files:**
- Modify: `frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx`
- Modify: `frontend/src/pages/exhibitor/selections/SelectionResultPage.tsx`
- Modify: `frontend/src/pages/exhibitor/pageLogic.ts`
- Modify: `frontend/src/pages/exhibitor/pageLogic.test.ts`

- [x] **Step 1: Add map selection helper tests**

Extend the existing `pageLogic.test.ts` import from `./pageLogic` to include `sortBoothsForMap`, then append the test below:

```ts
it('sorts booths by booth number for the simplified map', () => {
  expect(sortBoothsForMap([
    { id: '2', boothNo: 'A010' },
    { id: '1', boothNo: 'A002' },
  ])).toEqual([
    { id: '1', boothNo: 'A002' },
    { id: '2', boothNo: 'A010' },
  ]);
});
```

Expected before implementation: FAIL because `sortBoothsForMap` does not exist.

- [x] **Step 2: Add map sorting helper**

Append to `pageLogic.ts`:

```ts
export function sortBoothsForMap<T extends { boothNo: string }>(booths: T[]) {
  return [...booths].sort((left, right) => left.boothNo.localeCompare(right.boothNo, 'zh-CN', { numeric: true }));
}
```

- [x] **Step 3: Convert booth selection page to nested route**

In `BoothSelectionPage.tsx`:

- Read `orderId` from `useParams`.
- Load order through `/api/participation-orders/{orderId}`.
- Remove the manual order ID form.
- Load booths with `/api/booths?eventId={order.eventId}&status=AVAILABLE`.
- Keep table selection and area summary.
- Add confirmation modal before `apiPost('/api/booth-selections', ...)`.
- Navigate back to `/exhibitor/orders/{orderId}` after success.

- [x] **Step 4: Add simplified booth map**

Inside `BoothSelectionPage.tsx`, add a map block above the table:

```tsx
<div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(112px, 1fr))', gap: 8 }}>
  {sortBoothsForMap(booths).map((booth) => {
    const selected = selectedRowKeys.includes(booth.id);
    return (
      <Button
        key={booth.id}
        title={`${booth.boothNo} / ${formatArea(booth.areaSqm)}`}
        type={selected ? 'primary' : 'default'}
        onClick={() => {
          setSelectedRowKeys((current) =>
            current.includes(booth.id)
              ? current.filter((key) => key !== booth.id)
              : [...current, booth.id],
          );
        }}
        style={{ minHeight: 72, whiteSpace: 'normal', overflow: 'hidden' }}
      >
        <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{booth.boothNo}</div>
        <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{formatArea(booth.areaSqm)}</div>
      </Button>
    );
  })}
</div>
```

Use `formatArea` from `app/formatters`.

- [x] **Step 5: Turn selection result page into redirect/helper**

In `SelectionResultPage.tsx`, show a friendly result instead of an order ID form:

```tsx
import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function SelectionResultPage() {
  const navigate = useNavigate();
  return (
    <Result
      status="info"
      title="请从订单详情查看选位结果"
      subTitle="选位结果已经合并到订单详情页，便于同时查看合同、付款和选位状态。"
      extra={<Button type="primary" onClick={() => navigate('/exhibitor/orders')}>选择订单</Button>}
    />
  );
}
```

- [x] **Step 6: Run tests and build**

Run:

```powershell
cd frontend
cmd /c npm test -- pageLogic
cmd /c npm run build
```

Expected: both PASS.

- [x] **Step 7: Commit**

```powershell
git add frontend/src/pages/exhibitor/selections frontend/src/pages/exhibitor/pageLogic.ts frontend/src/pages/exhibitor/pageLogic.test.ts
git commit -m "feat: add order-based booth selection map"
```

---

## Task 7: Admin Dashboard, Order List, and Order Detail

**Files:**
- Create: `frontend/src/pages/admin/dashboard/dashboardLogic.ts`
- Create: `frontend/src/pages/admin/dashboard/dashboardLogic.test.ts`
- Modify: `frontend/src/pages/admin/DashboardPage.tsx`
- Modify: `frontend/src/pages/admin/orders/AdminOrderListPage.tsx`
- Create: `frontend/src/pages/admin/orders/AdminOrderDetailPage.tsx`

- [x] **Step 1: Add dashboard logic tests**

Create `dashboardLogic.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import type { ParticipationOrder } from '../../../app/domain';
import { countTodayOrders } from './dashboardLogic';

const baseOrder: ParticipationOrder = {
  id: 'order-1',
  eventId: 'event-1',
  exhibitorId: 'exhibitor-1',
  zoneId: null,
  areaTierId: 'tier-1',
  areaSqm: '18.00',
  boothType: 'STANDARD',
  status: 'PENDING_SALES_CONFIRM',
  areaFee: '21600.00',
  serviceFee: '0.00',
  discountAmount: '0.00',
  adjustmentAmount: '0.00',
  totalAmount: '21600.00',
  createdAt: '2026-06-14T09:00:00+08:00',
  updatedAt: '2026-06-14T09:00:00+08:00',
};

describe('dashboard logic', () => {
  it('counts orders created on the current local date', () => {
    expect(countTodayOrders([
      baseOrder,
      { ...baseOrder, id: 'order-2', createdAt: '2026-06-13T23:59:00+08:00' },
    ], new Date('2026-06-14T12:00:00+08:00'))).toBe(1);
  });
});
```

- [x] **Step 2: Implement dashboard logic**

Create `dashboardLogic.ts`:

```ts
import type { ParticipationOrder } from '../../../app/domain';

export function countTodayOrders(orders: ParticipationOrder[], now = new Date()) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const end = start + 24 * 60 * 60 * 1000;
  return orders.filter((order) => {
    const created = Date.parse(order.createdAt);
    return created >= start && created < end;
  }).length;
}
```

- [x] **Step 3: Implement real admin dashboard**

Replace `DashboardPage.tsx` with a page that:

- Loads:
  - `/api/participation-orders?status=PENDING_SALES_CONFIRM`
  - `/api/participation-orders`
  - `/api/payments?status=PENDING_FINANCE_CONFIRM`
  - `/api/booth-selections?status=PENDING_REVIEW`
- Shows four `Statistic` KPIs.
- Uses `Skeleton` while loading.
- Shows three compact task lists with buttons linking to:
  - `/admin/orders`
  - `/admin/payments`
  - `/admin/selections`

Use `ProCard`, `ProList`, `Statistic`, `Button`, `Space`, and shared formatters/status tags.

Performance note for this V1:

```text
The dashboard uses list APIs and calculates counts client-side. Render only the first 100 orders in compact dashboard lists. If the order volume grows beyond that, replace `/api/participation-orders` with a backend count or dashboard aggregation endpoint.
```

- [x] **Step 4: Productize admin order list**

In `AdminOrderListPage.tsx`:

- Use `ParticipationOrder` type.
- Remove any create-order toolbar.
- Add `查看详情` action linking to `/admin/orders/${record.id}`.
- Show `销售确认` only for `PENDING_SALES_CONFIRM`.
- Wrap sales confirmation in `Modal.confirm`.
- Use `StatusTag`, `formatMoney`, `formatArea`, and `formatShortId`.

Confirmation code shape:

```tsx
Modal.confirm({
  title: '确认销售审核通过？',
  content: `订单 ${formatShortId(record.id)} 确认后将生成合同。`,
  okText: '确认',
  cancelText: '取消',
  onOk: async () => {
    await apiPost<ParticipationOrder>(`/api/participation-orders/${record.id}/confirm`);
    message.success('销售确认已完成');
    actionRef.current?.reload();
  },
});
```

- [x] **Step 5: Create admin order detail page**

Create `AdminOrderDetailPage.tsx` using the same data-loading pattern as exhibitor detail, with:

- Header summary and sales confirm action.
- `Tabs`: `基本信息`, `合同`, `付款`, `选位`, `操作时间线`.
- `ProDescriptions` for basic info.
- `ProTable` for contracts, payments, selections.
- `Timeline` built from available contract/payment/selection timestamps.

The page must load:

```text
GET /api/participation-orders/{orderId}
GET /api/contracts?orderId={orderId}
GET /api/payments?orderId={orderId}
GET /api/booth-selections?orderId={orderId}
```

- [x] **Step 6: Run dashboard tests and build**

Run:

```powershell
cd frontend
cmd /c npm test -- dashboardLogic
cmd /c npm run build
```

Expected: both PASS.

- [x] **Step 7: Commit**

```powershell
git add frontend/src/pages/admin/DashboardPage.tsx frontend/src/pages/admin/dashboard frontend/src/pages/admin/orders
git commit -m "feat: productize admin dashboard and orders"
```

---

## Task 8: Admin Action Pages and Configuration Empty States

**Files:**
- Modify: `frontend/src/pages/admin/payments/PaymentConfirmPage.tsx`
- Modify: `frontend/src/pages/admin/selections/SelectionReviewPage.tsx`
- Modify: `frontend/src/pages/admin/contracts/ContractListPage.tsx`
- Modify: `frontend/src/pages/admin/events/EventListPage.tsx`
- Modify: `frontend/src/pages/admin/booths/BoothListPage.tsx`
- Modify: `frontend/src/pages/admin/area-tiers/AreaTierListPage.tsx`
- Modify: `frontend/src/pages/admin/price-rules/PriceRuleListPage.tsx`

- [ ] **Step 1: Add confirmation modals and loading to payment actions**

In `PaymentConfirmPage.tsx`:

- Import `Modal`.
- Track row loading:

```tsx
const [loadingId, setLoadingId] = useState<string>();
```

- Wrap confirm/fail actions with `Modal.confirm`.
- Set `loading={loadingId === record.id}` on row buttons.
- Use shared status/money/time formatters.
- Add `locale={{ emptyText: '当前没有待确认付款' }}` to `ProTable`.

- [ ] **Step 2: Add confirmation modals and loading to selection actions**

In `SelectionReviewPage.tsx`:

- Import `Modal`.
- Track row loading.
- Wrap `审核通过` and `超时释放` with `Modal.confirm`.
- Keep reject as `ModalForm`, but set submit loading through local state.
- Use shared status/area/time formatters.
- Add `locale={{ emptyText: '当前没有待审核选位' }}`.

- [ ] **Step 3: Productize contract actions**

In `ContractListPage.tsx`:

- Wrap `标记线下签署` in `Modal.confirm`.
- Use `StatusTag` and `formatFriendlyTime`.
- Add `locale={{ emptyText: '暂无合同记录' }}`.

- [ ] **Step 4: Add configuration empty states**

For each configuration table:

- `EventListPage`: `locale={{ emptyText: '先创建展会，再配置展位、面积档位和价格规则' }}`
- `BoothListPage`: `locale={{ emptyText: '当前展会还没有展位，请先创建展位' }}`
- `AreaTierListPage`: `locale={{ emptyText: '配置展商可购买的标准面积档位' }}`
- `PriceRuleListPage`: `locale={{ emptyText: '配置不同展位类型的每平方米价格' }}`

Keep existing `ModalForm` create actions.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/pages/admin
git commit -m "feat: add admin confirmations and empty states"
```

---

## Task 9: Full Verification and Product Smoke

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run all backend tests**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: `Tests run: 34` or more, `Failures: 0`, `Errors: 0`.

- [ ] **Step 2: Run frontend tests**

Run:

```powershell
cd frontend
cmd /c npm test -- pageLogic
cmd /c npm test -- dashboardLogic
```

Expected: all listed tests PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS. Existing Vite chunk-size warnings are acceptable.

- [ ] **Step 4: Run API smoke chain**

Use the existing smoke flow from `README.md` against `APP_DB_SCHEMA=exhibitor_sys`.

Expected final state:

```text
order.status=PAID
selectionEligibility.status=CLOSED
boothSelection.status=CONFIRMED
selected booths status=CONFIRMED
```

- [ ] **Step 5: Run UI smoke**

With backend and frontend running:

```text
1. Open /exhibitor and verify it shows an empty state or latest order dashboard.
2. Open /exhibitor/orders and verify top-level actions are 查看详情 / 下一步, not raw route fragments.
3. Open /exhibitor/orders/{orderId} and verify tabs render.
4. Open /exhibitor/orders/{orderId}/payment and verify no order ID input is required.
5. Open /exhibitor/orders/{orderId}/selection and verify map tiles and table both render.
6. Open /admin/dashboard and verify pending counts are not static zeros.
7. Open /admin/orders/{orderId} and verify tabs render.
8. Open /admin/payments and /admin/selections and verify dangerous actions show confirmation before request.
9. Resize the browser below 768px width and verify the desktop-only Result appears with `请在电脑端访问展商系统`.
```

- [ ] **Step 6: Update README productized routes**

Add this section:

```markdown
## Productized UI routes

Exhibitor:

- `/exhibitor` - home dashboard and next action
- `/exhibitor/orders/new` - create participation order
- `/exhibitor/orders` - order list
- `/exhibitor/orders/{orderId}` - order detail
- `/exhibitor/orders/{orderId}/payment` - submit payment for one order
- `/exhibitor/orders/{orderId}/selection` - select booths for one order

Admin:

- `/admin/dashboard` - pending work dashboard
- `/admin/orders` - order review list
- `/admin/orders/{orderId}` - order detail
- `/admin/payments` - payment confirmation
- `/admin/selections` - booth selection review
```

- [ ] **Step 7: Commit**

```powershell
git add README.md
git commit -m "docs: document productized ui routes"
```

---

## Self-Review Checklist

- [ ] Backend exposes `GET /api/participation-orders/{orderId}`.
- [ ] Order responses include `createdAt` and `updatedAt`.
- [ ] Exhibitor top-level menu is exactly `首页`, `我要参展`, `我的订单`.
- [ ] Legacy exhibitor payment and selection URLs redirect to order-centered routes.
- [ ] Exhibitor home uses Skeleton during loading and Empty when no orders exist.
- [ ] Payment and selection pages do not require users to paste an order ID.
- [ ] Booth selection includes both simplified map tiles and ProTable.
- [ ] Admin menu separates `业务处理` from `资源配置`.
- [ ] Admin dashboard uses live API data, not hard-coded zero values.
- [ ] Admin order list has no create-order action.
- [ ] Admin and exhibitor order details use Tabs.
- [ ] Dangerous admin actions use confirmation modals and loading states.
- [ ] Status, money, area, time, and short-ID formatting use shared helpers.
- [ ] Every list page has an explicit empty state.
- [ ] Mobile viewport shows the desktop-only Result.
- [ ] Backend tests, frontend logic tests, and frontend build pass.

## Known Deferrals After This Plan

- Real authentication and RBAC replace demo actors.
- Exhibitor company profile pages replace raw exhibitor IDs.
- Event detail APIs replace raw event IDs.
- Dedicated dashboard aggregation endpoint replaces multiple home-page requests.
- Full graphical booth map editor remains out of scope.
- Mobile responsive list/card mode remains out of scope.
