# CLAUDE.md · 项目编码规约（AI agent 自动加载）

> 本文是**编码期落地细则**，由 Claude Code / `ce-work` 在写代码时自动加载。
> 原则基线见 [`docs/engineering/constitution.md`](docs/engineering/constitution.md)（工程宪法），流程见 [`docs/engineering/workflow.md`](docs/engineering/workflow.md)。
> 优先级：**constitution > workflow > 本文**；本文是 constitution「工程基线」按本项目技术栈的细化，命名/注释规约**参阿里巴巴《Java 开发手册》**。
> 〔按项目填技术栈〕后端：`<后端栈，如 SpringBoot + MyBatis-Plus + Shiro>`；前端：`<前端栈，如 Vue2 + Element-UI>`。

## 一、命名规范（参阿里规约，强制）

1. **语言**：严禁拼音与英文混合，更不许中文命名；纯拼音也避免（国际通用词除外，如 `alibaba`）。杜绝无意义缩写、`a/b/c`/`tmp1` 临时名、中英混杂。
2. **大小写**
   - 类名 `UpperCamelCase`：`ExamBookingService`、`UserController`。
   - 方法名 / 参数 / 成员 / 局部变量 `lowerCamelCase`：`getUserById`、`departId`。
   - 常量全大写下划线、语义完整：`MAX_BOOK_DEPART`、`DEFAULT_PAGE_SIZE`；杜绝魔法值（抽常量/枚举）。
   - 包名全小写单数：`com.<org>.<app>.modules.exam`。
3. **后缀约定**
   - 接口实现类 `Impl` 后缀（`ExamServiceImpl`）；接口名**不加** `I` 前缀。
   - 抽象类 `Abstract`/`Base` 开头；异常类 `Exception` 结尾；枚举类 `Enum` 后缀，成员全大写下划线。
   - 测试类 `Test` 结尾。
   - 领域模型分层后缀（如用）：`DO`/`Entity`、`DTO`、`VO`、`Query`/`ReqDTO`；POJO 布尔成员**不加** `is` 前缀（数据库布尔字段加 `is_`）。
4. **方法动词统一**（Service/DAO）：`get` 取单个、`list` 取多个、`count` 统计、`save`/`insert` 新增、`update` 更新、`remove`/`delete` 删除。
5. **数组**：`int[] arr`，类型与中括号一起，不写 `int arr[]`。

## 二、注释规范（参阿里规约，强制）

1. **Javadoc**：类、类属性、公共方法、**接口方法**、枚举每个字段，必须用 `/** */` Javadoc，不得用 `//` 行注释代替。方法注释说明：做什么、入参、出参、可能抛的异常。
2. **类头**：每个类标注**职责说明**（及创建者/日期，按团队习惯）。
3. **方法内注释**：单行用 `//`，**另起一行置于被注释语句上方**（不跟在行尾）；多行用 `/* */`。只注释「为什么这么写」「边界/兼容/历史原因」，不复述代码。
4. **同步**：改代码必须同步改注释，尤其参数、返回值、异常、核心逻辑——杜绝注释与实现不符。
5. **废弃代码**：不保留被注释掉的代码（交给版本管理）；确需保留要在上方说明原因。不写废话注释。
6. **标记**：`// TODO 〔责任人〕 〔日期〕 说明`、`// FIXME 〔责任人〕 〔日期〕 说明`。
7. 中文把问题说清楚，专有名词、关键字、框架名保留英文。

## 三、分层与结构（按技术栈细化）

- 严格分层：`Controller → Service → Mapper/Repository → Entity`；禁跨层直连、禁 Controller 直接操作 DB。
- 对外**不返回 Entity**，用 `DTO`/`VO` 转换。
- 单一职责：一个方法只做一件事；固定状态/类型抽枚举（见 constitution「零魔法值」）。
- 新增枚举/异常码：按域续编，集中放置（如 `modules/<域>/enums`、统一 `ApiError`）。
- 〔补充本项目目录结构与既有模式：`<后端模块路径>`、`<前端目录路径>`〕

## 四、异常 / 日志 / 安全（承接 constitution）

- 异常分层兜底：区分业务/系统/第三方，全局统一处理，不裸抛原生堆栈（constitution #5）。
- 多表/多步操作加事务保证原子性；外部调用加超时/重试/降级；资源用完必释放（constitution #4）。
- 关键链路打印入参/出参/耗时/异常；**日志禁打手机号/密码/身份证等敏感数据**，敏感字段脱敏（constitution #1）。
- 入参全校验（非空/边界/格式/业务合法）。
- **字段长度全链路一致**（constitution #9）：业务最大字符数为唯一真值，后端按**字符数**校验（不靠 DB 截断）、前端 `maxLength`、接口文档三处对齐。DB 列长按所选库语义声明（存 10 个汉字为例）：

  | 数据库 | 写法 |
  | --- | --- |
  | MySQL / PostgreSQL | `varchar(10)`（按字符） |
  | SQL Server | `nvarchar(10)`（Unicode 按字符） |
  | Oracle | `varchar2(10 CHAR)`（默认按字节，须显式 CHAR） |
  | 达梦 DM | 开 `LENGTH_IN_CHAR=1` 后 `varchar(10)`，否则 UTF-8 `varchar(30)` |
  | 人大金仓 KingbaseES | PG 模式 `varchar(10)`；Oracle 兼容模式按 Oracle |
  | 神州通用 | 优先字符语义 `varchar(10)`，否则按字节折算（以版本文档为准） |

  > DB 声明值可因库而异，但「业务字符数」与前后端校验恒为同一个数。

## 五、前端规范（参阿里前端规约 + 框架官方 Style Guide，按 `<前端栈>` 取用）

### 1. 命名
- **组件名**：`PascalCase` 且**多词**（避免与 HTML 元素冲突，如 `UserCard` 不用 `Card`）；组件文件 `PascalCase.vue` 或 `index.vue`（随项目）。
- **目录 / 普通文件**：`kebab-case`（小写中划线）。
- **变量 / 函数**：`camelCase`；**常量** `UPPER_SNAKE_CASE`；布尔变量加 `is/has/can` 前缀；私有成员 `_` 前缀。
- **CSS class**：`kebab-case` 或 BEM（`block__element--modifier`）；不用 id 选择器写样式。
- 杜绝拼音/无意义缩写（同后端命名精神）。

### 2. 代码格式（ESLint + Prettier，统一，提交前 `lint`）
- 缩进 / 引号 / 分号 / 尾逗号 / 行宽以项目 `.eslintrc` + `.prettierrc` 为准〔本项目：`<2 空格 / 单引号 / 无分号…>`〕；**不手动对抗 formatter**。
- import 分组排序：第三方库 → 别名（`@/`）→ 相对路径。

### 3. JavaScript / ES
- `const` 优先、`let` 次之，**禁 `var`**；用 `===` 不用 `==`。
- 善用解构、模板字符串、箭头函数、可选链 `?.` / 空值合并 `??`。
- **魔法值抽常量**（同后端「零魔法值」）；异步用 `async/await` + `try-catch`，不裸吞错误。

### 4. 组件
- **单一职责**：一个组件只做一件事；公共逻辑抽通用组件 / `mixin` / `composable`（hooks）。
- **Vue**：`props` 定义 `type/required/default/validator`；`data` 必须是函数；`v-for` 必带稳定 `key` 且**不与 `v-if` 同元素**；组件选项顺序固定（`name/components/props/data/computed/watch/生命周期/methods`）；模板表达式保持简单，复杂逻辑入 `computed`；自定义事件名 `kebab-case`。
- **React**：函数组件 + Hooks；列表 `key` 稳定；`props` 用 TS / PropTypes；副作用进 `useEffect` 并清理；避免在 render 内新建函数 / 对象导致重渲染。

### 5. 状态与请求
- 全局状态用状态管理工具（Vuex / Pinia / Redux），**模块化 + 命名空间**；局部状态留组件内，不滥用全局。
- **统一封装请求**（拦截器 / Token / 超时 / 错误提示 / Loading）；高频操作**防抖节流**、**防重复提交**；不在代码里写 host，走代理 / 环境变量。

### 6. 样式
- **作用域化**（`scoped` / CSS Modules），禁全局污染；用预处理器变量（SCSS / Less），不散落硬编码色值。
- 避免 `!important` 与过深选择器；类名 `kebab-case` / BEM。

### 7. 性能
- 路由 / 重组件**懒加载**；长列表分页或**虚拟滚动**；图片懒加载；UI 库**按需引入**。
- 避免无效监听、重复请求、不必要的重渲染。

### 8. 安全
- **XSS**：慎用 `v-html` / `dangerouslySetInnerHTML`，必须转义不可信内容；不拼接不可信 URL。
- 敏感信息（token、个人隐私）不打前端日志、不长存 `localStorage`。

### 9. 注释（同后端「注释即文档」精神）
- 组件标注用途、`props` / `emit` 说明；复杂逻辑注释「为什么」；`TODO`/`FIXME` 带责任人与时间。

### 10. 字段与校验
- **输入框 `maxlength` 严格等于字段的业务字符数**（constitution #9）；表单校验文案标准化（如「最多 N 个字符」）。

### 11. UI 以原型为准（铁律）
- **对照 `docs/engineering/prototype/` 对应页面用真组件还原**（布局 / 字段 / 状态 / 交互 / 文案），不照抄原型 HTML/CSS；偏离先走 `/spec-change` 改原型再改代码。

## 六、提交与协作（承接 workflow 阶段 8）

- 不在默认分支直接开发，先开特性分支；改动需求范围先回流文档（`/spec-change`）。
- 小步增量、单一功能单一提交；提交信息 `<type>: <简述>`（`feat/fix/docs/refactor/chore`）；合入主干前 squash。

> 本文是模板：删除用不到的小节、把 `<占位>` 换成本项目实际技术栈与目录，并随项目演进维护。
