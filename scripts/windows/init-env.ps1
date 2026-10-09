# Creates .env from .env.example and fills in random secrets.
# Usage (from the repo root):  powershell -ExecutionPolicy Bypass -File scripts\windows\init-env.ps1
$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$example = Join-Path $root '.env.example'
$envFile = Join-Path $root '.env'

if (Test-Path $envFile) {
    Write-Host ".env already exists; only empty secrets will be filled in."
} else {
    Copy-Item $example $envFile
}

function New-Secret([int]$bytes = 48) {
    $buf = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buf)
    # URL-safe so it never breaks YAML/env parsing
    return [Convert]::ToBase64String($buf).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$secrets = 'AUTHENTIK_SECRET_KEY', 'AUTHENTIK_PG_PASS', 'VAULTWARDEN_ADMIN_TOKEN'
$lines = Get-Content $envFile
$lines = $lines | ForEach-Object {
    $line = $_
    foreach ($name in $secrets) {
        if ($line -match "^$name=\s*$") { $line = "$name=$(New-Secret)" }
    }
    $line
}
Set-Content -Path $envFile -Value $lines -Encoding ascii

Write-Host "Secrets written to $envFile"
Write-Host "Now edit it and set HOMEOS_DOMAIN, HOST_LAN_IP, HA_LAN_IP, MEDIA_DIR and the TLS settings."
