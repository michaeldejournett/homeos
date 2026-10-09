# MobileVault (your assistant on the phone)

[MobileVault](https://github.com/michaeldejournett/MobileVault) is the Android
client for an **OpenClaw** Gateway. HomeOS runs that Gateway on the always-on
Windows PC. Your assistant is then always reachable from the phone. With Home
Assistant connected, it can also run the house: "turn off the downstairs lights",
"is the garage door open?", or "every weekday at 7 dim the bedroom lights up".

```
 MobileVault (phone) ──wss──▶ Tailscale Serve (https://<pc>.<tailnet>.ts.net)
                                     │  real certificate, tailnet-only
                                     ▼
                         OpenClaw Gateway 127.0.0.1:18789  (Windows, native service)
                                     │  MCP (streamable HTTP)
                                     ▼
                         Home Assistant VM  http://HA_LAN_IP:8123/api/mcp
```

Why it's set up this way:

- **The Gateway stays on loopback.** MobileVault release builds only accept `https://`/`wss://` with a
  publicly trusted certificate, and the Gateway shouldn't be on the open LAN or the
  internet. Tailscale Serve meets both needs. It works at home and away with no port
  forwarding.
- **It runs natively, not in Docker.** That's the supported Windows install. OpenClaw also
  needs your user profile for model sign-in, and runs tools on this PC.

## Setup

1. Finish [setup.md](setup.md) through step 3, so Home Assistant is running.
2. Tailscale: sign in on the PC. In the [admin console](https://login.tailscale.com/admin/dns),
   turn on **MagicDNS** and **HTTPS certificates**.
3. In Home Assistant:
   - Go to Settings > Devices & services > Add integration > **Model Context Protocol Server**.
     On its options page, choose which assistant's exposed entities it can use.
   - Settings > Voice assistants > **Expose**: pick the entities the assistant may see or control.
     This is the main permission boundary between OpenClaw and your home.
   - Profile (bottom left) > Security > **Long-lived access tokens** > Create token.
4. On the PC, as your normal user (not elevated):

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\windows\setup-openclaw.ps1 -HaToken <token>
   ```

   This installs OpenClaw if needed and runs onboarding the first time (model sign-in). It
   configures the Tailscale Serve publishing, adds Home Assistant as an MCP server, and installs the
   Gateway as a service that starts before login. It prints your MobileVault address.
5. Put that address in `.env` as `OPENCLAW_URL`, then run `docker compose up -d` (for the dashboard link).

**Already running OpenClaw on another computer?** You can keep it there and skip step 4.
Moving it to the HomeOS PC means it stays up when your main computer sleeps.

## Home controls in the app

MobileVault's **Home** screen talks to Home Assistant directly, separately from the assistant:
rooms, scenes, automations and a favorites widget. Connect it in MobileVault › Home with
`https://ha.<domain>` and a Home Assistant long-lived token. This needs `TLS_MODE=cloudflare`,
because MobileVault only trusts publicly trusted certificates. Details are in MobileVault's
`docs/home-control.md`.

## Connect the phone

1. Install Tailscale on the phone and sign in to the same tailnet.
2. In MobileVault, open connection settings:
   - Address: `https://<pc>.<tailnet>.ts.net` (from the script)
   - Token: output of `openclaw config get gateway.auth.token` on the PC
3. The first connect pairs the phone as a device. If the app shows an approval id, run
   `openclaw devices approve <id>` on the PC. From then on the phone uses its own device
   token.
4. Optional: make MobileVault the phone's default assistant (Settings > Apps > Default apps >
   Digital assistant). Holding power/home then opens the MobileVault overlay.

Lost or replaced phone: "Forget this Gateway" in the app if you still have it, then
`openclaw devices remove <id>` on the PC.

## Permissions

- **Who can use the assistant:** only devices you've paired (`openclaw devices list`) and
  only on your tailnet. Use Tailscale ACLs to restrict the PC's `443` to your own devices
  if family members share the tailnet.
- **What it can do in the house:** only entities exposed to Assist in Home Assistant.
  Keep locks, alarms and garage doors unexposed, or add them on purpose.
- **What it can run on the PC:** OpenClaw's command approvals. MobileVault shows Allow once /
  Always / Deny prompts, so keep risky commands on approval.
- **Changing automations:** needs `operator.admin` on the Gateway.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Script says Serve failed / no certificate | Turn on HTTPS certificates in the Tailscale admin DNS page |
| MobileVault can't connect | Phone's Tailscale is off, or the address has `http://`; it must be `https://` |
| Connects but "pairing required" | `openclaw devices list`, then `openclaw devices approve <id>` |
| Assistant can't see devices | Expose them in HA > Voice assistants > Expose; check `openclaw mcp list` |
| `config set mcp.servers...` errors | Add the block by hand to `%USERPROFILE%\.openclaw\openclaw.json` (see below), then `openclaw doctor --fix` |

Manual MCP block:

```json5
{
  mcp: {
    servers: {
      homeassistant: {
        url: "http://192.168.1.11:8123/api/mcp",
        transport: "streamable-http",
        headers: { Authorization: "Bearer <HA long-lived token>" },
      },
    },
  },
}
```
