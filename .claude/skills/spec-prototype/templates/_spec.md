# 原型构建规范（prototype/_spec.md）

> 本文件是原型的**最高约束**。`/spec-prototype` 生成/更新页面时严格遵守这里。
> 放进某项目的 `docs/engineering/prototype/_spec.md` 后，即成为该项目的原型规范（覆盖 skill 内置默认）。

1. 每个页面是独立 HTML，`<head>` 引入 `<link rel="stylesheet" href="assets/app.css" />`。
2. **只用 `app.css` 里已有的 class**（btn / card / table / tag / form-item / input / select / steps / tabs / dialog / pagination / stat-card 等）；需要微调用内联 `style`，**不引入新 CSS 文件**。
3. **不写复杂 JS**。允许少量内联 `<script>` 做：标签页切换、打开/关闭 `.dialog-mask`、选项高亮。页面间跳转一律 `<a href="xxx.html">`。
4. 仿 **Element-UI** 风格；中文界面，措辞专业、贴合业务场景。
5. 新页面要挂进 `index.html` 导航。
6. 纯静态、零依赖、双击即在浏览器打开；可直接打包发客户。

新页面可参照同目录 `page.html` 骨架起步。
