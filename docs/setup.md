# Setup

## 0. What you need

- The Windows PC: Windows 10/11 **Pro** (for Hyper-V), 16 GB RAM is comfortable
  (8 GB works with a small Whisper model), wired Ethernet strongly preferred.
  - **Windows Home?** Hyper-V isn't available. Install VirtualBox and import the
    HAOS `.vdi` image from the [HA docs](https://www.home-assistant.io/installation/windows)
    with a *bridged* network adapter. Everything else is the same.
- A domain name (about $10/yr) with DNS on Cloudflare (free). This gets you real HTTPS
  certificates for LAN-only services, which the Bitwarden mobile app needs.
  Without one, use `TLS_MODE=internal` and install Caddy's root certificate on each device.
- Router access to reserve IP addresses.

## 1. Host setup

From an **elevated** PowerShell in the repo folder:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\install.ps1
```

This enables WSL2 and Hyper-V. It installs Docker Desktop, Steam, Sunshine, Tailscale and
Git, turns off sleep, and opens firewall ports on Private networks. Reboot when it
asks, then run it again.

Then:
- Start **Docker Desktop**. In Settings > General, enable *Start Docker Desktop when you sign in*.
- On your router, reserve a fixed IP for this PC.
- Set Windows to sign in automatically (`netplwiz`) so Docker, Steam and Sunshine
  come back after a power cut.

## 2. Configure

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\init-env.ps1
notepad .env
```

Set `HOMEOS_DOMAIN`, `HOST_LAN_IP`, `MEDIA_DIR`, `TZ` and the TLS settings.
`HA_LAN_IP` gets filled in during step 3.

**Cloudflare token:** Cloudflare dashboard > My Profile > API Tokens > *Create Token* >
"Edit zone DNS" template, limited to your zone.

## 3. Home Assistant VM

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\new-homeassistant-vm.ps1
```

The script prints the VM's MAC address. Reserve an IP for it on your router, put that IP in
`.env` as `HA_LAN_IP`, and restart the VM. Open `http://<HA_LAN_IP>:8123` and complete
onboarding.

Then install the HomeOS package, which also sets up the reverse-proxy settings.
Instructions are at the top of
[`homeassistant/packages/homeos.yaml`](../homeassistant/packages/homeos.yaml).

## 4. DNS

Every device needs `*.home.example.com` to resolve to `HOST_LAN_IP`. Pick one option:

- **Easiest:** in Cloudflare, add an `A` record for `*.home` that points to the LAN IP
  (e.g. `192.168.1.10`), DNS only (grey cloud). Public DNS hands out a private IP, so
  nothing is exposed. A few routers block this ("DNS rebinding protection"). If yours
  does, allow the domain in the router or use the next option.
- **Router / Pi-hole / AdGuard:** add a local DNS rewrite for `*.home.example.com` → `HOST_LAN_IP`.

## 5. Start the stack

```powershell
docker compose up -d --build
docker compose ps
```

## 6. First-run checklist

1. **Authentik:** `https://auth.<domain>/if/flow/initial-setup/`. Create the admin account,
   then set up groups ([permissions.md](permissions.md)).
2. **Homepage forward auth:** in Authentik, go to Applications > *Create with provider* >
   Proxy Provider > **Forward auth (single application)** with external host
   `https://home.<domain>`. Then add the application to the *embedded outpost*.
3. **Jellyfin:** `https://media.<domain>`. Create the admin and add libraries from `/media/...`.
4. **Vaultwarden:** go to `https://vault.<domain>/admin`. The token is `VAULTWARDEN_ADMIN_TOKEN` in `.env`.
   Invite family members by email, or temporarily set `VAULTWARDEN_SIGNUPS_ALLOWED=true`.
5. **Voice:** in Home Assistant, go to Settings > Devices & services > Add > **Wyoming Protocol** three
   times: `HOST_LAN_IP` ports `10300` (Whisper), `10200` (Piper) and `10400` (openWakeWord). Then
   Settings > Voice assistants > edit *Home Assistant*: set Speech-to-text to faster-whisper
   and Text-to-speech to Piper.
6. **Sunshine:** `https://localhost:47990` on the PC. Set a username and password
   ([gaming.md](gaming.md)).
7. **Tailscale:** sign in on the PC and your phone to reach everything away from home.
   In the Tailscale admin console, turn on *subnet routes* for your LAN, or use split DNS.

## Updating

```powershell
docker compose pull
docker compose up -d --build
```

Home Assistant updates itself from its own UI (Settings > System > Updates).

## Backups

Everything stateful lives in `DATA_DIR` (default `./data`) plus the HA VM.
- Back up `data/` nightly, e.g. with Windows File History or [Kopia](https://kopia.io).
  Stop `authentik-db` first, or use `docker compose exec authentik-db pg_dump -U authentik authentik`.
- Home Assistant: Settings > System > Backups. Turn on automatic backups to a network share or cloud.
- **Vaultwarden is your password vault. Test restoring it.**
