# PreToolUse hook: scan Bash/PowerShell commands for dangerous patterns.
# Reads hook JSON on stdin; emits a permissionDecision (deny/ask) when matched.
# Matches anywhere in the command (path-first ordering, abbreviations, compound
# commands), which static prefix permission rules cannot do.
# ASCII-only on purpose: PowerShell 5.1 on zh-CN reads non-BOM .ps1 as GBK.
$ErrorActionPreference = 'Stop'

$raw = [Console]::In.ReadToEnd()
try { $j = $raw | ConvertFrom-Json } catch { exit 0 }

$cmd = $j.tool_input.command
if ([string]::IsNullOrWhiteSpace($cmd)) { exit 0 }

# Normalize whitespace so multi-space / newline forms still match. -match is
# case-insensitive by default, so no lowercasing needed.
$n = ($cmd -replace '\s+', ' ').Trim()

function Emit($decision, $reason) {
  $out = @{
    hookSpecificOutput = @{
      hookEventName            = 'PreToolUse'
      permissionDecision       = $decision
      permissionDecisionReason = $reason
    }
  } | ConvertTo-Json -Compress -Depth 5
  [Console]::Out.Write($out)
  exit 0
}

# --- HARD DENY: irreversible / destructive (checked first) ---
$deny = @(
  @{ rx = '\brm\b\s+-[a-z]*r[a-z]*f';                         why = 'rm -rf recursive force delete' },
  @{ rx = '\brm\b\s+-[a-z]*f[a-z]*r';                         why = 'rm -fr recursive force delete' },
  @{ rx = '\brm\b\s+-r[a-z]*\s+.*-f';                         why = 'rm -r ... -f recursive force delete' },
  @{ rx = '\brm\b\s+-f[a-z]*\s+.*-r';                         why = 'rm -f ... -r recursive force delete' },
  @{ rx = '\bdd\b\s+.*\bof=';                                 why = 'dd writing to a device/disk' },
  @{ rx = '\bmkfs(\.\w+)?\b';                                 why = 'mkfs formats a partition' },
  @{ rx = ':\s*\(\s*\)\s*\{';                                 why = 'fork bomb' },
  @{ rx = '\bgit\s+push\b.*(--force|\s-f\b)';                 why = 'git push --force rewrites remote history' },
  @{ rx = '\bgit\s+clean\b.*-[a-z]*f[a-z]*d';                 why = 'git clean -fd deletes untracked files' },
  @{ rx = '\bgit\s+clean\b.*-[a-z]*d[a-z]*f';                 why = 'git clean -df deletes untracked files' },
  @{ rx = '\bgit\s+reset\b.*--hard';                          why = 'git reset --hard discards changes' },
  @{ rx = '\bchmod\b.*\b777\b';                               why = 'chmod 777 opens full permissions' },
  @{ rx = '\b(remove-item|ri)\b.*\s-(recurse|rec|r)\b';       why = 'Remove-Item -Recurse recursive delete' },
  @{ rx = '\b(remove-item|ri)\b.*\s-(force|forc|for|fo|f)\b'; why = 'Remove-Item -Force forced delete' },
  @{ rx = '\b(rm|del|erase)\b.*\s-(recurse|rec)\b';           why = 'Remove-Item alias recursive delete' },
  @{ rx = '\brmdir\b';                                        why = 'rmdir removes a directory' },
  @{ rx = '\b(del|erase|rd)\b.*\s/s\b';                       why = 'del /s recursive force delete' },
  @{ rx = '\b(format-volume|format-disk|clear-disk|clear-content)\b'; why = 'format/clear disk or file content' },
  @{ rx = '\b(curl|wget|iwr|invoke-webrequest|irm|invoke-restmethod)\b.*\|\s*(sh|bash|zsh|pwsh|powershell|cmd|iex|invoke-expression)\b'; why = 'pipe-download to shell (supply-chain risk)' },
  @{ rx = '\b(iex|invoke-expression)\b.*\b(downloadstring|iwr|invoke-webrequest|irm|invoke-restmethod|net\.webclient)\b'; why = 'iex executes remotely downloaded content' }
)
foreach ($p in $deny) { if ($n -match $p.rx) { Emit 'deny' $p.why } }

# --- ASK: outward / not-easily-reversible (force a confirmation prompt) ---
$ask = @(
  @{ rx = '\bgit\s+push\b';                        why = 'git push to remote' },
  @{ rx = '\bnpm\s+publish\b';                     why = 'npm publish a package' },
  @{ rx = '\bdocker\s+push\b';                     why = 'docker push an image' },
  @{ rx = '\bkubectl\s+delete\b';                  why = 'kubectl delete resources' },
  @{ rx = '\bterraform\s+(apply|destroy)\b';       why = 'terraform changes/destroys infra' },
  @{ rx = '\b(shutdown|reboot)\b';                 why = 'shutdown/reboot' },
  @{ rx = '\b(stop-computer|restart-computer)\b';  why = 'shutdown/reboot' }
)
foreach ($p in $ask) { if ($n -match $p.rx) { Emit 'ask' $p.why } }

exit 0
