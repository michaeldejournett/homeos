# Permissions

Authentik is the single source of truth for **who** someone is and **which apps**
they can open. Each app then enforces **what** they can do inside it, because apps
like Jellyfin have finer-grained controls (ratings, libraries) than any SSO layer.

## Groups

Create these in Authentik (Directory > Groups):

| Group | Who | Dashboard | Home Assistant | Jellyfin | Vault | Steam (Moonlight) | Admin pages |
|---|---|---|---|---|---|---|---|
| `homeos-admins` | You | ✅ | Admin | Admin | Own + family org (owner) | ✅ | ✅ |
| `homeos-adults` | Partner, adults | ✅ | User | All libraries | Own + family org | ✅ | ❌ |
| `homeos-kids` | Children | ✅ | User, kids' dashboard only | Kids libraries, max rating set | Own vault | Paired devices only | ❌ |
| `homeos-guests` | Visitors | Guest page | ❌ | Optional guest user | ❌ | ❌ | ❌ |

## How each layer is enforced

**Dashboard (home.)**: Caddy asks Authentik on every request (forward auth). To
restrict an app to groups, open it in Authentik > Applications and use *Policy / Group / User
Bindings*, e.g. bind `homeos-admins` + `homeos-adults` + `homeos-kids`.

**Authentik admin (auth.)**: only superusers can reach `/if/admin/`. Add the superuser
permission to `homeos-admins` only.

**Jellyfin**: own accounts, with per-user library access and *parental control* (max
rating, blocked tags, allowed hours). For single sign-on, install the
[SSO plugin](https://github.com/9p4/jellyfin-plugin-sso) and map Authentik groups → admin / user.
TV apps still use a Jellyfin login or Quick Connect code, which is easy on a remote.

**Home Assistant**: create HA users (Settings > People) as *Administrator* or regular users.
Give kids and the wall-panel phone their own dashboards, and hide admin panels from them.
HA has no native OIDC; community integrations exist, but separate HA logins are
the most reliable option today.

**Vaultwarden**: everyone has their own encrypted vault. Even the server admin can't
read it. Shared logins go in a "Family" organization with *collections*, e.g. "Streaming
logins" shared with adults and "Wi-Fi" shared with everyone. Signups stay off; invite people
from `/admin`.

**Steam / Sunshine**: only devices you've paired (PIN entry in the Sunshine UI) can stream.
Use Steam Family / Family View on the Steam side to limit which games kids can launch.

**Remote access**: Tailscale ACLs decide which family devices reach the PC from outside.
Nothing is port-forwarded on the router.

## Adding a family member (checklist)

1. Authentik: create the user and add them to a group.
2. Jellyfin: create the user (or let SSO create it), then set libraries and parental rating.
3. Home Assistant: add a Person/user and pick their dashboard.
4. Vaultwarden `/admin`: invite them, then add them to the Family org collections.
5. Tailscale: invite them if they need access from outside the house.
