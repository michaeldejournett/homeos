# Creates a Hyper-V VM running the latest Home Assistant OS.
# Run from an elevated PowerShell:
#   powershell -ExecutionPolicy Bypass -File scripts\windows\new-homeassistant-vm.ps1
param(
    [string]$VmName = 'HomeAssistant',
    [string]$VmDir = 'C:\HyperV\HomeAssistant',
    [int]$Cpus = 2,
    [long]$MemoryBytes = 4GB,
    [string]$SwitchName = 'HomeOS-External'
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'  # PS 5.1 downloads crawl with the progress bar on
Import-Module Hyper-V

if (Get-VM -Name $VmName -ErrorAction SilentlyContinue) {
    throw "VM '$VmName' already exists."
}

# --- External switch so the VM sits directly on your LAN (needed for discovery)
if (-not (Get-VMSwitch -Name $SwitchName -ErrorAction SilentlyContinue)) {
    $nic = Get-NetAdapter -Physical | Where-Object Status -eq 'Up' |
        Sort-Object @{ Expression = { $_.MediaType -ne '802.3' } }, ifIndex | Select-Object -First 1
    if (-not $nic) { throw 'No connected physical network adapter found.' }
    Write-Host "Creating external switch on '$($nic.Name)' (network drops for a few seconds)..."
    New-VMSwitch -Name $SwitchName -NetAdapterName $nic.Name -AllowManagementOS $true | Out-Null
}

# --- Download the latest HAOS Hyper-V disk image
New-Item -ItemType Directory -Force -Path $VmDir | Out-Null
$release = Invoke-RestMethod 'https://api.github.com/repos/home-assistant/operating-system/releases/latest'
$asset = $release.assets | Where-Object name -match '^haos_ova-.*\.vhdx\.zip$' | Select-Object -First 1
if (-not $asset) { throw "Couldn't find a .vhdx image in HAOS release $($release.tag_name)." }

$zip = Join-Path $VmDir $asset.name
Write-Host "Downloading Home Assistant OS $($release.tag_name)..."
Invoke-WebRequest $asset.browser_download_url -OutFile $zip
Expand-Archive $zip -DestinationPath $VmDir -Force
Remove-Item $zip
$vhdx = Get-ChildItem $VmDir -Filter 'haos_ova-*.vhdx' | Select-Object -First 1
Resize-VHD -Path $vhdx.FullName -SizeBytes 64GB

# --- Create the VM (Gen 2, Secure Boot off as HAOS requires)
New-VM -Name $VmName -Generation 2 -MemoryStartupBytes $MemoryBytes `
    -VHDPath $vhdx.FullName -SwitchName $SwitchName -Path $VmDir | Out-Null
Set-VM -Name $VmName -ProcessorCount $Cpus -StaticMemory `
    -AutomaticStartAction Start -AutomaticStopAction ShutDown -CheckpointType Disabled
Set-VMFirmware -VMName $VmName -EnableSecureBoot Off
Start-VM -Name $VmName

$mac = (Get-VMNetworkAdapter -VMName $VmName).MacAddress -replace '(..)(?!$)', '$1:'
Write-Host ''
Write-Host "Home Assistant OS is booting. VM MAC address: $mac"
Write-Host "  1. On your router, reserve an IP for that MAC and put it in .env as HA_LAN_IP."
Write-Host "  2. In a few minutes open http://homeassistant.local:8123 (or http://<HA_LAN_IP>:8123)."
