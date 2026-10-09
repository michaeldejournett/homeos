# Media and streaming

## Your own media: Jellyfin

Point `MEDIA_DIR` in `.env` at a folder (or a whole drive) laid out like this:

```
D:\Media\
  Movies\Movie Name (2020)\Movie Name (2020).mkv
  Shows\Show Name\Season 01\Show Name S01E01.mkv
  Music\Artist\Album\01 - Track.flac
  Photos\
```

Then add each folder as a library in Jellyfin. Clients: the Fire TV app, the phone
app, any browser, Kodi, and Infuse.

**Transcoding:** Docker Desktop can't pass an Intel/AMD/NVIDIA GPU to Jellyfin
for hardware transcoding, so it uses the CPU. That's fine when clients play files directly,
which the Fire TV handles for most formats. If you need heavy transcoding (several remote
streams, 4K→1080p), install Jellyfin natively on Windows instead (`winget install Jellyfin.Server`)
and enable hardware acceleration there. Then change the `media.` site in the Caddyfile to
`reverse_proxy host.docker.internal:8096` and remove the `jellyfin` service.

## Streaming services (Netflix, Disney+, Prime, etc.)

Paid streaming services use DRM and only play inside their own apps, so no
self-hosted software can merge them into Jellyfin. What HomeOS does instead:

- **One launcher:** the Fire TV home screen already lists every service. Home Assistant
  can open any of them by voice ("open Disney Plus on the TV") or from a dashboard button.
- **One search:** the Homepage dashboard links to JustWatch, which shows which of your
  services has a given title.
- **One remote:** HA controls the TV (power, volume, app) from the phone, a voice puck or an
  automation, e.g. "Movie night" dims the lights and opens Jellyfin.
- **Shared logins:** keep the family's streaming passwords in a Vaultwarden collection.

## Ideas to add later

All of these drop into `compose.yaml` the same way as the existing services:

- **[Immich](https://immich.app)**: Google Photos replacement. The phone app auto-backs up photos.
- **[Audiobookshelf](https://www.audiobookshelf.org)**: audiobooks and podcasts.
- **[Jellyseerr](https://github.com/Fallenbagel/jellyseerr)**: lets family members request titles.
- **[Navidrome](https://www.navidrome.org)**: dedicated music server with Subsonic apps.
