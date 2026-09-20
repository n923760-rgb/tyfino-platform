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

Fresh database initialization requires two distinct PostgreSQL identities: `POSTGRES_USER` owns the schema and is reserved for initialization, backup/restore, and controlled migrations; `POSTGRES_APP_USER` is the restricted runtime identity used in `DATABASE_URL`. Their passwords must be independently generated. Supplying the owner URL to the API defeats the audit boundary and is forbidden.

For an existing database, stop the API, take and validate a backup, set the four `POSTGRES_*` variables, and run `database/init/002_app_role.sh` as the schema owner before restarting traffic. The script is transactional and idempotent: it rotates the application password, removes inherited roles and all direct privileges in `public`, then grants only the reviewed runtime matrix. It preserves application rows and fails closed if the application role owns database objects, because ownership must be transferred through a separately reviewed migration. CI starts with an intentionally overprivileged legacy role, runs this migration twice, proves existing audit data remains, and then runs the API integration suite through the restricted identity. This repository evidence does not authorize execution against production without the backup, maintenance window, credential delivery, and rollback controls required by this document.

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
   HTTP request and PostgreSQL statement timeouts must likewise be approved from latency testing and set explicitly; production refuses missing, placeholder, sub-second, or greater-than-two-minute values.
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

Backups intentionally omit owners and ACLs so they remain portable. After restoring under the approved schema owner, rerun `database/init/002_app_role.sh` with the target environment's independently generated application credential before starting the API.

The API exposes separate `/healthz` liveness and `/readyz` traffic-readiness probes. Container dependency checks use `/readyz`, which requires a reachable database and the current licensing/audit protection schema. Full audit-chain verification is kept out of the high-frequency readiness path and must be monitored through the authorized Admin audit endpoint.
