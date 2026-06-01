param()

# Resolve the repo root from this script's location (.claude/hooks/ → .claude/ → repo root)
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$logDir   = Join-Path $repoRoot '.claude' 'logs'

$data = $input | Out-String | ConvertFrom-Json

$promptText = if ($data.prompt) {
    ($data.prompt -split "`r?`n" | ForEach-Object { "> $_" }) -join "`n"
} else {
    '> (prompt not captured)'
}

# Run git commands from the repo root so they always resolve
$author = (git -C $repoRoot config user.name 2>$null)
if (-not $author) { $author = (git -C $repoRoot config user.email 2>$null) }
if (-not $author) { $author = 'N/A' }

$branch = (git -C $repoRoot branch --show-current 2>$null)
if (-not $branch) { $branch = 'N/A' }

$now     = Get-Date
$date    = $now.ToString('yyyy-MM-dd')
$time    = $now.ToString('HH:mm')
$logFile = Join-Path $logDir "$date.md"

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

if (-not (Test-Path $logFile)) {
    Set-Content -Path $logFile -Encoding utf8 -Value @"
# Prompt Log - $date

---
"@
}

$entry = @"

**Prompt:**
$promptText

| Field  | Value   |
|--------|---------|
| Time   | $time   |
| Author | $author |
| Branch | $branch |

---
"@

Add-Content -Path $logFile -Encoding utf8 -Value $entry
