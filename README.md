# HomeOS

A self-hosted "home operating system" for an old Windows PC. It's assembled from
mature open-source projects instead of being written from scratch, so each part
gets updates and has a large community behind it:

| Need | Built on | Where it runs |
|---|---|---|
| Devices, automations, voice assistant, TV control | [Home Assistant OS](https://www.home-assistant.io) | Hyper-V VM |
| Voice pucks (wake word → speech → action) | HA Assist + [Wyoming](https://github.com/rhasspy/wyoming) Whisper / Piper / openWakeWord | Docker |
| Family accounts, groups, who-can-access-what | [Authentik](https://goauthentik.io) | Docker |
| Movies / shows / music in one library | [Jellyfin](https://jellyfin.org) | Docker |
| Password vault on phones and browsers | [Vaultwarden](https://github.com/dani-garcia/vaultwarden) + official Bitwarden apps | Docker |
| Steam library on the TV and phone | Steam + [Sunshine](https://github.com/LizardByte/Sunshine) → [Moonlight](https://moonlight-stream.org) | Windows (native) |
| One home screen for everything | [Homepage](https://gethomepage.dev) | Docker |
| HTTPS for every service | [Caddy](https://caddyserver.com) | Docker |
| Access from outside the house | [Tailscale](https://tailscale.com) | Windows (native) |

## How it fits together

```
                       ┌──────────────── Windows PC ────────────────────────────┐
  Fire TV ─────────┐   │  Native:  Steam + Sunshine   Tailscale                 │
  (Jellyfin,       │   │                                                        │
   Moonlight,      │   │  Docker Desktop:                                       │
   Netflix…)       ├──▶│   Caddy :443 ─▶ home.  → Homepage  (SSO via Authentik) │
                   │   │              ─▶ auth.  → Authentik                     │
  Spare phone ─────┤   │              ─▶ media. → Jellyfin                      │
  (wall dashboard, │   │              ─▶ vault. → Vaultwarden                   │
   Bitwarden, HA)  │   │              ─▶ ha.    → Home Assistant VM ──┐         │
                   │   │   Whisper :10300  Piper :10200  OWW :10400   │         │
  Voice pucks ─────┘   │        ▲ speech-to-text / text-to-speech     │         │
  (ESPHome) ──────────────────────────────────▶ Hyper-V: Home Assistant OS ◀───┘
                       └────────────────────────────────────────────────────────┘
```

Home Assistant gets its own VM with its own LAN IP because device discovery
(voice pucks, Fire TV, Chromecasts, Zigbee) needs to sit directly on the
network. Docker Desktop on Windows can't reliably provide that. Steam and
Sunshine run natively because they need the GPU and a signed-in desktop.

## Quick start

Full walkthrough: **[docs/setup.md](docs/setup.md)**.

```powershell
# Elevated PowerShell, in the repo folder
powershell -ExecutionPolicy Bypass -File scripts\windows\install.ps1              # features + apps (reboot, re-run)
powershell -ExecutionPolicy Bypass -File scripts\windows\init-env.ps1             # creates .env with secrets
notepad .env                                                                       # domain, IPs, media folder
powershell -ExecutionPolicy Bypass -File scripts\windows\new-homeassistant-vm.ps1 # HA OS VM
docker compose up -d --build
```

Then open `https://auth.<your-domain>/if/flow/initial-setup/` to create the admin
account.

## Docs

- [setup.md](docs/setup.md): install, DNS and certificates, first boot
- [devices.md](docs/devices.md): Fire TV, spare phone, voice pucks, mobile vault
- [permissions.md](docs/permissions.md): family groups and what each one can reach
- [media.md](docs/media.md): Jellyfin, streaming services, what "one place" can mean
- [gaming.md](docs/gaming.md): streaming your Steam library to the TV and phone

## Repo layout

```
compose.yaml                     service stack (Docker Desktop)
.env.example                     settings template
caddy/                           reverse proxy (with Cloudflare DNS module for real certs)
homepage/config/                 dashboard tiles and links
homeassistant/packages/          HA config: voice "open Netflix on the TV", movie night
scripts/windows/                 host setup, secrets, HA VM creation
docs/                            guides
```
