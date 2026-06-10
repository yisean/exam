# 研发流程总纲：从需求到上线

> 本文定义本项目「需求 → 原型 → 设计 → 计划 → 开发 → 代码评审 → 测试 → 合并」的标准流程：每个阶段**做什么、谁来做、产出什么、何时算完成**。
> 原则基线见 [`constitution.md`](constitution.md)（工程宪法，不可妥协原则 + 编码期基线）；本文管**流程与各阶段完成标准**。冲突时优先级：**`constitution.md` > 本文 > 各 skill 内置默认**。

## 项目文档目录结构

文档按 **产品 / 研发 / 运维** 三层组织，各阶段产出物对号入座。

```
docs/
├── README.md                    文档导航索引
├── product/                     做什么、为什么 —— 产品与需求
│   ├── STRATEGY.md              产品策略（阶段 0，可选）
│   ├── brainstorms/             需求探讨原始稿（阶段 1）
│   └── prd/                     正式需求定稿 PRD（阶段 1）
├── engineering/                 怎么做 —— 研发过程
│   ├── constitution.md          工程宪法（原则基线）
│   ├── workflow.md              ← 本文：研发流程总纲
│   ├── registry.md              NNN 取号登记表（多人协作，先登记后开工）
│   ├── prototype/               可点击静态原型 + _spec.md（阶段 2）
│   ├── design/                  技术设计：概要设计 + ER + 详细设计（阶段 3）
│   └── plans/                   实现计划：任务 + migration + 覆盖矩阵（阶段 4）
└── ops/                         怎么交付 —— 部署与运维
    └── install/                 安装资源 + 数据库/迁移脚本（阶段 4 落 migration）
```

---

## 全景流水线

```
策略(可选)      需求          原型         设计          计划         开发        代码评审       测试        合并
STRATEGY → brainstorm/PRD → prototype/ → design/ → plans/ → 编码 → code review → test/verify → commit + PR
   └──────────── product/ ────────────┘   └───────────────── engineering/ + 代码仓 ──────────────────┘
```

每个阶段都有**进入标准（Definition of Ready）**和**完成标准（Definition of Done）**——上一阶段的产出物就是下一阶段的进入条件。允许小步快跑、阶段回流（如开发中发现需求漏洞回到 PRD 修订），但产出物要同步更新，保持单一事实源（走 `/spec-change`）。

---

## 阶段 0 · 策略对齐（可选，低频）

| | |
| --- | --- |
| **目的** | 明确产品要解决的目标问题、目标用户、关键指标、工作主线，作为后续一切需求的上游依据 |
| **何时做** | 新产品启动、方向调整时；不是每个特性都要做 |
| **怎么做** | 运行 `/ce-strategy`，或手工维护 |
| **产出物** | `docs/product/STRATEGY.md` |
| **完成标准** | 目标问题、用户、关键指标、Tracks 四节齐全且互不矛盾 |

---

## 阶段 1 · 需求

| | |
| --- | --- |
| **目的** | 把模糊想法澄清成「问题 + 关键决策 + 可验收的需求清单」，固化为正式 PRD |
| **谁来做** | 产品/需求负责人，与开发协作 |
| **怎么做** | 用 `/spec-prd`（原始需求 → 规范化 PRD，自动带 `R/F` 与 `AE/AC` 编号）；探讨期可先 `/ce-brainstorm`。已有特性的增量改动改用 `/spec-change` |
| **产出物** | 原稿 → `docs/product/brainstorms/`；定稿 → `docs/product/prd/` |
| **进入标准** | 有一个明确的特性意图（一句话能说清要解决谁的什么问题） |
| **完成标准** | PRD 含：背景与问题、目标/**非目标**、用户与角色、关键决策、**带编号的功能需求**、**非功能约束**、**可验收的验收标准**、**覆盖矩阵**、成功指标、依赖与假设、待解决问题 |

---

## 阶段 2 · 原型（按需）

| | |
| --- | --- |
| **目的** | 写代码前把交互、信息架构、字段显隐用可点击页面表达出来，供内部对齐或向客户演示 |
| **何时做** | 涉及新界面/复杂交互的特性；纯后端或微调可跳过 |
| **怎么做** | 用 `/spec-prototype`，遵循 `prototype/_spec.md`：纯静态 HTML、无外部依赖、双击即开。发客户确认后若改需求，走 `/spec-change` |
| **产出物** | `docs/engineering/prototype/`（页面 + `index.html` 导航 + `_spec.md` 规范） |
| **进入标准** | 对应 PRD 的关键交互已明确 |
| **完成标准** | 关键页面可点击跑通主流程；PRD 中 UI 相关需求都能在原型找到对应 |

> 原型是**沟通与验证载体，不是实现**。定稿后成为设计/计划里前端任务的「UI 参考基线」。

---

## 阶段 3 · 设计

| | |
| --- | --- |
| **目的** | 定整体架构、数据模型与关键流程的详细设计，作为拆任务的前提 |
| **谁来做** | 开发负责人 / 架构 |
| **怎么做** | 用 `/spec-design`，以 PRD + 原型为输入 |
| **产出物** | `docs/engineering/design/`，命名 `YYYY-MM-DD-NNN-<type>-<slug>-design.md`，带 frontmatter（`title/type/status/date/origin→PRD`） |
| **进入标准** | PRD 完成标准达成；如有界面，原型已定稿 |
| **完成标准** | 含**概要设计**（架构/模块、技术选型与决策、接口清单与契约、前端设计、权限四级、可观测与审计、NFR 承接、风险）、**数据 ER 模型**（Mermaid `erDiagram`，带时间戳与字段长度规约）、**详细设计**（接口签名、核心逻辑、并发与幂等、代码结构落点）；每段标注覆盖的 `R/F` |

---

## 阶段 4 · 计划

| | |
| --- | --- |
| **目的** | 据设计把方案拆成可执行、可独立认领的工程任务，定依赖顺序、迁移脚本、覆盖矩阵 |
| **谁来做** | 开发负责人 |
| **怎么做** | 用 `/spec-plan`，以设计文档为主输入（PRD/原型作参照） |
| **产出物** | `docs/engineering/plans/`，命名 `YYYY-MM-DD-NNN-<type>-<slug>-plan.md`，带 frontmatter（`origin→design`） |
| **进入标准** | 设计完成标准达成 |
| **完成标准** | 实现单元可独立认领（Files / Dependencies / Patterns / 设计依据 / 覆盖需求 / Test scenarios）；DB 变更落 `docs/ops/install/` 的 migration（**回滚段必填**、时间戳、varchar 字段长度按字符数，与设计 ER 逐字段一致）；**三向覆盖矩阵**（`R/F → U → AE/AC`）无空格 |

**frontmatter `status` 流转**：`active`（进行中）→ `done`（已交付）→ 可选 `archived`。

---

## 阶段 5 · 开发

| | |
| --- | --- |
| **目的** | 按 plan 的任务实现功能，遵循 `constitution.md` 的编码期基线与现有代码风格 |
| **谁来做** | 开发 |
| **怎么做** | 在特性分支开发；可用 `/ce-work` 驱动；复杂/并行特性用 `/ce-worktree`。技术栈：后端 `<后端栈，如 SpringBoot + MyBatis-Plus>`，前端 `<前端栈，如 Vue + Element-UI>` |
| **产出物** | 代码变更；DB migration（`docs/ops/install/`）；必要的开发说明 |
| **进入标准** | plan 完成标准达成 |
| **完成标准** | 功能本地可跑；遵循 `constitution.md` 编码基线（安全合规、零魔法值、字段长度全链路一致等）与项目 `CLAUDE.md`；自测主流程通过 |

**约定**：
- 编码期标准见 `constitution.md`「工程基线」；AI agent 经仓库根 `CLAUDE.md` 自动加载。
- **前端 UI 以原型为准**：对照 `prototype/` 对应页面实现，偏离先走 `/spec-change` 改原型再改代码。
- 不在默认分支直接开发，先开特性分支；改动需求范围时**先回流 PRD/设计/plan**（`/spec-change`）。

---

## 阶段 6 · 代码评审

| | |
| --- | --- |
| **目的** | 合并前发现正确性 bug、安全/可靠性问题，并做简化清理 |
| **怎么做** | 本地 `/code-review`（可 `--fix`/`--comment`）；深度用 `/ce-code-review` 或云端 `/code-review ultra <PR#>`；纯质量清理 `/simplify` |
| **完成标准** | 高置信问题已修复或记录；安全敏感改动（鉴权、输入、权限）已专门过一遍；评审意见全部 resolve |

---

## 阶段 7 · 测试

| | |
| --- | --- |
| **目的** | 验证实现真的满足 PRD 的验收标准，而不只是「编译通过」 |
| **怎么做** | ① 按 PRD 的 `AE/AC` 逐条手工验证；② `/verify` 跑应用观察真实行为；③ 前端受影响页面用 `/ce-test-browser`；④ 关键逻辑补单测 |
| **完成标准** | PRD 全部验收标准通过；回归无新增问题；失败项要么修复、要么明确记录为已知限制 |

> 验收标准在 PRD 阶段就写好了——测试阶段是「拿着 PRD 的 `AE/AC` 逐条打勾」。

---

## 阶段 8 · 提交与合并

| | |
| --- | --- |
| **目的** | 把验证过的改动以清晰历史并入主干 |
| **怎么做** | `/ce-commit` 写提交信息；`/ce-commit-push-pr` 提交+推送+开 PR；合入主干前 squash 成干净可回滚的正式提交 |
| **完成标准** | CI 绿；评审通过；PR 描述说清「做了什么、为什么、怎么验证」；相关 plan/PRD 状态更新（plan `status: done`） |

**提交信息约定**：`<type>: <简述>`，正文列要点，结尾带 `Co-Authored-By`。常用 type：`feat / fix / docs / refactor / chore`。

---

## 产出物速查

| 阶段 | 产出物 | 位置 |
| --- | --- | --- |
| 策略 | STRATEGY.md | `docs/product/` |
| 需求 | brainstorm 原稿 → PRD 定稿 | `docs/product/brainstorms/` → `docs/product/prd/` |
| 原型 | 可点击静态页 | `docs/engineering/prototype/` |
| 设计 | 技术设计（架构/ER/详细设计） | `docs/engineering/design/` |
| 计划 | plan + migration + 覆盖矩阵 | `docs/engineering/plans/`、`docs/ops/install/` |
| 开发 | 代码 + migration | 代码仓、`docs/ops/install/` |
| 评审 | 评审结论 / PR 评论 | PR |
| 测试 | 验收结果 / 测试代码 | PR / 代码仓 |
| 合并 | commit / PR | git |

## 命名与追溯约定

- **同一特性用同一 `NNN` 序号串起来**：`prd/…-NNN-*` ↔ `design/…-NNN-*` ↔ `plans/…-NNN-*`，一眼对应。
- 文档统一 `YYYY-MM-DD-NNN-<type>-<slug>.md`，其余英文小写连字符。
- 可追溯链 `R/F`（要做什么）→ `U`（怎么做，详细设计在 design）→ `AE/AC`（怎么算做对了）三段保持引用，落成**覆盖矩阵**逐行自检。

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
