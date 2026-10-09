# Caddy with the Cloudflare DNS module so it can get real Let's Encrypt
# certificates for LAN-only hostnames (DNS-01 challenge, nothing exposed).
FROM caddy:2-builder AS builder
RUN xcaddy build --with github.com/caddy-dns/cloudflare

FROM caddy:2
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
