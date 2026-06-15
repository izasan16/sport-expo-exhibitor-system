# 展商企业档案与报名审核基础设计

## 1. 背景

当前系统已经具备展会、展位、面积档位、价格、参展订单、合同、付款、选位和后台审核的 V1 主链路，但展商身份仍主要依赖 `DEMO_EXHIBITOR_ID`。这会导致产品体验和业务约束都不完整：

- 展商端看起来像直接进入订单系统，缺少企业资料和报名资格。
- 后台无法管理展商企业主体，也无法审核参展报名。
- 订单虽然预留了 `registration_id`，但尚未真正绑定报名申请。
- 最初生产规划要求“报名审核通过后才能进入面积档位选择和订单流程”，当前还未落地。

本轮目标是补齐“业务身份层”：先建立展商企业档案和报名审核，不做真实账号注册登录。真实登录体系后续只负责把登录用户映射到本轮建立的 `exhibitor_id`。

## 2. 本轮目标

- 建立 `Exhibitor` 展商企业档案。
- 建立 `Registration` 参展报名申请。
- 展商端支持查看/维护企业资料、提交报名、查看报名进度。
- 后台支持展商管理、报名审核、驳回和审核意见。
- 订单创建必须校验该展商在该展会存在审核通过的报名。
- 订单记录必须绑定 `registration_id`。
- 展商首页从“订单概览”升级为“企业资料 + 报名资格 + 订单待办”的入口。
- 通知补充报名审核通过/驳回场景。

## 3. 明确不做

本轮不做真实认证和账号体系：

- 不做手机号注册。
- 不做密码登录。
- 不做短信验证码。
- 不做微信/公众号/OAuth/SSO。
- 不做完整 RBAC 和按钮权限。
- 不做企业多账号、多联系人账号绑定。

本轮仍使用现有 `DEMO_EXHIBITOR_ID`、`DEMO_ADMIN_ID` 模拟当前登录用户。区别是：系统里必须真的存在一条 `exhibitors.id = DEMO_EXHIBITOR_ID` 的展商企业档案，并且订单创建必须经过报名资格校验。

## 4. 业务流程

### 4.1 展商端流程

```text
进入展商中心
-> 查看企业资料
-> 填写或更新企业资料
-> 选择展会并提交参展报名
-> 等待主办方审核
-> 审核通过后进入我要参展/下单
-> 选择面积档位并创建订单
```

### 4.2 后台流程

```text
后台查看待审核报名
-> 查看展商企业资料和报名信息
-> 审核通过或驳回
-> 通过后展商可创建参展订单
-> 驳回后展商可修改报名资料并重新提交
```

### 4.3 订单约束

创建参展订单时，后端必须校验：

- `exhibitor_id` 对应展商存在且状态有效。
- `registration_id` 存在并属于同一 `event_id` 和 `exhibitor_id`。
- 报名状态为 `APPROVED`。
- 订单选择的品类/展区不得超出报名申请审核通过范围。

如果前端未传 `registrationId`，后端可以在该展商同一展会下查找唯一一条 `APPROVED` 报名并自动绑定；如果存在多条可用报名，必须返回业务错误，要求前端明确选择报名。

## 5. 状态设计

### 5.1 Exhibitor.status

```text
ACTIVE
SUSPENDED
ARCHIVED
```

规则：

- `ACTIVE` 展商可提交报名和创建订单。
- `SUSPENDED` 展商不可提交新报名或创建订单，但可查看历史数据。
- `ARCHIVED` 用于历史归档，不允许再参与新展会。

### 5.2 Registration.status

```text
DRAFT
SUBMITTED
APPROVED
REJECTED
CANCELLED
```

规则：

- `DRAFT` 可由展商编辑。
- `SUBMITTED` 等待后台审核，展商不可直接修改核心字段。
- `APPROVED` 允许创建参展订单。
- `REJECTED` 必须有驳回原因，展商可编辑后重新提交，重新提交后回到 `SUBMITTED`。
- `CANCELLED` 为展商主动取消或管理员取消，不可再用于创建订单。

驳回后的重新提交流程：

```text
REJECTED
-> 展商编辑报名字段
-> PUT /api/registrations/{id} 保存
-> POST /api/registrations/{id}/submit 重新提交
-> SUBMITTED
```

`PUT` 只保存编辑内容，不改变 `REJECTED` 状态。`submit` 负责把状态改为 `SUBMITTED`，并清空上一轮审核字段：

- `review_comment = null`
- `reviewed_by = null`
- `reviewed_at = null`

`REJECTED` 不再阻断同一展商同一展会新建报名；但如果展商选择继续处理原报名，必须编辑当前报名并通过上述流程重新提交，系统保留原驳回记录用于追溯。

### 5.3 Registration.category

`category` 采用展会配置枚举值，不使用自由文本。

本轮新增展会品类配置 `EventCategory`：

- 后台按展会维护可报名品类。
- 展商提交报名时从该展会启用的品类中选择一个 `category_code`。
- `Registration.category` 存储 `category_code`，页面展示 `category_name`。
- 后端创建或更新报名时必须校验 `category_code` 属于同一展会且处于启用状态。
- 订单创建时，若订单也携带品类字段，必须等于报名已审核通过的 `category_code`；若订单暂不携带品类字段，则本轮只校验 `zone_id` 不超出报名范围。

## 6. 数据模型

### 6.1 exhibitors

字段：

- `id uuid primary key`
- `company_name varchar(200) not null`
- `credit_code varchar(80)`
- `contact_name varchar(100)`
- `contact_phone varchar(40)`
- `contact_email varchar(120)`
- `address varchar(300)`
- `status varchar(40) not null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `created_by uuid`
- `updated_by uuid`

约束：

- `credit_code` 非空时全局唯一。
- `company_name` 用于业务展示，不能再只显示展商 UUID。

### 6.2 registrations

字段：

- `id uuid primary key`
- `event_id uuid not null references events(id)`
- `exhibitor_id uuid not null references exhibitors(id)`
- `status varchar(40) not null`
- `category varchar(120)`
- `zone_id uuid references zones(id)`
- `intent_area_sqm numeric(14,2)`
- `contact_name varchar(100)`
- `contact_phone varchar(40)`
- `remark varchar(500)`
- `review_comment varchar(500)`
- `reviewed_by uuid`
- `reviewed_at timestamptz`
- `submitted_at timestamptz`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `created_by uuid`
- `updated_by uuid`

索引：

- `(event_id, exhibitor_id, status)`
- `(status, submitted_at desc)`

约束：

- 同一展商同一展会同一时间最多一条进行中报名，进行中仅指 `DRAFT` 和 `SUBMITTED`。
- `REJECTED` 报名不占用新建报名资格；展商仍可编辑该驳回报名并重新提交，重新提交后重新进入 `SUBMITTED` 审核队列。
- `CANCELLED` 报名不可恢复；取消后允许重新创建报名。

### 6.3 event_categories

字段：

- `id uuid primary key`
- `event_id uuid not null references events(id)`
- `code varchar(80) not null`
- `name varchar(120) not null`
- `enabled boolean not null default true`
- `sort_order int not null default 0`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

约束：

- `(event_id, code)` 唯一。
- 报名只能选择同一展会下 `enabled = true` 的品类。

### 6.4 participation_orders 扩展

当前已存在 `registration_id uuid`。本轮要求：

- 创建新订单时 `registration_id` 必填或由后端唯一推导。
- 新订单必须写入 `registration_id`。
- 后续可考虑把 `registration_id` 增加外键到 `registrations(id)`；本轮若 schema 初始化顺序允许，应直接加外键。

## 7. 后端 API

### 7.1 展商企业档案

```http
GET  /api/exhibitors
GET  /api/exhibitors/{id}
POST /api/exhibitors
PUT  /api/exhibitors/{id}
```

本轮后台可以创建和维护展商。展商端只允许维护当前模拟登录展商：

```http
GET /api/exhibitors/current
PUT /api/exhibitors/current
```

`current` 暂时解析为 `DEMO_EXHIBITOR_ID`。

### 7.2 报名申请

```http
GET  /api/registrations?eventId=&exhibitorId=&status=
GET  /api/registrations/{id}
POST /api/registrations
PUT  /api/registrations/{id}
POST /api/registrations/{id}/submit
POST /api/registrations/{id}/approve
POST /api/registrations/{id}/reject
```

驳回请求：

```json
{ "reason": "资质材料不完整" }
```

审核通过/驳回必须写审计日志和站内通知。

驳回后重新提交的 API 顺序必须是：

```http
PUT  /api/registrations/{id}
POST /api/registrations/{id}/submit
```

`PUT` 只保存编辑内容，不改变 `REJECTED` 状态。`submit` 负责把状态改为 `SUBMITTED`，并清空 `review_comment`、`reviewed_by`、`reviewed_at`。

### 7.3 展会报名品类

```http
GET  /api/event-categories?eventId=
POST /api/event-categories
PUT  /api/event-categories/{id}
```

展商端报名页只加载启用项。后台可维护启用、禁用和排序。

### 7.4 订单创建联动

`POST /api/participation-orders` 请求新增或启用 `registrationId`：

```json
{
  "eventId": "...",
  "exhibitorId": "...",
  "registrationId": "...",
  "zoneId": "...",
  "areaTierId": "...",
  "boothType": "STANDARD"
}
```

后端校验失败返回业务错误：

- `Exhibitor does not exist`
- `Registration does not exist`
- `Registration is not approved`
- `Registration does not belong to this exhibitor`
- `Registration does not belong to this event`
- `Order zone is outside approved registration scope`

## 8. 前端页面

### 8.1 展商端

底部导航保持 4 个主入口，但调整为：

```text
首页
报名
订单
通知
```

页面：

- `/exhibitor`：总览企业资料、报名状态、订单待办。
- `/exhibitor/profile`：企业资料维护。
- `/exhibitor/registrations`：报名列表和报名进度。
- `/exhibitor/registrations/new`：提交报名。
- `/exhibitor/registrations/:registrationId`：报名详情、驳回后编辑、重新提交。
- `/exhibitor/orders/new`：下单前必须先选择一条已通过报名；如果没有已通过报名，显示引导去报名。

移动端体验：

- 表单采用纵向布局。
- 展会选择使用下拉，不手输 ID。
- 报名状态使用中文标签。
- 驳回原因必须明显展示。
- 报名通过后在详情页提供“去下单”按钮。

### 8.2 后台

新增后台菜单：

```text
展商管理
报名审核
```

页面：

- `/admin/exhibitors`：展商列表、创建、编辑、查看报名和订单。
- `/admin/registrations`：报名审核列表，支持按展会、状态、展商筛选。
- `/admin/registrations/:registrationId`：报名详情，审核通过或驳回。

后台工作台新增：

- 待审核报名数量。
- 待审核报名前 5 条。
- 点击 KPI 跳转到 `/admin/registrations?status=SUBMITTED`。

## 9. 通知与审计

新增通知类型：

- `REGISTRATION_SUBMITTED`：展商提交报名，通知管理员工作台。
- `REGISTRATION_APPROVED`：报名审核通过，通知展商。
- `REGISTRATION_REJECTED`：报名被驳回，通知展商。

通知应包含关联业务对象：

- `related_type = Registration`
- `related_id = registrationId`
- `related_order_id` 可为空

本轮同时补强通知深链基础：通知记录需要具备关联对象字段，前端可以根据通知类型跳转到报名详情或订单详情。

审计日志必须记录：

- 展商资料创建和修改。
- 报名创建、修改、提交。
- 报名审核通过。
- 报名驳回和驳回原因。
- 订单创建时绑定的 `registration_id`。
- 驳回报名重新提交时清空的审核字段。
- 报名品类创建、修改、启用和禁用。

## 10. 兼容与种子数据

因为当前系统已有 demo 订单，本轮需要兼容旧数据：

- schema 初始化时创建 demo 展商 `DEMO_EXHIBITOR_ID`，公司名为“演示展商”。
- 必须把 demo 报名绑定到数据库中已有的 demo 展会，优先选择 `events` 中 `year = 2026` 且状态未归档的第一条；如果没有任何展会，则只创建 demo 展商，不创建悬空报名。
- 可创建一条 demo 已审核通过报名，用于现有 demo 流程继续可用；该报名的 `event_id` 必须来自上一条解析出的已有 demo 展会。
- 可创建至少一条 demo 展会品类，例如 `SPORTS_EQUIPMENT` / `体育用品`，用于报名表单和现有演示流程；该品类必须绑定同一个已有 demo 展会。
- 旧订单如果 `registration_id` 为空，详情页显示“历史订单，未绑定报名”。
- 新订单不得再允许空 `registration_id`。

## 11. 测试策略

### 11.1 后端

- 创建展商档案。
- 更新展商档案。
- 创建报名草稿。
- 提交报名。
- 审核通过报名并生成通知。
- 驳回报名必须填写原因并生成通知。
- 订单创建无报名时失败。
- 订单创建使用未审核报名时失败。
- 订单创建使用已审核报名时成功并写入 `registration_id`。
- 同一展商同一展会不能同时存在两条进行中报名，进行中仅指 `DRAFT` 和 `SUBMITTED`。
- `REJECTED` 不阻断新建报名；驳回报名也可以编辑并重新提交。
- 重新提交后 `review_comment`、`reviewed_by`、`reviewed_at` 被清空。
- 报名品类必须来自该展会启用的品类枚举。

### 11.2 前端

- 展商首页在没有报名时显示报名引导。
- 展商首页在报名待审核时显示等待审核。
- 展商首页在报名驳回时显示驳回原因和重新提交入口。
- 展商首页在报名通过时显示可下单入口。
- 下单页没有已通过报名时禁用下单并引导去报名。
- 后台报名审核页展示展商公司名，不只显示 UUID。
- 后台驳回报名原因必填。
- 驳回后的报名详情提供编辑、保存、重新提交入口。
- 报名品类下拉来自 `/api/event-categories?eventId=`，不允许手输。
- 通知点击报名类通知进入报名详情。

## 12. 验收标准

- 展商端不再从“我要参展”直接无条件下单。
- 展商必须先有审核通过的报名，才能创建订单。
- 后台能看到待审核报名并完成通过/驳回。
- 展商能看到报名进度和驳回原因。
- 后台订单和报名页面展示展商公司名。
- 现有 demo 主链路仍可跑通。
- 不引入真实登录，不阻塞后续接入真实账号。

## 13. 后续衔接

本轮完成后，真实账号体系可以按以下方式接入：

```text
登录用户
-> 账号绑定关系
-> exhibitor_id
-> 企业资料 / 报名 / 订单
```

后续轮次可继续建设：

- 真实注册登录和账号绑定。
- 销售跟进和客户归属。
- 文件附件和资质材料上传。
- 服务中心和服务订单。
- 发票、退款、冲正和释放待办。
