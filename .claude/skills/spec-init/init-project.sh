#!/usr/bin/env bash
# 把 spec-* 的工程骨架文件初始化到当前项目
#   docs/engineering/constitution.md（工程宪法）
#   docs/engineering/workflow.md    （流程总纲）
#   CLAUDE.md                        （项目根：编码规约，AI 自动加载）
# 用法（在目标项目根目录运行）：
#   "$HOME/.claude/skills/spec-init/init-project.sh"            # 缺失才建，不覆盖
#   "$HOME/.claude/skills/spec-init/init-project.sh" --force    # 覆盖已存在的
#   "$HOME/.claude/skills/spec-init/init-project.sh" --project /path/to/proj
set -euo pipefail

project_root="$(pwd)"
force=""
while [ $# -gt 0 ]; do
  case "$1" in
    --force) force="1"; shift ;;
    --project) project_root="$2"; shift 2 ;;
    *) echo "未知参数：$1" >&2; exit 1 ;;
  esac
done

src="$(cd "$(dirname "$0")" && pwd)/templates"
eng="$project_root/docs/engineering"
mkdir -p "$eng"

copy_one() {  # $1=模板名  $2=目标绝对路径  $3=显示名
  local from="$src/$1" to="$2" label="$3"
  if [ ! -f "$from" ]; then echo "模板缺失：$from" >&2; return; fi
  if [ -f "$to" ] && [ -z "$force" ]; then
    echo "  skip    $label（已存在，加 --force 覆盖）"
    return
  fi
  cp -f "$from" "$to"
  echo "  created $label"
}

copy_one constitution.md "$eng/constitution.md"        "docs/engineering/constitution.md"
copy_one workflow.md     "$eng/workflow.md"            "docs/engineering/workflow.md"
copy_one CLAUDE.md       "$project_root/CLAUDE.md"     "CLAUDE.md（项目根）"

echo ""
echo "完成。已初始化到 $project_root"
echo "下一步：编辑 constitution.md 的 version/date；按项目技术栈补全 workflow.md 与 CLAUDE.md 的占位符（<后端栈>/<前端栈>、目录结构），再开始跑 /spec-prd。"
