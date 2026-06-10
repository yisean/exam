---
name: spec-prd
description: '把原始需求澄清并定稿为规范化 PRD（带 R/F 功能需求编号与 AE/AC 可验收标准），写入 docs/product/prd/。Use when 用户给出原始需求/想法、要写或更新一份 PRD、把一个特性意图固化成正式需求。新需求取新序号；已有特性的增量改动请改用 /spec-change。'
argument-hint: "[原始需求 / 特性意图，可留空由我追问]"
---

# 需求阶段：原始需求 → 规范化 PRD

**当前年份 2026**，用于给文档命名与写日期。

这是「需求 → 原型 → 设计 → 计划 → 开发 → 评审 → 测试 → 合并」流水线的**第 1 阶段（需求）**，回答 **WHAT**。下一步 `/spec-prototype`（界面长什么样），再下一步 `/spec-design`（HOW 的结构层）、`/spec-plan`（HOW 的执行层）。本 skill 只澄清与沉淀需求，**不写代码、不做实现设计**。

## 术语表（编号约定的名词）

| 缩写 | 全称 | 含义 | 写法 | 例子 |
| --- | --- | --- | --- | --- |
| **R** | Requirement | 功能需求编号 | `R1 / R2 / R3…` | `R8 预约成功占用该时段一个部门名额` |
| **F** | Feature / Function | 功能需求编号（与 R 等价，另一种字母） | `F1 / F2 / F3…` | `F7 模拟考试不限考试次数` |
| **AE** | Acceptance Example | 验收示例/场景，须标注覆盖的需求号 | `AE1 / AE2…` | `AE2（R8）约满后第 4 个部门不可预约` |
| **AC** | Acceptance Criteria | 验收标准（与 AE 等价，另一种字母） | `AC1 / AC2…` | `AC5（F8/F10）超出补考次数被拒绝` |

> **R/F 二选一、AE/AC 二选一**：一份 PRD 内部统一用一种字母即可（R+AE 或 F+AC 均可）。两套字母只是不同 PRD 的习惯差异，作用完全相同。

## 编号约定（生成 PRD 时必须遵守）

1. **功能需求**逐条编号 `R/F`，**验收标准**逐条编号 `AE/AC`。
2. **续编不重排**：新增需求往后接（如已有到 R15，新增就是 R16、R17），**绝不把已有需求重新编号**——否则 plan、测试、历史引用全部失效。
3. **每条 AE/AC 必须在括号里标注它覆盖的需求号**，如 `AE2（R8）…`、`AC5（F8/F10）…`。
4. **覆盖完整**：每条 `R/F` 至少被一条 `AE/AC` 覆盖；没有验收的需求要么补验收，要么降级为非目标。
5. **可追溯链**：`R/F`（要做什么）→ plan 任务（怎么做）→ `AE/AC`（怎么算做对了），三段保持引用、双向可查。
6. 建议每条 `R/F` 隐含「**作为<角色>，我想<做什么>，以便<价值>**」的价值表达，必要时直接用该句式书写，避免只写功能不写价值。
7. **可测句式（按需，EARS 风格）** — 复杂或高风险的 `R/F`，用结构化句式写成可直接验收的形式，消除歧义：
   - 事件驱动：**当 <触发> 时，系统应 <可观测行为>**
   - 条件约束：**若 <条件>，则系统应 <可观测行为>**
   - 状态驱动：**在 <某状态> 期间，系统应 <可观测行为>**
   简单需求仍可用第 6 条价值句式；两者可叠加（价值说"为什么"，可测句式说"做对了是什么样"）。

## 核心原则（compound-engineering）

1. **单一事实源** — 优先级 `docs/engineering/constitution.md`（工程宪法 / 原则）> `docs/engineering/workflow.md`（流程与阶段标准）> `docs/README.md` > 本 skill 内置默认值。下面各条原则是宪法的精简内置版，项目文档存在时以其为准。
2. **双向可追溯** — 见上「编号约定」。
3. **右尺寸** — 简单清晰的需求少追问、直接定稿；模糊或跨切的需求才多轮澄清。
4. **显式非目标** — 必须写清「这次不做什么」，避免范围蔓延。
5. **回流而非另起** — 已有特性的改动是**变更**，复用原序号，改用 `/spec-change`；不要为它新建 PRD。

## 交互规则

需要澄清时用 `AskUserQuestion`（schema 未加载先 `ToolSearch` `select:AskUserQuestion`），**一次一个问题**，多用单选、少用多选。答案已经清晰就不追问，直接生成。

## 原始需求

<requirement> #$ARGUMENTS </requirement>

**若上面为空**，先问用户：「请描述你要做的需求 / 想解决谁的什么问题（一句话也行）。」拿到再继续。

## 执行流程

### Phase 0 · 加载约定 + 判定增量/新特性

1. 读以下文件（存在才读，作为单一事实源）：
   - `docs/engineering/constitution.md` —— 工程宪法（不可妥协原则），最高优先级。
   - `docs/engineering/workflow.md` —— 阶段 1「完成标准」与「命名与追溯约定」。
   - `docs/README.md` —— 目录结构与文件命名。
   - `docs/engineering/registry.md` —— **NNN 取号登记表**（多人协作下保证序号唯一的单一事实源），读出你为本特性预留的 `NNN`。
   - `docs/product/prd/` 现有 PRD —— 沿用既有编号字母（R 还是 F），并与 registry 交叉确认 NNN 没被占用。
   - 若都不存在（新项目）→ 用内置默认约定，并提示：「该项目还没有 docs 流程骨架，要不要我先建 `docs/product/prd|brainstorms`、`docs/engineering/prototype|design|plans`，并初始化 `docs/engineering/constitution.md`（工程宪法）+ `docs/engineering/workflow.md`（流程总纲）+ 项目根 `CLAUDE.md`（编码规约）？」初始化这三份用 **`spec-init`** 资源包的脚本：`~/.claude/skills/spec-init/init-project.ps1`（Windows）/ `init-project.sh`（macOS/Linux）——它把 `spec-init/templates/` 下的 `constitution.md`、`workflow.md` 复制到 `docs/engineering/`、`CLAUDE.md` 复制到项目根（缺失才建，`-Force`/`--force` 覆盖）；或手动复制这三份模板。
2. **判定**：本需求是「已有特性的增量改动」还是「全新特性」？
   - 属于某个已有 PRD 的主题 → **停下**，告知这是变更，建议改用 `/spec-change`（复用原 `NNN`、续编 `R/F`、就地扩 PRD）。
   - 全新特性 → **取号走「先登记后开工」**（见下）。

   **NNN 取号约定（多人协作必读）**：序号分配必须发生在共享的 main 上、且在开特性分支之前——否则各自分支里 `max+1` 互相看不见，合并才撞车。
   - **标准做法**：先在最新的 main 上往 `docs/engineering/registry.md` 追加一行 `reserved`（`| NNN | slug | owner | date | reserved |`），push 进 main（仅这一行，可免 PR）；并发抢号时后 push 者会被拒/冲突，当场暴露，改取下一个号。**号一经预留即作废不回收**（回收会打断历史引用）。然后开 `feat/NNN-slug` 分支再跑本 skill，直接用登记好的 NNN。
   - **registry 不存在时**（老项目/未初始化）：退回 `NNN = 现有最大序号 + 1`，并提示「多人协作建议建 `docs/engineering/registry.md` 先登记后开工，避免并行取号撞号」。
   - 详见 `docs/engineering/workflow.md`「取号约定」节。

### Phase 1 · 澄清（按需，右尺寸）

可选：探讨期先用 `/ce-brainstorm` 把模糊想法发散成需求原稿，落到 `docs/product/brainstorms/YYYY-MM-DD-<slug>-requirements.md`，再回来定稿。

一次一问澄清，直到能写出完整 PRD：目标/非目标、用户与角色、关键决策与取舍、边界、成功指标、待解决问题。**澄清得到的结论与取舍记进 PRD 的「关键决策」，未决的记进「待解决问题」——别让答案只停在对话里。**

### Phase 2 · 生成 PRD

**先用 Read 读取本 skill 目录下的 `templates/prd.md`**（PRD 骨架模板，含 frontmatter + 各节占位 + 可测句式提示），按它逐节填充，写到 `docs/product/prd/YYYY-MM-DD-NNN-<slug>.md`。要点（模板没读到时按此兜底）：

- **frontmatter `origin`**：有 brainstorm 写其 repo-relative 路径（如 `docs/product/brainstorms/2026-06-03-xxx-requirements.md`）；无则写来源简述（`直接需求` / `客户口头需求` / `线上反馈`），不留空、不瞎指文件。
- **正文结构**：背景 → 目标/**非目标** → 用户与角色 → 关键决策 → **功能需求**（`R/F`，续编不重排）→ **非功能约束**（无则写「无特殊要求」）→ **验收标准**（`AE/AC`，每条标注覆盖的需求号）→ **覆盖矩阵**（生成规则见 Phase 3）→ 成功指标 → 依赖与假设 → 待解决问题。
- 全部用 **repo-relative 路径**引用文件，不用绝对路径。

### Phase 3 · 自检（覆盖矩阵）+ 可选评审

- **生成覆盖矩阵**并写进 PRD，逐行核对，缺口即补：

  | 需求 | 覆盖它的验收 | 非功能约束 |
  | --- | --- | --- |
  | R1 | AE1、AE2 | 无 |
  | R8 | AE2、AE5 | 约满判定需并发安全 |

  规则：**每条 `R/F` 至少一条 `AE/AC`**（该行验收为空 = 漏验收）；**每条 `AE/AC` 至少指向一条 `R/F`**（悬空 = 多余验收或漏写了需求）。缺口要么补、要么把该需求降级为非目标。
- 其余自检：非目标已写；待解决问题已列；编号无重排；高风险需求已用可测句式。
- 高风险/大特性可跑 `/ce-doc-review` 让 persona 评审找漏洞（或用 Workflow 扇出可行性/安全/范围/一致性多视角并行评审），再回流修订。

### Phase 4 · 交接

输出：PRD 路径、序号 `NNN`、需求号范围（如 R1–R9 / AE1–AE6）一句话摘要。然后提示下一步：

- 涉及新界面/复杂交互 → `/spec-prototype`（以本 PRD 为输入画原型，发客户确认）。
- 纯后端或微调 → 可跳过原型，直接 `/spec-design`。
- 后续若需求有改动 → `/spec-change`（先回流文档再改代码）。
