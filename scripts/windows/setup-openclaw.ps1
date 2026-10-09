# Installs and configures the OpenClaw Gateway that MobileVault connects to.
# Run as your normal (NOT elevated) user, from the repo root, after install.ps1
# and after signing in to Tailscale:
#   powershell -ExecutionPolicy Bypass -File scripts\windows\setup-openclaw.ps1 [-HaToken <token>]
#
# -HaToken  A Home Assistant long-lived access token. When given, Home
#           Assistant's MCP server is added to OpenClaw so the assistant can
#           see and control your home. See docs/mobilevault.md.
param(
    [string]$HaToken
)
$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')

function Get-EnvValue([string]$name) {
    $envFile = Join-Path $root '.env'
    if (-not (Test-Path $envFile)) { return $null }
    $line = Get-Content $envFile | Where-Object { $_ -match "^$name=" } | Select-Object -First 1
    if ($line) { return ($line -split '=', 2)[1].Trim() }
}

# Windows PowerShell 5.1 strips embedded double quotes from native arguments.
function ConvertTo-NativeArg([string]$s) {
    if ($PSVersionTable.PSVersion.Major -lt 7) { return $s -replace '"', '\"' }
    return $s
}

# --- Tailscale: required, it provides the HTTPS address MobileVault trusts ---
if (-not (Get-Command tailscale -ErrorAction SilentlyContinue)) {
    throw 'Tailscale is not installed. Run scripts\windows\install.ps1 first.'
}
$ts = tailscale status --json | ConvertFrom-Json
if ($ts.BackendState -ne 'Running') {
    throw 'Tailscale is not signed in. Open Tailscale from the tray and log in, then re-run.'
}
$tsHost = $ts.Self.DNSName.TrimEnd('.')

# --- OpenClaw -------------------------------------------------------------------
if (-not (Get-Command openclaw -ErrorAction SilentlyContinue)) {
    Write-Host 'Installing OpenClaw (official installer)...'
    Invoke-Expression (Invoke-WebRequest -UseBasicParsing 'https://openclaw.ai/install.ps1').Content
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')
}
if (-not (Test-Path (Join-Path $HOME '.openclaw\openclaw.json'))) {
    Write-Host 'First-time OpenClaw onboarding (sign in to your model provider)...'
    openclaw onboard
}

# Keep the Gateway on loopback; Tailscale Serve publishes it to your tailnet
# only, with a real certificate (release MobileVault builds require HTTPS).
openclaw config set gateway.bind loopback
openclaw config set gateway.tailscale.mode serve

# --- Home Assistant as an MCP tool server ---------------------------------------
if ($HaToken) {
    $haIp = Get-EnvValue 'HA_LAN_IP'
    if (-not $haIp) { throw 'HA_LAN_IP is not set in .env.' }
    $server = @{
        url       = "http://${haIp}:8123/api/mcp"
        transport = 'streamable-http'
        headers   = @{ Authorization = "Bearer $HaToken" }
    } | ConvertTo-Json -Compress
    openclaw config set mcp.servers.homeassistant (ConvertTo-NativeArg $server) --strict-json
    Write-Host '[ok] Home Assistant MCP server added to OpenClaw.'
}

# --- Run as a background service that starts before login ----------------------
openclaw gateway install --force
openclaw gateway restart
openclaw gateway status

Write-Host ''
Write-Host "MobileVault address:  https://$tsHost"
Write-Host 'Gateway token:        openclaw config get gateway.auth.token'
Write-Host ''
Write-Host "Put OPENCLAW_URL=https://$tsHost in .env for the dashboard link."
Write-Host 'If Serve fails, enable HTTPS certificates in the Tailscale admin console (DNS page).'
Write-Host 'After the phone connects, approve it with: openclaw devices approve <id>'
