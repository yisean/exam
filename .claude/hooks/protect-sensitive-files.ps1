# PreToolUse hook: block Write/Edit to sensitive files (secrets, keys, SSH).
# Reads hook JSON on stdin; emits permissionDecision=deny when the target path
# looks like a secret. Template files (.env.example etc.) are allowed.
# ASCII-only: PowerShell 5.1 on zh-CN reads non-BOM .ps1 as GBK.
$ErrorActionPreference = 'Stop'

$raw = [Console]::In.ReadToEnd()
try { $j = $raw | ConvertFrom-Json } catch { exit 0 }

$f = $j.tool_input.file_path
if ([string]::IsNullOrWhiteSpace($f)) { exit 0 }

$path = $f -replace '\\', '/'
$base = ($path -split '/')[-1]

function Deny($reason) {
  $out = @{
    hookSpecificOutput = @{
      hookEventName            = 'PreToolUse'
      permissionDecision       = 'deny'
      permissionDecisionReason = $reason
    }
  } | ConvertTo-Json -Compress -Depth 5
  [Console]::Out.Write($out)
  exit 0
}

# Allow committed templates regardless of the .env prefix.
$isTemplate = $base -match '\.(example|sample|template|dist|md)$'

if (-not $isTemplate) {
  if ($base -match '^\.env')                           { Deny "protected secret file: $base (.env)" }
  if ($base -match '\.(pem|key|p12|pfx|keystore)$')    { Deny "protected key/cert file: $base" }
  if ($base -match '^(id_rsa|id_ed25519|id_dsa|id_ecdsa)(\.pub)?$') { Deny "protected SSH key: $base" }
  if ($path -match '(^|/)\.ssh/')                      { Deny "protected SSH directory: $path" }
  if ($base -match '(^|[._-])secrets?([._-]|$)')       { Deny "protected secrets file: $base" }
}

exit 0
