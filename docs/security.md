# Security baseline

- Keep the repository private.
- Never commit `.env`, passwords, API tokens, IPTV credentials, SSH keys, or backups.
- Store high-entropy activation codes and opaque session tokens as keyed HMAC hashes; show the plain activation code only once.
- Encrypt provider credentials at application level before storing them in PostgreSQL.
- Use HTTPS only in production; Caddy manages certificates automatically.
- Use a dedicated, non-root `deploy` user on the VPS.
- Limit inbound firewall ports to SSH, HTTP, and HTTPS.
- Protect the admin panel with short sessions, rate limits, audit logging, and MFA when implemented.
- Remove `ADMIN_BOOTSTRAP_PASSWORD` from the server environment after the first owner account is created.
- Do not expose PostgreSQL outside the Docker network.
- Back up the database off the VPS and regularly test restoration.
- Do not scrape or automate the reseller panel without explicit provider permission.

Backend v0.2 includes the security foundation, but production release still requires the admin UI, end-to-end tests against PostgreSQL, backups, monitoring, and VPS hardening.
