# 研发流程总纲：从需求到上线

> 本文定义本项目「需求 → 原型 → 设计 → 计划 → 开发 → 代码评审 → 测试 → 合并」的标准流程：每个阶段**做什么、谁来做、产出什么、何时算完成**。配合 [`docs/README.md`](../README.md) 的目录结构使用。
> 原则基线见 [`constitution.md`](constitution.md)（工程宪法）；开发细则见 [`conventions.md`](conventions.md)。冲突时优先级：**`constitution.md` > 本文 > skill 内置默认**。

## 项目文档目录结构

文档按 **产品 / 研发 / 运维** 三层组织，流程各阶段的产出物对号入座（详见 [`docs/README.md`](../README.md)）。

```
docs/
├── README.md                    文档导航索引
├── product/                     做什么、为什么 —— 产品与需求
│   ├── STRATEGY.md              产品策略（阶段 0）
│   ├── functional-spec.md       功能方案总纲
│   ├── brainstorms/             需求探讨原始稿（阶段 1）
│   └── prd/                     正式需求定稿 PRD（阶段 1）
├── engineering/                 怎么做 —— 研发过程
│   ├── constitution.md          工程宪法（原则基线）
│   ├── workflow.md              ← 本文：研发流程总纲
│   ├── conventions.md           开发规范（命名/分层/约定）
│   ├── registry.md              NNN 取号登记表（多人协作，先登记后开工）
│   ├── prototype/               可点击静态原型 + _spec.md（阶段 2）
│   ├── design/                  技术设计：概要设计 + ER + 详细设计（阶段 3）
│   ├── plans/                   实现计划：任务 + migration + 覆盖矩阵（阶段 4）
│   └── architecture/            架构、数据库结构、源码说明
└── ops/                         怎么交付 —— 部署与运维
    ├── 部署手册.pdf
    ├── install/                 安装资源 + 数据库/迁移脚本（阶段 4 落 migration）
    ├── windows-service/         注册 Windows 服务
    └── run-package/             运行包（启动脚本、本地配置示例）
```

---

## 全景流水线

```
策略(可选)        需求            原型           设计          计划          开发          代码评审        测试         合并
STRATEGY.md → brainstorm/PRD → prototype/ → design/ → plans/ → 编码 → code review → test/verify → commit + PR
   └────────────── product/ ──────────────┘   └───────────────── engineering/ + 代码仓 ──────────────────┘
```

每个阶段都有**进入标准（Definition of Ready）**和**完成标准（Definition of Done）**——上一阶段的产出物就是下一阶段的进入条件。允许小步快跑、阶段回流（如开发中发现需求漏洞回到 PRD 修订），但产出物要同步更新，保持单一事实源（走 `/spec-change`）。

---

## 阶段 0 · 策略对齐（可选，低频）

| | |
| --- | --- |
| **目的** | 明确产品要解决的目标问题、目标用户、关键指标、工作主线，作为后续一切需求的上游依据 |
| **何时做** | 新产品启动、方向调整时；不是每个特性都要做 |
| **怎么做** | 运行 `/ce-strategy`，或手工维护 |
| **产出物** | [`docs/product/STRATEGY.md`](../product/STRATEGY.md) |
| **完成标准** | 目标问题、用户、关键指标、Tracks 四节齐全且互不矛盾 |

---

## 阶段 1 · 需求

| | |
| --- | --- |
| **目的** | 把一个模糊想法澄清成「问题 + 关键决策 + 可验收的需求清单」，再固化为正式 PRD |
| **谁来做** | 产品/需求负责人，与开发协作 |
| **怎么做** | 用 `/spec-prd`（原始需求 → 规范化 PRD，自动带 R/F 与 AE/AC 编号）；探讨期可先 `/ce-brainstorm` 产出需求原稿，再定稿。已有特性的增量改动改用 `/spec-change` |
| **产出物** | 原稿 → [`docs/product/brainstorms/`](../product/brainstorms/)；定稿 → [`docs/product/prd/`](../product/prd/) |
| **进入标准** | 有一个明确的特性意图（一句话能说清要解决谁的什么问题） |
| **完成标准** | PRD 含：背景与问题、目标/非目标、用户与角色、关键决策、**带编号的功能需求**、**可验收的验收标准**、成功指标、依赖与假设、待解决问题 |

**PRD 必备要素**（参考 [PRD 001](../product/prd/2026-05-29-001-exam-assistant-booking.md)）：
- 功能需求编号（`R1/R2…` 或 `F1/F2…`），验收标准编号（`AE/AC`）并标注覆盖的需求号——保证 **PRD → 设计 → plan → 测试** 可双向追溯。
- 显式写出**非目标**与**待解决问题**，避免范围蔓延和隐性假设。

---

## 阶段 2 · 原型（按需）

| | |
| --- | --- |
| **目的** | 在写代码前把交互、信息架构、字段显隐用可点击页面表达出来，供内部对齐或向客户演示 |
| **何时做** | 涉及新界面/复杂交互的特性；纯后端或微调可跳过 |
| **怎么做** | 用 `/spec-prototype`，遵循 [`prototype/_spec.md`](prototype/_spec.md)：纯静态 HTML + `assets/app.css`，仿 Element-UI，无外部依赖，双击即可打开。发客户确认后若改需求，走 `/spec-change` |
| **产出物** | [`docs/engineering/prototype/`](prototype/)（页面 + `index.html` 导航 + `_spec.md` 规范） |
| **进入标准** | 对应 PRD 的关键交互已明确 |
| **完成标准** | 关键页面可点击跑通主流程；PRD 中的 UI 相关需求都能在原型找到对应 |

> 原型是**沟通与验证载体，不是实现**。它定稿后，相关页面成为设计/计划里前端任务的「UI 参考基线」。

---

## 阶段 3 · 设计

| | |
| --- | --- |
| **目的** | 定整体架构、数据模型与关键流程的详细设计，作为拆任务的前提（**设计独立成阶段，不再揉在计划里**） |
| **谁来做** | 开发负责人 / 架构 |
| **怎么做** | 用 `/spec-design`，以 PRD + 原型为输入 |
| **产出物** | [`docs/engineering/design/`](design/)，命名 `YYYY-MM-DD-NNN-<type>-<slug>-design.md`，带 frontmatter（`title/type/status/date/origin→PRD`） |
| **进入标准** | PRD 完成标准达成；如有界面，原型已定稿 |
| **完成标准** | 含**概要设计**（架构/模块、技术选型与决策、接口清单与契约、前端设计、权限四级、可观测与审计、NFR、风险）、**数据 ER 模型**（Mermaid `erDiagram`，带时间戳与字段长度规约）、**详细设计**（接口签名、核心逻辑、并发与幂等、代码结构落点）；每段标注覆盖的 `R/F`。先把概要 + ER 确认再写详细设计 |

---

## 阶段 4 · 计划（Plan）

| | |
| --- | --- |
| **目的** | 据设计把方案拆成可执行、可独立认领的工程任务：涉及哪些表/接口/页面、依赖顺序、迁移脚本、覆盖矩阵 |
| **谁来做** | 开发负责人 |
| **怎么做** | 用 `/spec-plan`（或 `/ce-plan`），以设计文档为主输入（PRD/原型作参照） |
| **产出物** | [`docs/engineering/plans/`](plans/)，命名 `YYYY-MM-DD-NNN-<type>-<slug>-plan.md`，带 frontmatter（`origin→design`） |
| **进入标准** | 设计完成标准达成 |
| **完成标准** | 实现单元可独立认领（Files / Dependencies / Patterns to follow / **设计依据**（指回 design 小节）/ 覆盖需求 / Test scenarios）；DB 变更落 `docs/ops/install/` 的 migration sql（**回滚段必填**、时间戳、varchar 字段长度，与 design 的 ER 逐字段一致）；**三向覆盖矩阵**（`R/F → U → AE/AC`）无空格；与其他 plan 的并行/冲突关系已说明 |

**frontmatter `status` 流转**：`active`（进行中）→ `done`（已交付）→ 可选 `archived`。

---

## 阶段 5 · 开发

| | |
| --- | --- |
| **目的** | 按 plan 的任务实现功能，保持与现有代码风格一致 |
| **谁来做** | 开发 |
| **怎么做** | 在特性分支上开发；可用 `/ce-work` 驱动；复杂/并行特性用 `/ce-worktree` 隔离工作区。后端 `exam-api`（SpringBoot + MyBatis-Plus + Shiro），前端 `exam-vue`（Vue2 + Element-UI） |
| **产出物** | 代码变更；DB migration（`docs/ops/install/`）；必要的开发说明 |
| **进入标准** | plan 完成标准达成 |
| **完成标准** | 功能本地可跑；遵循 [`constitution.md`](constitution.md) 编码基线（命名/注释参阿里、字段长度全链路一致等）与 [`conventions.md`](conventions.md) 的命名/分层/约定；自测主流程通过；plan 对应任务勾掉 |

**约定**：
- 编码基线见 [`constitution.md`](constitution.md)「工程基线」，开发细则见 [`conventions.md`](conventions.md)；AI agent 经仓库根 `CLAUDE.md` 自动加载。
- **前端 UI 以原型为准**：对照 [`prototype/`](prototype/) 对应页面实现（用真 Element-UI 还原其布局/字段/状态/交互/文案），偏离要先走 `/spec-change` 改原型再改代码。详见 [`conventions.md` §二·0](conventions.md)。
- 不在默认分支直接开发，先开特性分支。
- 一次提交聚焦一件事；提交信息遵循仓库习惯（见阶段 8）。
- 改动需求范围时，**先回流更新 PRD/设计/plan**，再继续写——用 `/spec-change`（先文档后代码，复用原序号、续编不重排）。

---

## 阶段 6 · 代码评审（Code Review）

| | |
| --- | --- |
| **目的** | 在合并前发现正确性 bug、安全/可靠性问题，并做简化清理 |
| **谁来做** | 作者自检 + 评审者（或工具） |
| **怎么做** | 本地用 `/code-review`（可 `--fix` 应用建议、`--comment` 发 PR 评论）；需要深度多维评审用 `/ce-code-review` 或云端 `/code-review ultra <PR#>`；纯质量清理用 `/simplify` |
| **产出物** | 评审结论 / PR 行内评论 / 已应用的修复 |
| **进入标准** | 开发完成标准达成，改动可成 diff |
| **完成标准** | 高置信问题已修复或记录；安全敏感改动（鉴权、输入、权限）已专门过一遍；评审意见全部 resolve（`/ce-resolve-pr-feedback`） |

**重点关注**（结合本项目）：Shiro + JWT 鉴权与角色边界、组卷/判分逻辑正确性、MyBatis SQL 与部门树查询性能、DB migration 的安全与可回滚。

---

## 阶段 7 · 测试

| | |
| --- | --- |
| **目的** | 验证实现真的满足 PRD 的验收标准，而不只是「编译通过」 |
| **谁来做** | 开发 / 测试 |
| **怎么做** | ① 按 PRD 验收标准（`AE/AC`）逐条手工验证；② 用 `/verify` 跑起应用观察真实行为；③ 前端受影响页面用 `/ce-test-browser`；④ 后端关键逻辑补单测 |
| **产出物** | 验收结果（哪条通过/失败）；必要的自动化测试；缺陷清单 |
| **进入标准** | 代码评审完成标准达成 |
| **完成标准** | PRD 全部验收标准通过；回归无新增问题；失败项要么修复、要么明确记录为已知限制 |

> 验收标准在 PRD 阶段就写好了——测试阶段是「拿着 PRD 的 AE/AC 逐条打勾」，不是临时想测什么。

---

## 阶段 8 · 提交与合并

| | |
| --- | --- |
| **目的** | 把验证过的改动以清晰历史并入主干 |
| **怎么做** | `/ce-commit` 写提交信息；`/ce-commit-push-pr` 提交+推送+开 PR；PR 描述价值优先、附原型/截图（`/ce-demo-reel`） |
| **产出物** | commit、PR、（合并后）更新 plan 的 `status: done` |
| **完成标准** | CI 绿；评审通过；PR 描述说清「做了什么、为什么、怎么验证」；相关 plan/PRD 状态更新 |

**提交信息约定**（本仓库现状）：`<type>: <简述>`，正文列要点，结尾带 `Co-Authored-By`。常用 type：`feat / fix / docs / refactor / chore`。

---

## 产出物速查

| 阶段 | 产出物 | 位置 |
| --- | --- | --- |
| 策略 | STRATEGY.md | `docs/product/` |
| 需求 | brainstorm 原稿 → PRD 定稿 | `docs/product/brainstorms/` → `docs/product/prd/` |
| 原型 | 可点击静态页 | `docs/engineering/prototype/` |
| 设计 | 技术设计（架构/ER/详细设计） | `docs/engineering/design/` |
| 计划 | plan + migration + 覆盖矩阵 | `docs/engineering/plans/`、`docs/ops/install/` |
| 开发 | 代码 + migration | `exam-api/`、`exam-vue/`、`docs/ops/install/` |
| 评审 | 评审结论 / PR 评论 | PR |
| 测试 | 验收结果 / 测试代码 | PR / 代码仓 |
| 合并 | commit / PR | git |

## 命名与追溯约定

- **同一特性用同一 `NNN` 序号串起来**：`prd/2026-05-29-001-*` ↔ `design/2026-05-29-001-*` ↔ `plans/2026-05-29-001-*`，一眼对应。
- 计划/需求类文档统一 `YYYY-MM-DD-NNN-<type>-<slug>.md`，其余英文小写连字符。
- 可追溯链 `R/F`（要做什么）→ `U`（怎么做，详细设计在 design）→ `AE/AC`（怎么算做对了）三段保持引用，落成**覆盖矩阵**逐行可核对。

## 取号约定（多人协作：保证编号唯一）

多分支并行下，`NNN = 现有最大号 + 1` 在各自分支里互相看不见，合并才撞号。规矩是**把取号从特性分支里挪到共享的 main 上、且在开工之前**。

**NNN（特性号）—— 先登记后开工**
1. **取号**：在最新 main 上往 `docs/engineering/registry.md` 追加一行 `| NNN | slug | owner | date | reserved |`，push 进 main（仅这一行，可免 PR）。并发抢同号时，后 push 者被拒/冲突 → 当场暴露 → 改取下一个号。
2. **开工**：再开 `feat/NNN-slug` 分支，跑 `/spec-prd`，直接用登记好的号。
3. **收尾**：特性合并后把该行 `reserved` 改 `done`；中途放弃改 `abandoned`。**号一经预留即作废不回收**（回收会打断 design/plan/测试/历史的引用）。

**`R/F`、`AE/AC`、`U`（特性内子编号）—— rebase 后续编 + 校验兜底**
- 同一特性多人并发改时，**续编前先 `git pull --rebase` 到最新**，从合并后的真实最大号往后接，绝不重排。
- 万一仍撞号，`/spec-check` 的 C1（编号重复=FAIL）作为最后一道网在合并/评审前拦住。

> 老项目未建 `registry.md` 时，`/spec-prd` 退回 `max+1` 并提示补建登记表——多人协作强烈建议补上。

## 一句话版

> **需求写清楚（PRD 带编号验收）→ 界面先画（原型）→ 系统怎么搭（设计）→ 任务拆明白（plan）→ 照着实现（开发）→ 合并前挑刺（评审）→ 拿验收标准逐条验（测试）→ 历史干净地并入（PR）。** 每一步的产出物就是下一步的入场券，全程单一事实源、可双向追溯。
