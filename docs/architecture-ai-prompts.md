# 无连线技术架构图 AI 生图提示词

本文用于生成项目介绍页、技术方案或汇报材料中的技术架构视觉图。目标是表现系统分层和模块关系，但画面中不使用连接线、箭头或流程线，避免生成传统网络拓扑图效果。

## 推荐主提示词

```text
为“中国国际体育用品博览会展商业务系统”生成一张现代企业级软件技术架构信息图，16:9 横向构图，中文标签清晰可读，简洁高级的蓝白配色，少量橙色作为强调色，白色背景，轻微玻璃拟态和卡片阴影。

采用“分层模块墙”布局，不使用任何连线：顶部是一整块“用户与终端”区域，包含管理员、组织方、销售、财务、客服、展商、浏览器和移动端；中部是“业务前端”区域，包含 React、TypeScript、Ant Design、场景编辑器、订单与选位、RBAC 权限中心；下部是“后端服务”区域，包含 Spring Boot 3、Java 17、认证服务、订单服务、场景服务、CAD 转换任务、文件服务；底部是“数据与基础设施”区域，包含 PostgreSQL、Redis、持久化文件存储、DWG/DXF 转 SVG 转换器、Render 部署。

每个模块使用独立圆角卡片或立体面板，按照网格对齐，模块之间使用留白和不同深浅背景分区表达层级关系。整体像产品架构海报和 SaaS 技术白皮书插图，信息密度适中，文字层级清楚，专业、可信、可用于 GitHub README 和项目路演。
``` 

## 中文海报版提示词

```text
企业软件技术架构海报，项目名称：中国国际体育用品博览会展商业务系统，横向 16:9，白色和浅蓝色背景，蓝色主色，橙色点缀，现代 SaaS 产品视觉，中文排版，清晰的标题和模块卡片。

画面分成四个水平色块区域：用户终端、业务前端、后端服务、数据基础设施。每个区域内部放置多个独立的圆角矩形模块卡片：管理员、组织方、销售、财务、客服、展商；React、TypeScript、Ant Design、场景编辑器、订单管理、展位选位、RBAC 权限；Spring Boot、Java 17、认证服务、业务服务、场景服务、CAD 转换、文件服务；PostgreSQL、Redis、文件存储、DWG/DXF、SVG、Render。

重点展示“场景编辑器”“CAD 转换”“RBAC 权限”“展商订单”四个核心能力。卡片之间只用空间分层、颜色深浅、标题分区和对齐关系表达架构，不要表达数据流。
``` 

## 英文模型增强版提示词

```text
Modern enterprise software architecture infographic for a Chinese sports expo exhibitor business platform, 16:9 landscape, clean white and cool blue palette with subtle orange accents, premium SaaS product design, readable Chinese labels, high-end technical whitepaper illustration.

Use a layered modular wall layout with four horizontal sections: Users & Devices, Frontend Experience, Backend Services, Data & Infrastructure. Show separate rounded cards for administrators, organizers, sales, finance, customer service, exhibitors, browser, mobile browser, React, TypeScript, Ant Design, scene editor, order management, booth selection, RBAC permission center, Spring Boot 3, Java 17, authentication, business services, venue scene service, CAD conversion jobs, file service, PostgreSQL, Redis, persistent file storage, DWG/DXF to SVG converter, and Render deployment.

No network topology. No connector lines. No arrows. No flow lines. No sequence arrows. Do not connect any cards. Communicate hierarchy through spacing, alignment, section backgrounds, card sizes, typography and subtle shadows only. Balanced composition, editorial architecture poster, polished product documentation visual.
``` 

## 反向提示词

```text
不要连接线，不要箭头，不要流程线，不要网络拓扑，不要 UML 连线，不要时序图，不要树状连线，不要交叉线，不要复杂背景，不要霓虹赛博朋克，不要服务器机房照片，不要人物大特写，不要乱码，不要不可读的小字，不要过度拥挤，不要重复模块，不要透视变形，不要水印。
``` 

## 版式约束

- 画布比例：16:9，建议 1920 × 1080。
- 每个模块使用独立卡片，卡片之间至少保留 24 px 空白。
- 采用四层横向区域，不使用纵向流程箭头。
- 标题与模块名尽量短，生成后建议在 Figma、PPT 或 SVG 中重新排版中文文字。
- 如果模型对中文文字生成不稳定，可先生成无文字卡片布局，再使用设计工具补充准确标签。
