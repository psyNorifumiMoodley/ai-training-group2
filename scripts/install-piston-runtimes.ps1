# Run once after 'docker-compose up' to install code execution runtimes into Piston.
# Packages persist in the piston_data Docker volume and only need to be installed once
# per environment (re-running this script is safe — it skips already-installed runtimes).

param(
    [string]$PistonUrl = "http://localhost:2000"
)

$runtimes = @(
    @{ language = "java";   version = "15.0.2" },
    @{ language = "python"; version = "3.10.0" },
    @{ language = "mono";   version = "6.12.0" }
)

Write-Host "Checking Piston at $PistonUrl..."
try {
    $installed = (Invoke-RestMethod -Uri "$PistonUrl/api/v2/runtimes" -Method GET).language
} catch {
    Write-Error "Could not reach Piston. Make sure 'docker-compose up' is running first."
    exit 1
}

foreach ($rt in $runtimes) {
    if ($installed -contains $rt.language) {
        Write-Host "  $($rt.language)-$($rt.version) already installed, skipping."
    } else {
        Write-Host "  Installing $($rt.language)-$($rt.version) (this may take a few minutes)..."
        Invoke-RestMethod -Uri "$PistonUrl/api/v2/packages" -Method POST `
            -ContentType "application/json" `
            -Body ($rt | ConvertTo-Json) | Out-Null
        Write-Host "  Done."
    }
}

Write-Host "All runtimes ready."
