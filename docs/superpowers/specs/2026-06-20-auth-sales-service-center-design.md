# 第六轮：账号权限、销售协作与服务中心设计

## 1. 背景

当前系统已经具备展商企业档案、报名审核、参展订单、合同、线下付款、展位选择、后台审核、通知和移动端展商体验。系统已经能跑通“报名 -> 下单 -> 合同 -> 付款 -> 选位 -> 审核”的主链路，但距离真实上线可用仍有三个明显缺口：

- 仍使用 `DEMO_EXHIBITOR_ID` 和 `DEMO_ADMIN_ID` 模拟登录用户，缺少真实账号、密码登录、角色权限和接口鉴权。
- 后台还没有销售负责人、协作销售和销售跟进能力，无法支撑招商团队日常客户管理。
- 选位完成后，展商真实参展还需要办理证件、楣板、用电、家具、广告、物流、搭建等服务事项，当前没有服务中心、服务申请和服务订单闭环。

本轮建设目标是把系统从“核心交易主链路可演示”提升到“具备真实运营组织和履约服务能力”的生产级雏形。

## 2. 总体目标

第六轮一次性建设以下能力：

- 真实账号密码登录。
- JWT Bearer Token 登录态。
- 角色权限与接口鉴权。
- 展商账号与展商企业主体绑定。
- 内部用户按角色进入后台。
- 销售负责人和协作销售数据权限。
- 销售跟进基础 CRM。
- 服务中心配置。
- 服务申请配置化表单。
- 服务申请审核、驳回、补充资料和执行。
- 收费服务生成独立服务订单。
- 服务订单线下付款和财务确认。
- 服务相关通知和审计日志。

本轮不再使用 demo 身份作为业务权限依据。允许保留 demo 种子账号用于演示登录，但所有接口必须基于真实登录用户解析身份和权限。

## 3. 范围边界

### 3.1 本轮纳入

- 用户账号、密码哈希、登录、当前用户信息。
- 用户角色：系统管理员、主办方管理员、销售、财务、服务人员、展商。
- 后端接口鉴权，匿名接口仅限登录和健康检查。
- 登录失败锁定和基础 IP 限流。
- 前端登录页、登录态保存、401/403 处理。
- 后台菜单按角色展示。
- 展商端按展商账号绑定的 `exhibitor_id` 访问数据。
- 销售按负责人或协作关系访问展商、报名、订单、选位和服务数据。
- 管理员分配主销售和协作销售。
- 销售跟进记录。
- 服务类型配置和服务字段配置。
- 服务开放条件配置。
- 展商服务中心列表、申请、详情和服务订单付款入口。
- 后台服务申请审核和服务订单管理。
- 服务订单线下付款记录、财务确认和失败原因。
- 通知和审计补强。

### 3.2 本轮不做

- 手机号验证码登录。
- 微信、公众号、OAuth 或 SSO。
- 找回密码、自助注册、邮箱验证。
- 复杂 RBAC 权限矩阵配置页面。
- 真实支付接口。
- 电子发票、退款、冲正完整流程。
- 文件对象存储真实上传；本轮可保留附件元数据和文件服务抽象。
- 多级审批流引擎。
- 服务执行排班、工单派工、短信提醒。
- 客户公海、线索池和复杂商机管理。

## 4. 角色与权限

### 4.1 角色定义

| 角色编码 | 角色名称 | 主要职责 |
| --- | --- | --- |
| `SYSTEM_ADMIN` | 系统管理员 | 用户、角色、系统参数、审计日志、全量数据管理 |
| `ORGANIZER_ADMIN` | 主办方管理员 | 展会运营、展商管理、报名审核、订单审核、销售分配、服务监管 |
| `SALES` | 销售 | 负责或协作客户的报名、订单、选位、服务跟进 |
| `FINANCE` | 财务 | 参展订单付款确认、服务订单付款确认、后续发票入口 |
| `SERVICE_STAFF` | 服务人员 | 服务类型维护、服务申请审核、服务执行状态更新 |
| `EXHIBITOR` | 展商 | 企业资料、报名、订单、付款、选位、服务申请和通知 |

### 4.2 权限原则

- 前端隐藏菜单只用于体验，不能作为安全边界。
- 所有后端业务接口必须校验登录态、角色和数据权限。
- 管理员角色拥有全量业务数据访问能力。
- 销售只能访问自己负责或协作的展商数据。
- 展商只能访问自己绑定的企业主体数据。
- 财务可以查看付款相关上下文，但不能绕过业务审核修改订单、选位和服务申请。
- 服务人员可以查看服务履约所需的展商、订单、展位基础信息，但不能修改参展订单金额和付款状态。

### 4.3 菜单入口

展商端：

```text
首页
报名
订单
服务
通知
企业资料
```

后台端：

```text
工作台
展会管理
展商管理
销售跟进
报名审核
订单管理
合同管理
付款确认
选位审核
服务类型配置
服务申请审核
服务订单管理
服务付款确认
账号管理
角色管理
审计日志
```

菜单显示规则：

- `EXHIBITOR` 只能进入展商端。
- `SALES` 默认进入销售工作台。
- `FINANCE` 默认进入财务工作台。
- `SERVICE_STAFF` 默认进入服务工作台。
- `ORGANIZER_ADMIN` 和 `SYSTEM_ADMIN` 默认进入后台总工作台。

## 5. 账号与登录

### 5.1 用户账号

用户账号表示可以登录系统的人。用户可以是内部人员，也可以是展商企业用户。

字段要求：

- 用户名。
- 手机号，可为空。
- 邮箱，可为空。
- 密码哈希。
- 显示名称。
- 用户类型：内部用户、展商用户。
- 绑定展商 ID，仅展商用户使用。
- 状态：启用、禁用。
- 最近登录时间。
- 创建人、创建时间、更新人、更新时间。

业务规则：

- 用户名全局唯一。
- 密码不得明文保存，必须使用 BCrypt。
- 禁用用户不可登录。
- 展商用户必须绑定一个有效 `exhibitor_id`。
- 内部用户不得绑定 `exhibitor_id` 作为权限依据。
- 一个用户可以拥有多个角色，但本轮页面分流以最高优先级角色确定默认入口。
- 用户可以配置 `default_role`。当用户拥有多个角色且配置了默认角色时，登录后优先进入默认角色工作台；未配置时按角色优先级进入。
- 多角色用户登录后允许在前端手动切换工作台，切换只影响前端入口和菜单，不放大后端数据权限。

角色优先级：

```text
SYSTEM_ADMIN
ORGANIZER_ADMIN
FINANCE
SERVICE_STAFF
SALES
EXHIBITOR
```

### 5.2 登录流程

```text
打开登录页
-> 输入用户名和密码
-> POST /api/auth/login
-> 后端校验密码和用户状态
-> 生成 JWT accessToken
-> 返回 currentUser、roles、expiresAt
-> 前端保存 token
-> 根据角色跳转到展商端或后台
```

登录失败规则：

- 用户不存在：返回“账号或密码错误”。
- 密码错误：返回“账号或密码错误”。
- 用户禁用：返回“账号已禁用，请联系管理员”。
- 用户无角色：返回“账号未分配角色，请联系管理员”。

为避免泄露账号枚举信息，用户不存在和密码错误使用同一提示。

登录防护规则：

- 同一账号连续登录失败 5 次后锁定 15 分钟。
- 锁定期间再次登录返回“账号暂时锁定，请稍后再试”。
- 登录失败次数按账号维度记录，同时记录失败 IP。
- 同一 IP 在短时间内大量失败时触发全局限流，本轮可先用内存或 Redis 计数器实现。
- 登录成功后清空该账号失败计数。

### 5.3 JWT 规则

- 使用 Bearer Token。
- 请求头格式：`Authorization: Bearer <token>`。
- Token 默认有效期 8 小时。
- Token 载荷只包含鉴权所需最小信息，不包含手机号、邮箱、密码哈希、联系人等敏感信息。
- Token 载荷至少包含 `sub/userId`、`username`、`roles`、`exhibitorId`、`jti`、`iat`、`exp`。
- `jti` 用于后续 token 黑名单、强制登出和审计追踪。
- JWT secret 必须通过环境变量配置，不得硬编码到代码或配置文件默认值中。
- JWT secret 长度不得低于 32 字节。
- 后端每次请求仍需校验用户未被禁用。
- 前端刷新页面后调用 `/api/auth/me` 恢复当前用户。
- 401 自动跳转登录页。
- 403 展示无权限页面或无权限提示。

本轮 `logout` 由前端删除 token 完成；后续如需要强制踢下线，再引入 token 黑名单或 session 版本号。

登录响应同时返回响应头：

```http
X-Token-Type: Bearer
X-Expires-In: 28800
```

### 5.4 敏感操作安全

本轮使用 JWT 存储在前端的方案，必须增加以下约束：

- 前端不得把 token 写入 URL、日志或错误上报内容。
- 前端配置基础 CSP，限制脚本来源，降低 XSS 后 token 被窃取风险。
- 删除、作废、重置密码、服务订单作废、销售归属调整等敏感操作必须二次确认。
- 当前方案不使用 Cookie，因此不引入 CSRF token；如果后续切换到 HttpOnly Cookie，则必须同步增加 CSRF 防护。

## 6. 销售归属与协作权限

### 6.1 销售归属模型

销售归属不直接写成 `exhibitors.sales_id` 单字段，而使用关系表承载负责人和协作人。

关系类型：

```text
PRIMARY
COLLABORATOR
```

业务规则：

- 一个展商最多一个主负责人。
- 一个展商可以有多个协作销售。
- 主负责人和协作销售必须是启用状态的 `SALES` 用户。
- 只有 `SYSTEM_ADMIN` 和 `ORGANIZER_ADMIN` 可以分配、调整或移除销售关系。
- 销售不能自行把展商分配给自己。
- 移除销售关系后，该销售不再能访问该展商的新数据，但历史跟进记录保留。

数据库约束：

- `(exhibitor_id, sales_user_id, assignment_type)` 唯一，避免同一销售重复分配同一类型关系。
- `PRIMARY` 负责人必须增加部分唯一约束：同一 `exhibitor_id` 在 `assignment_type = 'PRIMARY'` 时只能有一条记录。
- 如果当前 schema 初始化器不支持部分唯一约束表达式，本轮必须在应用服务层使用事务和行级锁兜底校验，避免并发分配两个主负责人。

### 6.2 数据权限传播

以下对象都通过 `exhibitor_id` 归属到展商，销售权限按展商归属传播：

- 展商企业档案。
- 报名申请。
- 参展订单。
- 合同。
- 参展订单付款概览。
- 选位申请。
- 服务申请。
- 服务订单。
- 服务付款。
- 销售跟进记录。

统一过滤规则：

```text
SYSTEM_ADMIN / ORGANIZER_ADMIN:
  可访问全部

SALES:
  只能访问 assigned_exhibitor_ids 中的展商及其关联业务对象

EXHIBITOR:
  只能访问 current_user.exhibitor_id 的业务对象

FINANCE:
  可访问付款确认所需业务对象，修改范围限制在付款记录和财务状态

SERVICE_STAFF:
  可访问服务履约所需业务对象，修改范围限制在服务申请和服务执行状态
```

后端应集中实现数据权限服务，例如 `DataScopeService`，避免每个 controller 分散拼接权限条件。

实现约束：

- Controller 或 Service 方法使用 Spring Security 方法级鉴权，例如 `@PreAuthorize`。
- 数据权限校验集中在 `DataScopeService`，提供 `canAccessExhibitor(user, exhibitorId)`、`filterAccessibleExhibitorIds(user, ids)`、`requireCanAccessOrder(user, orderId)` 等方法。
- 列表查询必须在 repository 或 service 查询层带上数据范围条件，不允许先查全量再在前端过滤。
- 本轮不启用 PostgreSQL Row-Level Security，避免与应用层权限、测试数据和 schema 初始化复杂度叠加；后续如有多租户或更高安全要求再评估 RLS。
- 销售可访问展商 ID 可以使用 Redis 缓存，缓存 key 按 `sales_user_id` 维度存储；销售归属调整后必须主动失效该销售和相关展商的权限缓存。

推荐方法级鉴权示例：

```java
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ORGANIZER_ADMIN') or @dataScopeService.canAccessExhibitor(authentication, #exhibitorId)")
public ExhibitorResponse getExhibitor(UUID exhibitorId) {
    ...
}
```

### 6.3 销售跟进

销售跟进是基础 CRM，不做复杂商机系统。

跟进字段：

- 展商 ID。
- 跟进人 ID。
- 跟进方式：电话、微信、邮件、现场、其他。
- 跟进内容。
- 下次跟进时间。
- 跟进状态：待跟进、已跟进、无需跟进。
- 创建时间、更新时间。

业务规则：

- 销售只能给自己负责或协作的展商新增跟进。
- 销售只能编辑自己创建的跟进记录。
- 管理员可以查看全部跟进记录。
- 管理员可以修改跟进状态，但不能改写原始跟进内容。
- 展商端不可见销售跟进记录。
- 工作台显示当前登录销售的“今日待跟进”和“逾期待跟进”。

## 7. 服务中心总体设计

### 7.1 服务类型范围

服务中心一次性覆盖以下服务分类：

| 分类编码 | 名称 | 典型服务 |
| --- | --- | --- |
| `BADGE` | 证件服务 | 展商证、施工证、车辆证、布撤展证 |
| `FASCIA` | 楣板信息 | 标准展位楣板公司名、中英文名称 |
| `ELECTRICITY` | 用电网络 | 额外用电、网络、水、气、压缩空气 |
| `FURNITURE` | 家具租赁 | 桌椅、展柜、洽谈桌、电视、灯具 |
| `ADVERTISING` | 广告服务 | 会刊广告、现场广告、LED 屏、资料袋广告 |
| `LOGISTICS` | 物流运输 | 展品运输、进馆时间、车辆信息、物流服务 |
| `CONSTRUCTION` | 特装搭建 | 施工图、效果图、搭建资质、安全承诺书 |

本轮不为每个服务分类写死专用表单和专用流程，而使用服务类型配置和字段配置生成申请表单。

### 7.2 服务办理粒度

服务类型支持三种办理粒度：

```text
EXHIBITOR
ORDER
BOOTH
```

规则：

- `EXHIBITOR`：按展商办理，可不绑定订单和展位，适合广告、会刊、资料类服务。
- `ORDER`：按参展订单办理，必须选择一个该展商的订单。
- `BOOTH`：按展位办理，必须选择一个或多个已确认展位。

默认配置：

- 证件服务：`ORDER`
- 楣板信息：`BOOTH`
- 用电网络：`BOOTH`
- 家具租赁：`BOOTH`
- 广告服务：`EXHIBITOR` 或 `ORDER`
- 物流运输：`ORDER`
- 特装搭建：`BOOTH`

### 7.3 服务开放条件

服务类型可配置多个开放条件：

```text
REGISTRATION_APPROVED
ORDER_CREATED
CONTRACT_EFFECTIVE
PAYMENT_CONFIRMED
BOOTH_CONFIRMED
```

规则：

- 条件为空时，报名通过后即可显示。
- 多个条件同时配置时，必须全部满足才可提交申请。
- 未满足条件的服务可以显示为不可办理，但必须展示原因。
- 广告类服务可早于展位确认开放。
- 楣板、用电、家具、搭建等依赖具体位置的服务默认要求展位确认。

不可办理原因示例：

- “需先完成报名审核”。
- “需先创建参展订单”。
- “需先完成合同确认”。
- “需先完成付款确认”。
- “需先确认展位后办理”。

### 7.4 服务收费方式

服务类型支持三种收费方式：

```text
FREE
FIXED_PRICE
MANUAL_QUOTE
```

规则：

- `FREE`：审核通过后直接进入执行。
- `FIXED_PRICE`：审核通过后按固定价格生成服务订单。
- `MANUAL_QUOTE`：服务人员先填写报价和收费明细，再生成服务订单。

收费服务必须生成独立 `ServiceOrder`，不得复用参展订单。

## 8. 服务字段配置

### 8.1 字段类型

服务字段支持：

```text
TEXT
TEXTAREA
NUMBER
DATE
SINGLE_SELECT
MULTI_SELECT
FILE
IMAGE
PHONE
EMAIL
```

字段配置内容：

- 字段编码。
- 字段名称。
- 字段类型。
- 是否必填。
- 提示文案。
- 默认值。
- 选项 JSON。
- 排序。
- 是否启用。
- 校验规则 JSON。

同一服务类型下字段编码唯一。

### 8.2 字段值存储

服务申请字段值采用 EAV 行存储，不把整张动态表单压成一个不可查询的大 JSON 字段。

`service_application_values` 字段：

- `id uuid primary key`
- `application_id uuid not null`
- `field_code varchar(100) not null`
- `value_text text`
- `value_json jsonb`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

约束：

- `(application_id, field_code)` 唯一。
- `TEXT`、`TEXTAREA`、`NUMBER`、`DATE`、`PHONE`、`EMAIL` 存入 `value_text`，其中数字和日期使用标准字符串格式保存。
- `SINGLE_SELECT` 存入 `value_text`。
- `MULTI_SELECT` 存入 `value_json`，格式为字符串数组。
- `FILE`、`IMAGE` 的附件元数据优先存入 `service_application_attachments`，必要时在 `value_json` 中保存引用列表。
- 后端返回申请详情时必须按字段配置顺序组装表单值，前端不直接依赖 EAV 表结构。

### 8.3 字段校验

字段校验规则：

- 必填字段不能为空。
- `PHONE` 按手机号或联系电话格式校验。
- `EMAIL` 按邮箱格式校验。
- `NUMBER` 支持最小值、最大值、小数位。
- `TEXT` 支持最大长度。
- `TEXTAREA` 支持最大长度。
- `SINGLE_SELECT` 的值必须来自配置选项。
- `MULTI_SELECT` 的值必须全部来自配置选项。
- `FILE` 和 `IMAGE` 本轮先保存附件元数据，文件服务可使用本地或对象存储抽象。

配置错误处理：

- 服务字段配置缺少字段编码时不允许保存。
- 单选或多选缺少选项时不允许启用字段。
- 已有申请引用的字段不得物理删除，只能禁用。

## 9. 服务申请流程

### 9.1 展商端申请流程

```text
展商登录
-> 进入服务中心
-> 查看可办理服务和不可办理原因
-> 选择服务类型
-> 选择目标订单或展位
-> 填写配置化表单
-> 保存草稿或提交
-> 查看审核进度
-> 若需补充资料，编辑后重新提交
-> 若产生服务订单，提交线下付款
-> 查看服务执行状态
```

### 9.2 服务申请状态

```text
DRAFT
SUBMITTED
UNDER_REVIEW
APPROVED
REJECTED
NEED_SUPPLEMENT
IN_PROGRESS
COMPLETED
CANCELLED
```

状态规则：

| 当前状态 | 事件 | 下一状态 | 责任角色 |
| --- | --- | --- | --- |
| `DRAFT` | 展商提交 | `SUBMITTED` | 展商 |
| `SUBMITTED` | 服务人员开始处理 | `UNDER_REVIEW` | 服务人员 |
| `SUBMITTED` / `UNDER_REVIEW` | 审核通过，免费服务 | `IN_PROGRESS` | 服务人员 |
| `SUBMITTED` / `UNDER_REVIEW` | 审核通过，收费服务 | `APPROVED` | 服务人员 |
| `SUBMITTED` / `UNDER_REVIEW` | 驳回 | `REJECTED` | 服务人员 |
| `SUBMITTED` / `UNDER_REVIEW` | 要求补充资料 | `NEED_SUPPLEMENT` | 服务人员 |
| `NEED_SUPPLEMENT` | 展商补充后提交 | `SUBMITTED` | 展商 |
| `APPROVED` | 服务订单已付款 | `IN_PROGRESS` | 系统 |
| `IN_PROGRESS` | 服务完成 | `COMPLETED` | 服务人员 |
| `DRAFT` / `SUBMITTED` / `NEED_SUPPLEMENT` | 取消 | `CANCELLED` | 展商或管理员 |

业务规则：

- 驳回必须填写原因。
- 要求补充资料必须填写补充说明。
- `REJECTED` 不允许直接再次提交，展商需要复制或重新创建申请；如产品需要原申请重提，必须保留上一轮审核意见。
- 系统必须提供“基于被驳回申请创建新申请”能力，新申请自动复制原申请的字段值、附件引用和目标订单/展位，状态为 `DRAFT`，原申请保持 `REJECTED` 不变。
- `NEED_SUPPLEMENT` 允许展商编辑字段值和附件后重新提交。
- `COMPLETED` 后不可修改申请内容。

### 9.3 目标展位存储

按展位办理的服务申请支持一个或多个目标展位。为便于查询、权限校验和后续按展位统计，本轮使用关联表，不使用 `target_booth_ids jsonb`。

新增 `service_application_booths`：

- `id uuid primary key`
- `application_id uuid not null`
- `booth_id uuid not null`
- `created_at timestamptz not null`

约束：

- `(application_id, booth_id)` 唯一。
- 提交 `BOOTH` 粒度服务时，至少存在一条目标展位记录。
- 目标展位必须属于该展商已确认的选位结果。
- 更新草稿或补充资料时，目标展位集合以请求为准整体替换，并写审计摘要。

### 9.4 审核通过后的处理

免费服务：

```text
审核通过
-> 服务申请进入 IN_PROGRESS
-> 服务人员后续标记 COMPLETED
```

固定价服务：

```text
审核通过
-> 服务申请进入 APPROVED
-> 系统生成 service_orders
-> 系统生成 service_order_items
-> 展商提交服务付款
-> 财务确认后 service_order 进入 PAID
-> 服务申请进入 IN_PROGRESS
```

后台报价服务：

```text
审核通过
-> 服务人员填写报价明细
-> 系统生成 service_orders
-> 系统写入报价有效期
-> 展商查看报价和应付金额
-> 展商提交服务付款
-> 财务确认后进入执行
```

本轮不做展商在线确认报价按钮；生成报价后默认展商可通过提交付款表示接受报价。

报价有效期：

- 后台报价服务必须填写 `quote_expires_at`。
- 默认有效期为报价生成后 7 天，可由服务人员在 1 至 30 天范围内调整。
- 超过有效期且未付款的服务订单不得继续提交付款，需服务人员重新报价或作废后重新生成。

## 10. 服务订单与服务付款

### 10.1 服务订单

服务订单是收费服务事项产生的独立财务对象。

字段要求：

- 展会 ID。
- 展商 ID。
- 服务申请 ID。
- 服务类型 ID。
- 参展订单 ID，可为空；按订单服务必须填写。
- 目标展位 ID 或目标展位集合，可为空；按展位服务必须填写。
- 订单状态。
- 服务费。
- 优惠金额。
- 调整项。
- 订单总价。
- 已确认付款金额。
- 报价有效期 `quote_expires_at`，仅后台报价服务必填。
- 创建人、创建时间、更新人、更新时间。

状态：

```text
PENDING_PAYMENT
PARTIALLY_PAID
PAID
IN_PROGRESS
COMPLETED
REFUNDING
REFUNDED
VOIDED
```

规则：

- 服务订单必须关联服务申请。
- 服务订单付款不影响参展订单选位资格。
- 服务订单可包含多条明细。
- 服务订单金额不得直接覆盖，修改必须记录调整项和原因。
- 服务订单作废必须填写原因。
- 本轮不做退款和冲正实现，但保留状态枚举。

### 10.2 服务订单明细

明细字段：

- 服务订单 ID。
- 明细名称。
- 数量。
- 单价。
- 金额。
- 备注。

示例：

```text
家具租赁 - 洽谈椅 x 4
用电服务 - 220V/10A x 2
广告服务 - 会刊内页广告 x 1
```

### 10.3 服务付款

服务付款独立于参展订单付款。

状态：

```text
PENDING_CONFIRM
CONFIRMED
FAILED
VOIDED
```

流程：

```text
展商提交线下付款凭证
-> 服务付款记录进入 PENDING_CONFIRM
-> 财务确认
-> 重新聚合服务订单已确认付款金额
-> 足额后服务订单进入 PAID
-> 服务申请进入 IN_PROGRESS
```

规则：

- 财务确认失败必须填写原因。
- 部分付款时服务订单进入 `PARTIALLY_PAID`。
- 足额付款时服务订单进入 `PAID`。
- 财务确认必须在事务内锁定服务订单行，避免并发确认导致聚合错误。
- 服务付款不允许物理删除，只能失败、作废或后续冲正。

## 11. 后端 API

### 11.1 认证

```http
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

登录请求：

```json
{
  "username": "sales01",
  "password": "password"
}
```

登录响应：

```json
{
  "accessToken": "...",
  "expiresAt": "2026-06-20T20:00:00+08:00",
  "user": {
    "id": "...",
    "username": "sales01",
    "displayName": "销售一",
    "roles": ["SALES"],
    "exhibitorId": null
  }
}
```

### 11.2 用户与角色

```http
GET  /api/users
POST /api/users
PUT  /api/users/{id}
POST /api/users/{id}/reset-password
GET  /api/roles
PUT  /api/users/{id}/roles
```

权限：

- 仅 `SYSTEM_ADMIN` 可管理系统管理员。
- `SYSTEM_ADMIN` 和 `ORGANIZER_ADMIN` 可管理业务用户。
- 普通用户可通过 `/api/auth/me` 查看自己，但不可改角色。

### 11.3 销售归属与跟进

```http
GET  /api/exhibitors/{id}/sales-assignments
PUT  /api/exhibitors/{id}/sales-assignments
GET  /api/sales-follow-ups?exhibitorId=&status=
POST /api/sales-follow-ups
PUT  /api/sales-follow-ups/{id}
```

分配请求：

```json
{
  "primarySalesId": "...",
  "collaboratorSalesIds": ["...", "..."]
}
```

跟进请求：

```json
{
  "exhibitorId": "...",
  "method": "PHONE",
  "content": "确认客户计划购买 36 平方米展位",
  "nextFollowUpAt": "2026-06-22T10:00:00+08:00",
  "status": "PENDING"
}
```

### 11.4 服务类型和字段

```http
GET  /api/service-types?eventId=&enabled=
POST /api/service-types
PUT  /api/service-types/{id}

GET  /api/service-types/{id}/fields
POST /api/service-types/{id}/fields
PUT  /api/service-fields/{id}
```

服务类型请求：

```json
{
  "eventId": "...",
  "name": "标准展位楣板信息",
  "category": "FASCIA",
  "targetScope": "BOOTH",
  "openConditions": ["PAYMENT_CONFIRMED", "BOOTH_CONFIRMED"],
  "chargeMode": "FREE",
  "fixedPrice": null,
  "requiresReview": true,
  "deadlineAt": "2026-07-01T23:59:59+08:00",
  "enabled": true,
  "sortOrder": 10
}
```

字段请求：

```json
{
  "code": "company_name_cn",
  "label": "楣板中文名称",
  "fieldType": "TEXT",
  "required": true,
  "placeholder": "请输入需要展示在楣板上的公司中文名称",
  "optionsJson": null,
  "validationJson": {
    "maxLength": 80
  },
  "sortOrder": 1,
  "enabled": true
}
```

### 11.5 服务申请

```http
GET  /api/service-applications?eventId=&exhibitorId=&status=&serviceTypeId=
POST /api/service-applications
GET  /api/service-applications/{id}
PUT  /api/service-applications/{id}
POST /api/service-applications/{id}/submit
POST /api/service-applications/{id}/start-review
POST /api/service-applications/{id}/approve
POST /api/service-applications/{id}/reject
POST /api/service-applications/{id}/request-supplement
POST /api/service-applications/{id}/complete
POST /api/service-applications/{id}/copy-from-rejected
```

本轮不提供服务申请批量提交和批量取消接口。服务申请包含动态字段、附件和目标订单/展位，批量操作容易造成误提交和附件归属错误，先保持单笔操作。

创建或更新请求：

```json
{
  "serviceTypeId": "...",
  "orderId": "...",
  "boothIds": ["..."],
  "values": {
    "company_name_cn": "演示体育用品有限公司",
    "company_name_en": "Demo Sports Co., Ltd."
  },
  "attachments": [
    {
      "fieldCode": "construction_drawing",
      "fileId": "...",
      "fileName": "施工图.pdf"
    }
  ]
}
```

驳回请求：

```json
{
  "reason": "楣板英文名称超过长度限制，请调整后重新提交"
}
```

补充资料请求：

```json
{
  "reason": "请补充施工单位资质文件"
}
```

### 11.6 服务订单和服务付款

```http
GET  /api/service-orders?eventId=&exhibitorId=&status=
GET  /api/service-orders/{id}
POST /api/service-orders/{id}/quote
POST /api/service-orders/{id}/void

GET  /api/service-payments?serviceOrderId=&status=
POST /api/service-payments
POST /api/service-payments/{id}/confirm
POST /api/service-payments/{id}/fail
```

报价请求：

```json
{
  "items": [
    {
      "itemName": "洽谈椅租赁",
      "quantity": 4,
      "unitPrice": 80.00,
      "remark": "租期为展期全程"
    }
  ],
  "discountAmount": 0,
  "adjustmentAmount": 0,
  "reason": "按家具租赁标准报价"
}
```

服务付款请求：

```json
{
  "serviceOrderId": "...",
  "amount": 320.00,
  "method": "OFFLINE_TRANSFER",
  "payerName": "演示体育用品有限公司",
  "paidAt": "2026-06-20T12:00:00+08:00",
  "voucherFileId": "..."
}
```

### 11.7 错误码规范

接口错误响应继续使用统一 `ApiResponse.error` 外壳，但本轮需要增加稳定业务错误码，便于前端做精确提示。

HTTP 状态：

- `401`：未登录、token 缺失、token 过期、token 篡改。
- `403`：已登录但角色不足或数据权限不足。
- `400`：业务规则校验失败。
- `409`：唯一约束、状态冲突、并发冲突。

错误码示例：

| 错误码 | HTTP | 场景 |
| --- | --- | --- |
| `E_AUTH_BAD_CREDENTIALS` | 401 | 账号或密码错误 |
| `E_AUTH_USER_DISABLED` | 403 | 用户被禁用 |
| `E_AUTH_ACCOUNT_LOCKED` | 403 | 登录失败过多临时锁定 |
| `E_AUTH_TOKEN_EXPIRED` | 401 | token 过期 |
| `E_AUTH_TOKEN_INVALID` | 401 | token 无效或被篡改 |
| `E_FORBIDDEN_ROLE` | 403 | 角色无权访问 |
| `E_FORBIDDEN_DATA_SCOPE` | 403 | 数据权限不足 |
| `E_SALES_ASSIGNMENT_CONFLICT` | 409 | 展商主销售冲突 |
| `E_SERVICE_CONDITION_UNMET` | 400 | 服务开放条件未满足 |
| `E_SERVICE_FIELD_INVALID` | 400 | 服务字段值不合法 |
| `E_SERVICE_TARGET_REQUIRED` | 400 | 缺少目标订单或展位 |
| `E_SERVICE_QUOTE_EXPIRED` | 400 | 服务报价已过期 |
| `E_PAYMENT_STATUS_CONFLICT` | 409 | 付款状态已变化，不能重复确认 |

## 12. 前端体验

### 12.1 登录页

登录页要求：

- 桌面和移动端都可用。
- 输入用户名和密码。
- 登录失败显示业务提示。
- 登录成功按角色跳转。
- 已登录用户访问 `/login` 时跳转默认首页。

### 12.2 展商端服务中心

页面：

```text
/exhibitor/services
/exhibitor/services/:serviceTypeId/apply
/exhibitor/service-applications
/exhibitor/service-applications/:id
/exhibitor/service-orders
/exhibitor/service-orders/:id/payment
```

体验要求：

- 移动优先。
- 服务中心按分类展示。
- 可办理服务突出显示。
- 不可办理服务显示原因。
- 表单字段来自服务字段配置。
- 目标订单和目标展位必须下拉或卡片选择，不能手敲 ID。
- 服务申请详情展示状态、审核意见、补充资料原因、付款入口和执行状态。
- 服务订单付款页显示服务名称、金额、订单短号和付款状态。

字段类型与 Ant Design 组件映射：

| 字段类型 | 前端组件 | 说明 |
| --- | --- | --- |
| `TEXT` | `Input` | 普通单行文本 |
| `TEXTAREA` | `Input.TextArea` | 多行文本，显示字数限制 |
| `NUMBER` | `InputNumber` | 使用后端配置的最小值、最大值和精度 |
| `DATE` | `DatePicker` | 提交 ISO 日期字符串 |
| `SINGLE_SELECT` | `Select` | 选项来自字段配置 |
| `MULTI_SELECT` | `Select mode="multiple"` | 提交字符串数组 |
| `FILE` | `Upload` | 文件服务抽象，先保存附件元数据 |
| `IMAGE` | `Upload accept="image/*"` | 限制图片类型 |
| `PHONE` | `Input` | 手机或联系电话校验 |
| `EMAIL` | `Input` | 邮箱格式校验 |

### 12.3 后台服务管理

页面：

```text
/admin/service-types
/admin/service-applications
/admin/service-applications/:id
/admin/service-orders
/admin/service-payments
```

体验要求：

- 服务类型使用 `ProTable + ModalForm`。
- 字段配置支持排序、启用、禁用。
- 服务申请列表展示展会、展商、服务类型、状态、提交时间。
- 详情页展示申请表单值、附件、关联订单、关联展位。
- 审核通过、驳回、要求补充资料使用明确按钮。
- 驳回和补充资料原因必填，最多 500 字。
- 后台报价服务必须可以添加多条收费明细。
- 服务付款确认页展示展商名、服务名称、服务订单号、金额、付款方式、状态。

### 12.4 账号和销售后台

页面：

```text
/admin/users
/admin/roles
/admin/exhibitors/:id/sales
/admin/sales-follow-ups
```

体验要求：

- 用户列表展示用户名、显示名、角色、状态、绑定展商。
- 创建用户时必须设置初始密码。
- 重置密码需要二次确认。
- 展商管理页显示主销售和协作销售。
- 销售跟进列表支持按展商、跟进人、状态筛选。
- 销售工作台显示我的客户、今日待跟进、逾期待跟进。

## 13. 通知与审计

### 13.1 新增通知类型

```text
SERVICE_APPLICATION_SUBMITTED
SERVICE_APPLICATION_APPROVED
SERVICE_APPLICATION_REJECTED
SERVICE_APPLICATION_NEED_SUPPLEMENT
SERVICE_ORDER_CREATED
SERVICE_PAYMENT_CONFIRMED
SERVICE_PAYMENT_FAILED
SALES_ASSIGNED
FOLLOW_UP_DUE
```

通知规则：

- 展商提交服务申请后通知服务人员或服务工作台。
- 服务申请通过、驳回、需补充资料后通知展商。
- 服务订单生成后通知展商付款。
- 服务付款确认成功或失败后通知展商。
- 销售被分配客户后通知销售。
- 跟进到期可在销售工作台提示，本轮可不做主动推送。

### 13.2 审计日志

必须记录：

- 登录成功。
- 登录失败。
- 用户创建、禁用、启用。
- 密码重置。
- 用户角色变更。
- 销售负责人和协作销售调整。
- 跟进记录创建和修改。
- 服务类型创建、修改、启用、禁用。
- 服务字段创建、修改、启用、禁用。
- 服务申请创建、修改、提交。
- 服务申请审核通过、驳回、要求补充资料。
- 服务订单生成、报价、作废。
- 服务付款提交、确认、失败。

审计日志应包含：

- 操作人。
- 操作角色。
- 业务对象类型。
- 业务对象 ID。
- 操作动作。
- 操作时间。
- 请求来源。
- 变更摘要。

## 14. 数据模型

### 14.1 新增表

```text
users
roles
user_roles
exhibitor_sales_assignments
sales_follow_ups
service_types
service_fields
service_applications
service_application_booths
service_application_values
service_application_attachments
service_orders
service_order_items
service_payments
```

### 14.2 关键约束

- `users.username` 唯一。
- `users.password_hash` 非空。
- `users.jwt_version` 或等效字段默认 0，为后续强制失效 token 预留。
- `users.default_role` 可为空；非空时必须是该用户已拥有角色之一。
- `users.failed_login_count`、`users.locked_until` 用于登录失败锁定。
- `roles.code` 唯一。
- `user_roles(user_id, role_id)` 唯一。
- `exhibitor_sales_assignments(exhibitor_id, sales_user_id, assignment_type)` 唯一。
- 一个展商最多一个 `PRIMARY` 负责人。
- `service_fields(service_type_id, code)` 唯一。
- `service_applications` 必须关联 `event_id`、`exhibitor_id`、`service_type_id`。
- `ORDER` 粒度服务申请必须有 `order_id`。
- `BOOTH` 粒度服务申请必须在 `service_application_booths` 中有目标展位。
- `service_application_values(application_id, field_code)` 唯一。
- `service_application_booths(application_id, booth_id)` 唯一。
- `service_orders` 必须关联 `service_application_id`。
- 金额字段统一 `numeric(14,2)`。

关键索引：

```sql
create index idx_sales_assignments_sales_user
  on exhibitor_sales_assignments(sales_user_id, assignment_type);

create index idx_sales_assignments_exhibitor
  on exhibitor_sales_assignments(exhibitor_id, assignment_type);

create unique index uk_exhibitor_primary_sales
  on exhibitor_sales_assignments(exhibitor_id)
  where assignment_type = 'PRIMARY';

create index idx_orders_exhibitor_status
  on participation_orders(exhibitor_id, status);

create index idx_service_applications_exhibitor_status
  on service_applications(exhibitor_id, status);

create index idx_service_orders_exhibitor_status
  on service_orders(exhibitor_id, status);

create index idx_service_payments_order_status
  on service_payments(service_order_id, status);
```

### 14.3 种子账号

为了演示和本地开发，初始化以下账号：

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | `SYSTEM_ADMIN` | 系统管理员 |
| `organizer` | `ORGANIZER_ADMIN` | 主办方管理员 |
| `sales01` | `SALES` | 销售演示账号 |
| `finance01` | `FINANCE` | 财务演示账号 |
| `service01` | `SERVICE_STAFF` | 服务人员演示账号 |
| `exhibitor01` | `EXHIBITOR` | 绑定 demo 展商 |

默认密码只用于本地和演示环境，生产部署必须要求首次登录修改。

种子 SQL 必须使用确定性 UUID，避免重复初始化产生多套角色。密码哈希在实施时由 BCrypt 生成，不在文档中写明明文哈希。

角色种子逻辑：

```sql
insert into roles (id, code, name, created_at, updated_at)
values
  ('00000000-0000-0000-0000-000000000101', 'SYSTEM_ADMIN', '系统管理员', now(), now()),
  ('00000000-0000-0000-0000-000000000102', 'ORGANIZER_ADMIN', '主办方管理员', now(), now()),
  ('00000000-0000-0000-0000-000000000103', 'SALES', '销售', now(), now()),
  ('00000000-0000-0000-0000-000000000104', 'FINANCE', '财务', now(), now()),
  ('00000000-0000-0000-0000-000000000105', 'SERVICE_STAFF', '服务人员', now(), now()),
  ('00000000-0000-0000-0000-000000000106', 'EXHIBITOR', '展商', now(), now())
on conflict (code) do nothing;
```

演示账号种子必须使用 `on conflict (username) do nothing`，并把 `exhibitor01` 绑定到现有 demo 展商 `00000000-0000-0000-0000-000000000001`。

## 15. 兼容现有功能

现有 demo 数据需要平滑过渡：

- demo 展商继续存在。
- `exhibitor01` 绑定 demo 展商。
- 现有展商端页面不再读固定 demo ID，而从当前用户获取 `exhibitorId`。
- 后台接口不再默认 `DEMO_ADMIN_ID`，操作人来自当前登录用户。
- 审计日志 actor 由当前用户填充。
- 如果历史数据缺少销售归属，管理员仍可查看；销售不可见，直到管理员分配。
- 如果历史订单未绑定服务数据，不影响订单详情。

### 15.1 现有数据升级口径

本项目当前不使用 Flyway/Liquibase，schema 仍由受控初始化和版本化 SQL 脚本管理。第六轮实施时必须形成一次明确的 schema 升级脚本，不能只靠修改最终 baseline。

升级顺序：

```text
1. 创建 users、roles、user_roles。
2. 创建销售归属和跟进表。
3. 创建服务中心相关表。
4. 为现有业务表补充必要索引。
5. 插入角色种子数据。
6. 插入演示用户。
7. 将 demo 展商绑定到 exhibitor01。
8. 将历史 created_by / updated_by 为空的记录回填为 SYSTEM_BOOTSTRAP_USER_ID。
9. 为 demo 展商分配 sales01 为 PRIMARY，便于销售账号演示。
10. 执行后向兼容 smoke test。
```

历史数据规则：

- 现有订单、报名、选位、付款不强制补销售归属；管理员始终可查看。
- 需要销售演示的数据可把 demo 展商分配给 `sales01`。
- 现有审计日志 actor 为空的历史记录不重写业务含义，只在缺失创建人/更新人字段时回填系统启动用户。
- 服务中心新表为空不影响既有主链路。

### 15.2 部署环境变量

新增环境变量：

```text
JWT_SECRET
JWT_EXPIRATION_SECONDS=28800
JWT_ISSUER=sport-expo-exhibitor-system
LOGIN_MAX_FAILURES=5
LOGIN_LOCK_MINUTES=15
LOGIN_IP_RATE_LIMIT_PER_MINUTE=30
APP_CSP_ENABLED=true
```

部署要求：

- `JWT_SECRET` 生产环境必须由部署平台注入，不得提交到仓库。
- `JWT_SECRET` 不得使用默认值启动生产服务；缺失时后端应启动失败。
- 升级前备份 PostgreSQL schema。
- 升级后先使用管理员账号登录，再验证展商、销售、财务、服务人员账号。

## 16. 测试验收

### 16.1 后端测试

必须覆盖：

- 登录成功返回 token 和用户角色。
- 密码错误登录失败。
- 禁用用户不可登录。
- JWT token 过期后返回 401 和 `E_AUTH_TOKEN_EXPIRED`。
- JWT token 被篡改后返回 401 和 `E_AUTH_TOKEN_INVALID`。
- 无 token 访问业务接口返回 401。
- 角色不足访问接口返回 403。
- 展商只能查看自己的订单、报名和服务申请。
- 销售只能查看负责或协作展商的数据。
- 销售归属调整后，旧销售的数据权限立即失效，新销售立即生效。
- 管理员可以分配主销售和协作销售。
- 一个展商不能有两个主销售。
- 销售创建跟进记录成功。
- 销售不能给无权限展商创建跟进。
- 服务类型创建成功。
- 字段配置校验生效。
- 未满足开放条件不能提交服务申请。
- 按订单服务必须选择订单。
- 按展位服务必须选择已确认展位。
- 免费服务审核通过后进入执行。
- 固定价服务审核通过后生成服务订单。
- 后台报价服务生成服务订单明细。
- 服务付款确认后服务订单进入已付款。
- 服务付款失败记录失败原因。
- 两笔服务付款并发确认时，服务订单付款聚合最终正确。
- 服务订单付款不影响参展订单选位资格。

### 16.2 前端测试

必须覆盖：

- 登录页成功跳转。
- 401 后跳转登录。
- 403 显示无权限提示。
- 展商服务中心展示可办理服务。
- 展商服务中心展示不可办理原因。
- 配置化表单渲染文本、数字、日期、单选、多选和文件占位。
- 目标订单和目标展位通过选择控件选择。
- 服务申请详情显示审核意见。
- 服务订单付款页显示服务名称和金额。
- token 过期后前端跳转登录并保留当前路径用于重新登录后返回。
- 网络中断时，服务申请表单保留当前已填写值，不清空页面状态。
- 后台服务类型配置可创建字段。
- 后台服务申请审核可通过、驳回、要求补充。
- 销售工作台只显示自己客户待办。

### 16.3 浏览器验收

移动端：

- 展商登录后进入展商首页。
- 展商可以进入服务中心。
- 展商可以按服务类型提交申请。
- 展商可以查看服务申请状态。
- 展商可以对服务订单提交线下付款。

桌面后台：

- 管理员可以创建用户和分配角色。
- 管理员可以给展商分配主销售和协作销售。
- 销售登录后只能看到自己客户。
- 服务人员可以配置服务类型和字段。
- 服务人员可以审核服务申请。
- 财务可以确认服务付款。
- 长时间停留后 token 过期，下一次操作提示重新登录。
- 网络中断后刷新失败时，页面展示重试入口，不丢失未提交表单内容。

## 17. 风险与后续衔接

- JWT 存储在前端存在 XSS 风险，后续上线前可评估 HttpOnly Cookie 或 token rotation。
- 本轮 RBAC 为固定角色模型，不做复杂权限矩阵，后续可扩展菜单和按钮权限配置。
- 文件上传本轮先做附件元数据和文件服务抽象，后续接对象存储。
- 服务字段配置使用 JSON 校验规则，后续如服务复杂度提高，可引入表单设计器。
- 服务订单退款、发票和冲正后续接财务模块。
- 销售跟进本轮为基础记录，后续可扩展客户等级、商机阶段、自动提醒和客户公海。

## 18. 验收标准

- 系统不再依赖 demo 身份访问业务数据。
- 不同角色登录后看到不同菜单和数据范围。
- 销售只能看到负责人或协作客户的数据。
- 管理员能分配销售负责人和协作销售。
- 展商能在移动端办理服务申请。
- 服务类型和表单字段可由后台配置。
- 服务申请能审核、驳回、要求补充资料和完成。
- 收费服务能生成服务订单。
- 服务订单能提交线下付款并由财务确认。
- 服务订单付款不影响参展订单选位资格。
- 通知和审计记录覆盖关键操作。
