# TYFINO Platform

Private control plane and application foundation for **TYFINO by Techify**.

TYFINO is an IPTV player platform for Techify customers. It manages application activation, devices, provider connection metadata, and administrative operations. It does **not** sell IPTV subscriptions or relay video traffic.

## Current milestone

`Foundation v0.1` provides:

- A lightweight TypeScript/Fastify API with health and readiness checks.
- PostgreSQL schema for customers, provider accounts, activation codes, devices, hosts, and audit logs.
- Docker Compose deployment suitable for the temporary 2 GB Ubuntu VPS.
- Caddy routing and automatic HTTPS for the public site, admin panel, and API.
- Static placeholders that keep the domains presentable until the React applications are connected.
- A documented deployment and security model.

## Domain layout

| Address | Purpose |
| --- | --- |
| `tyfino.online` | Product and download website |
| `admin.tyfino.online` | Private Techify administration panel |
| `api.tyfino.online` | Application API |

## Local startup

1. Copy `.env.example` to `.env`.
2. Replace every value marked `CHANGE_ME`.
3. Run `docker compose up --build`.
4. Check `http://localhost:3000/healthz`.

Production is intentionally not deployed automatically yet. Server secrets and an isolated deployment user must be configured first.

## Repository map

```text
apps/api/                 Fastify control-plane API
database/init/            Initial PostgreSQL schema
docs/                     Architecture, security, and deployment notes
infrastructure/caddy/     Domain routing and TLS configuration
sites/                    Temporary public/admin pages
docker-compose.yml        VPS service definition
```

## Important boundaries

- Stream traffic goes directly from the IPTV provider to the customer device.
- Provider credentials are never committed to GitHub.
- The provider's reseller panel currently has no API, so account mapping is manual.
- App activation codes (`TYF-XXXX-XXXX`) are separate from provider credentials.
- Future provider integrations must use an adapter interface; scraping the reseller panel is not part of this foundation.

See [`docs/architecture.md`](docs/architecture.md) and [`docs/deployment.md`](docs/deployment.md) before production deployment.
