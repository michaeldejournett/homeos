# Password manager (Vaultwarden)

[Vaultwarden](https://github.com/dani-garcia/vaultwarden) is a lightweight server
for **Bitwarden**. You use the official Bitwarden apps on phones, browsers and
desktops. The vault data lives on the HomeOS PC instead of Bitwarden's cloud.

## First-time setup

1. Go to `https://vault.<domain>/admin` and log in with `VAULTWARDEN_ADMIN_TOKEN` from `.env`.
2. *Users* > **Invite user** for each family member. Without email (SMTP) set up, the invite
   still works: they go to `https://vault.<domain>`, choose *Create account* with the
   invited address, and pick a **master password**. Nobody can recover a lost master password,
   so write it down somewhere safe.
3. Create a **Family organization** (web vault > New organization) and collections, e.g.
   *Streaming*, *Wi-Fi*, *Bills*. Then share each one with the right people.

Signups stay off (`VAULTWARDEN_SIGNUPS_ALLOWED=false`), so strangers can't create accounts
even if they reach the server.

## Apps

| Device | App | Setup |
|---|---|---|
| Android / iPhone | Bitwarden | Login screen > region dropdown > **Self-hosted** > `https://vault.<domain>` |
| Browsers | Bitwarden extension | Same as above |
| Windows / Mac | Bitwarden desktop | Same as above |

On Android, enable *Autofill service* in the Bitwarden app settings so it fills
logins in other apps.

**Away from home:** the apps keep a cached copy of the vault, so logins work offline.
Edits sync when the phone can reach the server again, at home or anywhere through Tailscale.

## Security notes

- Vaults are end-to-end encrypted with each person's master password. The server, and
  anyone who steals the PC, only has ciphertext.
- Turn on **two-step login** for every account (Settings > Security > Two-step login, using an
  authenticator app).
- Once everyone is set up, you can disable the admin page by deleting
  `VAULTWARDEN_ADMIN_TOKEN` from `.env` and restarting. Leave it in place if you'd rather keep it.
- Don't make it public on the internet. Tailscale covers remote access.

## Backups

This is the most important data on the box. Back up `data/vaultwarden` (it holds the
`db.sqlite3` database, attachments and keys) to somewhere off the PC. Also export an encrypted
`.json` from the web vault now and then (Tools > Export vault), and **test a restore once**.
