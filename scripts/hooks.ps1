# =============================================================
#  CompileTime — PowerShell Hooks
#  Fires HTTP pings to the companion app on every command so
#  the overlay knows when a build is starting / finishing.
#  Sourced by $PROFILE via Install-CompileTime.ps1
# =============================================================

$global:COMPILETIME_URL = "http://localhost:9999"

# ── Helper: synchronous HTTP POST with short timeout ─────────
function _ct_ping {
    param([string]$Path)
    try {
        Invoke-WebRequest -Uri "$global:COMPILETIME_URL$Path" `
            -Method Post -UseBasicParsing -TimeoutSec 1 `
            -ErrorAction Stop | Out-Null
    } catch {
        # Companion not running — silently skip
    }
}

# ── Pre-command hook (fires when user presses Enter) ─────────
#    Intercepts the Enter key via PSReadLine before the command runs
Set-PSReadLineKeyHandler -Key Enter -ScriptBlock {
    $line   = $null
    $cursor = $null
    [Microsoft.PowerShell.PSConsoleReadLine]::GetBufferState([ref]$line, [ref]$cursor)

    if ($line.Trim() -ne '') {
        $encoded = [uri]::EscapeDataString($line.Trim())
        _ct_ping "/api/command/start?cmd=$encoded"
    }

    [Microsoft.PowerShell.PSConsoleReadLine]::AcceptLine()
}

# ── Post-command hook (fires when prompt redraws after command)
#    Wraps the existing prompt function so we don't break other tools
$global:_ct_prev_prompt = $function:prompt

function prompt {
    _ct_ping "/api/command/done?exit=$LASTEXITCODE"

    if ($global:_ct_prev_prompt) {
        & $global:_ct_prev_prompt
    } else {
        "PS $($executionContext.SessionState.Path.CurrentLocation)> "
    }
}
