# CLAUDE.md · exam-api（后端）

SpringBoot + MyBatis-Plus + Shiro，根包 `com.yf.exam`。完整规范见 [`../docs/engineering/conventions.md`](../docs/engineering/conventions.md#一后端规范exam-api)，本文是高频速记。

## 分层与命名

- 业务代码放 `modules/<模块>/`，固定子包：`controller` · `service` + `service/impl` · `mapper` · `entity` · `dto`(`request`/`response`/`ext`) · `enums`。
- `XxxController`（`@RestController` + `@RequestMapping("/exam/api/<模块>/<资源>")`，继承 `BaseController`）· `XxxService extends IService` · `XxxServiceImpl extends ServiceImpl implements` · `XxxMapper extends BaseMapper`（无 `@Mapper`）· `Xxx extends Model`（`@Data` + `@TableName("el_xxx")`）。

## 必守约定

- **端点**：全 `POST`，标准动作 `/save` `/delete` `/detail` `/paging` `/state`，`@RequestBody` 接收。
- **响应**：统一 `ApiRest<T>`，用 `super.success()/success(data)/failure(msg)` 构造，**不直接 new**。分页入参 `PagingReqDTO<T>`、出参 `IPage<T>`。
- **DTO**：入参 `XxxReqDTO`、出参 `XxxRespDTO`、JOIN 结果 `XxxExtDTO`；Entity↔DTO 用 `BeanMapper.copy()`，不手搬字段。
- **异常**：`throw new ServiceException(ApiError.ERROR_xxx)`，全局 `ServiceExceptionHandler` 处理，不在 Controller try-catch 业务异常。错误码加在 `ApiError` 枚举，按域分段。
- **鉴权**：方法上 `@RequiresRoles("sa")` / 多角色 `logical = Logical.OR`。角色码 `sa/teacher/assistant/student`。
- **事务**：写方法加 `@Transactional(rollbackFor = Exception.class)`。
- **ID**：`IdWorker.getIdStr()` 雪花 ID，主键 `String` / `varchar(64)`。
- **枚举**：接口常量式放 `enums/`（`interface Xxx { Integer A = 1; }`）。
- **DB**：表 `el_`/`sys_` 前缀、下划线小写、写 COMMENT；DDL 进 `docs/ops/install/migration-*.sql`。
