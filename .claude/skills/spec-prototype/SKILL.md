---
name: spec-prototype
description: '依据已定稿 PRD，按 prototype/_spec.md 生成或更新纯静态可点击原型（仿 Element-UI、无外部依赖、双击即开），用于内部对齐或向客户演示。Use when PRD 已完成、需要把界面/交互画成原型、或要把原型发客户确认。客户确认后的需求变更请走 /spec-change（先回流 PRD 再改原型）。'
argument-hint: "[目标 PRD 路径或特性名，可留空由我识别最近的 PRD]"
---

# 原型阶段：PRD → 可点击静态原型

**当前年份 2026**。

流水线**第 2 阶段（原型，按需）**，回答**「界面长什么样、交互怎么走」**。上游是 `/spec-prd`，下游是 `/spec-design`。

> 原型是**沟通与验证载体，不是实现**。它定稿后，相关页面成为 plan 里前端任务的「UI 参考」。**纯后端或微调的特性可跳过本阶段**。

## 术语提示

PRD 里的功能需求用 **R/F** 编号、验收标准用 **AE/AC** 编号（详见 `/spec-prd` 术语表）。原型只需覆盖其中**与界面相关的 R/F**；纯后端/逻辑类需求不画页面。

## 核心原则

1. **单一事实源** — 原型规范以 `docs/engineering/prototype/_spec.md` 为最高约束（不存在则用 `templates/_spec.md`）；跨阶段原则以 `docs/engineering/constitution.md`（工程宪法）为准。本 skill 内置默认是它们的精简版。
2. **PRD 驱动** — 每个页面/元素都对应 PRD 的某条 `R/F`；反过来 PRD 的每条 UI 相关需求都要能在原型找到落点。
3. **零依赖、可直接发客户** — 纯静态 HTML，双击即在浏览器打开，无需联网/后端。

## 原型构建规范

项目的 `docs/engineering/prototype/_spec.md` 是**最高约束**。它不存在时，**先用 Read 读取本 skill 目录下的 `templates/_spec.md`**（内置默认规范）。核心红线（模板没读到时按此兜底）：

- 每页独立 HTML，只引 `assets/app.css`；**只用 `app.css` 已有 class**（btn / card / table / tag / form-item / input / select / steps / tabs / dialog / pagination / stat-card 等），微调用内联 `style`，**不引新 CSS 文件**。
- **不写复杂 JS**：仅少量内联 `<script>` 做 tab 切换、`.dialog-mask` 开关、选项高亮；页面跳转用 `<a href="xxx.html">`。
- 仿 Element-UI、中文界面、措辞专业贴合业务；新页面挂进 `index.html`；纯静态零依赖、双击即开。
- 新页面可参照 `templates/page.html` 骨架起步。

## 执行流程

### Phase 0 · 加载约束 + 选定 PRD

1. 读 `docs/engineering/prototype/_spec.md`（强约束，存在则覆盖上面的内置默认）。
2. 读 `docs/engineering/prototype/assets/app.css`，列出**可用 class 清单**，生成时只能用这些。
3. 读 `docs/engineering/prototype/index.html`（现有导航）与已有页面，沿用风格、避免重复造页。
4. 读目标 PRD（`$ARGUMENTS` 指定，或取 `docs/product/prd/` 中最近 `status: active` 的一份），确认本次要覆盖哪些 `R/F`。
5. 若 `prototype/` 目录不存在（新项目）→ 提示先建 `prototype/` 并放入 `_spec.md`、`index.html`、`assets/app.css` 骨架（可直接复制本 skill 的 `templates/_spec.md` 与 `templates/page.html` 起步），或由我搭一个最小骨架。

### Phase 1 · 选页面

从 PRD 的 **UI 相关 R/F** 推出需要新增/修改哪些页面（管理端/学员端/其它角色端分组）。纯后端需求不画。列出页面清单与各自覆盖的需求号，必要时一次一问跟用户确认范围。

### Phase 2 · 生成/更新页面

按「原型构建规范」逐页产出 HTML，放 `docs/engineering/prototype/<page>.html`，并把入口挂进 `index.html`。关键交互（多步骤、弹窗、状态切换如「约满/已截止/已约」）要在原型里**可点击跑通主流程**。

### Phase 3 · 自检

- PRD 的每条 UI 相关 `R/F` 都能在原型找到对应页面/元素。
- 只用了 `app.css` 已有 class；无外部依赖；双击可打开。
- 主流程从入口到结果可点击走通。

### Phase 4 · 交接 + 客户确认

输出：新增/修改的页面清单 + 各自覆盖的 `R/F` + `index.html` 入口。然后提示：

1. **把原型发客户确认**（可直接打包 `prototype/` 目录发送）。
2. 客户反馈若**改了需求**（增删字段、改流程、改规则）→ 走 `/spec-change`：**先回流 PRD（续编/调整 R/F 与 AE/AC）→ 再回到本 skill 改原型**，保持单一事实源、先文档后实现。
3. 客户确认无变更 → 进入 `/spec-design`，以「PRD + 已定稿原型」为输入做技术设计（架构 + ER + 详细设计）。
