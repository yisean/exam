# CLAUDE.md · exam-vue（前端）

Vue2 + Element-UI（vue-element-admin 风格）。完整规范见 [`../docs/engineering/conventions.md`](../docs/engineering/conventions.md#二前端规范exam-vue)，本文是高频速记。

## 目录分层

`api/`（请求，按模块分文件）· `views/<模块>/<资源>/`（`index.vue`列表/`form.vue`增改/`view.vue`详情）· `components/<PascalCase>/index.vue`（复用组件）· `router/` · `store/modules/` · `utils/` · `directive/` · `filters/` · `styles/`。

## 必守约定

- **UI 以原型为准（铁律）**：涉及界面的开发必须对照 `docs/engineering/prototype/` 对应页面实现——布局/字段/控件/列表列/操作/状态/弹窗流程/文案与原型一致；用**真 Element-UI 组件**实现其等价视觉与交互（原型是仿 Element-UI 的静态 `app.css` 版，**不要照抄 HTML/CSS**）。发现原型不合理就先走 `/spec-change` 改原型再改代码，**不得私自偏离**。对应的原型页面在 plan 的前端任务里有标注。
- **API**：`src/api/<模块>/<资源>.js`，从 `@/utils/request` 引 `post`/`upload`/`download`；函数名 `fetchList/fetchDetail/saveData/updateData/deleteData`。写全路径 `/exam/api/...`，host 靠 `vue.config.js` 代理，不写死。响应 `code===0` 即成功（拦截器已统一处理报错与 `10010002` 重登）。
- **路由**：`name` 用 PascalCase，`meta` 带 `title`(中文)/`icon`/`roles`；组件懒加载 `() => import(...)`。
- **组件**：通用组件 `name` 用 PascalCase；**优先复用**既有封装：`DataTable`（列表，传 `options`/`listQuery` + `#filter-content`/`#data-columns` 插槽）、`Pagination`、`DepartTreeSelect`、`MeetRole`、`FileUpload`、`SvgIcon`，不裸写 el-table 分页。
- **表单/弹窗**：`el-form` + `:model`/`:rules`/`prop` + `ref` 提交前 `validate`；`el-dialog` + `:visible.sync` + footer 插槽。
- **状态**：Vuex 模块放 `store/modules/`；token 走 `utils/auth.js`。
- **字典/枚举显示**：用 `src/filters/index.js` 过滤器（`{{ row.state | stateFilter }}`），不硬编码映射。
- **按钮权限**：`v-permission="['sa','teacher']"`。
- **风格（ESLint 强制）**：无分号、单引号、2 空格缩进、函数名后无空格；样式 `<style scoped>`，全局类名 kebab-case。提交前 `npm run lint`。
