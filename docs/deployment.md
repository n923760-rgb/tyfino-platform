# TYFINO Deployment Status

Status: DEFERRED  
Last reviewed: 2026-09-08

## Current rule

Production deployment is not authorized in the foundation phase.

The following are **DECIDED**:

- Do not configure production DNS, hosting, secrets, signing, releases, or automated deployment without explicit instruction.
- Do not deploy the Android foundation, legacy provider-account backend path, or unapproved licensing behavior.
- Do not use GitHub Actions as a trial-and-error deployment environment.
- The IPTV provider stream path must never pass through TYFINO infrastructure.
- Existing unrelated services on any future host must remain isolated and untouched.

## Possible V1 shape

A single domain with paths such as `/admin` and `/api/v1` is **PROPOSED**, not finalized.

The production domain, DNS records, hosting target, API endpoints, secret management, backup location, monitoring, and deployment mechanism are **OPEN**.

## Entry gate

Deployment planning must not begin until:

1. The licensing/backend boundary is audited against the current implementation.
2. Legacy IPTV provider-account handling is removed or explicitly isolated from the approved product.
3. Licensing and trial contracts are approved.
4. Production endpoints and hosting details are explicitly approved.
5. Secrets, backups, restoration, monitoring, and rollback are designed and tested.
   OWNER TOTP provisioning must use a unique `ADMIN_OWNER_TOTP_SECRET` from the production secret store; no secret or provisioning URI may enter source control, logs, screenshots, or support messages.
   Audit-chain verification must be monitored and periodically anchored outside the primary database; the in-database chain alone cannot detect a privileged owner who disables its triggers and rewrites the chain.
   All five API rate-limit counts in `.env.example` must be approved from expected traffic and abuse testing. Production configuration fails closed while any value is missing or still marked `CHANGE_ME`.
6. Android identity, versioning, signing, and release policy are approved.
7. CI and security qualification are `PASS`; `BLOCKED` is not `PASS`.

## Historical deployment material

Earlier VPS, DNS, Docker, and Caddy instructions are not authoritative for the current foundation. They may be reconsidered later only after the entry gate is satisfied.

No DNS record, server, domain, tag, release, or deployment is changed by this document.

## PostgreSQL backup and restore evidence

The repository provides two Linux/PostgreSQL-client scripts:

- `database/scripts/create-backup.sh` creates a restrictive-permission custom-format dump, validates its catalog, and writes a SHA-256 checksum without overwriting an existing backup.
- `database/scripts/verify-backup-restore.sh` verifies that checksum, restores into a new disposable database whose name must start with `tyfino_restore_`, checks required tables and the audit hash chain, confirms audit mutation rejection, and removes only that disposable database.

CI runs both scripts against PostgreSQL 16 on every change. This is repeatable restore evidence for the current schema; it does not approve a production backup location, retention period, encryption/key policy, schedule, recovery-point objective, or recovery-time objective. Those choices remain **OPEN**, and a production-target restore drill remains **BLOCKED** until the hosting and data-retention decisions are approved.

The API exposes separate `/healthz` liveness and `/readyz` traffic-readiness probes. Container dependency checks use `/readyz`, which requires a reachable database and the current licensing/audit protection schema. Full audit-chain verification is kept out of the high-frequency readiness path and must be monitored through the authorized Admin audit endpoint.
