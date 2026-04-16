# CompileTime - One-Time Installer
# Run this once to:
#   1. Patch your PowerShell $PROFILE to load hooks on every terminal
#   2. Register the companion app to auto-start on Windows boot
#
# Usage (from any PowerShell terminal):
#   E:\Compiletime\scripts\Install-CompileTime.ps1

$ErrorActionPreference = "Stop"
$CompileTimeRoot = "E:\Compiletime"
$HooksLine       = ". ""$CompileTimeRoot\scripts\hooks.ps1"""
$StartupFolder   = [System.Environment]::GetFolderPath("Startup")
$VbsSource       = "$CompileTimeRoot\scripts\start-compiletime.vbs"
$VbsDest         = "$StartupFolder\start-compiletime.vbs"

Write-Host ""
Write-Host "  CompileTime Installer" -ForegroundColor Cyan
Write-Host "  =====================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Patch $PROFILE
Write-Host "  [1/2] Patching PowerShell profile..." -ForegroundColor Yellow

if (!(Test-Path $PROFILE)) {
    New-Item -ItemType File -Path $PROFILE -Force | Out-Null
    Write-Host "        Created new profile at: $PROFILE"
}

$profileContent = Get-Content $PROFILE -Raw -ErrorAction SilentlyContinue

if ($profileContent -and $profileContent.Contains("hooks.ps1")) {
    Write-Host "        Already patched - skipping." -ForegroundColor Gray
} else {
    Add-Content -Path $PROFILE -Value "`n# CompileTime hooks`n$HooksLine"
    Write-Host "        Added hooks to: $PROFILE" -ForegroundColor Green
}

# Step 2: Register auto-start
Write-Host "  [2/2] Registering auto-start on Windows boot..." -ForegroundColor Yellow

Copy-Item -Path $VbsSource -Destination $VbsDest -Force
Write-Host "        Copied launcher to: $VbsDest" -ForegroundColor Green

# Done
Write-Host ""
Write-Host "  Installation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "  Next steps:" -ForegroundColor Cyan
Write-Host "   - Restart any open terminals (hooks load on new sessions)"
Write-Host "   - Companion will auto-start on next Windows login"
Write-Host "   - To start it now: cd E:\Compiletime\companion && mvn spring-boot:run"
Write-Host ""
