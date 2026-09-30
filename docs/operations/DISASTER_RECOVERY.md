# TYFINO Disaster Recovery Plan

Status: PROPOSED OPERATIONS DESIGN — objectives and production topology OPEN
Baseline: `main@41c046ad313d447d8d5213f3a431cadf7cbf4df5` (2026-09-27)

## Current recovery capability

CI on PostgreSQL 16 creates a custom-format dump, checks its catalog and SHA-256 checksum, restores into an isolated disposable database, verifies required tables and audit-chain integrity, and confirms the application role cannot mutate audit records. The scripts are `database/scripts/create-backup.sh` and `database/scripts/verify-backup-restore.sh`. This proves a repeatable **test-schema restore**, not production backup freshness, off-host durability, encryption, or a timed restore on the selected host.

`infrastructure/server/deploy-preproduction.sh` contains an initial local dump step, but its existence is not evidence that it ran safely on production or that off-host copies exist. Production deployment is still governed by `docs/deployment.md`.

## Objectives requiring approval

Recovery time objective (RTO), recovery point objective (RPO), backup retention, and deletion reconciliation are OPEN. The supplied one-hour RTO, 15-minute RPO, daily full backup, and WAL archiving are hypotheses for capacity/cost evaluation. A daily dump alone cannot substantiate a 15-minute RPO. Do not advertise these values until a production-like timed restore and measured backup cadence prove them.

The owner and operator must select a topology, storage region, encryption/key custody, access model, schedule, immutable/off-host copy strategy, retention period, and regular restore drill. A standby, secondary region, HSM, blue/green deployment, and DNS failover are **not present in verified repository architecture**; those options require separate design and authorization.

## Backup and restore runbook

1. Identify the exact deployed SHA, PostgreSQL version/schema, incident window, latest known-good backup, checksum, audit checkpoint, and owner of the recovery decision. Preserve evidence before any destructive step.
2. Isolate the affected service and prevent writes where data consistency requires it. Record whether the incident is an outage, corruption, compromise, or accidental deletion; a compromised backup must not be trusted automatically.
3. Verify checksum and backup catalog. Restore first into an isolated target using an authorized schema-owner identity. Use the repository's restore verification script only with its documented disposable database prefix and after confirming target identity.
4. Verify schema, audit chain, business invariants, and a sanitized licensing smoke path. Reapply `database/init/002_app_role.sh` with a separately generated application credential; the API must never connect as schema owner.
5. Reconcile records deleted under approved data-rights processes so restoring a historical backup does not reactivate data that must remain removed. Revoke affected sessions or credentials only under the relevant authorized incident procedure.
6. Route traffic only after readiness, audit integrity, TLS, Admin access, and monitoring checks pass. Record observed data-loss window and actual recovery duration, then notify the owner.

Production restore, credential rotation, database replacement, DNS changes, and deployment are protected actions. This runbook is preparation, not authorization to execute them.

## Failure paths and rollback

| Failure | Safe first action | Qualification still needed |
| --- | --- | --- |
| API process or container | Diagnose `/healthz` versus `/readyz`; restart only within an approved incident runbook | Host test, service recovery timing |
| Database unavailable | Stop write traffic and investigate; restore into isolation if needed | Backup freshness, restore drill, RPO |
| Corrupt or tampered audit history | Preserve evidence, restrict privileged access, compare external anchor | External anchoring and incident exercise |
| Bad API deployment | Use a known-good compatible artifact/SHA after schema compatibility check | Versioned artifacts and rehearsal |
| Bad Android release | Halt distribution and issue a corrected signed build through protected release workflow | Installed-client behavior and upgrade test |

Do not assume every database migration has a safe `down` path. Prefer compatible, reversible rollout plans, verified backups, and a forward repair for migrations where down migration would destroy data. Android APK rollback is constrained by version codes, signing continuity, and local data compatibility; retaining old APKs alone does not make downgrade safe. No server-side kill switch for Android is established by this document.

## Exercise and evidence

Before a production recovery claim, complete a timed restore on a representative target using an off-host encrypted backup, validate application-role separation and audit integrity, exercise service rollback against compatible schema, and record actual RTO/RPO. Record SHA, artifact/checksum, backup timestamp and location label, database version, operator, start/end, validation results, deviations, and sanitized logs. Never include secrets, provider data, or customer identities in the evidence.
