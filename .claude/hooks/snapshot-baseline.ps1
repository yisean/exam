# SessionStart hook: record the set of already-dirty files at session start,
# so the Stop hook can diff against it to report only THIS session's changes.
# ASCII-only: PowerShell 5.1 on zh-CN reads non-BOM .ps1 as GBK.
$ErrorActionPreference = 'SilentlyContinue'

$raw = [Console]::In.ReadToEnd()
try { $j = $raw | ConvertFrom-Json } catch { $j = $null }
$sid = if ($j -and $j.session_id) { "$($j.session_id)" } else { 'default' }
$sid = $sid -replace '[^a-zA-Z0-9_-]', '_'

$root = & git rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0 -or -not $root) { exit 0 }
$root = ($root | Select-Object -First 1)

$snap = & git -C $root status --porcelain 2>$null
$baseFile = Join-Path $env:TEMP ("claude-baseline-$sid.txt")
Set-Content -LiteralPath $baseFile -Value $snap -Encoding UTF8
exit 0
