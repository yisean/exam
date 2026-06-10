---
name: spec-check
description: '对某个特性（同一 NNN）的 PRD → 原型 → 设计 → plan 做一致性体检：校验覆盖矩阵（每条 R/F 都有 AE/AC 验收且落到实现单元、AE/AC 不悬空）、编号续编无重排无重复、跨文档 NNN 与 status 一致、设计 ER 与 migration 是否一致、NFR 是否缺位，输出带定位与修法的报告。只读不改文档。Use when 想确认某特性的需求/原型/设计/计划是否对得上、提交或评审前自检、spec-change 回流后核对、怀疑有漏验收或悬空需求。修复请回到 /spec-prd、/spec-change、/spec-design 或 /spec-plan。'
argument-hint: "[NNN 序号 / PRD 路径 / 特性名，可留空由我取最近 active 的一份]"
---

# 校验阶段：spec 产出物一致性体检

**当前年份 2026**。

这是横切流水线的**只读校验** skill。它**不产出新文档、不改任何产出物**，只把同一特性（同一 `NNN`）的 PRD / 原型 / 设计 / plan 摆在一起，沿可追溯链逐项核对，输出一份**带定位与修法**的体检报告。类比 spec-kit 的 `/analyze`。

> **铁律：只读。** spec-check 只报告问题、指出该跑哪个 skill 修，**绝不自己改 PRD/原型/设计/plan/代码**——保持「先文档后实现、改动走对应 skill」的单一事实源纪律。需求层面的修复回 `/spec-prd` 或 `/spec-change`，设计层面的回 `/spec-design`，实现层面的回 `/spec-plan`。

## 术语提示

功能需求用 **R/F** 编号、验收用 **AE/AC** 编号、计划里的实现单元用 **U** 编号（详见 `/spec-prd`、`/spec-plan`）。可追溯链：**`R/F`（要做什么）→ `U`（怎么做）→ `AE/AC`（怎么算做对了）**。本 skill 校验的就是这条链有没有断、有没有悬空。

## 核心原则

1. **只读不改** — 见上铁律。
2. **沿同一 NNN 横切** — `prd/…-NNN-*` ↔ `prototype/*` ↔ `design/…-NNN-*` ↔ `plans/…-NNN-*` ↔ `migration-*` 必须对得上。
3. **覆盖矩阵是判据** — 校验直接复用 PRD/plan 里已落盘的覆盖矩阵；矩阵缺失也照样从正文现抽一份来对。
4. **定位 + 修法** — 每个问题都给「在哪、为什么是问题、怎么修、该跑哪个 skill」，不只说"有问题"。
5. **右尺寸** — 没有 plan/原型的早期特性，只校验已存在的产出物，缺的阶段标"未到"而非报错。
6. **只管文档一致性，不查代码规约** — 本 skill 校验的是 spec 产出物（PRD/原型/设计/plan/migration）之间对不对得上。**命名/注释/分层/前后端编码规约属编码期**，由 `/code-review`·`/ce-code-review` 对照 `constitution`「工程基线」与项目 `CLAUDE.md` 把关，**不在 spec-check 范围**（它也读不到业务代码）。

## 执行流程

### Phase 0 · 定位特性与产出物

1. 由 `$ARGUMENTS` 确定目标：可以是 `NNN`、PRD 路径、或特性名；留空则取 `docs/product/prd/` 中最近 `status: active` 的一份，回显「正在体检 NNN-<slug>，对不对？」。
2. 沿同一 `NNN` 找齐（存在才纳入，不存在标记「未到/不适用」）：
   - `docs/product/prd/…-NNN-*.md`（必需，找不到则停下报错）
   - `docs/engineering/prototype/`（涉及界面的页面 + `index.html`）
   - `docs/engineering/design/…-NNN-*.md`（技术设计：架构/接口/ER/详细设计）
   - `docs/engineering/plans/…-NNN-*.md`
   - `docs/ops/install/migration-*.sql`
3. 读 `docs/engineering/constitution.md`（工程宪法 / 原则）与 `docs/engineering/workflow.md`（命名与追溯约定）作为最高判据（存在则覆盖下面的内置默认）；读 `docs/engineering/registry.md`（NNN 取号登记表，供 C8b 核对）。
4. 体检以**本特性单一 NNN** 为主；唯有 C16「跨特性引用一致」会按需到全 `docs/` 做文本检索、读别的 NNN 产出物——但仍是**只读**，不改任何文件。

### Phase 1 · 抽取编号与映射

从各产出物现抽出事实，建一张内存中的映射：

- PRD：全部 `R/F`（及其文字）、全部 `AE/AC` 及其括号里**标注覆盖的需求号**、是否有「非功能约束」节、frontmatter（`status` / `date` / `origin`）。
- 设计：接口清单、ER 模型的实体/字段、详细设计覆盖的 `R/F`、frontmatter（`origin` 应指向 PRD）。
- plan：全部实现单元 `U` 及各自的**覆盖需求**（实现哪些 `R/F`）、**设计依据**（指向 design 的小节）与 **Test scenarios**（对应哪些 `AE/AC`）、frontmatter（`origin` 应指向设计）、引用的 migration。
- 原型：现有页面清单 + `index.html` 入口、各页面对应的 `R/F`（若标注）。

### Phase 2 · 逐项检查（给每项判 PASS / WARN / FAIL）

| # | 检查项 | 不通过判级 |
| --- | --- | --- |
| C1 | **编号规整**：`R/F`、`AE/AC`、`U` 各自无重复、无重排迹象；断号有解释（如某号已废弃） | 重复/重排=FAIL；裸断号=WARN |
| C2 | **验收覆盖**：每条 `R/F` 至少被一条 `AE/AC` 覆盖（除非显式标为非目标） | FAIL |
| C3 | **验收不悬空**：每条 `AE/AC` 括号里的需求号都真实存在，且至少指向一条 | FAIL |
| C4 | **实现落地**（有 plan 时）：每条 `R/F` 至少落到一个 `U`（除非非目标） | FAIL |
| C5 | **单元不悬空**（有 plan 时）：每个 `U` 的覆盖需求都指向存在的 `R/F` | FAIL |
| C6 | **验证闭环**（有 plan 时）：每个 `U` 的 Test scenarios 指向存在的 `AE/AC` | WARN |
| C7 | **NFR 在位**：PRD 有「非功能约束」节（无则该节显式写「无特殊要求」也算通过） | WARN |
| C8 | **跨文档一致**：prd / design / plan / migration 同 `NNN`；frontmatter `status` 合理（如 plan 已 done 但 PRD 还 active 要提示）；`origin` 链 plan→design→prd 指向真实存在的文件或写了来源简述 | 不一致=WARN |
| C8b | **取号登记一致**（有 `registry.md` 时）：本特性 `NNN` 在 `docs/engineering/registry.md` 有登记、且该号未被两条记录重复占用；status 与 PRD/plan 现状不矛盾（如特性已交付但登记仍 `reserved`）。registry 缺失则标「未建登记表」并提示多人协作建议补上 | 号未登记/被重复占用=FAIL；status 不符=WARN |
| C9 | **原型覆盖**（有原型时）：每条 **UI 相关** `R/F` 有对应页面，且页面都挂进了 `index.html` | 缺页面=WARN |
| C10 | **数据一致**（有 design + plan 时）：**设计**的 ER 模型实体/字段与 migration sql 的表大致对得上（粗检字段/表名） | WARN |
| C11 | **设计落地**（有 plan 时）：有设计文档；每个 `U` 的「设计依据」指向设计里真实存在的小节；设计的接口清单覆盖的 `R/F` 都进了某个 `U` | 缺设计=WARN；悬空设计依据=WARN |
| C12 | **回滚段在位**（有 migration 时）：migration 脚本含**回滚/down 段**（或显式写「无」）；不能只有 up 没有撤回说明 | 缺回滚段=WARN |
| C13 | **时间戳规约**（有 migration 时）：新建**业务表**带创建/更新时间戳两列（命名沿用项目惯例，如 `create_time`/`update_time`）；豁免的表（字典/只读/中间表）在设计或脚本里注明原因 | 缺时间戳且无豁免说明=WARN |
| C14 | **字段长度一致**（有 design + migration 时）：设计/接口契约标注了字符串字段的最大字符数；migration 的列长与之一致（按所选库语义：MySQL `varchar(N)` 按字符 / Oracle `varchar2(N CHAR)` / 达梦 等，见 `constitution` #9） | 漏标或与列长不一致=WARN |
| C15 | **设计章节完整**（有 design 时，涉及才查）：涉及界面的特性 design 有「前端设计」节；占名额/高频写场景有「并发与幂等」；多角色特性有「权限」相关设计 | 该有却缺=WARN |
| C16 | **跨特性引用一致**（事后兜底）：本特性**对外暴露**的接口/表/字段/共享原型组件，去全 `docs/` 检索是否有**别的 NNN** 仍引用了已不存在或已改签名的旧版本；本特性**对外依赖**的接口/表，确认对方 NNN 现状仍提供 | 悬空跨特性引用=WARN |

### Phase 3 · 输出体检报告

报告结构：

1. **总判定**一行：`✅ 通过` / `⚠️ N 项提示` / `❌ N 项必修 + M 项提示`。
2. **覆盖矩阵总表**（现抽或复用已落盘的），把断链行用 `⚠️/❌` 标出：

   | 需求 | 验收 | 实现单元 | 状态 |
   | --- | --- | --- | --- |
   | R8 | AE2、AE5 | U2、U3 | ✅ |
   | R9 | —— | U4 | ❌ 漏验收 |
   | R10 | AE7 | —— | ❌ 漏实现 |

3. **问题清单**（先 FAIL 后 WARN），每条：`[检查项] 位置 → 为什么是问题 → 怎么修 → 跑哪个 skill`。
   - 例：`[C2] R9（PRD §2 第 9 条）没有任何 AE/AC 覆盖 → 补一条标注（R9）的验收，或降级为非目标 → /spec-change`。

### Phase 4 · 交接（修复建议，不自己改）

按问题归属给出该跑的命令，**等用户决定**：

- 需求/验收层（漏验收、悬空 AE/AC、NFR 缺、编号要续编）→ 已上线/在研特性走 `/spec-change`；尚未定稿的新 PRD 走 `/spec-prd`。
- 设计层（缺设计文档、接口/详细设计缺、ER 与 migration 不一致、单元「设计依据」悬空）→ `/spec-design`。
- 实现层（漏实现单元、单元悬空、Test scenarios 缺、migration 未落）→ `/spec-plan`。
- 原型层（UI 相关需求缺页面、入口未挂）→ `/spec-prototype`。
- 全部通过 → 明确告知「可追溯链完整，可进入下一阶段」。
