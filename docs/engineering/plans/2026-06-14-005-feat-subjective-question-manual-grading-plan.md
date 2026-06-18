---
title: "简答题与人工阅卷 · 实现计划"
type: feat
status: active
date: 2026-06-14
origin: docs/engineering/design/2026-06-14-005-feat-subjective-question-manual-grading-design.md
---

# 005 简答题与人工阅卷 · Plan

## Summary

把设计接通主观题骨架的方案拆为 10 个可独立认领的实现单元：题型枚举 + 简答录题、组卷抽取简答、考生作答（复用 `fillAnswer`）、**整份阅卷接口（核心）**、待阅卷列表与权限放开、学员成绩呈现，及 1 列 DB 迁移。整体复用既有底层（`has_saq/WAIT_OPT/sumSubjective/sumObjective/handExam` 分支与基线库已存在的 `saq_count/saq_score/obj_score/subj_score`），新增代码集中在阅卷接口与前端阅卷页。

## Problem Frame

承接 PRD 005 与设计：系统已具备主观题骨架但链路断裂（无 type 4 枚举、无阅卷接口、组卷不抽简答、`has_saq` 恒为 false）。本计划在不触碰综合题判分链（PRD 非目标）的前提下补齐缺口，并把简答分值统一到组卷 `saq_score`（与单选/多选一致，复用基线库已存在列）。

## 设计依据

技术方案、接口契约、ER、详细设计见 [设计文档](2026-06-14-005-feat-subjective-question-manual-grading-design.md)，本计划只引用不重抄。关键：
- 概要§接口清单（4 个接口 + 鉴权 + 错误码）
- 详细设计 §1 录题、§2 组卷抽取与 has_saq、§3 阅卷接口（状态机/校验/并发）、§4 待阅卷列表与权限、§5 成绩呈现、§6 考生作答
- 数据 ER 模型（仅新增 `el_paper_qu.comment`）

## DB 迁移

见 [`docs/ops/install/migration-2026-subjective-grading.sql`](../../ops/install/migration-2026-subjective-grading.sql)，与设计 ER 逐字段一致。

- **唯一新增列**：`el_paper_qu.comment varchar(200)`（阅卷点评，考生可见）。幂等（information_schema + PREPARE）。
- **已存在、不重复添加**（经核验基线库 `数据库脚本.sql`）：`el_exam_repo.saq_count/saq_score`、`el_paper.obj_score/subj_score/user_score/has_saq`、`el_paper_qu.answer/actual_score`。
- **回滚段**：含 DOWN（`DROP COLUMN comment`），注明丢点评数据、回滚前备份。
- **时间戳**：未建新业务表，无新增时间戳列（`el_paper_qu` 为快照明细表，沿用现状）。
- **字段长度**：`comment` 最大 200 字符（MySQL utf8mb4 按字符）；前端 `maxlength=200`、后端按字符校验，三处对齐。

## Requirements 映射 + 覆盖矩阵

| 需求 | 实现单元 | 验收 |
| --- | --- | --- |
| R1 简答题型/筛选 | U1、U2 | AE1 |
| R2 简答录题（题干/参考答案/解析，无分值） | U1、U2 | AE2 |
| R3 组卷可选含0 + 统一配分 | U3、U4 | AE3 |
| R4 考生作答简答 | U5 | AE4 |
| R5 含简答→待阅卷 | U3、U7 | AE5 |
| R6 纯简答客观0待阅卷 | U3 | AE6 |
| R7 整份阅卷 | U6、U8、U10 | AE7 |
| R8 得分区间/漏评校验 | U6、U8 | AE8 |
| R9 合分=客观+主观、判定 | U6 | AE5 |
| R10 阅完即可见（总分/参考答案/点评） | U6、U9 | AE9 |
| R11 阅卷限 sa/teacher、考生端无入口 | U7、U8 | AE10 |

> 每条 R 至少落到一个 U、且至少被一条 AE 验证；每个 U 的覆盖需求均指向存在的 R。无空格。

## Implementation Units

### U1 题型枚举 + 简答录题（后端）
- **Files**：`exam-api/.../modules/qu/enums/QuType.java`、`exam-api/.../modules/qu/service/impl/QuServiceImpl.java`、`exam-api/.../modules/qu/dto/QuDTO.java`（如需 referenceAnswer 透传）、`exam-api/.../core/api/ApiError.java`
- **Dependencies**：无（最先做，U3/U6 依赖其枚举）
- **Patterns to follow**：现有 `QuType` 常量式枚举；`QuServiceImpl.save` 既有按题型分支校验（单选/不定项）；参考答案复用 `el_qu_answer` 单行（仿现有选项落库）
- **设计依据**：design 详细设计 §1
- **Execution note**：test-first——先写简答校验单测（无参考答案被拒、带选项被拒）
- **覆盖需求**：R1、R2
- **Test scenarios**：AE1（题型可选简答、保存、列表筛选、回显）、AE2（题干/参考答案/解析超长拦截；缺参考答案抛 `ERROR_QU_SAQ_INVALID`）

### U2 简答录题前端
- **Files**：`exam-vue/src/views/qu/`（录题表单 form、列表筛选）、题型过滤器 `exam-vue/src/filters`
- **Dependencies**：U1（接口字段）
- **Patterns to follow**：现有录题表单按题型显隐；简答隐藏选项区、显示参考答案、**不显示分值**（分值在组卷）
- **设计依据**：design §前端设计；概要§接口清单（/qu/save）
- **原型页面**：`docs/engineering/prototype/qu-form.html`、`qu.html`（UI 以原型为准）
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检设计保真度
- **覆盖需求**：R1、R2
- **Test scenarios**：AE1、AE2

### U3 组卷抽取简答 + 据实置 has_saq（后端）
- **Files**：`exam-api/.../modules/exam/entity/ExamRepo.java`（+`saqCount`/`saqScore` 字段映射，列已存在）、相关组卷 DTO（`ExamRepoDTO`/`ExamRepoExtDTO`）、`exam-api/.../modules/paper/service/impl/PaperServiceImpl.java`（`generateByRepo`/`processPaperQu`/`savePaper`）
- **Dependencies**：U1（`QuType.SHORT_ANSWER`）
- **Patterns to follow**：`generateByRepo` 现有各题型抽取分支；`processPaperQu` 按题型 `setScore(repo.getXxxScore())`；`savePaper` 现有 totalScore 累加
- **设计依据**：design 详细设计 §2
- **Execution note**：test-first——覆盖纯客观/含简答/纯简答三态的 `has_saq` 与 totalScore
- **覆盖需求**：R3、R5、R6
- **Test scenarios**：AE3（数量 N>0 抽 N 道、每题取 saq_score；N=0 不含简答）、AE5（含简答 → has_saq=true → 交卷进待阅卷）、AE6（纯简答 → 客观分 0、待阅卷）

### U4 组卷简答配置前端
- **Files**：`exam-vue/src/views/exam/`（创建考试 · 组卷规则编辑）
- **Dependencies**：U3（ExamRepo 字段）
- **Patterns to follow**：组卷规则表既有「单选/多选（数量/分值）」编辑，简答同模式加「数量/分值」
- **设计依据**：design §前端设计
- **原型页面**：`docs/engineering/prototype/exam-form.html`（组卷规则表「简答题（数量/分值）」列）
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检
- **覆盖需求**：R3
- **Test scenarios**：AE3

### U5 考生作答简答前端（后端复用 fillAnswer）
- **Files**：`exam-vue/src/views/`（在线答题页，简答多行文本框 + 答题卡分组）。后端无改动——复用 `PaperServiceImpl.fillAnswer` 既有主观题分支。
- **Dependencies**：U3（试卷含简答题）
- **Patterns to follow**：答题页既有按题型渲染与答题卡分组；`fillAnswer` 主观题分支（保存 answer、answered、isRight=true）
- **设计依据**：design 详细设计 §6
- **原型页面**：`docs/engineering/prototype/exam-taking.html`
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检
- **覆盖需求**：R4
- **Test scenarios**：AE4（作答落库 answer、清空回未答、简答不进错题本）

### U6 整份阅卷接口（后端 · 核心）
- **Files**：`exam-api/.../modules/paper/controller/PaperController.java`（+`/review`、`/review-detail`）、`exam-api/.../modules/paper/service/impl/PaperServiceImpl.java`（+`review`/`reviewDetail`）、`exam-api/.../modules/paper/dto/request/PaperReviewReqDTO.java`、`exam-api/.../modules/paper/dto/response/PaperReviewRespDTO.java`、`exam-api/.../core/api/ApiError.java`（`ERROR_PAPER_NOT_WAIT`/`ERROR_SCORE_RANGE`）
- **Dependencies**：U1（枚举）、U10（`comment` 列）
- **Patterns to follow**：`PaperController` 既有端点与 `@RequiresRoles`；`handExam` 完成分支（合分 + `joinResult`）；`sumSubjective` 已存在直接调用
- **设计依据**：design 详细设计 §3（状态机 `WAIT_OPT→FINISHED`、得分区间/漏评校验、带状态条件更新的并发兜底、必要时序图）
- **Execution note**：test-first——重点覆盖状态机与并发（非待阅卷拒绝、越界/漏评回滚、并发双提交仅一次生效）；写操作 `@Transactional(rollbackFor=Exception.class)`
- **覆盖需求**：R7、R8、R9、R10
- **Test scenarios**：AE5（55+12+9=76、判定通过、转 FINISHED）、AE7（review-detail 加载题干/作答/参考答案/解析）、AE8（满分 15 填 18 或漏评被 `ERROR_SCORE_RANGE` 拒）

### U7 待阅卷列表 + 阅卷权限放开（后端）
- **Files**：`exam-api/.../modules/exam/controller/ExamController.java`（`review-paging` 鉴权 `sa`→`sa,teacher`）、`exam-api/.../modules/paper/`（按 `state=WAIT_OPT` 过滤的分页条件，`PaperListReqDTO` 如无状态入参则补）
- **Dependencies**：U1
- **Patterns to follow**：`@RequiresRoles(value={"sa","teacher"}, logical=Logical.OR)`；既有 `paper/paging`
- **设计依据**：design 详细设计 §4、概要§权限矩阵
- **Execution note**：test-first——teacher 可调 review-paging；student/assistant 调阅卷接口被拒
- **覆盖需求**：R5、R11
- **Test scenarios**：AE10（sa/teacher 可进待阅卷；student/assistant 无入口、无法调用）

### U8 阅卷前端（阅卷页 + 待阅卷列表入口）
- **Files**：`exam-vue/src/views/`（新增整份阅卷页；考试记录页加状态列与「阅卷」入口）
- **Dependencies**：U6、U7（接口）
- **Patterns to follow**：试卷详情页渲染；逐题得分输入 + 区间校验 + 点评 textarea + 整份提交
- **设计依据**：design §前端设计、详细设计 §3
- **原型页面**：`docs/engineering/prototype/paper-grade.html`、`exam-papers.html`（待阅卷状态列 + 阅卷入口）
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检（含得分区间校验与实时合分交互）
- **覆盖需求**：R7、R8、R11
- **Test scenarios**：AE7、AE8、AE10

### U9 学员成绩呈现（后端查询 + 前端）
- **Files**：`exam-api/.../modules/paper/`（`paperResult`/`listForPaperResult` 带出简答 `actual_score`/`comment`/参考答案）、`exam-vue/src/views/`（成绩详情、我的成绩列表）
- **Dependencies**：U6（阅卷写回 comment/actual_score）
- **Patterns to follow**：既有 `paperResult` 与成绩详情逐题渲染；按 `state` 渲染「待阅卷/已完成」
- **设计依据**：design 详细设计 §5
- **原型页面**：`docs/engineering/prototype/exam-result.html`、`my-records.html`、`paper-detail.html`
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检
- **覆盖需求**：R10
- **Test scenarios**：AE9（阅完即时可见总分、逐题得分、参考答案、老师点评；待阅卷期显示「待阅卷」不出总分）

### U10 DB 迁移（comment 列）
- **Files**：`docs/ops/install/migration-2026-subjective-grading.sql`（已落盘）
- **Dependencies**：无（U6 依赖本单元执行）
- **Patterns to follow**：`migration-2026-综合题与不定项.sql` 的 information_schema + PREPARE 幂等写法
- **设计依据**：design 数据 ER 模型
- **Execution note**：执行前于 dev 库核对 `el_paper_qu` 现状；注意 dev 库若未跑过 004 迁移需先补 004（与本特性无关但同库）
- **覆盖需求**：R7（点评存储落地）
- **Test scenarios**：迁移可重复执行（幂等）、回滚段可撤列

## 与其他 plan 的关系

- 与 004（综合题/不定项）同库不同列，无字段冲突；本特性不改综合题判分链。
- **环境提示**：会话起始报错 `Unknown column 'q.parent_id'` 表明 dev 库尚未执行 004 迁移；开发/联调本特性前需先在 dev 库执行 `migration-2026-综合题与不定项.sql`，再执行本特性迁移。
