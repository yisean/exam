# 把 spec-* 的工程骨架文件初始化到当前项目
#   docs/engineering/constitution.md（工程宪法）
#   docs/engineering/workflow.md    （流程总纲）
#   CLAUDE.md                        （项目根：编码规约，AI 自动加载）
# 用法（在目标项目根目录运行）：
# 注：-ExecutionPolicy Bypass 为临时授权，仅对本次调用生效、不改系统设置（规避 Restricted 策略禁止跑脚本）。
#   powershell -ExecutionPolicy Bypass -File "$HOME\.claude\skills\spec-init\init-project.ps1"            # 缺失才建，不覆盖
#   powershell -ExecutionPolicy Bypass -File "$HOME\.claude\skills\spec-init\init-project.ps1" -Force     # 覆盖已存在的
#   powershell -ExecutionPolicy Bypass -File "$HOME\.claude\skills\spec-init\init-project.ps1" -ProjectRoot D:\path\to\proj
param(
    [string]$ProjectRoot = (Get-Location).Path,
    [switch]$Force
)
$ErrorActionPreference = 'Stop'

$src = Join-Path $PSScriptRoot 'templates'
$eng = Join-Path $ProjectRoot 'docs\engineering'
New-Item -ItemType Directory -Force -Path $eng | Out-Null

function Copy-One($name, $to, $label) {
    $from = Join-Path $src $name
    if (-not (Test-Path $from)) { Write-Warning "模板缺失：$from"; return }
    if ((Test-Path $to) -and -not $Force) {
        Write-Host "  skip    $label（已存在，加 -Force 覆盖）"
        return
    }
    Copy-Item -Force $from $to
    Write-Host "  created $label"
}

Copy-One 'constitution.md' (Join-Path $eng 'constitution.md') 'docs/engineering/constitution.md'
Copy-One 'workflow.md'     (Join-Path $eng 'workflow.md')     'docs/engineering/workflow.md'
Copy-One 'CLAUDE.md'       (Join-Path $ProjectRoot 'CLAUDE.md') 'CLAUDE.md（项目根）'

Write-Host ""
Write-Host "完成。已初始化到 $ProjectRoot" -ForegroundColor Green
Write-Host "下一步：编辑 constitution.md 的 version/date；按项目技术栈补全 workflow.md 与 CLAUDE.md 的占位符（<后端栈>/<前端栈>、目录结构），再开始跑 /spec-prd。"
