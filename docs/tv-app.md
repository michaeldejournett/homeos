# HomeOS TV app

One home screen on the Fire TV / Android TV for the whole house:

```
 HomeOS                                                    7:42 PM   [Settings]
 Home            💡 Living room  🎬 Movie night  🔌 Fan  🔒 Front door (status)
 Continue watching   [ Show S2·E4 ▬▬▬──── ] [ Movie ▬────── ] …
 Next up             [ … ]
 Streaming           [Jellyfin] [Netflix] [YouTube] [Disney+] [Prime Video] …
 Recently added      [ … ]
 Games               [Moonlight → your Steam library]
 All apps            …
```

| Row | Source | What pressing does |
|---|---|---|
| **Home** | Home Assistant entities with the label **TV** | Toggles lights/switches/fans, runs scenes/scripts, presses buttons. Locks, alarms and garage doors only show status. |
| **Continue watching / Next up / Recently added** | Jellyfin | Plays in the app's built-in player and resumes where you left off. Progress syncs to Jellyfin, so your phone and other TVs stay in step. |
| **Streaming** | Installed streaming apps (Netflix, YouTube, Disney+, Prime Video, Max, Hulu, Apple TV, Paramount+, Peacock, Plex, Spotify, Twitch…) | Opens the app directly. |
| **Games** | Moonlight / Steam Link | Opens Moonlight, which streams your Steam library from the PC ([gaming.md](gaming.md)). |
| **All apps** | Everything else installed | Opens it. |

Netflix and the other paid services still play inside their own apps, because of their
copy protection. This app is the one place you open them from, next to your own media
and the house controls.

## Install

1. **Get the APK.** Each push that changes `clients/androidtv/` runs the *Android TV app*
   workflow on GitHub. Download `homeos-tv-apk` from the run's *Artifacts*. You can also build it
   yourself: `cd clients/androidtv` then `.\gradlew.bat :app:assembleDebug` (needs Android Studio or the SDK).
2. **On the TV:** turn on Developer options > *ADB debugging* (see [devices.md](devices.md)),
   and reserve its IP on your router.
3. **In Home Assistant:** create a long-lived access token (Profile > Security). Then add the
   label **TV** to everything you want on the TV: Settings > Labels, or the entity's settings
   dialog. Scenes like "Movie night" make great tiles.
4. **On the PC:**

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\windows\setup-tv.ps1 `
       -TvIp 192.168.1.50 -Apk $HOME\Downloads\app-debug.apk `
       -HaToken <token> -JellyfinUser <your jellyfin user>
   ```

   This installs the app and sends it the Home Assistant and Jellyfin addresses from `.env`. It
   signs in to Jellyfin (it asks for the password) and opens the app. Tokens are stored encrypted
   on the TV with the Android Keystore.

Without the PC, open **Settings** in the app and fill in the same details with the remote. The
Fire TV phone app's keyboard makes typing easier.

**Make it the first thing you see:** the app appears in the TV's apps list. On Fire TV, move it to the
front of *Your Apps*. On Google TV, add it to the favourites row.

## Kids and guests

The TV app uses one Home Assistant token and one Jellyfin user:
- Use a Jellyfin user with the right parental rating for a kids' TV.
- Use a non-admin HA user's token, and only label safe entities. A kids' room can use a
  different label (e.g. `kids-tv`) with `-HaLabel kids-tv`.

## Development

```
clients/androidtv/
  app/src/main/java/com/homeos/tv/
    data/        ConfigStore (Keystore-sealed settings), Jellyfin + Home Assistant clients, installed apps
    ui/          Home screen rows, settings, view model
    player/      Media3 ExoPlayer: direct play, falling back to Jellyfin HLS transcode
    ProvisionReceiver.kt   adb setup (protected by android.permission.DUMP)
```

Kotlin + Jetpack Compose for TV, the same toolchain as MobileVault. Run the unit tests with
`./gradlew :app:testDebugUnitTest`.

Ideas for later: a voice button that sends to Home Assistant Assist or OpenClaw, camera
snapshots in the Home row, and Jellyfin library browsing beyond the three rows (the Jellyfin
app in the Streaming row covers that for now).
