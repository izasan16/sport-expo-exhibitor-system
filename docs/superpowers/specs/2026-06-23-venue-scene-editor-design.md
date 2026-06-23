# 展馆场景编辑器与可视化选位设计

## 1. 目标

把当前“表格 + 简化格子图”的展位配置与选位能力，升级为一套可生产落地的展馆场景能力：

```text
管理员维护展会模式
  -> 维护展馆边界 / 通道 / 障碍 / 出入口 / 分区 / 展位范围
  -> 发布展馆场景
  -> 销售与展商按可视化图形查看展位
  -> 在现有订单 / 付款 / 选位 / 审核规则下完成选位
```

本设计新增“半结构化场景编辑器”，但不重写既有交易主链：

- 订单、合同、付款、选位资格、锁位、审核仍以现有业务状态机为准。
- 图形场景是新的空间承载层，不是另一套独立业务系统。

## 2. 设计原则

- 一场展会只能选择一种展位数据来源：`FORM_ONLY` 或 `SCENE_DRIVEN`，不得混用。
- `FORM_ONLY` 保持现有纯表单模式；`SCENE_DRIVEN` 走场景编辑和可视化选位。
- 管理员编辑的是场景草稿；销售和展商消费的是已发布场景，避免编辑中数据污染前台业务。
- 展位图元允许自由多边形，以适配异形展位、拼角位、特殊馆内切角。
- 管理员桌面优先；展商移动优先；销售兼顾桌面效率与移动查看。
- 页面设计仍以 Ant Design / ProComponents 为基础；图形编辑区域允许引入专用 2D 画布库，不要求纯 Ant Design 实现。

## 3. 范围与非范围

### 3.1 本轮范围

- 展会新增“展位来源模式”属性。
- 后台新增展馆场景编辑器。
- 后台支持馆图草稿、发布、回看和版本审计。
- `SCENE_DRIVEN` 展会支持从图形展位生成真实 booth 主数据。
- 销售与展商支持地图 + 列表的可视化选位。
- 现有选位提交、锁位、审核通过、驳回、超时释放继续沿用。

### 3.2 本轮非范围

- 不做 CAD 原生导入、DWG 解析、矢量识别。
- 不做自动布展算法、自动推荐最优组合。
- 不做 3D 场馆视图。
- 不做复杂打印出图排版。
- 不做实时协同多人编辑同一草稿。

## 4. 展会模式

### 4.1 新增属性

`events` 新增：

- `booth_source_mode`
  - `FORM_ONLY`
  - `SCENE_DRIVEN`
- `visual_scene_enabled`
  - 冗余展示字段，可由 `booth_source_mode` 推导

### 4.2 业务规则

- `FORM_ONLY`：
  - 保持现有 `展位管理`、`面积档位`、`价格规则`、`选位页` 逻辑。
  - 不要求存在场景。
  - 选位页继续显示列表和简化图块。

- `SCENE_DRIVEN`：
  - 该展会的 booth 主数据必须来自已发布场景中的展位图元。
  - 后台不允许手工新增“脱离图形”的 booth。
  - 销售 / 展商选位页优先显示可视化场景，不退回简化格子图。

- 切换规则：
  - 已存在订单、选位申请、已确认展位的展会，不允许直接切换模式。
  - 仅允许在“未开始运营”且无交易数据时修改模式。

## 5. 核心模型

### 5.1 VenueScene

表示某个展会当前的场景容器。

建议字段：

- `id`
- `event_id`
- `status`
  - `DRAFT`
  - `PUBLISHED`
  - `ARCHIVED`
- `canvas_width`
- `canvas_height`
- `coordinate_unit`
  - 默认 `PX`
- `viewport_min_x`
- `viewport_min_y`
- `viewport_max_x`
- `viewport_max_y`
- `published_revision_id`
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

### 5.2 SceneRevision

表示一次完整场景快照。

建议字段：

- `id`
- `scene_id`
- `revision_no`
- `status`
  - `DRAFT`
  - `PUBLISHED`
  - `DISCARDED`
- `change_summary`
- `snapshot_json`
- `published_at`
- `published_by`
- `created_at`
- `created_by`

### 5.3 SceneFeature

表示场景中的单个图元对象。

建议字段：

- `id`
- `scene_revision_id`
- `event_id`
- `feature_type`
  - `HALL_BOUNDARY`
  - `AISLE`
  - `OBSTACLE`
  - `WALL`
  - `ENTRANCE`
  - `SERVICE_POINT`
  - `ZONE_BOUNDARY`
  - `BOOTH`
- `geometry_type`
  - `POLYGON`
  - `POLYLINE`
  - `POINT`
- `geometry_json`
- `label`
- `code`
- `layer_order`
- `style_json`
- `meta_json`
- `locked`
- `created_at`
- `updated_at`

### 5.4 BoothSceneBinding

把图形展位与真实 booth 主数据绑定。

建议字段：

- `id`
- `event_id`
- `scene_revision_id`
- `scene_feature_id`
- `booth_id`
- `binding_status`
  - `ACTIVE`
  - `REPLACED`
  - `REMOVED`
- `created_at`
- `updated_at`

### 5.5 Geometry 约束

- 展馆边界、分区边界、障碍区、展位统一使用多边形。
- 通道、墙体使用折线或带宽折线。
- 出入口、服务点使用点位。
- Booth 图元必须为闭合多边形。
- Booth 图元必须保存面积缓存值，用于快速比对图形面积和业务面积。

## 6. 管理后台编辑器

### 6.1 页面结构

后台新增：

- `/admin/events/:eventId/scene`

页面分为三栏：

- 左侧：图层树 + 对象列表
- 中间：画布区
- 右侧：属性面板

顶部操作：

- 保存草稿
- 发布
- 放大 / 缩小 / 适应画布
- 撤销 / 重做
- 显示网格
- 显示图层
- 场景校验

### 6.2 支持的编辑能力

- 新建馆区外轮廓
- 绘制通道
- 绘制障碍区
- 绘制墙体
- 设置出入口
- 设置服务点
- 绘制分区边界
- 绘制 Booth 多边形
- 编辑 Booth 顶点
- 删除 Booth 图元
- Booth 编号、类型、分区、面积、备注编辑
- Booth 批量复制、对齐、等距排列
- Booth 批量编号
- Booth 批量类型设置

### 6.3 半结构化能力边界

编辑器不是通用矢量工具，需保留业务结构约束：

- 管理员不能随意定义未知图元类型。
- Booth 图元必须携带业务字段：
  - `boothNo`
  - `boothType`
  - `zoneId`
  - `areaSqm`
  - `selectable`
- `ZONE_BOUNDARY` 必须关联现有业务 `zoneId` 或创建待绑定分区。
- 删除已绑定 booth 图元时，若 booth 已被订单、锁位、已确认选位引用，则禁止直接删除。

### 6.4 校验规则

发布前至少校验：

- 必须存在且仅存在一个 `HALL_BOUNDARY`
- Booth 多边形必须闭合且面积大于 0
- Booth 不得完全落在馆外
- Booth 不得与 `OBSTACLE` 重叠
- Booth 不得穿越 `WALL`
- 可选 Booth 应处于某个可用分区内
- Booth 编号在同一 event 内唯一
- `SCENE_DRIVEN` 模式下，图形 booth 数量与将发布的 booth 主数据数量一致
- 若已有 area tier，图形面积与业务面积的误差必须在允许范围内

面积误差规则建议：

- 默认容忍误差 `<= 0.5㎡`
- 超过阈值禁止发布，需管理员手工修正

## 7. 草稿、发布与同步

### 7.1 版本状态

- `DRAFT`：管理员当前编辑版，仅后台可见
- `PUBLISHED`：前台业务使用版
- `DISCARDED`：废弃草稿

### 7.2 发布规则

采用双版本模式：

- 编辑只修改草稿 revision
- 发布成功后，系统把该 revision 标记为 `PUBLISHED`
- 销售和展商只读取 `published_revision`

### 7.3 发布同步策略

发布时系统执行：

1. 校验草稿
2. 生成待发布 booth 图元清单
3. 与当前 booth 主数据做差异比较
4. 对未被交易引用的 booth 执行新增 / 更新 / 删除
5. 生成新的 `booth_scene_bindings`
6. 写审计日志
7. 原子切换 `published_revision_id`

### 7.4 发布冲突规则

如果以下任一条件成立，禁止发布：

- 存在待审核选位申请
- 存在锁位中的 booth
- 存在已确认 booth 被删除或改动边界
- 存在与现有订单 boothType / zone 规则冲突的图形变更

允许发布的变更：

- 新增未被使用的 booth
- 调整通道、障碍、墙体、服务点等非交易对象
- 调整未被锁定 / 未被确认的 booth 几何和样式

### 7.5 历史回看

管理员可查看历史发布版本，但本轮不要求可一键回滚；若后续需要回滚，应生成新的草稿并重新发布，而不是直接把旧版强切为当前版。

## 8. 与现有 booth 业务的关系

### 8.1 FORM_ONLY

- `booths` 仍由后台表单维护
- `x/y/width/height` 可继续作为简化平面坐标
- 不要求存在 `scene_feature`

### 8.2 SCENE_DRIVEN

- `booths` 仍是交易主表
- 但 booth 的来源为已发布 `BOOTH` 图元
- `booths` 中保留基础业务字段：
  - `event_id`
  - `zone_id`
  - `booth_no`
  - `booth_type`
  - `area_sqm`
  - `status`
- 几何不再依赖 `x/y/width/height` 矩形表达；矩形字段仅作为兼容缓存，不作为主真相

### 8.3 一致性规则

- `SCENE_DRIVEN` 下，booth 图元是几何真相，`booths` 是交易真相。
- booth 图元发布后同步刷新 `booths` 的展示缓存字段。
- 选位、锁位、审核、释放等业务状态继续只写 `booths` 及其关联表。
- 前台展示时按 `published_revision + booth status` 合并渲染。

## 9. 销售与展商可视化选位

### 9.1 入口

- 销售：订单详情中的“协助选位”入口
- 展商：订单详情中的“去选位”入口

路由不变：

- `/admin/orders/:orderId`
- `/exhibitor/orders/:orderId/selection`

### 9.2 页面结构

#### 桌面端

- 左侧筛选面板
- 中间场景图
- 右侧当前选中清单 / 详情

#### 移动端

- 顶部订单上下文
- 中部地图
- 底部抽屉显示当前选择与操作按钮

### 9.3 视图模式

必须同时支持：

- 地图视图
- 列表视图

交互规则：

- 展商移动端默认单点查看，再加入已选
- 销售桌面端可直接多选
- 桌面端和移动端共享同一份 `selectedBoothIds` 状态语义

### 9.4 地图颜色语义

- `AVAILABLE`：可选
- `LOCKED`：锁定中
- `CONFIRMED`：已确认
- `UNAVAILABLE`：不可选
- `SELECTED`：当前已选

颜色只表达业务状态，不叠加过多视觉噪音。

### 9.5 选位约束

沿用现有选位规则，并额外补充：

- 仅可选当前订单所属 event 的已发布场景中的 booth
- booth `boothType` 必须等于订单 `boothType`
- booth 必须处于 `AVAILABLE`
- 所选 booth 组合面积必须严格等于订单要求面积
- 同一订单同一时刻只能存在一个有效选位申请

### 9.6 信息展示

地图点击 Booth 时，详情面板显示：

- 展位号
- 展位类型
- 分区名称
- 面积
- 当前状态
- 是否已被锁定
- 若不可选，显示原因

订单上下文必须显示：

- 展会名称
- 订单短号
- 要求面积
- 展位类型
- 当前已选面积
- 剩余面积差

## 10. 管理后台页面调整

### 10.1 展会管理

展会创建 / 编辑页新增：

- 展位来源模式
  - `纯表单模式`
  - `可视化场景模式`

若选择 `可视化场景模式`：

- 展会详情页显示“编辑馆图”入口

### 10.2 展位管理

- `FORM_ONLY` 展会显示现有表格管理页
- `SCENE_DRIVEN` 展会改为只读清单 + 跳转场景编辑器
- 对 `SCENE_DRIVEN` 展会隐藏手工新增 Booth 按钮

### 10.3 选位审核

审核页需支持：

- 查看选中 booth 在地图中的位置
- 高亮本次申请涉及的 booth
- 查看相邻通道、障碍、出入口等上下文

## 11. API 设计

### 11.1 管理端

```text
GET    /api/events/{eventId}/scene
POST   /api/events/{eventId}/scene/draft
PUT    /api/events/{eventId}/scene/draft/features
POST   /api/events/{eventId}/scene/draft/validate
POST   /api/events/{eventId}/scene/draft/publish
GET    /api/events/{eventId}/scene/revisions
GET    /api/events/{eventId}/scene/revisions/{revisionId}
```

### 11.2 选位端

```text
GET /api/events/{eventId}/scene/published
GET /api/events/{eventId}/scene/published/booths
```

其中 `published/booths` 返回：

- 图形 booth 几何
- booth 业务状态
- booth 业务展示字段

避免前端自己拼多次接口。

### 11.3 兼容现有接口

- `GET /api/booths?eventId=&status=` 继续保留
- `FORM_ONLY` 展会继续由该接口直接支撑选位页
- `SCENE_DRIVEN` 展会可由新的聚合接口提供地图数据，`/api/booths` 作为兼容与后台列表用途

## 12. 权限

### 12.1 新增权限点

- `menu.admin.scene`
- `page.admin.scene.editor`
- `action.admin.scene.save-draft`
- `action.admin.scene.validate`
- `action.admin.scene.publish`
- `action.admin.scene.discard-draft`
- `page.admin.scene.revision-list`
- `page.admin.scene.revision-detail`

### 12.2 权限原则

- 系统管理员、主办方管理员可编辑和发布
- 销售只读已发布场景，不可编辑
- 展商只读已发布场景，不可见编辑结构和历史版本

## 13. 技术实现建议

### 13.1 前端

- 总体 UI 仍使用 React + TypeScript + Ant Design + ProComponents
- 图形编辑与渲染层允许引入专用 2D 图形库
- 推荐采用“业务壳 + 图形内核”分层：
  - Ant Design 负责布局、表单、面板、工具栏、列表
  - 图形库负责画布、拾取、多边形编辑、缩放和平移

### 13.2 后端

- PostgreSQL 存储场景对象与 revision 快照
- `geometry_json` 使用 JSONB
- 关键发布事务必须单事务完成
- 若后续要做空间检索，可再引入 PostGIS；本轮不强依赖

### 13.3 审计

必须记录：

- 草稿保存
- 图元新增 / 编辑 / 删除
- 场景校验失败
- 场景发布
- 发布失败原因
- 版本查看

## 14. 测试与验收

### 14.1 管理端

- 创建 `SCENE_DRIVEN` 展会
- 画出馆区边界
- 画出通道、障碍、墙体、出入口、服务点
- 创建多个 Booth 多边形
- 保存草稿成功
- 发布成功后生成 booth 绑定

### 14.2 约束校验

- Booth 在馆外时禁止发布
- Booth 与障碍重叠时禁止发布
- Booth 编号重复时禁止发布
- 已锁定 booth 被删除时禁止发布
- 已确认 booth 边界被改动时禁止发布

### 14.3 选位端

- 销售桌面端可在地图上多选 Booth
- 展商移动端可点击单个 Booth 查看详情并加入已选
- 所选 Booth 面积不等于订单面积时不得提交
- 同一 booth 被他人锁定后地图状态及时刷新
- 审核页可在地图中回看本次申请位置

### 14.4 兼容性

- `FORM_ONLY` 展会的既有下单、付款、选位链路不受影响
- `SCENE_DRIVEN` 展会可正常走订单、付款、选位、审核链路

## 15. 与现有文档的关系

- 本设计扩展并部分替代 [D:\project\pe\docs\superpowers\specs\2026-06-14-ui-productization-design.md](D:\project\pe\docs\superpowers\specs\2026-06-14-ui-productization-design.md) 中“选位页采用简单格子图、不含可视化编辑器”的约束。
- 该旧约束继续适用于 `FORM_ONLY` 展会。
- 对于 `SCENE_DRIVEN` 展会，以本设计为准。

## 16. 结论

本轮采用：

- 展会级模式开关：`FORM_ONLY` / `SCENE_DRIVEN`
- 管理员桌面端半结构化场景编辑器
- 草稿 / 已发布双版本场景
- Booth 多边形建模
- 销售与展商基于已发布场景的可视化选位

这样既保留当前系统已实现的业务主链，也能把“真实场馆图 + 在线绘制 + 可视化选位”做成产品亮点，而不是孤立 demo。
