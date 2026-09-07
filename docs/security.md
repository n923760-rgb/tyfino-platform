# Security baseline

- Keep the repository private.
- Never commit `.env`, passwords, API tokens, IPTV credentials, SSH keys, or backups.
- Store activation codes as slow hashes; show the plain code only once when generated.
- Encrypt provider credentials at application level before storing them in PostgreSQL.
- Use HTTPS only in production; Caddy manages certificates automatically.
- Use a dedicated, non-root `deploy` user on the VPS.
- Limit inbound firewall ports to SSH, HTTP, and HTTPS.
- Protect the admin panel with short sessions, rate limits, audit logging, and MFA when implemented.
- Do not expose PostgreSQL outside the Docker network.
- Back up the database off the VPS and regularly test restoration.
- Do not scrape or automate the reseller panel without explicit provider permission.

The current milestone contains no admin login or activation endpoint, so it must not be treated as a production-ready customer release.
