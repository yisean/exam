# CLAUDE.md

本仓库是「云帆培训考试系统」：后端 `exam-api`（SpringBoot + MyBatis-Plus + Shiro），前端 `exam-vue`（Vue2 + Element-UI）。

## 必读规范（开发前先看）

- **研发流程**：[`docs/engineering/workflow.md`](docs/engineering/workflow.md) —— 需求→原型→计划→开发→评审→测试→合并，每阶段做什么、何时算完成。
- **开发规范**：[`docs/engineering/conventions.md`](docs/engineering/conventions.md) —— 命名/分层/API/数据库/异常/鉴权/前端约定的**单一事实源**。写代码必须遵循。
- 后端细则见 [`exam-api/CLAUDE.md`](exam-api/CLAUDE.md)，前端细则见 [`exam-vue/CLAUDE.md`](exam-vue/CLAUDE.md)（在对应子目录工作时自动加载）。

## 硬约束（最高频，务必遵守）

1. **跟随既有代码**：命名、分层、风格与同模块现有代码一致；优先复用既有基类/工具/组件，不擅自引入新依赖或新风格。
2. **流程优先文档**：改动需求范围时**先回流 PRD/plan 再写代码**（用 `/spec-change`）；需求→原型→计划分别用 `/spec-prd`、`/spec-prototype`、`/spec-plan`。
3. **追溯编号**：PRD 用 `R/F` 编号需求、`AE/AC` 编号验收，续编不重排；plan 任务映射到 `R/F`，测试对照 `AE/AC`。
4. **不在默认分支开发**：先开特性分支；一次提交聚焦一件事，文档与代码改动分开提交。
5. **DB 变更**：落 `docs/ops/install/migration-YYYY-<特性名>.sql`，并与 plan 的 ER 模型一致。

## 提交

`<type>: <简述>`，正文列要点，结尾带 `Co-Authored-By`。type：`feat / fix / docs / refactor / chore`。
