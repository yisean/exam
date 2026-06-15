# Stop hook: summarize files changed during THIS session.
# Diffs the current `git status` against the baseline recorded by
# snapshot-baseline.ps1 (SessionStart). If no baseline exists (session predates
# the hook), falls back to tracked changes only, to avoid listing pre-existing
# untracked files. Emits a systemMessage; never blocks stopping.
# ASCII-only: PowerShell 5.1 on zh-CN reads non-BOM .ps1 as GBK.
$ErrorActionPreference = 'SilentlyContinue'

$raw = [Console]::In.ReadToEnd()
try { $j = $raw | ConvertFrom-Json } catch { $j = $null }
$sid = if ($j -and $j.session_id) { "$($j.session_id)" } else { 'default' }
$sid = $sid -replace '[^a-zA-Z0-9_-]', '_'

$root = & git rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0 -or -not $root) { exit 0 }
$root = ($root | Select-Object -First 1)

function Get-PathFromLine($line) {
  if ($line.Length -lt 4) { return $null }
  $p = $line.Substring(3)
  if ($p -match '->') { $p = ($p -split '->')[-1] }
  return $p.Trim().Trim('"').Trim()
}

$nowRaw = & git -C $root status --porcelain 2>$null
$nowLines = @($nowRaw -split "`n" | Where-Object { $_ -ne '' })
if ($nowLines.Count -eq 0) { exit 0 }

$baseFile = Join-Path $env:TEMP ("claude-baseline-$sid.txt")
if (Test-Path -LiteralPath $baseFile) {
  $baseSet = @{}
  foreach ($l in (Get-Content -LiteralPath $baseFile)) {
    $bp = Get-PathFromLine $l
    if ($bp) { $baseSet[$bp] = $true }
  }
  $changed = $nowLines | Where-Object {
    $p = Get-PathFromLine $_
    $p -and (-not $baseSet.ContainsKey($p))
  }
} else {
  # No baseline: show tracked changes only (hide pre-existing untracked noise).
  $changed = $nowLines | Where-Object { $_ -notmatch '^\?\?' }
}

$changed = @($changed)
if ($changed.Count -eq 0) { exit 0 }

$body = ($changed | ForEach-Object { '  ' + $_.Trim() }) -join "`n"
$msg = "Files changed this session ($($changed.Count)):`n$body"
$out = @{ systemMessage = $msg } | ConvertTo-Json -Compress
[Console]::Out.Write($out)
exit 0
