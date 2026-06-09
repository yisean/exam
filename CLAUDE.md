# CLAUDE.md

本仓库是「云帆培训考试系统」：后端 `exam-api`（SpringBoot + MyBatis-Plus + Shiro），前端 `exam-vue`（Vue2 + Element-UI）。

## 必读规范（开发前先看）

- **工程宪法**：[`docs/engineering/constitution.md`](docs/engineering/constitution.md) —— 不可妥协原则 + 编码期基线（命名语义化、注释即文档（参阿里）、字段长度全链路一致等）。〔若缺：在仓库根跑 `init-project` 生成，见 spec-prd skill〕
- **研发流程**：[`docs/engineering/workflow.md`](docs/engineering/workflow.md) —— 需求→原型→**设计**→计划→开发→评审→测试→合并，每阶段做什么、何时算完成。
- **开发规范**：[`docs/engineering/conventions.md`](docs/engineering/conventions.md) —— 命名/分层/API/数据库/异常/鉴权/前端约定的**单一事实源**。写代码必须遵循。
- 后端细则见 [`exam-api/CLAUDE.md`](exam-api/CLAUDE.md)，前端细则见 [`exam-vue/CLAUDE.md`](exam-vue/CLAUDE.md)（在对应子目录工作时自动加载）。

## 硬约束（最高频，务必遵守）

1. **跟随既有代码**：命名、分层、风格与同模块现有代码一致（详见 `conventions.md`）；优先复用既有基类/工具/组件（`BaseController`/`ApiRest`/`ServiceImpl`/`BeanMapper`/`DataTable`/`DepartTreeSelect` 等），不擅自引入新依赖或新风格。
2. **命名与注释（参阿里规约）**：类 `UpperCamelCase`、方法/变量 `lowerCamelCase`、常量全大写下划线；杜绝拼音英混、无意义缩写；接口实现 `Impl` 后缀、异常 `Exception` 结尾、枚举 `Enum` 后缀。类/公共方法/接口方法/枚举字段必须 Javadoc（`/** */`）；方法内 `//` 注释另起一行置于被注释语句上方；改代码同步改注释，不留废弃注释代码。
3. **字段长度全链路一致**：以**业务最大字符数为唯一真值**，后端入参按字符数前置校验（不靠 DB 截断）、前端输入框 `maxlength`、接口文档三处对齐。本项目 MySQL（utf8mb4，按字符）直接 `varchar(10)` 存 10 汉字、不按字节估算（别写 `varchar(30)`）；若迁异构/信创库（SQL Server `nvarchar`、Oracle `varchar2(N CHAR)`、达梦 `LENGTH_IN_CHAR=1`、金仓/神通字符语义），DB 声明随库变、但业务字符数与前后端校验不变（详见 constitution #9）。
4. **流程优先文档**：改动需求范围**先回流 PRD/设计/plan 再写代码**（`/spec-change`）；各阶段分别用 `/spec-prd`、`/spec-prototype`、`/spec-design`、`/spec-plan`。
5. **追溯编号**：PRD 用 `R/F` 编号需求、`AE/AC` 编号验收，续编不重排；plan 实现单元 `U` 映射 `R/F`、引用 design 小节，测试对照 `AE/AC`。
6. **前端 UI 以原型为准**：对照 `docs/engineering/prototype/` 对应页面用真 Element-UI 还原（不照抄原型 HTML/CSS）；偏离先 `/spec-change` 改原型。
7. **DB 变更**：落 `docs/ops/install/migration-YYYY-<特性名>.sql`，与 design 的 ER 模型逐字段一致，含**回滚/down 段**；表/字段带 `COMMENT`、带 `create_time`/`update_time`。
8. **鉴权与事务**：权限标在 Controller 方法 `@RequiresRoles`（`sa`/`teacher`/`assistant`/`student`）；写操作 Service 加 `@Transactional(rollbackFor = Exception.class)`；敏感数据不进日志。
9. **不在默认分支开发**：先开特性分支；一次提交聚焦一件事，文档与代码改动分开提交。

## 提交

`<type>: <简述>`，正文列要点，结尾带 `Co-Authored-By`。type：`feat / fix / docs / refactor / chore`。

> 详细约定永远以 `conventions.md` 为准；本文只列最高频硬约束与新增规约（设计阶段、命名注释参阿里、字段长度全链路一致）。
