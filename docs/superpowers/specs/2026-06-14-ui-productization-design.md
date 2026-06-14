# UI Productization Design

## 1. Goal

Turn the current runnable engineering demo into a usable V1 product surface for the already implemented business loop:

```text
展会配置 -> 展商下单 -> 销售确认 -> 合同线下签署 -> 付款确认 -> 展商选位 -> 管理员审核
```

This design does not add real authentication, registration approval, invoice management, service center, refund processing, or production RBAC. It productizes the current second-round scope so exhibitors and administrators can understand where they are, what needs action, and what happens next.

## 2. Product Principles

- The exhibitor experience is order-centered. Payment, booth selection, and selection results are stages inside one order, not separate top-level destinations.
- The administrator experience is task-centered. Daily work starts from a dashboard of pending actions, not from a flat list of configuration pages.
- Ant Design and ProComponents remain the strict UI foundation. Use `ProLayout`, `PageContainer`, `ProTable`, `ProDescriptions`, `Steps`, `Result`, `Empty`, `Modal`, `Drawer`, `Statistic`, `Tag`, `Alert`, `Timeline`, `Card`, and `Button`; do not invent custom base table, form, or modal components.
- UUIDs are technical identifiers. They can be copied from details, but they must not be the primary visual language of list pages.
- Status, money, and time must be shown in business language.

## 3. Information Architecture

### 3.1 Admin

Admin navigation is grouped by work type:

```text
工作台
业务处理
  订单管理
  合同管理
  付款确认
  选位审核
资源配置
  展会管理
  展位管理
  面积档位
  价格规则
```

Default route:

```text
/admin -> /admin/dashboard
```

Admin routes:

```text
/admin/dashboard
/admin/orders
/admin/orders/:orderId
/admin/contracts
/admin/payments
/admin/selections
/admin/events
/admin/booths
/admin/area-tiers
/admin/price-rules
```

`订单管理` must not show a create-order button. Orders are created by exhibitors in this V1. Admins confirm, inspect, and handle downstream tasks.

### 3.2 Exhibitor

Exhibitor navigation is reduced to the user's mental model:

```text
首页
我要参展
我的订单
```

Payment, booth selection, and selection history are accessed from an order detail page.

Exhibitor routes:

```text
/exhibitor
/exhibitor/orders/new
/exhibitor/orders
/exhibitor/orders/:orderId
/exhibitor/orders/:orderId/payment
/exhibitor/orders/:orderId/selection
```

The existing standalone routes may remain as compatibility redirects:

```text
/exhibitor/payments/new?orderId=... -> /exhibitor/orders/:orderId/payment
/exhibitor/selections/new?orderId=... -> /exhibitor/orders/:orderId/selection
/exhibitor/selections?orderId=... -> /exhibitor/orders/:orderId
```

If no `orderId` is provided, redirect to `/exhibitor/orders` with an inline message asking the user to choose an order first.

## 4. Shared UI Language

### 4.1 Status Labels

Render backend statuses as Chinese labels with Ant Design `Tag` colors.

Order:

```text
PENDING_SALES_CONFIRM -> 待销售确认
PENDING_CONTRACT -> 待生成合同
PENDING_PAYMENT -> 待付款
PARTIALLY_PAID -> 部分付款
PAID -> 已付款
COMPLETED -> 已完成
```

Payment:

```text
PENDING_FINANCE_CONFIRM -> 待财务确认
CONFIRMED -> 已确认到账
CONFIRM_FAILED -> 确认失败
```

Contract:

```text
GENERATED -> 已生成
EFFECTIVE -> 已生效
UNSIGNED -> 待线下签署
MARKED_SIGNED -> 已标记签署
```

Selection:

```text
PENDING_REVIEW -> 待审核
CONFIRMED -> 已确认
REJECTED -> 已驳回
TIMEOUT -> 已超时释放
```

Booth:

```text
AVAILABLE -> 可选
LOCKED -> 锁定中
CONFIRMED -> 已确认
RESERVED -> 预留
DISABLED -> 禁用
```

### 4.2 Formatting

- Money: `¥21,600.00`.
- Area: `18.00 ㎡`.
- Time:
  - Today: `今天 11:51`
  - Yesterday: `昨天 14:30`
  - Earlier: `2026-06-10 09:00`
- IDs:
  - Lists show shortened IDs such as `e0e5589c`.
  - Details show full IDs with a copy action.

### 4.3 Empty States

Every list page must define an explicit empty state:

- Exhibitor home with no order: guide to `我要参展`.
- My orders with no order: `您还没有参展订单` plus `去下单`.
- Selection history with no record: `暂无选位记录`.
- Admin pending lists with no records: `当前没有待处理事项`.
- Configuration pages with no data: explain the first setup action, then show the create button.

### 4.4 Confirmations and Loading

The following actions require confirmation:

```text
销售确认订单
标记合同线下签署
确认付款
确认失败
审核通过选位
驳回选位
超时释放
```

Every action button must show loading while the request is in flight. Success must show a concise `message.success` and refresh affected data. Failure must show the backend `ApiResponse.message` when available.

## 5. Exhibitor Experience

### 5.1 Exhibitor Home

The exhibitor home becomes a working dashboard, not a static step card.

Content:

- Primary order summary: latest active order, status, total amount, area, booth type.
- Next action panel:
  - No order: `创建参展订单`.
  - Pending sales confirmation: `等待销售确认`.
  - Pending payment: `提交付款`.
  - Paid and selection available: `选择展位`.
  - Selection pending review: `等待管理员审核`.
  - Selection confirmed: `查看选位结果`.
- Flow progress using `Steps`.
- Recent selections and payments as compact lists.

Data source:

- `GET /api/participation-orders?exhibitorId=DEMO_EXHIBITOR_ID`
- For the selected latest order:
  - `GET /api/payments?orderId=...`
  - `GET /api/booth-selections?orderId=...`
  - `GET /api/selection-eligibilities?orderId=...`

Performance tradeoff:

- V1 uses existing list APIs, so the home page performs one order-list request and up to three related-data requests for the latest active order.
- This is acceptable for the current implementation scope, but the page must show `Skeleton` loading blocks for the summary, next action, and recent activity areas to avoid a white-screen feel.
- A future backend aggregation endpoint such as `GET /api/exhibitors/{exhibitorId}/dashboard` can replace these calls, but it is not part of this round.

Latest active order rule:

```text
Prefer the newest order whose status is not COMPLETED.
If all orders are completed, show the newest order.
```

### 5.2 My Orders

`我的订单` becomes a scan-friendly order list.

Required display:

- Order identity: shortened order ID and event ID.
- Business fields: area, booth type, total amount.
- Status tag and next action.
- Row click or `查看详情` enters `/exhibitor/orders/:orderId`.

Actions:

- Pending payment: `去付款`.
- Paid and eligibility available: `去选位`.
- Pending review: `查看审核状态`.
- Confirmed selection: `查看展位`.

Disabled actions must show a reason in tooltip text.

### 5.3 Exhibitor Order Detail

`/exhibitor/orders/:orderId` is the center of the exhibitor workflow.

Layout:

- Use `Tabs` for the main detail sections: `基本信息`, `合同`, `付款`, `选位`.
- Keep the header and progress `Steps` above the tabs so the current stage and next action remain visible.
- Each tab may use `Card` or `ProCard` internally, but cards must not be nested inside other cards.

Sections:

1. Header
   - Order short ID
   - Status tag
   - Next action button
   - Copy full order ID
2. Progress
   - Steps: 下单 -> 销售确认 -> 合同 -> 付款 -> 选位 -> 完成
3. Basic information
   - Event ID, area, booth type, total amount, created time
4. Contract
   - Contract number, contract status, latest sign status
5. Payment
   - Payment records table
   - Submit payment button when order is payable
6. Booth selection
   - Eligibility status
   - Latest selection status
   - Selection history table
   - Select booth button when selectable

The page must not require the user to paste an order ID. It gets the ID from the route.

### 5.4 Payment Page

`/exhibitor/orders/:orderId/payment` is a child flow of the order.

Behavior:

- Load the order from the exhibitor's order list and verify it belongs to `DEMO_EXHIBITOR_ID`.
- Show order amount and current payment records.
- Default payment method: `OFFLINE_BANK`.
- Amount input:
  - Required.
  - Minimum `0.01`.
  - Two decimal places.
- On success, navigate back to `/exhibitor/orders/:orderId`.

### 5.5 Booth Selection Page

`/exhibitor/orders/:orderId/selection` is a child flow of the order.

Behavior:

- Load order by route ID.
- Load selectable booths with `GET /api/booths?eventId=...&status=AVAILABLE`.
- Filter by order `boothType`.
- Enforce strict area equality in the UI, while backend remains source of truth.

Layout:

- Left or top: order requirement summary, selected area, target area, submit button.
- Main area: simplified booth map plus table.
- The map is a simple selectable grid, not a visual editor:
  - Each booth is a tile.
  - Tile shows booth number and area.
  - Color reflects status.
  - Selected booths show a checked state.
  - Tiles are sorted by booth number.
- Table remains available for search and precise selection.

Submit:

- Disabled until selected area equals order area.
- Confirmation modal before submit.
- After success, navigate to `/exhibitor/orders/:orderId`.

## 6. Admin Experience

### 6.1 Admin Dashboard

`/admin/dashboard` becomes a real operations dashboard.

KPIs:

- 待销售确认订单: count of `PENDING_SALES_CONFIRM`.
- 待财务确认付款: count of `PENDING_FINANCE_CONFIRM`.
- 待审核选位: count of `PENDING_REVIEW`.
- 今日新增订单: count of orders whose `createdAt` falls on the current local date.

Task lists:

- Pending orders list with direct `确认` and `查看`.
- Pending payments list with direct `确认到账`.
- Pending selections list with direct `审核`.

Each KPI links to the corresponding page with a status filter.

Data source:

```text
GET /api/participation-orders?status=PENDING_SALES_CONFIRM
GET /api/payments?status=PENDING_FINANCE_CONFIRM
GET /api/booth-selections?status=PENDING_REVIEW
```

### 6.2 Admin Order List

Changes:

- Remove create-order action.
- Add status search.
- Primary columns:
  - Order short ID
  - Exhibitor short ID
  - Area and booth type
  - Total amount
  - Status tag
  - Actions
- Actions:
  - `查看详情`
  - `销售确认` only when status is `PENDING_SALES_CONFIRM`

### 6.3 Admin Order Detail

`/admin/orders/:orderId` gives administrators a single place to inspect and act.

Layout:

- Use `Tabs` for the main detail sections: `基本信息`, `合同`, `付款`, `选位`, `操作时间线`.
- Keep the header summary and primary action area above the tabs.
- This avoids a long undifferentiated page while still keeping a single order-detail route.

Sections:

1. Header
   - Order short ID, status, total amount
   - Sales confirm action when allowed
2. Basic information
   - Event ID, exhibitor ID, area, booth type, zone ID
3. Contract
   - Contract records and version drawer entry
4. Payment
   - Payment records and finance status
5. Selection
   - Selection history and latest status
6. Operation timeline
   - Use available payment, contract, and selection timestamps.
   - This round does not require a full audit-log API. The timeline uses existing related records only.

### 6.4 Admin Payment and Selection Pages

Payment confirmation:

- Default filter: `PENDING_FINANCE_CONFIRM`.
- `确认到账` and `确认失败` require confirm modals.
- Buttons show loading per row.

Selection review:

- Default filter: `PENDING_REVIEW`.
- `审核通过`, `驳回`, and `超时释放` require confirm modals.
- Reject requires a reason field.
- Row details show selected booth IDs, total area, lock expiry, and current status.

### 6.5 Configuration Pages

Configuration pages remain ProTable + ModalForm, but their role is clarified by navigation grouping.

Empty state copy:

- Events: `先创建展会，再配置展位、面积档位和价格规则`.
- Booths: `当前展会还没有展位，请先创建展位`.
- Area tiers: `配置展商可购买的标准面积档位`.
- Price rules: `配置不同展位类型的每平方米价格`.

## 7. API Gaps and Minimal Backend Support

The UI can be implemented with current APIs, but product quality improves if these minimal backend additions are included:

### 7.1 Detail Lookup APIs

Add:

```text
GET /api/participation-orders/{orderId}
```

Reason: detail pages should not fetch all orders and filter client-side.

Not in this round:

```text
GET /api/events/{eventId}
GET /api/exhibitors/{exhibitorId}
```

For this V1, event and exhibitor names may remain unavailable; the UI must label them as IDs and show copy actions.

### 7.2 Response Fields

This round must expose these fields on order, payment, contract, and selection list/detail responses:

```text
createdAt
updatedAt
```

The admin dashboard depends on `createdAt` to calculate `今日新增订单`. The UI must not fake timestamps.

### 7.3 Dashboard Counts

Counts can be calculated client-side from filtered list APIs for V1. Dedicated count endpoints are not required in this round.

## 8. Error Handling

- `apiGet` and `apiPost` must parse non-2xx `ApiResponse.message`.
- Forms should map known validation errors to field-level messages when the error clearly belongs to a field.
- Unknown errors show a persistent `Alert` inside the page section plus `message.error`.
- Actions that fail must not refresh away the user's context.

## 9. Mobile Scope

V1 is desktop-first.

If viewport width is below `768px`, show a clear Ant Design `Result`:

```text
请在电脑端访问展商系统
当前版本面向展会运营和展商选位工作台，暂不支持手机端操作。
```

Do not attempt full mobile adaptation in this round.

## 10. Acceptance Criteria

### 10.1 Exhibitor

- Opening `/exhibitor` never shows a blank or purely static page.
- A new exhibitor sees a clear empty state and `我要参展` action.
- An exhibitor with an order sees the current step and next action.
- Payment and selection are reachable from the order detail page without pasting an order ID.
- `提交付款`, `选择展位`, and `选位结果` are no longer top-level exhibitor menu items.
- The booth selection page includes both a simplified map and a table.

### 10.2 Admin

- Opening `/admin` shows a real dashboard with pending task counts.
- Admin navigation separates business processing from resource configuration.
- Order management has no create-order button.
- Admin can inspect an order detail page with related contract, payment, and selection sections.
- Dangerous actions use confirmation modals and loading states.

### 10.3 Shared

- Status values are rendered as Chinese labels.
- Money and area formatting are consistent.
- Empty states exist for every list.
- Frontend build passes.
- Existing backend tests pass.
- New page logic tests cover next-action derivation, status labels, and area equality display behavior.

## 11. Out of Scope

- Real login and RBAC.
- Exhibitor profile management.
- Registration category approval.
- Invoice and service center workflows.
- Refund and booth release task UI.
- Visual booth map editor or drag-and-drop planning.
- Full mobile support.
- Production analytics dashboard.
