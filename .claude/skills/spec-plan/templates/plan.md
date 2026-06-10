---
title: <特性名> · 实现计划
type: feat            # feat / fix / refactor
status: active
date: <YYYY-MM-DD，如 2026-06-04>
origin: <对应设计文档路径，如 docs/engineering/design/2026-06-04-NNN-xxx-design.md>
---

# <NNN> <特性名> · Plan

## Summary
〔一段话：做什么、整体落地路径〕

## Problem Frame
〔要解决的问题与约束，承接 PRD 的目标/非目标〕

## 设计依据
〔链接 `docs/engineering/design/<YYYY>-<MM>-<DD>-NNN-<slug>-design.md`；架构、接口清单、数据 ER、详细设计都在设计文档，这里只引用，不重抄。缺设计先跑 /spec-design〕

## DB 迁移
〔引用 `docs/ops/install/migration-<YYYY>-<特性名>.sql`；建表/加字段/索引/唯一键/种子数据。须与设计文档的 ER 模型逐字段一致。骨架见本目录 `migration.sql`〕

两条强制规约：
- **回滚段必填**：脚本含 up + 对应的回滚/down，并注明会丢什么数据、是否需先备份（无 schema 变更时写「无」）。
- **时间戳落地**：业务表带创建/更新时间戳两列（沿用项目惯例命名）；豁免表注明原因。

## Requirements 映射 + 覆盖矩阵
| 需求 | 实现单元 | 验收 |
| --- | --- | --- |
| R8 | U2、U3 | AE2、AE5 |

> 规则：每条 R/F 至少落到一个 U、且至少被一条 AE/AC 验证；任何一格为空都要回头补，或把该需求降级为非目标。

## Implementation Units

### U1 <单元名>
- **Files**：〔要新增/修改的文件，repo-relative 路径〕
- **Dependencies**：〔依赖哪个单元先完成〕
- **Patterns to follow**：〔参照现有哪段代码的写法/分层〕
- **设计依据**：〔指向 design 文档对应的接口/详细设计小节，如 design.md「详细设计 §占名额接口」；详细设计不在此重抄〕
- **原型页面**（前端/界面任务必填）：〔docs/engineering/prototype/<page>.html，UI 以原型为准；偏离先走 /spec-change 改原型〕
- **Execution note**（前端/界面任务必填）：用 `/ce-frontend-design` 方法论实现，收尾前按其要求截图自检设计保真度〔纯后端/逻辑单元留空或写执行姿态，如 test-first / characterization-first〕
- **覆盖需求**：〔实现哪些 R/F〕
- **Test scenarios**：〔对应哪些 AE/AC，怎么验〕

### U2 <单元名>
…
