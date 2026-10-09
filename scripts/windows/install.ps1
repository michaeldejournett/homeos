# One-time host setup for HomeOS on Windows 10/11.
# Run from an elevated PowerShell in the repo root:
#   powershell -ExecutionPolicy Bypass -File scripts\windows\install.ps1
# Reboot when it asks, then run it again until it reports everything is done.
$ErrorActionPreference = 'Stop'

$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Run this script from an elevated (Administrator) PowerShell.'
}

# --- Windows features --------------------------------------------------------
$edition = (Get-CimInstance Win32_OperatingSystem).Caption
$features = @('Microsoft-Windows-Subsystem-Linux', 'VirtualMachinePlatform')
if ($edition -match 'Home') {
    Write-Warning "$edition has no Hyper-V. Run Home Assistant OS in VirtualBox instead (see docs/setup.md)."
} else {
    $features += 'Microsoft-Hyper-V-All'
}

$needReboot = $false
foreach ($f in $features) {
    $state = (Get-WindowsOptionalFeature -Online -FeatureName $f).State
    if ($state -ne 'Enabled') {
        Write-Host "Enabling $f..."
        $r = Enable-WindowsOptionalFeature -Online -FeatureName $f -All -NoRestart
        if ($r.RestartNeeded) { $needReboot = $true }
    }
}
if ($needReboot) {
    Write-Warning 'Reboot now, then re-run this script.'
    exit 0
}

# --- Apps --------------------------------------------------------------------
$apps = [ordered]@{
    'Docker.DockerDesktop' = 'Docker Desktop (runs the service stack)'
    'Valve.Steam'          = 'Steam'
    'LizardByte.Sunshine'  = 'Sunshine (streams games to Fire TV / phone via Moonlight)'
    'Tailscale.Tailscale'  = 'Tailscale (secure remote access, no port forwarding)'
    'Git.Git'              = 'Git'
}
foreach ($id in $apps.Keys) {
    $installed = winget list --id $id --exact --accept-source-agreements 2>$null | Select-String $id
    if ($installed) {
        Write-Host "[ok] $($apps[$id])"
    } else {
        Write-Host "Installing $($apps[$id])..."
        winget install --id $id --exact --silent --accept-package-agreements --accept-source-agreements
    }
}

# --- Always-on server behaviour ----------------------------------------------
Write-Host 'Disabling sleep/hibernate on AC power...'
powercfg /change standby-timeout-ac 0
powercfg /change hibernate-timeout-ac 0
powercfg /change monitor-timeout-ac 10

# --- Firewall: let the HA VM and LAN devices reach host services ---------------
$rules = @(
    @{ Name = 'HomeOS HTTP/HTTPS'; Protocol = 'TCP'; Port = '80,443' },
    @{ Name = 'HomeOS HTTP/3';     Protocol = 'UDP'; Port = '443' },
    @{ Name = 'HomeOS Jellyfin';   Protocol = 'TCP'; Port = '8096' },
    @{ Name = 'HomeOS Jellyfin discovery'; Protocol = 'UDP'; Port = '7359' },
    @{ Name = 'HomeOS Wyoming voice'; Protocol = 'TCP'; Port = '10200,10300,10400' }
)
foreach ($r in $rules) {
    if (-not (Get-NetFirewallRule -DisplayName $r.Name -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $r.Name -Direction Inbound -Action Allow `
            -Protocol $r.Protocol -LocalPort ($r.Port -split ',') -Profile Private | Out-Null
        Write-Host "[firewall] $($r.Name)"
    }
}

$net = Get-NetConnectionProfile | Where-Object NetworkCategory -ne 'Private'
if ($net) {
    Write-Warning "Network '$($net.Name)' is not set to Private; the firewall rules above only apply to Private networks."
    Write-Warning "Fix with: Set-NetConnectionProfile -InterfaceAlias '$($net.InterfaceAlias)' -NetworkCategory Private"
}

Write-Host ''
Write-Host 'Host setup done. Next:'
Write-Host '  1. Start Docker Desktop once and enable "Start Docker Desktop when you sign in".'
Write-Host '  2. scripts\windows\init-env.ps1, then edit .env'
Write-Host '  3. scripts\windows\new-homeassistant-vm.ps1'
Write-Host '  4. docker compose up -d --build'
Write-Host '  5. (non-elevated) scripts\windows\setup-openclaw.ps1 for MobileVault'
