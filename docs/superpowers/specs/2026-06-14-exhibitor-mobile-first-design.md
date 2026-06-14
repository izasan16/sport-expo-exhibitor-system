# 展商端移动优先改造设计

## 背景

展商端的真实使用场景更接近移动办公：展会现场、出差途中、收到销售或财务通知后即时处理。当前系统仍在 `App.tsx` 全局包裹 `MobileUnsupported`，手机访问会看到“请在电脑端访问展商系统”，这会直接阻断展商的核心流程。

后台端仍以销售、财务、运营人员的高频批量操作为主，应继续保留 Ant Design + ProComponents 的桌面工作台形态。本轮只改造展商端，不把后台改成移动端。

## 目标

- 展商端在 375px 至 768px 宽度可完整完成：查看首页、创建订单、查看订单、提交付款、选择展位、查看选位结果。
- 移除手机端阻断提示，保留后台桌面体验。
- 展商端移动布局优先，桌面端兼容显示。
- 继续遵守 Ant Design + ProComponents 约束，不自造基础表格、表单、弹窗组件。

## 非目标

- 不引入 `antd-mobile`，本轮仍使用现有 Ant Design 技术栈做响应式 H5。
- 不拆成 `frontend-mobile` 独立工程。
- 不做批量审核、通知系统、自动超时释放、选位审核小地图。
- 不新增登录、真实展商资料、真实联系人体系；展商身份仍使用当前 demo actor。

## 设计方案

采用“方案 A：单前端响应式移动优先”。

展商端页面根据 viewport 切换展示密度：

- 移动端使用 `List`、`Card`、`Checkbox`、`Collapse`、纵向 `ProForm`、block 按钮。
- 桌面端可保留 `ProTable`、`Tabs`、横向统计卡，但不得在移动端产生横向滚动。
- 后台端继续使用 `ProTable` 和桌面工作台，不受本轮影响。

## 文件边界

- `frontend/src/app/App.tsx`：移除全局 `MobileUnsupported` 包裹。
- `frontend/src/app/MobileUnsupported.tsx`：删除或停止使用。
- `frontend/src/app/responsive.ts`：新增 `useIsMobile` 和响应式辅助常量。
- `frontend/src/pages/exhibitor/mobileComponents.tsx`：新增展商端共享移动卡片组件和展示 helpers。
- `frontend/src/pages/exhibitor/HomePage.tsx`：空状态增加参展流程；移动端改为卡片/纵向布局。
- `frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx`：移动端用订单卡片列表，桌面端保留表格。
- `frontend/src/pages/exhibitor/orders/ExhibitorOrderDetailPage.tsx`：移动端用 Collapse 分组，补操作时间线和等待说明。
- `frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx`：移动端表单改为 vertical，按钮 block。
- `frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx`：付款方式显示中文业务项，移动端付款记录用列表。
- `frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx`：移动端展位选择用 List + Checkbox，补已选展位列表和取消按钮；桌面端保留表格选择。
- `frontend/src/pages/admin/payments/PaymentConfirmPage.tsx`：确认失败改为带原因的表单，原因作为本轮前端输入；若后端尚不接收原因，则先随请求 body 提交，后端忽略不影响兼容。

## 用户体验规则

### 展商首页

无订单时显示：

- “您还没有参展订单”
- 主按钮“我要参展”
- 折叠或卡片式“参展流程”：下单、销售确认、合同、付款、选位、审核、完成

有订单时显示：

- 展会名称为第一视觉信号
- 金额、面积、状态、下一步按钮
- 最近付款和最近选位使用短列表，不使用宽表格

### 订单列表

移动端订单卡片显示：

- 展会名称
- 状态 Tag
- 金额、面积、展位类型
- 订单短号作为次要信息
- 主要下一步按钮 block 展示

### 订单详情

移动端使用 Collapse：

- 基本信息
- 合同信息
- 付款记录
- 选位记录
- 操作时间线

状态说明必须告诉展商“现在等谁处理”和“下一步是什么”。例如待审核选位显示“选位申请已提交，管理员审核通过后展位将确认”。

### 付款

付款方式显示中文：

- `OFFLINE_TRANSFER`：银行转账
- `OFFLINE_CASH`：现金
- `OFFLINE_CHECK`：支票

提交成功后仍返回订单详情页。

### 选位

移动端不用 ProTable 作为主交互：

- 使用 `List` + `Checkbox` 展示可选展位
- 显示订单面积、已选面积、已选展位数
- 显示已选展位列表，每项可点击“取消”
- 面积不足/超出时显示明确提示
- 提交按钮固定在内容末尾，移动端 block

桌面端可继续使用表格选择。

## 数据与接口

本轮优先使用现有接口：

- `/api/participation-orders`
- `/api/participation-orders/{id}`
- `/api/payments`
- `/api/booth-selections`
- `/api/selection-eligibilities`
- `/api/events`
- `/api/area-tiers`
- `/api/booths`

不新增后端聚合接口。Dashboard 聚合和展商 dashboard 聚合接口列为后续优化。

## 测试策略

- 单元测试：
  - `useIsMobile` 相关纯函数或断点 helper。
  - 订单卡片展示模型。
  - 付款方式中文展示。
  - 已选展位列表取消逻辑。
- 构建验证：
  - `npm test -- display pageLogic responsive mobile`
  - `npm run build`
- 后端回归：
  - `mvn test`，使用外部 PostgreSQL 测试 schema。
- 视觉验证：
  - 375px 宽度打开 `/exhibitor`、`/exhibitor/orders`、`/exhibitor/orders/new`、付款页、选位页。
  - 不出现“请在电脑端访问展商系统”。
  - 不出现横向滚动作为核心操作路径。

## 风险与后续

- 当前没有真实展商名称 API，只能继续显示“演示展商”或短 ID 兜底。
- 当前没有展区字典 API，分区字段仍不能完整下拉化。
- 如果展位数量达到几百个，移动端展位列表需要增加搜索、筛选、分页或虚拟列表；本轮先保证基础可用。
- 独立 H5 工程与 `antd-mobile` 更适合生产最终态，但应在核心业务闭环稳定后单独立项。
