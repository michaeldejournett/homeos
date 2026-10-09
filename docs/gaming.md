# Your Steam library on every screen

Steam stays installed on the Windows PC with your games on its disks. **Sunshine**
captures the PC's screen with GPU encoding and streams it, and **Moonlight**
plays it on the Fire TV, phone, laptop or another TV. It feels like playing locally,
and controllers connect to the Fire TV or phone.

## Setup

1. `install.ps1` installed Steam and Sunshine. Sign in to Steam and set it to start with Windows.
2. Open `https://localhost:47990` on the PC (accept the self-signed certificate) and create
   a Sunshine username and password.
3. Sunshine > Applications already has **Steam Big Picture**. Leave it as the default app.
4. Install **Moonlight** on the Fire TV (Appstore) and phone (Play Store / App Store).
   It finds the PC automatically. Pick it and type the shown PIN into Sunshine > PIN.
5. Pair a Bluetooth controller (Xbox, PlayStation, 8BitDo) to the Fire TV.

## Tips

- **Wired Ethernet** for the PC is the biggest quality improvement. The Fire TV should
  be wired too, or on 5 GHz Wi-Fi close to the router.
- Fire TV Stick 4K / 4K Max decode H.265/HEVC well. In Moonlight, set 1080p60 and
  ~20 Mbps to start, then raise it.
- **Headless PC** (no monitor attached): use a cheap HDMI dummy plug, or a
  virtual display driver, so Windows keeps rendering at the right resolution.
- The PC must be **signed in** (auto-login, see setup.md). Sunshine can't stream the lock screen.
- **Away from home:** Moonlight over Tailscale works. Use the PC's Tailscale IP.
- **Faster downloads:** if several PCs in the house use Steam, add
  [LANCache](https://lancache.net) to cache game downloads.
- Steam's built-in **Remote Play** also works for the Steam Link app on phones, with no extra
  setup. Sunshine + Moonlight usually has lower latency and works with non-Steam games.

## Voice

With the Fire TV set up in HA ([devices.md](devices.md)), "play Steam on the TV"
opens Moonlight on the Fire TV.
