---
title: "feat: 开放-关闭状态、模拟/正式考试与补考次数（全栈）"
type: feat
status: active
date: 2026-05-29
---

# feat: 开放-关闭状态、模拟/正式考试与补考次数（全栈）

## Summary

为现有题库、试题、考试增加「开放 / 关闭」状态（关闭项不进新组卷、关闭考试学员端不可见），并为考试增加「模拟 / 正式」类型与正式考试的「补考次数」限制。全栈实现，覆盖 `exam-api`（MySQL + Java）与 `exam-vue`（Vue2 + Element-UI）。

---

## Problem Frame

来源：`docs/product/functional-spec.md §8`（原型阶段确定的产品决策）与 `docs/engineering/prototype/` 中 `repo.html`、`qu.html`、`exam.html`、`exam-form.html` 的交互。

当前 `el_repo`、`el_qu` 没有可用/停用的概念，废弃或暂不启用的题库与试题无法从组卷中排除，只能删除（有数据丢失风险）。`el_exam` 没有"模拟练习 vs 正式考核"的区分，正式考核也无法限制重复作答次数——员工可无限次刷正式考试，削弱考核效力。本特性补齐这三项控制能力。

本计划与「考试助理与预约考试」计划（`docs/engineering/plans/2026-05-29-001-feat-exam-assistant-booking-plan.md`）相互独立，触及不同表与路径，可并行开发。

---

## Key Technical Decisions

- **开放/关闭用独立状态列，不复用 `el_exam.state`。** `el_exam.state` 已表达考试生命周期（未开始/进行中/已结束），语义不同；为考试新增独立的开放状态列。`el_repo`、`el_qu` 同样新增开放状态列（默认开放）。

- **关闭只影响"新使用"，不回溯。** 关闭题库/试题仅将其从**新建/编辑考试的组卷可选范围**与**取卷抽题**中排除；已建考试的组卷规则、已生成试卷不受影响。关闭考试仅使其在学员端不可见，不中断进行中的作答。（见 Assumptions，可调整）

- **补考次数 = 额外补考次数。** 正式考试允许的总作答次数 = `1 + retake_count`（首考 + 补考次数）。模拟考试不限次数。次数校验落在取卷创建处，依据 `el_user_exam.try_count`。

- **模拟/正式与补考次数挂在考试上。** `el_exam` 增加 `exam_type`（1 模拟 / 2 正式）与 `retake_count`；模拟考试忽略 `retake_count`。

---

## Requirements

### 开放-关闭状态
- R1. `el_repo`（题库）、`el_qu`（试题）、`el_exam`（考试）均有开放/关闭状态，默认开放。
- R2. 管理端可在列表中按状态筛选，并对单个对象切换开放/关闭。
- R3. 关闭的题库及其试题、以及单独关闭的试题，不出现在新建/编辑考试的组卷可选范围，且不被取卷抽取。
- R4. 关闭的考试在学员端在线考试列表不可见。

### 模拟 / 正式 与补考次数
- R5. 考试有类型：模拟 / 正式。
- R6. 模拟考试不限作答次数。
- R7. 正式考试可设补考次数（额外次数）；同一用户对该考试的总作答次数不超过 `1 + 补考次数`，超出则不能再次取卷。
- R8. 创建/编辑考试时可设置考试类型与（正式考试的）补考次数。

---

## Implementation Units

### U1. 数据库迁移

- **Goal**：为 `el_repo`、`el_qu` 增加开放状态列；为 `el_exam` 增加开放状态、`exam_type`、`retake_count`。
- **Requirements**：R1, R5, R7。
- **Dependencies**：无。
- **Files**：新增 `docs/ops/install/migration-2026-开放状态与补考.sql`（`alter table el_repo add state int default 1`；`alter table el_qu add state int default 1`；`alter table el_exam add open_status int default 1, add exam_type int default 2, add retake_count int default 0`）。
- **Approach**：状态列 1=开放 0=关闭，默认开放以兼容存量数据；`exam_type` 默认 2（正式）；`retake_count` 默认 0。注释风格对齐 `数据库脚本.sql`。
- **Test scenarios**：`Test expectation: none -- DDL；正确性由后续单元服务测试间接覆盖。`
- **Verification**：脚本在 MySQL 可执行，存量行获默认值（题库/试题=开放、考试=正式）。

### U2. 后端：题库/试题开放-关闭与组卷过滤

- **Goal**：题库、试题支持状态切换与按状态查询；组卷可选范围与取卷抽题排除关闭项。
- **Requirements**：R2, R3。
- **Dependencies**：U1。
- **Files**：`exam-api/.../modules/qu/entity/Repo.java`、`Qu.java`（加 `state` 字段）；`modules/qu/controller/RepoController.java`、`QuController.java`（加状态切换接口 + 列表查询支持 state 过滤，`@RequiresRoles("sa")`）；`modules/exam/service/impl/ExamRepoServiceImpl.java` 与取卷抽题处（`modules/paper/service/impl/PaperServiceImpl.java` 组卷选题）加 `state=开放` 过滤；题库选择接口仅返回开放题库。
- **Approach**：抽题 SQL/QueryWrapper 增加 `state=1` 且其题库 `state=1` 的条件；状态切换复用现有 `BaseStateReqDTO` 风格（参考 `ExamController.state`）。
- **Patterns to follow**：`modules/exam/controller/ExamController.java` 的 `/state` 接口；现有 `modules/qu` service/mapper。
- **Test scenarios**：
  - 关闭某题库后，新建考试的题库选择列表不含它；取卷不从中抽题。
  - 关闭某试题后，取卷抽题不命中它；同题库其余开放试题正常抽取。
  - 已建考试引用了后被关闭的题库 → 该考试既有组卷规则与已生成试卷不受影响（仅新组卷受限）。
  - 状态切换接口将 state 在 0/1 间正确翻转。
- **Verification**：service 单测覆盖抽题过滤；管理端列表筛选与切换正确。

### U3. 后端：考试开放-关闭与学员端可见性

- **Goal**：考试支持开放/关闭，关闭考试学员端不可见。
- **Requirements**：R2（考试侧）, R4。
- **Dependencies**：U1。
- **Files**：`modules/exam/entity/Exam.java`（加 `openStatus`）；`dto/request/ExamSaveReqDTO.java`（携带 openStatus）；`ExamController.java`（管理端列表可按状态筛选 + 切换；可复用 `/state` 模式或新增 `/open-state`）；`service/impl/ExamServiceImpl.java` 的 `onlinePaging`（排除 `openStatus=关闭` 的考试）。
- **Approach**：学员视角查询追加 `open_status=1`；管理端视角不过滤，显示状态列。关闭不影响进行中作答（取卷已创建则继续）。
- **Patterns to follow**：`ExamServiceImpl.onlinePaging` 现有过滤；`ExamController` 注解风格。
- **Test scenarios**：
  - 关闭考试后，学员在线考试列表不再出现该考试。
  - 管理端列表仍可见关闭的考试并显示状态。
  - 已开考学员（已有试卷）在考试被关闭后仍可继续作答与交卷。
- **Verification**：学员端列表与 `open_status` 一致；管理端可切换。

### U4. 后端：考试类型与补考次数校验

- **Goal**：考试携带模拟/正式类型与补考次数；取卷创建处按类型校验次数。
- **Requirements**：R5, R6, R7, R8。
- **Dependencies**：U1。
- **Files**：`modules/exam/entity/Exam.java`（加 `examType`、`retakeCount`）；`dto/request/ExamSaveReqDTO.java`；`modules/paper/service/impl/PaperServiceImpl.java`（`createPaper` 取卷前校验：正式考试且 `el_user_exam.try_count >= 1 + retakeCount` 时拒绝；模拟考试跳过校验）。
- **Approach**：复用现有 `el_user_exam.try_count`（既有作答次数累加）；模拟考试不增计或不校验（按现状仍可累加但不限制）。拒绝时返回明确业务错误（参考 `ServiceException`）。
- **Patterns to follow**：`modules/paper/service/impl/PaperServiceImpl.java` 现有取卷与 `el_user_exam` 累加逻辑；`core/exception/ServiceException`。
- **Execution note**：先为次数校验写失败用例（正式达上限、模拟不限），再改实现。
- **Test scenarios**：
  - 正式考试补考次数=2 → 同一用户第 1/2/3 次取卷成功，第 4 次被拒。
  - 模拟考试 → 同一用户多次取卷均成功，不受次数限制。
  - 正式考试补考次数=0 → 第 2 次取卷即被拒（仅一次首考）。
  - 校验依据 `el_user_exam.try_count` 当前值，跨会话持久。
- **Verification**：service 单测覆盖上述次数矩阵。

### U5. 前端：题库/试题状态列与切换

- **Goal**：题库、试题管理页展示状态、按状态筛选、一键开放/关闭。
- **Requirements**：R2, R3（可见性提示）。
- **Dependencies**：U2；原型 `docs/engineering/prototype/repo.html`、`qu.html` 为参考。
- **Files**：`exam-vue/src/views/qu/repo/index.vue`、`exam-vue/src/views/qu/qu/index.vue`（状态列 tag + 状态筛选 + 切换操作）；`exam-vue/src/api/qu/repo.js`、`qu.js`（状态切换接口）。
- **Approach**：对齐原型：开放=success tag、关闭=info tag；行内切换；提示"关闭项不进组卷"。
- **Patterns to follow**：现有 `views/qu/repo/index.vue`、`views/qu/qu/index.vue` 列表与操作列；`docs/engineering/prototype/repo.html`、`qu.html`。
- **Test scenarios**：`Test expectation: none（前端无单测设施）— 手动验证：` 切换状态后列表 tag 更新、筛选生效；新建考试时关闭题库不在可选列表。
- **Verification**：与 U2 接口联调一致。

### U6. 前端：考试表单与列表（开放类型/考试类型/补考次数）

- **Goal**：创建/编辑考试支持开放类型、考试类型、补考次数（正式时显示）；考试列表展示这些列。
- **Requirements**：R2（考试侧）, R5, R8。
- **Dependencies**：U3, U4；原型 `docs/engineering/prototype/exam-form.html`、`exam.html` 为参考。
- **Files**：`exam-vue/src/views/exam/exam/form.vue`（开放类型/考试类型单选、补考次数输入随"正式"显隐）；`exam-vue/src/views/exam/exam/index.vue`（新增考试类型/开放/补考次数列与筛选）；`exam-vue/src/api/exam/exam.js`（save/detail 携带新字段）。
- **Approach**：补考次数行随考试类型切换显隐（对齐原型 `exam-form.html` 的 `setExamType`）；列表列对齐原型 `exam.html`。
- **Patterns to follow**：现有 `views/exam/exam/form.vue`、`index.vue`；`docs/engineering/prototype/exam-form.html`、`exam.html`。
- **Test scenarios**：`Test expectation: none（前端无单测设施）— 手动验证：` 选"模拟"隐藏补考次数、选"正式"显示；保存后详情回读一致；列表正确展示三列。
- **Verification**：与 U3/U4 接口联调一致。

### U7. 前端：学员端取卷次数用尽提示

- **Goal**：学员对正式考试达次数上限时，前端给出明确反馈。
- **Requirements**：R7（用户反馈）。
- **Dependencies**：U4。
- **Files**：`exam-vue/src/views/paper/exam/preview.vue` 或 `list.vue`（取卷被拒时提示"已达考试次数上限"）。
- **Approach**：依据 U4 返回的业务错误展示提示；关闭考试由后端过滤、前端无需额外处理。
- **Test scenarios**：`Test expectation: none（前端无单测设施）— 手动验证：` 正式考试次数用尽时点击开始考试，显示次数上限提示，不进入答题。
- **Verification**：与 U4 错误返回联调一致。

---

## Scope Boundaries

- 不做题库/试题/考试的批量启停。
- 不做关闭对象的"回收站"或归档管理。
- 不改动既有删除逻辑。

### Deferred to Follow-Up Work
- 关闭语义若需"立即全局生效（含中断进行中考试）"，作为后续增强（本计划默认仅影响新使用）。
- 模拟考试成绩是否计入正式统计、与正式成绩分离展示等统计口径，单独规划。

---

## Risks & Dependencies

- **前端无单元测试设施**（`exam-vue` 仅 lint）：U5–U7 以手动/组件验证为主，行为正确性主要靠后端 U2/U4 单测。
- **存量数据默认值**：迁移须确保存量题库/试题默认开放、考试默认正式，避免上线后既有考试行为突变（尤其正式考试 `retake_count=0` 会立即限制为仅一次——需确认存量正式考试是否需要更宽默认，见 Assumptions）。
- **取卷次数校验与 `try_count` 累加时序**：校验须在累加前判断，避免 off-by-one。

---

## Assumptions

- 关闭只影响新使用（不回溯既有考试/试卷、不中断进行中作答）。如需立即全局生效，回到 Scope 的 Deferred 项重新规划。
- 存量考试迁移后默认 `exam_type=正式`、`retake_count=0`。若这会让现网正式考试突然限制为仅一次作答不可接受，迁移时可改默认或给存量考试设较大补考次数（实现期确认）。
- `el_user_exam.try_count` 是判定作答次数的权威字段（沿用现有累加）。

---

## Sources & Research

- 产品决策来源：`docs/product/functional-spec.md §8`
- 原型（UI 参考）：`docs/engineering/prototype/repo.html`、`qu.html`、`exam.html`、`exam-form.html`
- 后端模式：`exam-api/src/main/java/com/yf/exam/modules/exam/controller/ExamController.java`（`/state` 切换）、`modules/exam/entity/Exam.java`、`modules/paper/service/impl/PaperServiceImpl.java`（取卷与 `el_user_exam`）
- 前端模式：`exam-vue/src/views/qu/repo/index.vue`、`views/qu/qu/index.vue`、`views/exam/exam/form.vue`、`views/exam/exam/index.vue`
- 数据脚本风格：`docs/ops/install/数据库脚本.sql`
- 相关计划：`docs/engineering/plans/2026-05-29-001-feat-exam-assistant-booking-plan.md`（独立、可并行）
