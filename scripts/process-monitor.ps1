# CompileTime - Process Monitor (simple version)
$COMPILETIME_URL = "http://localhost:9999"
$BUILD_TOOLS = @("npm","npx","yarn","pnpm","mvn","mvnw","gradle","gradlew","docker","git","pip","cargo","make","ping","timeout")
$activePids = @{}

function Send-Ping($path) {
    try {
        Invoke-WebRequest "$COMPILETIME_URL$path" -Method Post -UseBasicParsing -TimeoutSec 1 | Out-Null
    } catch {}
}

Write-Host "[Monitor] Started" -ForegroundColor Cyan

while ($true) {
    # Find all running build tool processes
    $found = Get-Process -ErrorAction SilentlyContinue | Where-Object {
        $BUILD_TOOLS -contains $_.Name.ToLower()
    }

    $foundIds = @($found | ForEach-Object { $_.Id })

    # New processes
    foreach ($p in $found) {
        if (-not $activePids.ContainsKey($p.Id)) {
            $activePids[$p.Id] = $p.Name
            Write-Host "[Monitor] START: $($p.Name) (PID $($p.Id))" -ForegroundColor Yellow
            Send-Ping "/api/command/start?cmd=$([uri]::EscapeDataString($p.Name))"
        }
    }

    # Ended processes
    foreach ($id in @($activePids.Keys | Where-Object { $foundIds -notcontains $_ })) {
        Write-Host "[Monitor] DONE: $($activePids[$id]) (PID $id)" -ForegroundColor Gray
        $activePids.Remove($id)
        Send-Ping "/api/command/done?exit=0"
    }

    Start-Sleep -Milliseconds 500
}
