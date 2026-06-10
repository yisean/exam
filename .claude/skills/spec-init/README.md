# spec-init · 项目初始化脚手架

> **这不是一个斜杠命令 skill**（没有 `SKILL.md`、不出现在 `/spec-` 菜单），而是 spec-* 套件的**项目初始化资源包**：在新项目启动时，把工程宪法、流程总纲、编码规约一次装进项目。

## 内容

```
spec-init/
├── templates/
│   ├── constitution.md   工程宪法（原则 + 编码期基线）→ 装到 docs/engineering/
│   ├── workflow.md       研发流程总纲（8 阶段）        → 装到 docs/engineering/
│   └── CLAUDE.md         项目编码规约（AI 自动加载）   → 装到项目根
├── init-project.ps1      初始化脚本（Windows）
└── init-project.sh       初始化脚本（macOS / Linux）
```

> 为什么独立成目录（不放进 spec-prd）：这三份是**整套流程共用的项目脚手架**，不是某个 skill 的产出（spec-prd 的产出模板是它自己的 `templates/prd.md`）。独立出来语义清晰；它仍随 `install` 一起装到用户级 `~/.claude/skills/spec-init/`，供 `init-project` 调用。

## 用法

在**目标项目根目录**运行（skill 已装到用户级时）：

- **Windows**：
  ```powershell
  powershell -ExecutionPolicy Bypass -File "$HOME\.claude\skills\spec-init\init-project.ps1"
  ```
- **macOS / Linux**：
  ```bash
  "$HOME/.claude/skills/spec-init/init-project.sh"
  ```

它把 `templates/` 下的 `constitution.md`、`workflow.md` 复制到项目 `docs/engineering/`，`CLAUDE.md` 复制到项目根（**缺失才建**，加 `-Force` / `--force` 覆盖）。装好后编辑 `constitution.md` 的 `version/date`，按技术栈补全 `workflow.md` 与 `CLAUDE.md` 的 `<占位符>`。
