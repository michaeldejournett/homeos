# Installs the HomeOS TV app on a Fire TV / Android TV and configures it over
# the network with adb, so nothing has to be typed with the remote.
#
# Prerequisites on the TV: Developer options > ADB debugging ON (same setting
# Home Assistant's Fire TV integration uses), and a reserved IP on your router.
#
# Usage (from the repo root):
#   powershell -ExecutionPolicy Bypass -File scripts\windows\setup-tv.ps1 `
#       -TvIp 192.168.1.50 -Apk .\homeos-tv.apk -HaToken <token> -JellyfinUser <name>
#
# -Apk is the APK from the "Android TV app" GitHub Actions run (or your own
# build at clients\androidtv\app\build\outputs\apk\debug\app-debug.apk).
# Omit -Apk to only update settings on a TV that already has the app.
param(
    [Parameter(Mandatory = $true)][string]$TvIp,
    [string]$Apk,
    [string]$HaToken,
    [string]$HaLabel = 'tv',
    [string]$JellyfinUser,
    [SecureString]$JellyfinPassword
)
$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')

function Get-EnvValue([string]$name) {
    $envFile = Join-Path $root '.env'
    if (-not (Test-Path $envFile)) { return $null }
    $line = Get-Content $envFile | Where-Object { $_ -match "^$name=" } | Select-Object -First 1
    if ($line) { return ($line -split '=', 2)[1].Trim() }
}

# Single-quote a value for the TV's shell (adb shell runs one sh command line).
function Quote-Remote([string]$s) { "'" + ($s -replace "'", "'\''") + "'" }

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host 'Installing Android platform tools (adb)...'
    winget install --id Google.PlatformTools --exact --silent --accept-package-agreements --accept-source-agreements
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')
}

$device = "${TvIp}:5555"
Write-Host "Connecting to $device (accept the prompt on the TV the first time)..."
adb connect $device | Out-Host
adb -s $device wait-for-device

if ($Apk) {
    Write-Host 'Installing HomeOS TV app...'
    adb -s $device install -r $Apk | Out-Host
}

$domain = Get-EnvValue 'HOMEOS_DOMAIN'
if (-not $domain) { throw 'HOMEOS_DOMAIN is not set in .env.' }

$extras = @(
    '--es', 'ha_url', (Quote-Remote "https://ha.$domain"),
    '--es', 'ha_label', (Quote-Remote $HaLabel),
    '--es', 'jellyfin_url', (Quote-Remote "https://media.$domain")
)
if ($HaToken) { $extras += @('--es', 'ha_token', (Quote-Remote $HaToken)) }
if ($JellyfinUser) {
    if (-not $JellyfinPassword) { $JellyfinPassword = Read-Host "Jellyfin password for $JellyfinUser" -AsSecureString }
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($JellyfinPassword))
    $extras += @('--es', 'jellyfin_user', (Quote-Remote $JellyfinUser), '--es', 'jellyfin_password', (Quote-Remote $plain))
}

$cmd = 'am broadcast -a com.homeos.tv.PROVISION -n com.homeos.tv/.ProvisionReceiver ' + ($extras -join ' ')
$out = adb -s $device shell $cmd
$out | Out-Host
if (-not ($out -match 'data="ok')) {
    Write-Warning 'The TV did not confirm the settings. Check the message above (wrong token, password or URL?).'
}

Write-Host ''
Write-Host 'Done. In Home Assistant, add the label "' -NoNewline
Write-Host $HaLabel -NoNewline
Write-Host '" to any light, switch, scene or script you want on the TV home screen.'
adb -s $device shell monkey -p com.homeos.tv -c android.intent.category.LEANBACK_LAUNCHER 1 | Out-Null
