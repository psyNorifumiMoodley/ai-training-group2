param()
$data = $input | Out-String | ConvertFrom-Json

$promptText = if ($data.prompt) {
    ($data.prompt -split "`r?`n" | ForEach-Object { "> $_" }) -join "`n"
} else {
    '> (prompt not captured)'
}

$author = (git config user.name 2>$null)
if (-not $author) { $author = (git config user.email 2>$null) }
if (-not $author) { $author = 'N/A' }

$branch = (git branch --show-current 2>$null)
if (-not $branch) { $branch = 'N/A' }

$now    = Get-Date
$date   = $now.ToString('yyyy-MM-dd')
$time   = $now.ToString('HH:mm')
$logFile = ".claude/logs/$date.md"

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
