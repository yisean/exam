# 项目文档索引

本目录按 **产品 / 研发 / 运维** 三层组织文档。新建文档时请放入对应目录，目录名统一用英文小写连字符，正文用中文。

```
docs/
├── product/        做什么、为什么 —— 产品与需求
├── engineering/    怎么做 —— 研发过程与技术资料
└── ops/            怎么交付 —— 部署与运维交付物
```

## product/ —— 产品与需求

| 路径 | 说明 |
| --- | --- |
| [`product/STRATEGY.md`](product/STRATEGY.md) | 产品策略：目标问题、用户、关键指标、工作主线 |
| [`product/functional-spec.md`](product/functional-spec.md) | 功能方案总纲（原 `功能方案.md`） |
| [`product/brainstorms/`](product/brainstorms/) | 需求探讨原始稿（脑暴 → 定稿前） |
| [`product/prd/`](product/prd/) | 正式需求定稿（PRD）：[001 考试助理与预约考试](product/prd/2026-05-29-001-exam-assistant-booking.md)、[002 开放状态·模拟/正式·补考](product/prd/2026-05-29-002-exam-open-status-mock-retake.md)、[003 管理员一键重置用户密码](product/prd/2026-06-03-003-admin-reset-user-password.md) |

## engineering/ —— 研发过程

| 路径 | 说明 |
| --- | --- |
| [`engineering/workflow.md`](engineering/workflow.md) | **研发流程总纲**：需求→原型→计划→开发→评审→测试→合并，各阶段做法与产出物 |
| [`engineering/conventions.md`](engineering/conventions.md) | **开发规范**：命名/分层/API/数据库/异常/鉴权/前端约定的单一事实源（后端 + 前端）。AI agent 经根 `CLAUDE.md` 自动加载 |
| [`engineering/plans/`](engineering/plans/) | 实现计划，命名 `日期-序号-类型-描述.md`，带 frontmatter |
| [`engineering/prototype/`](engineering/prototype/) | 纯静态 HTML 原型 + 构建规范 `_spec.md` |
| [`engineering/architecture/`](engineering/architecture/) | 架构、数据库结构、源码说明等技术资料 |

## ops/ —— 部署与运维

| 路径 | 说明 |
| --- | --- |
| [`ops/部署手册.pdf`](ops/部署手册.pdf) | 部署手册 |
| [`ops/deployment-frontend-nginx.md`](ops/deployment-frontend-nginx.md) | 前后端解耦部署：Nginx 托管前端 + 反代后端 |
| [`ops/install/`](ops/install/) | 安装资源（含 `数据库脚本.sql`、迁移脚本） |
| [`ops/windows-service/`](ops/windows-service/) | 注册 Windows 服务相关文件 |
| [`ops/run-package/`](ops/run-package/) | 运行包（启动脚本、本地配置示例） |

## 约定

- **新需求流转**：`product/brainstorms/` → `product/prd/` → `engineering/plans/` → 编码。
- **架构决策**：技术选型、数据库变更等沉淀到 `engineering/architecture/`，PDF 建议逐步转为 Markdown 以便评审与检索。
- **文件命名**：计划/记录类文档用 `YYYY-MM-DD-NNN-<type>-<slug>.md`；其余用英文小写连字符。
