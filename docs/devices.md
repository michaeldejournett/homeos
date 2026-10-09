# Connecting devices

## Fire TV

The Fire TV is the main screen. Install these apps from the Amazon Appstore:

| App | Purpose |
|---|---|
| **Jellyfin** | Your own media. Server: `https://media.<domain>` (or `http://HOST_LAN_IP:8096`) |
| **Moonlight** | Your Steam library, streamed from the PC ([gaming.md](gaming.md)) |
| Netflix, Disney+, etc. | Streaming services stay in their own apps ([media.md](media.md)) |

**Let Home Assistant control it** (power, launch apps, voice):
1. On the Fire TV, open Settings > My Fire TV > About and click the device name 7 times to unlock Developer Options.
   Then Developer Options > **ADB debugging: On**.
2. Reserve the Fire TV's IP on your router.
3. In HA, add the integration **Android Debug Bridge**, enter the Fire TV IP, and accept the prompt on the TV.
4. Put the resulting entity id (e.g. `media_player.fire_tv`) in the *Fire TV media player entity*
   helper (Settings > Devices > Helpers).

Now you can say *"open Netflix on the TV"* or *"play Steam on the TV"* to a voice puck.
See `homeassistant/packages/homeos.yaml`.

## Voice assistant pucks

Recommended: **[Home Assistant Voice Preview Edition](https://www.home-assistant.io/voice-pe/)**.
It's open hardware running ESPHome, and it does wake-word detection on the device.
DIY options like the ESP32-S3-BOX or M5Stack Atom Echo work the same way.

1. Plug it in. HA discovers it automatically, because the HA VM sits on your LAN. Click *Configure*.
2. Choose the *Home Assistant* voice pipeline. It uses the local Whisper and Piper containers, so
   no audio leaves your house.
3. Assign each puck to an **area** (Kitchen, Living Room). Then "turn off the lights" acts on
   the room you're in.

**Speed vs. accuracy:** if speech recognition is slow on an old CPU, set
`WHISPER_MODEL=tiny-int8` in `.env`. If it mishears you, try `small-int8`. For
smarter conversations, add an LLM conversation agent in HA. A local one like
Ollama is fine if you have a GPU.

## Spare phone

Pick one or both:

- **Wall dashboard:** install the **Home Assistant** companion app. Turn on kiosk mode, or
  use [Fully Kiosk Browser](https://www.fully-kiosk.com) pointed at `https://home.<domain>`
  or an HA dashboard. Keep it plugged in and turn on Developer options > Stay awake.
- **Extra voice satellite:** the HA companion app can be the phone's default assistant
  (Android: Settings > Apps > Default apps > Digital assistant > Home Assistant).
  For always-listening, use [Wyoming Satellite](https://github.com/rhasspy/wyoming-satellite)
  in Termux with the `openwakeword` container.

Give the phone its own **"Wall Panel"** HA user, a non-admin with access to only the
dashboards it needs. That way a guest picking it up can't change settings.

## MobileVault

The phone app for your OpenClaw assistant. Setup, pairing and permissions are in
[mobilevault.md](mobilevault.md). On the spare phone it also works as the default
assistant: hold power to ask it something about the house.

## Anything else

- **Zigbee / Thread / Matter devices:** plug a USB coordinator (e.g. Home Assistant
  Connect ZBT-1, SONOFF dongle) into the PC. In Hyper-V, USB passthrough is awkward. Prefer a
  network coordinator (e.g. SMLIGHT SLZB-06), or use VirtualBox for USB passthrough.
- **Phones as presence sensors:** the HA companion app reports home/away for automations.
