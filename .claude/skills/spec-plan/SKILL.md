---
name: spec-plan
description: '以已定稿 PRD + 原型 + 设计为输入，生成可执行的实现计划：把设计拆成可独立认领的实现单元（U：Files/依赖/Patterns/原型页面/设计依据/Execution note/覆盖需求/Test scenarios）、DB migration（与设计 ER 逐字段一致）、三向覆盖矩阵，写入 docs/engineering/plans/。Use when 技术设计（/spec-design）已完成、要据设计拆任务与迁移脚本。计划复用与 PRD/设计相同的 NNN 序号；架构与数据模型在 /spec-design，不在此重做。'
argument-hint: "[目标 PRD/设计 路径或特性名，可留空由我识别最近的设计]"
---

# 计划阶段：设计 → 可执行实现计划

**当前年份 2026**。

流水线**第 4 阶段（计划）**，回答 **HOW 的执行层**：据已定稿的**设计**（`/spec-design`）把方案拆成可独立认领的实现单元、定依赖顺序、写迁移脚本、出覆盖矩阵。上游是 `/spec-design`（其上又是 `/spec-prd` + `/spec-prototype`），下游是开发与后续 workflow 阶段。

> **计划据设计而来，不在 plan 里重做设计**：架构、数据 ER、详细设计都在 `docs/engineering/design/…-NNN-*`，本计划**引用**它，单元的「详细设计」指回设计文档对应小节。缺设计就先跑 `/spec-design`。需求层面的改动回流 `/spec-change`，不在 plan 里临时发明需求。

## 术语提示

PRD 的功能需求用 **R/F** 编号、验收用 **AE/AC** 编号，计划里的实现单元用 **U** 编号（详见 `/spec-prd` 术语表）。计划负责把它们接成可追溯链：**`R/F`（要做什么）→ `U`（怎么做，详细设计引设计文档）→ `AE/AC`（怎么算做对了）**。

## 核心原则

1. **单一事实源** — 优先级 `docs/engineering/constitution.md`（工程宪法 / 原则）> `docs/engineering/workflow.md` 阶段 4「完成标准」> 本 skill 内置默认值；项目文档存在时以其为准。
2. **同序号串联** — plan 复用与 PRD/设计 **完全相同**的 `YYYY-MM-DD-NNN`，`prd/…-NNN-*` ↔ `design/…-NNN-*` ↔ `plans/…-NNN-*` 一眼对应。
3. **据设计拆分** — 设计是输入，不在 plan 重做架构/ER/详细设计；单元的详细设计**引用** `design/…-NNN-*` 对应小节。
4. **任务可独立认领** — 每个单元标注 Files / Dependencies / Patterns to follow / 设计依据 / Execution note，并映射它实现的 `R/F` 和验证它的 `AE/AC`。
5. **migration 与设计 ER 一致** — migration sql（物理脚本）依设计文档的 ER 模型（逻辑视图）落地，逐字段一致。

## 执行流程

### Phase 0 · 加载输入

1. 读 `docs/engineering/workflow.md` 阶段 4 与「命名与追溯约定」。
2. 读目标 PRD（`$ARGUMENTS` 指定，或 `docs/product/prd/` 最近 `status: active` 的一份），取其 `NNN` 与全部 `R/F`、`AE/AC`。
3. **读对应设计文档**（`docs/engineering/design/…-NNN-*`）：架构/接口清单/ER/详细设计——这是拆任务与写 migration 的**主要依据**。**缺设计就停下**，提示先跑 `/spec-design`。
4. 读对应原型页面（`docs/engineering/prototype/`），作为前端任务的 UI 参考。
5. 扫相关代码与现有数据库脚本（`docs/ops/install/`），识别要改的表/接口/页面与既有模式（Patterns to follow）。

### Phase 1 · DB 变更与迁移

依**设计文档的 ER 模型**，把数据库改动落到 `docs/ops/install/migration-YYYY-<特性名>.sql`：建表/加字段/索引/唯一键/种子数据，并在相关任务里引用该脚本。**确保与设计的 ER 模型逐字段一致**（ER 是逻辑视图，migration 是其物理实现，本计划不重画 ER 只落地它）。两条强制规约：

- **回滚段必填**：每份 migration 写一段**回滚/down**——up 建/改了什么，down 就逆序撤什么（先建后删、注意外键依赖顺序），并**注明会丢什么数据、是否需先备份**（结构能回滚，数据不一定能）。无 schema 变更时写「无（纯数据/逻辑改动）」也算填。
- **时间戳落地**：建业务表时带上设计 ER 规约的创建/更新时间戳两列（沿用项目惯例命名，如 `create_time`/`update_time`）；豁免的表沿用设计里的豁免说明。
- **字段长度对齐（生成 SQL 前先定数据库语义）**：业务最大字符数（来自设计标注）是唯一真值，后端按字符校验、前端 `maxLength` 对齐，Test scenarios 覆盖超长拦截。**写含 `varchar` 的建表 SQL 前，先确定目标数据库与其长度语义**：
  - 优先读项目已记录的（`constitution.md` #9 / `CLAUDE.md` / `conventions.md`，如 MySQL/PG → 按字符 `varchar(10)`）——**已记录就直接用，不打扰用户**。
  - **若项目没记录用哪种库/语义，停下来用 `AskUserQuestion` 问一次**（MySQL/PG、SQL Server、Oracle、达梦、人大金仓、神州通用…），确认后按对应写法生成（`varchar` / `nvarchar(N)` / `varchar2(N CHAR)` / 达梦 `LENGTH_IN_CHAR` 等，见 `constitution.md` #9），并建议把该选择**记进 `constitution.md` #9** 供后续复用，不再重复问。

### Phase 2 · 拆任务 + 追溯

把设计方案拆成可独立认领的实现单元（U1/U2…），每个单元标注：
- **Files**：要新增/修改的文件（repo-relative 路径）。
- **Dependencies**：依赖哪个单元先完成。
- **Patterns to follow**：参照现有哪段代码的写法/分层。
- **设计依据**：指向 `design/…-NNN-*` 文档里对应的接口/详细设计小节（详细设计不在此重抄，引用即可）。
- **原型页面**（前端/界面任务必填）：对应 `docs/engineering/prototype/<page>.html`，作为该任务的 **UI 验收基线，以原型为准**；偏离需先走 `/spec-change` 改原型。
- **Execution note**（前端/界面任务必填）：标注 `用 /ce-frontend-design 方法论实现，收尾前按其要求截图自检设计保真度`。`ce-work` 执行时会读此字段，从而把原型从「Element-UI 草图」落成有设计质量的真实前端；纯后端/逻辑单元留空或写其执行姿态（如 test-first）。
- **覆盖需求**：实现哪些 `R/F`。
- **Test scenarios**：对应哪些 `AE/AC`，怎么验。

并说明与其他 plan 的并行/冲突关系。

**收尾出一张三向覆盖矩阵**（随 plan 落盘），把 PRD 的覆盖矩阵延伸到实现单元：

| 需求 | 实现单元 | 验收 |
| --- | --- | --- |
| R8 | U2、U3 | AE2、AE5 |

规则：每条 `R/F` 至少落到一个 `U`、且至少被一条 `AE/AC` 验证；任何一格为空都要回头补——漏实现（缺 U）、漏验收（缺 AE/AC），或该需求本就该降级为非目标。

### Phase 3 · 写计划文件

**先用 Read 读取本 skill 目录下的 `templates/plan.md`**（计划骨架）与 `templates/migration.sql`（迁移脚本骨架），按骨架填充。写到 `docs/engineering/plans/YYYY-MM-DD-NNN-<type>-<slug>-plan.md`（`<type>` 常用 `feat/fix/refactor`）。结构（模板没读到时按此兜底）：
- **frontmatter**：`title` / `type` / `status: active` / `date` / `origin`（指向对应设计文档 `design/…-NNN-*`）。
- 正文：Summary → Problem Frame → **设计依据**（链接 `design/…-NNN-*`，不重抄架构/ER）→ **DB 迁移**（引用 `docs/ops/install/migration-YYYY-<特性名>.sql`，与设计 ER 一致）→ Requirements 映射 + **覆盖矩阵**（Phase 2 三向表）→ **Implementation Units**（Phase 2 单元，详细设计引设计文档）。

### Phase 4 · 交接（进入 workflow 后续阶段）

输出计划路径、任务清单与依赖顺序。然后提示按 `docs/engineering/workflow.md` 继续：

- **阶段 5 开发** → `/ce-work` 驱动，先开特性分支；并行/复杂特性用 `/ce-worktree`。前端/界面单元已带 `Execution note`，`ce-work` 会据此自动套用 `/ce-frontend-design`。改动需求范围时**先回流 `/spec-change`**。
- **阶段 6 评审** → `/code-review`（可 `--fix`/`--comment`）；深度用 `/ce-code-review`；纯质量清理 `/simplify`。
- **阶段 7 测试** → 拿 PRD 的 `AE/AC` 逐条验；`/verify` 跑应用；前端 `/ce-test-browser`。
- **阶段 8 合并** → `/ce-commit-push-pr`；合并后把本 plan 的 `status` 更新为 `done`。
