# 开发规范（命名 / 分层 / 约定）

> 本文是本项目**开发规范的单一事实源**，从现有 `exam-api`（SpringBoot + MyBatis-Plus + Shiro）与 `exam-vue`（Vue2 + Element-UI）代码中提炼。新代码**遵循既有约定**，与本文冲突时以本文为准；本文与实际代码冲突时，先改代码对齐、或回流修订本文。配合 [`workflow.md`](workflow.md)（阶段 4 开发）使用。
>
> AI agent 通过仓库根 [`CLAUDE.md`](../../CLAUDE.md) 及各栈子目录 `CLAUDE.md` 自动加载本规范的要点。

## 通用原则

- **跟随既有代码**：新增代码的命名、分层、风格与同模块现有代码保持一致，优先复用既有基类/工具/组件，不引入新风格或新依赖。
- **中文业务、英文标识**：注释、Swagger 描述、界面文案用中文；类名、方法名、变量名用英文。
- **单一职责分层**：后端 Controller 薄、Service 厚；前端 view 管交互、api 管请求、components 管复用。

---

# 一、后端规范（exam-api）

根包 `com.yf.exam`。分层：`ability`（基础能力）/`core`（通用框架）/`modules`（业务模块）/`config`/`aspect`。

## 1. 包结构与分层

业务代码放 `modules/<模块>/`，模块内固定子包：

```
modules/<模块>/
├── <Xxx>Controller.java        # 控制器，直接放模块根或 controller/
├── controller/                 # 控制器（模块大时单独建）
├── service/<Xxx>Service.java        # Service 接口
├── service/impl/<Xxx>ServiceImpl.java  # Service 实现
├── mapper/<Xxx>Mapper.java          # MyBatis-Plus Mapper
├── entity/<Xxx>.java                # ORM 实体
├── dto/<Xxx>DTO.java                # 通用 DTO
├── dto/request/<Xxx>ReqDTO.java     # 请求入参
├── dto/response/<Xxx>RespDTO.java   # 响应出参
├── dto/ext/<Xxx>ExtDTO.java         # 多表 JOIN 结果
└── enums/<Xxx>.java                 # 业务枚举（接口常量式）
```

模块大时可二级分层（如 `modules/sys/{config,depart,user,...}/`，每个子模块再含上面结构）。

## 2. 类命名与注解

| 角色 | 命名 | 基类 / 注解 |
| --- | --- | --- |
| Controller | `XxxController` | `@RestController` + `@Api(tags=...)` + `@RequestMapping("/exam/api/<模块>/<资源>")`，继承 `BaseController` |
| Service 接口 | `XxxService` | `extends IService<Entity>` |
| Service 实现 | `XxxServiceImpl` | `@Service`，`extends ServiceImpl<XxxMapper, Entity> implements XxxService` |
| Mapper | `XxxMapper` | `extends BaseMapper<Entity>`（不加 `@Mapper`，由 `@MapperScan` 统一扫描） |
| Entity | `Xxx`（业务名） | `@Data` + `@TableName("el_xxx")`，`extends Model<Xxx>` |

**Controller 端点约定**：统一用 `POST`，标准动作 `/save`（新增或改）、`/delete`、`/detail`、`/paging`、`/state`。示例：

```java
@Api(tags = {"考试"})
@RestController
@RequestMapping("/exam/api/exam/exam")
public class ExamController extends BaseController {
    @Autowired
    private ExamService baseService;

    @RequiresRoles("sa")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public ApiRest save(@RequestBody ExamSaveReqDTO reqDTO) {
        baseService.save(reqDTO);
        return super.success();
    }

    @RequestMapping(value = "/paging", method = RequestMethod.POST)
    public ApiRest<IPage<ExamDTO>> paging(@RequestBody PagingReqDTO<ExamDTO> reqDTO) {
        return super.success(baseService.paging(reqDTO));
    }
}
```

**Entity 示例**：

```java
@Data
@TableName("el_exam")
public class Exam extends Model<Exam> {
    @TableId(value = "id", type = IdType.ASSIGN_ID)   // 雪花 ID，String
    private String id;

    @TableField("title")
    private String title;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;
}
```

## 3. DTO 分层

| 后缀 | 用途 | 继承 |
| --- | --- | --- |
| `XxxDTO` | 通用传输/列表展示 | `extends BaseDTO` |
| `XxxReqDTO`（放 `dto/request/`） | API 入参 | 常 `extends XxxDTO` 扩字段 |
| `XxxRespDTO`（放 `dto/response/`） | API 出参 | 常 `extends XxxDTO` 扩展示字段 |
| `XxxExtDTO`（放 `dto/ext/`） | 多表 JOIN 结果 | 按需 |

- DTO 用 `@Data` + `@ApiModel` / `@ApiModelProperty` 做 Swagger 文档。
- Entity ↔ DTO 转换统一用 `BeanMapper.copy(src, target)`（Dozer），**不手写 getter/setter 搬运**。
- 日期入参/出参用 `@JsonFormat` + `@DateTimeFormat` 指定 `GMT+8` 格式。

## 4. 数据库命名

- **表前缀**：业务表 `el_`，系统表 `sys_`；表名/字段名一律**下划线小写**。
- **主键**：字段 `id`，`varchar(64)`，存雪花 ID（`IdWorker.getIdStr()` / `@TableId(type = IdType.ASSIGN_ID)`）。
- **审计字段**：`create_time` / `update_time`（`datetime`）。
- **状态字段**：用业务状态列代替物理删除，如 `state int`（`0` 正常 / `1` 禁用）、`open_type int` 等，**不真删数据**。
- **关联唯一约束**：用复合唯一键防重，如 `el_exam_booking` 的 `UNIQUE KEY exam_depart(exam_id, depart_id)`。
- **字段长度（全链路一致）**：`varchar(N)` 的 N=字符数（utf8mb4 下 N 个汉字），按业务最大汉字数定义、不按字节估算（杜绝「想存 10 字却定义 `varchar(30)`」）。**DB 字符数为唯一真值**：后端入参按字符数前置校验（不靠 DB 截断兜底）、前端输入框 `maxlength`、接口文档三处对齐。（本项目 MySQL；若迁异构/信创库的声明语义见 [`constitution.md`](constitution.md) #9。）
- 表、字段都要写 `COMMENT`；DDL 落到 `docs/ops/install/migration-YYYY-<特性名>.sql`，含**回滚/down 段**，并与 design 里的 ER 模型逐字段一致。

## 5. API 与响应

- **路由**：`/exam/api/<模块>/<资源>/<动作>`，全 `POST`，`@RequestBody` 接收。
- **统一响应**：`ApiRest<T>`（`code` 0 成功 / 非 0 失败，`msg`，`data`）。Controller 用 `BaseController` 的 `super.success()` / `super.success(data)` / `super.failure(msg)` 构造，**不直接 new**。
- **分页**：入参 `PagingReqDTO<T>`（`current` / `size` / `params` / `orderBy`），出参 `IPage<T>`；Mapper 自定义分页签名 `IPage<Dto> paging(Page page, @Param("query") Dto query)`。

## 6. 异常与错误码

- 业务校验失败 `throw new ServiceException(...)`；优先用错误码枚举：`throw new ServiceException(ApiError.ERROR_10010003)`。
- 全局由 `@RestControllerAdvice` 的 `ServiceExceptionHandler` 捕获并转 `ApiRest`，**不在 Controller 里 try-catch 包业务异常**。
- 错误码定义在 `ApiError` 枚举，命名 `ERROR_<8位>`，按域分段：`1001xxxx` 通用、`2001xxxx` 考试、`9001xxxx` 用户、`6000xxxx` 其它。新增错误码续段、写中文 msg。

## 7. 鉴权

- Shiro + JWT。权限**标在 Controller 方法**上，用 `@RequiresRoles`：
  - 单角色 `@RequiresRoles("sa")`；多角色 OR `@RequiresRoles(value = {"sa", "teacher"}, logical = Logical.OR)`。
- 角色码：`sa`（超管）/`teacher`（教师）/`assistant`（考试助理）/`student`（学员）。新增角色在 `sys_role` 加种子数据。

## 8. 其它

- **事务**：写操作的 Service 方法加 `@Transactional(rollbackFor = Exception.class)`。
- **日志**：类加 Lombok `@Slf4j`，用 `log.info/debug`。
- **工具类**：集中在 `core/utils/`（`BeanMapper`、`DateUtils`、`StringUtils`、`PassHandler`、excel 等），优先复用不重造。
- **常量/枚举**：枚举用**接口常量式**（`interface BookingStatus { Integer BOOKABLE = 1; ... }`），放模块 `enums/`；全局常量放 `ability/Constant.java`。
- **ID 生成**：统一 `IdWorker.getIdStr()`。

---

# 二、前端规范（exam-vue）

基于 vue-element-admin 风格。`src/` 下分层：`api`/`views`/`components`/`router`/`store`/`utils`/`directive`/`filters`/`styles`/`layout`。

## 0. UI 以原型为准（开发铁律）

涉及界面的开发，**必须对照 [`docs/engineering/prototype/`](prototype/) 中对应页面实现**，以原型为准：

- **必须与原型一致**：页面布局与信息结构、表单字段与控件类型、列表列、操作按钮、状态与空态、弹窗/多步流程、关键交互步骤、核心文案。
- **应当不同（不是偏离）**：用**真 Element-UI 组件**实现原型的等价视觉与交互（原型是仿 Element-UI 的纯静态 `app.css` 版，**不要照抄它的 HTML/CSS**）；真实数据替换占位；接后端联动与校验；按需做响应式。
- **绑定关系**：plan 里每个前端任务都标注其对应的原型页面路径，作为该任务的 UI 验收基线。
- **偏离处理**：开发中发现原型不合理或缺页，**先走 `/spec-change` 改原型（必要时连同 PRD），再改代码**——不得让实现私自偏离原型、也不得只改代码不回流原型。
- **验收**：测试阶段对照原型逐项核对（布局/字段/状态/交互/文案），可用 `/ce-test-browser` 或截图比对。

## 1. 目录与分层

| 目录 | 职责 |
| --- | --- |
| `api/<模块>/<资源>.js` | 请求函数，按模块分文件 |
| `views/<模块>/<资源>/` | 页面组件：`index.vue`（列表）/`form.vue`（增改）/`view.vue`（详情） |
| `components/<PascalCase>/index.vue` | 通用复用组件 |
| `router/index.js` | `constantRoutes` + `asyncRoutes`（按角色动态） |
| `store/modules/<name>.js` | Vuex 模块（命名空间，自动加载） |
| `utils/` | `request.js`（axios 封装）、`auth.js`、`validate.js` 等 |
| `directive/` | 自定义指令（`v-permission`、`waves`…） |
| `filters/index.js` | 全局过滤器（字典/枚举显示） |
| `styles/` | 全局 SCSS（`index.scss` 入口、`variables.scss` 变量） |

## 2. API 层

- 按模块建文件：`src/api/<模块>/<资源>.js`，从 `@/utils/request` 引入 `post`/`upload`/`download`。
- 函数命名：`fetchDetail` / `fetchList` / `saveData` / `updateData` / `deleteData`（`fetch*/save*/update*/delete*/list*` 前缀，camelCase）。
- 后端地址写全路径 `/exam/api/...`，由 `vue.config.js` 的 devServer 代理转发；**不在代码里写 host**。

```javascript
import { post } from '@/utils/request'
export function saveData(data) { return post('/exam/api/exam/exam/save', data) }
export function fetchDetail(id) { return post('/exam/api/exam/exam/detail', { id }) }
```

- 响应约定：`code === 0` 成功；`code === 10010002` 触发重新登录；其它 code 由 `request.js` 拦截器统一弹错。**业务代码默认拿到的就是成功数据**。
- **防重复提交 / 防抖节流**：保存/提交按钮在请求期间禁用或加 `loading`，防连点重复提交；搜索框、滚动等高频触发用防抖 / 节流。

## 3. 路由与页面

- `views/` 按业务模块建文件夹，页面文件用 `index.vue`/`form.vue`/`view.vue`。
- 路由 `name` 用 **PascalCase**，`meta` 带 `title`（中文）、`icon`、`roles`（角色数组，做动态路由权限）。组件用懒加载 `() => import('@/views/...')`。
- 权限：路由级靠 `meta.roles` + `permission/generateRoutes` 过滤 `asyncRoutes`；登录守卫在 `src/permission.js`。

## 4. 组件

- 通用组件放 `src/components/<PascalCase>/index.vue`，组件 `name` 用 **PascalCase**（ESLint 强制）。
- 优先复用既有二次封装组件，不直接重写：
  - `DataTable`（列表 = el-table + 分页 + 多选 + 操作，传 `options`/`listQuery`，用 `#filter-content` / `#data-columns` 插槽）
  - `Pagination`（分页）、`DepartTreeSelect`（部门树）、`MeetRole`（角色）、`FileUpload`、`SvgIcon`、`ExamSelect`/`RepoSelect`。
- **组件 `props`** 明确定义 `type`/`required`/`default`/`validator`；`data` 必须是函数 `data() { return {...} }`。
- **`v-for` 必带稳定 `key`**（用业务 id，不用 index），且**不与 `v-if` 同元素**（先 `computed` 过滤再渲染）。
- 组件选项顺序固定：`name → components → props → data → computed → watch → 生命周期 → methods`。
- 模板表达式保持简单，复杂逻辑提到 `computed`；自定义事件名用 `kebab-case`（`@row-click`）。公共逻辑抽 `mixin` / 工具方法复用，不在多组件复制粘贴。

## 5. Element-UI 用法

- 列表页用 `DataTable` + `Pagination` 组合，不裸写 el-table 分页逻辑。
- 表单用 `el-form` + `:model` + `:rules` + `prop`，`ref="postForm"` 提交前 `validate`。
- 弹窗用 `el-dialog` + `:visible.sync` + footer 插槽放取消/确定。
- 全局尺寸 `medium`（`main.js` 中 `Vue.use(Element, { size: 'medium' })`）。

## 6. 状态管理（Vuex）

- 模块放 `store/modules/<name>.js`，命名空间，`store/index.js` 自动加载。
- 常用模块：`user`（token/roles/用户信息）、`app`、`permission`、`tagsView`、`settings`。
- 组件里用 `mapGetters(['token','roles'])`、`this.$store.dispatch('user/login', ...)`。
- token 读写走 `utils/auth.js`，不直接操作 cookie/localStorage。

## 7. 代码风格（ESLint 强制）

- **无分号、单引号、2 空格缩进、函数名后无空格**（`.eslintrc.js`）。
- 变量/方法 `camelCase`；组件/类 `PascalCase`；常量大写。
- 组件样式用 `<style scoped>`；全局样式进 `src/styles/`，类名 `kebab-case`（`.app-container`、`.filter-container` 等）。
- 字典/枚举的展示用 `src/filters/index.js` 的过滤器（如 `stateFilter`、`quTypeFilter`），模板里 `{{ row.state | stateFilter }}`；**不在多处硬编码映射**。
- 按钮级权限用指令：`v-permission="['sa','teacher']"`。
- 提交前 `npm run lint` 过 ESLint。

## 8. 环境与构建

- 环境变量 `.env.development` / `.env.production`；`VUE_APP_BASE_API` 留空，靠 `vue.config.js` 代理（`/exam`、`/common`、`/upload` → `http://localhost:8101`）。
- 脚本：`npm run dev`（开发，端口 9527）、`npm run build:prod`（生产）、`npm run lint`。

## 9. JavaScript / ES 基础

- `const` 优先、`let` 次之，**禁 `var`**；用 `===` 不用 `==`。
- 善用解构、模板字符串、箭头函数、可选链 `?.` 与空值合并 `??`。
- **魔法值抽常量**（同后端「零魔法值」），状态/类型映射走 `filters` 或常量，不硬编码。
- 异步用 `async/await` + `try-catch`，不裸吞错误；`import` 分组排序（第三方 → `@/` 别名 → 相对路径）。

## 10. 性能

- 路由与重组件**懒加载**（`() => import(...)`，见 §3）；长列表分页（`DataTable`），超长数据考虑虚拟滚动。
- 避免无效监听、重复请求、不必要的重渲染；UI 库与工具**按需引入**。

## 11. 安全（XSS）

- **慎用 `v-html`**，渲染不可信内容必须转义；不拼接不可信 URL。
- token、个人隐私等敏感信息不打印到控制台、不长存 `localStorage`（token 走 `utils/auth.js`）。

## 12. 注释与字段长度

- 组件标注用途、`props`/`emit` 说明；复杂逻辑注释「为什么」；`TODO`/`FIXME` 带责任人与时间。
- **输入框 `maxlength` 严格等于字段的业务字符数**（见 [`constitution.md`](constitution.md) #9）；表单校验文案标准化（如「最多 N 个字符」）。

---

# 三、提交规范

见 [`workflow.md`](workflow.md) 阶段 8：`<type>: <简述>`，正文列要点，结尾带 `Co-Authored-By`。常用 type：`feat / fix / docs / refactor / chore`。文档与代码改动分开提交（如 `docs(prd):` 与 `feat:`）。
