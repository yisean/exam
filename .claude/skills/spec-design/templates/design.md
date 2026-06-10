---
title: <特性名> · 技术设计
type: feat            # feat / fix / refactor
status: active
date: <YYYY-MM-DD，如 2026-06-04>
origin: <对应 PRD 路径，如 docs/product/prd/2026-06-04-NNN-xxx.md>
---

# <NNN> <特性名> · Design

## Summary
〔一段话：整体技术方案是什么〕

## Problem Frame
〔要解决的问题与约束，承接 PRD 的目标/非目标〕

## 概要设计
- **架构与模块**：〔后端模块 / 前端页面 / 外部依赖，及它们的协作关系〕
- **核心业务流程**：

  ```mermaid
  flowchart TD
      A[发起] --> B{资格判定}
      B -- 通过 --> C[占名额/写库]
      B -- 不通过 --> D[拒绝并提示]
      C --> E[返回成功]
  ```
- **技术选型与关键决策**：〔选了什么、为什么、放弃的备选（可追溯）〕
- **接口清单与契约**：〔path / 方法 / 入参出参 + 鉴权（如 @RequiresRoles）、字段校验、分页约定、新增错误码（按域续编），对应覆盖的 R/F〕

  | 方法 | path | 鉴权 | 入参 | 出参 | 新增错误码 | 覆盖 |
  | --- | --- | --- | --- | --- | --- | --- |
  | POST | /xxx | sa | {…} | {…} | ERR_XXX「…」 | R8 |

  典型请求/返回示例：
  ```http
  POST /xxx   { "id": "..." }
  200 { "code": 0, "msg": "ok" }   // 失败：{ "code": ERR_XXX, "msg": "..." }
  ```

- **前端设计**（涉及界面时）：
  - 页面清单 + 路由：〔page → route〕
  - 页面间关系：〔A 页 →（带 id）→ B 页；列表→详情→编辑 等跳转/数据流〕
  - 关键组件 + 每页调用的接口：〔组件划分；某页调哪几个接口〕
  - UI 以 `docs/engineering/prototype/<page>.html` 为基线，本节只讲结构与协作
- **权限设计（四级）**：菜单权限 / 按钮权限（前端可见性）+ 接口权限（下面矩阵）+ 数据权限（行级可见域）
  - 接口权限矩阵：〔角色 × 接口/操作，谁能调〕

    | 接口/操作 | sa | teacher | assistant | student |
    | --- | --- | --- | --- | --- |
    | /xxx | ✓ | ✗ | ✗ | ✗ |

  - 行级可见域：〔按部门/角色过滤数据，如 assistant 只看本部门〕
- **非功能约束承接**：〔PRD 的每条 NFR → 对应的设计手段或校验点〕
- **可观测与审计设计**：〔关键日志点（入参/出参/耗时/异常，敏感字段脱敏）；审计记录（谁何时对谁做了什么）；监控指标/告警（必要时）〕
- **风险与回滚**：〔高风险点、并发/权限/性能注意项〕

## 数据 ER 模型
```mermaid
erDiagram
    EXAM ||--o{ EXAM_TIME_SLOT : has
    EXAM_TIME_SLOT ||--o{ EXAM_BOOKING : booked_by
    DEPART ||--o{ EXAM_BOOKING : makes
    EXAM_TIME_SLOT {
        bigint id PK
        bigint exam_id FK
        datetime start_time
        datetime end_time
        int open_type
    }
    EXAM_BOOKING {
        bigint id PK
        bigint slot_id FK
        bigint depart_id FK
        string booker
        datetime create_time
        datetime update_time
    }
```
> ER 是逻辑视图，是 `/spec-plan` 物理 migration（建表脚本）的唯一逻辑来源，二者逐字段一致。
>
> **时间戳规约**：业务实体默认带创建/更新时间戳两列（命名沿用项目惯例，如 `create_time`/`update_time`）；纯字典/只读/中间表可豁免，但要注明原因（如 `EXAM_SLOT_DEPART：纯关联表，无需时间戳`）。
>
> **字段长度规约**：`varchar(N)` 的 N 是字符数（utf8mb4 下 N 个汉字），按业务最大汉字数定义、不按字节。字符串字段在下表标注最大字符数，作为前后端校验唯一真值：
>
> | 字段 | 类型 | 最大字符数 | 内容类型 |
> | --- | --- | --- | --- |
> | booker | varchar | 32 | 汉字/字母 |

## 详细设计
〔按模块 / 接口 / 关键流程组织，不按实现单元——拆单元在 /spec-plan，那里反向引用本节〕
〔右尺寸：只写 AI 推不出/推错代价高的决策（并发/事务/状态机/判定规则/跨单元契约/权限边界）；CRUD 骨架、DTO 映射、样板签名等能可靠生成的不写，留给 ce-work 生成后评审〕

### <模块/接口名>（覆盖 R8、R9）
- **依赖**：〔本段依赖的概要/ER 小节，如 概要§接口清单、ER§EXAM_BOOKING——ER 改时据此定位要跟改的详细设计〕
- **接口签名**：〔入参出参类型、错误码/异常约定〕
- **核心逻辑**：〔关键算法 / 判定规则 / 状态机〕
- **并发与幂等**（占名额、重复提交等高频写场景才写）：〔乐观锁 / 悲观锁 / 唯一键兜底 / 接口幂等键，及冲突时的失败处理与提示〕
- **代码结构落点**：〔分层落点（Controller/Service/Mapper）；新增枚举/常量；异常码/异常体系（按域续编）；可复用的公共组件/工具〕
- **必要时序**：〔复杂交互的调用顺序与边界条件，如资格判定、并发占名额、回滚补偿〕

### <模块/接口名>（覆盖 R10）
…
