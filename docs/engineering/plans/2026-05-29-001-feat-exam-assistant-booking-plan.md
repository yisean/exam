---
title: "feat: 考试助理与预约考试（全栈）"
type: feat
status: active
date: 2026-05-29
origin: docs/engineering/design/2026-05-29-001-feat-exam-assistant-booking-design.md
---

# Plan 001 · 考试助理与预约考试（全栈）

## Summary

新增「考试助理」角色与「预约考试」能力：把考试的「开放方式 + 时间」下沉到新的**时间段**实体（不限人员 / 指定部门 / 预约考试），助理代表本部门（含下级）认领预约型时段、占部门名额，应考资格开考时按部门树动态判定。覆盖 PRD 001 的 R1–R19 / AE1–AE8。

## Problem Frame

承接 PRD：集中排期在部门多、作息各异时协调成本高。现有 `el_exam` 单一 `open_type` + 单值起止时间无法承载「一考多段、每段独立开放方式与窗口」，是本计划结构性改动来源。

## 设计依据

架构、核心流程、接口契约、前端设计、权限四级、数据 ER、详细设计（并发占名额 / 动态资格 / 管理员取消 / 改约时序）全部见
[`docs/engineering/design/2026-05-29-001-feat-exam-assistant-booking-design.md`](../design/2026-05-29-001-feat-exam-assistant-booking-design.md)。本计划只做任务拆分与追溯，不重抄设计；各单元「设计依据」指回 design 对应小节。

## DB 迁移

落点 [`docs/ops/install/migration-2026-预约考试.sql`](../../ops/install/migration-2026-预约考试.sql)：建 `el_exam_time_slot` / `el_exam_slot_depart` / `el_exam_booking` 三表（`(exam_id,depart_id)` 唯一键）、`sys_config` 加 `advance_visible_minutes`、种子 `assistant` 角色。与设计 ER 逐字段一致。

- **回滚**：`DROP TABLE el_exam_booking / el_exam_slot_depart / el_exam_time_slot`；`ALTER TABLE sys_config DROP COLUMN advance_visible_minutes`；`DELETE FROM sys_role WHERE id='assistant'`。DROP 连数据一起删，生产回滚前先备份三表。**现有脚本缺独立 down 段，建议按新规约补上。**
- **时间戳**：三表为操作型记录，`el_exam_booking.create_time` 即预约时间，无 `update_time`（改约=删+建），属豁免。
- **字段长度**：全为 `varchar(64)` ID + int + datetime，无自由文本输入字段，无前后端字符数对齐项。

## Requirements 映射 + 覆盖矩阵

| 需求 | 实现单元 | 验收 |
| --- | --- | --- |
| R1 多时段 | U1、U3、U9 | AE1（间接） |
| R2 开放类型 | U1、U3、U9 | AE1 |
| R3 仅 sa/teacher 改时段 | U3 | —（鉴权） |
| R4 预约型上限 | U1、U3、U9 | AE2 |
| R4b 指定部门 | U1、U3、U5、U9 | AE1 |
| R4c 提前可见 | U1、U5、U12 | AE5（窗口） |
| R4d 免约部门 | U1、U3、U5、U9 | AE1 |
| R5 助理可见 | U6、U8、U10 | —（入口） |
| R6 本部门含下级 | U4、U6 | AE1 |
| R7 每场一段 | U6 | AE4 |
| R8 占名额上限 | U6 | AE2 |
| R9 截止开始 | U5、U6、U10 | AE5 |
| R10 取消释放 | U6、U10 | AE3 |
| R11 改约 | U6、U10 | AE3 |
| R11b 预约人/时间 + 查看 | U6、U7 | AE7 |
| R12 预约型资格 | U5 | AE1、AE6 |
| R13 指定部门资格 | U5 | AE1 |
| R14 不限人员 | U5 | —（窗口内开放） |
| R15 动态判定 | U5 | AE6 |
| R16 管理端查看预约入口 | U14 | AE7 |
| R17 管理员取消 | U13、U14 | AE8 |
| R18 二次确认 + 保留试卷 | U13、U14 | AE8 |
| R19 查看/取消放宽 teacher | U7、U13 | AE7 |

> 每条 R 至少落到一个 U 且至少被一条 AE 验证；R3/R5/R14 为鉴权/入口/开放类语义，无独立 AE（由相应行为间接覆盖）。

## Implementation Units

后端先行（数据→服务），前端随后；按依赖排序。详细设计一律见 design 文档，本处不重抄。

### U1 · 数据库迁移与 assistant 角色种子
- **Files**：`docs/ops/install/migration-2026-预约考试.sql`。
- **Dependencies**：无。
- **Patterns to follow**：`docs/ops/install/数据库脚本.sql` 的建表/注释/索引风格。
- **设计依据**：design「数据 ER 模型」（三表 + 唯一键 + sys_config 列）。
- **Execution note**：DDL/种子，无行为逻辑；补 down 段。
- **覆盖需求**：R1、R2、R4、R4b、R4c、R4d、R7、R11b 的存储基础。
- **Test scenarios**：脚本在 MySQL 5.7/8 可执行，表/唯一键/角色/配置列创建成功。

### U2 · 后端实体 / Mapper / DTO + SlotOpenType 枚举
- **Files**：`modules/exam/entity/{ExamTimeSlot,ExamSlotDepart,ExamBooking}.java`、对应 `mapper/*`、`dto/*`、`enums/SlotOpenType.java`。
- **Dependencies**：U1。
- **Patterns to follow**：`modules/exam/entity/Exam.java`、`ExamRepo.java`、`mapper/ExamRepoMapper.java`。
- **设计依据**：design「数据 ER 模型」「技术决策 · SlotOpenType 枚举」。
- **Execution note**：纯数据结构/映射，逻辑在 U5/U6 测试。
- **覆盖需求**：R1、R2、R4、R4b、R4d、R11b。
- **Test scenarios**：MyBatis-Plus CRUD 基本读写（由后续服务测试间接覆盖）。

### U3 · 考试保存/详情扩展（写入时段与段-部门）
- **Files**：`dto/request/ExamSaveReqDTO.java`（加 `timeSlots`）、`service/impl/ExamServiceImpl.java`（save/findDetail 级联）、`controller/ExamController.java`（复用 `/save`、`/detail`，`@RequiresRoles("sa")`）。
- **Dependencies**：U2。
- **Patterns to follow**：`ExamServiceImpl` 现有 save 级联 `ExamRepo`/`ExamDepart` 的全量重写方式。
- **设计依据**：design「接口契约 · exam/save」「技术决策 · 段-部门共表」。
- **Execution note**：test-first（保存/回读/校验失败用例）。
- **覆盖需求**：R1、R2、R3、R4、R4b、R4d。
- **Test scenarios**：保存三类段→回读一致；预约型缺 `max_depart`/指定型未选部门→校验报错；删减段→旧段清除。

### U4 · 部门子树解析工具
- **Files**：`modules/sys/depart/service/SysDepartService.java#listSelfAndChildren(departId)` + impl。
- **Dependencies**：U1（依赖 `sys_depart`，无新表）。
- **Patterns to follow**：`modules/sys/depart` 既有 service。
- **设计依据**：design「详细设计 · 应考资格动态判定（子树解析 dept_code 前缀）」。
- **Execution note**：test-first，覆盖前缀相似不误纳（`A01` vs `A011`）。
- **覆盖需求**：R6、R12、R13（含下级语义）。
- **Test scenarios**：技术部→含全部下级；叶子→仅自身；根→整树；相似前缀不误纳。

### U5 · 应考资格与在线列表按时段判定
- **Files**：`ExamServiceImpl#onlinePaging`（段级聚合可见）、`PaperServiceImpl#createPaper`（开考资格门禁）。
- **Dependencies**：U2、U4。
- **Patterns to follow**：`onlinePaging` 现有按 `open_type`/`ExamDepart` 过滤（替换为段级）。
- **设计依据**：design「核心业务流程」「详细设计 · 应考资格动态判定（窗口 + 三类判定）」。
- **Execution note**：先写失败用例（各段类型 × 命中/未命中 × 窗口内/外），再实现。
- **覆盖需求**：R5、R9、R12、R13、R14、R15、R4c。
- **Test scenarios**：AE1（子树成员窗口内可考）、AE6（新入职动态获资格）、三类段判定、提前可见窗口。

### U6 · 助理预约服务与接口
- **Files**：`controller/ExamBookingController.java`（`@RequiresRoles("assistant")`）、`service/impl/ExamBookingServiceImpl.java`。
- **Dependencies**：U2、U4。
- **Patterns to follow**：`ExamController` 控制器/注解风格、`ServiceImpl` 事务写法。
- **设计依据**：design「详细设计 · 占名额并发与幂等」「改约时序」「接口契约 · booking/*」。**并发兜底（唯一键 + 事务复核）严格按 design 实现，勿自行简化。**
- **Execution note**：先写并发/边界失败用例（约满、重复约、已开始、抢最后名额），再实现。
- **覆盖需求**：R5、R6、R7、R8、R9、R10、R11、R11b。
- **Test scenarios**：AE2（约满拒绝）、AE3（改约释放/占用）、AE4（每场一段）、AE5（已开始拒绝）、并发抢名额仅一成功、非 assistant 被拒。

### U7 · 管理端查看某段预约（读侧，sa/teacher）
- **Files**：`ExamBookingController#slot-bookings`（联表 booking×depart×user），`@RequiresRoles({"sa","teacher"})`（R19 放宽）。
- **Dependencies**：U2、U6。
- **Patterns to follow**：现有 join 查询 ext DTO（`dto/ext/*`）。
- **设计依据**：design「接口契约 · exam/slot-bookings」。
- **覆盖需求**：R11b、R19。
- **Test scenarios**：某段两条预约→返回部门名/预约人/时间；空段→空列表；teacher 也可调（AE7）。

### U8 · 前端：assistant 角色路由接入
- **Files**：`exam-vue/src/router/index.js`（`roles:['assistant']` 路由组）。
- **Dependencies**：无（联调依赖 U6）。
- **Patterns to follow**：现有 `roles:['sa','teacher']` 路由组、`store/modules/permission.js`。
- **设计依据**：design「权限设计（四级）· 菜单权限」。
- **覆盖需求**：R5（入口可见性）。
- **Test scenarios**：assistant 登录只见「考试预约」，sa/student 不受影响。

### U9 · 前端：考试表单时间段配置
- **Files**：`exam-vue/src/views/exam/exam/form.vue`、`src/api/exam/exam.js`（save/detail 带 timeSlots）。
- **Dependencies**：U3；原型 `prototype/exam-form.html`。
- **Patterns to follow**：现有 `views/exam/exam/form.vue`、`views/qu/repo/form.vue`；`el-tree` 部门勾选。
- **设计依据**：design「前端设计」；UI 以 `prototype/exam-form.html` 为基线。
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检设计保真度。
- **覆盖需求**：R1、R2、R3、R4、R4b、R4d。
- **Test scenarios**：配置三类段保存→详情回读一致；预约型缺容量前端拦截；切类型显隐正确。

### U10 · 前端：考试助理预约页
- **Files**：`exam-vue/src/views/booking/index.vue`、`src/api/exam/booking.js`（bookable/book/cancel/rebook）。
- **Dependencies**：U6、U8；原型 `prototype/assistant-booking.html`。
- **Patterns to follow**：现有列表页 + `api/*.js` 的 `post()` 封装。
- **设计依据**：design「前端设计」「接口契约 · booking/*」；UI 以 `prototype/assistant-booking.html` 为基线。
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检设计保真度。
- **覆盖需求**：R5、R6、R7、R8、R9、R10、R11。
- **Test scenarios**：约满禁用并显已满、已开始显已截止、本部门已约显取消/改约、操作后状态刷新（AE2–AE5）。

### U11 · 前端：学员端时段感知（列表/应考）
- **Files**：`exam-vue/src/views/paper/exam/list.vue`、`preview.vue`。
- **Dependencies**：U5。
- **Patterns to follow**：现有 `views/paper/exam/list.vue`、`preview.vue`。
- **设计依据**：design「前端设计」「详细设计 · 窗口（可见/可作答）」。
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检。
- **覆盖需求**：R5、R9、R12、R13、R14、R4c。
- **Test scenarios**：提前可见显示但不可开始、进入窗口可开始、过期不可开始。

### U12 · 前端：系统配置「提前可见时长」
- **Files**：`exam-vue/src/views/sys/config/index.vue`（+ 必要时后端 config DTO 加字段）。
- **Dependencies**：U1。
- **Patterns to follow**：现有系统配置表单；原型 `prototype/sys-config.html`。
- **设计依据**：design「前端设计」。
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检。
- **覆盖需求**：R4c。
- **Test scenarios**：保存后值生效于学员端可见窗口（经 U5/U11 验证）。

### U13 · 管理员取消预约（后端，R17/R18）
- **Files**：`ExamBookingController#slot-bookings/cancel`（`@RequiresRoles({"sa","teacher"})`）、`ExamBookingServiceImpl`（查试卷 + 释放名额）。
- **Dependencies**：U6、U7。
- **Patterns to follow**：`ExamBookingServiceImpl` 事务写法、试卷查询既有 service。
- **设计依据**：design「详细设计 · 管理员取消 + 二次确认」。管理员**不受**「开始前」限制；仅释放名额、**不删试卷/成绩**。
- **Execution note**：先写「有试卷→needConfirm」「确认后释放、成绩仍在」用例。
- **覆盖需求**：R17、R18。
- **Test scenarios**：AE8（取消已有试卷部门：二次确认→释放名额、成绩保留、已开始也可取消）。

### U14 · 前端：管理端查看/取消预约弹窗（R16）
- **Files**：`exam-vue/src/views/exam/exam/form.vue`（时段表「查看预约」弹窗 + 行内「取消」二次确认）、`src/api/exam/booking.js`（slotBookings/adminCancel）。
- **Dependencies**：U7、U13；原型 `prototype/exam-form.html`。
- **Patterns to follow**：现有弹窗 + `this.$confirm`。
- **设计依据**：design「前端设计 · 时段表→查看预约弹窗」「权限四级 · 按钮权限」。
- **Execution note**：用 `/ce-frontend-design` 方法论实现，收尾前截图自检。
- **覆盖需求**：R16。
- **Test scenarios**：AE7（点查看预约见已约部门/预约人/时间）、有试卷取消弹二次确认（AE8 前端侧）。

## Scope Boundaries

继承 PRD 非目标：员工个人自助预约、线下考场/机位预约、代报名审核、预约通知提醒、预约统计报表、候补排队——均不做。
