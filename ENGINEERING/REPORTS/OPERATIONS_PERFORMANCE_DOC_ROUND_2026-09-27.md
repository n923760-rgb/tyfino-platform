# Operations and Performance Documentation Round — 2026-09-27

Status: DRAFT — exact PR-head CI pending
Repository: `n923760-rgb/tyfino-platform`
Starting official `main`: `41c046ad313d447d8d5213f3a431cadf7cbf4df5`
Task branch: `docs/operations-performance-foundation`

## Problem and scope

The owner supplied observability, disaster-recovery, and performance recommendations. The live repository has some API probes, redacted logging, CI database restore checks, Android size metadata, and emulator tests. It has no verified production metrics/alerts, off-host backups, timed recovery drill, or physical performance baseline. This round documents the next qualification work without changing runtime behavior.

## Source reconciliation

| Finding | Evidence | Classification |
| --- | --- | --- |
| `/healthz` and `/readyz`, redacted completion logs, rate-limit configuration, Admin audit-chain verification exist | `docs/api.md`, `docs/security.md`, `apps/api/src/`, `.env.example` | FACT, source |
| Backup/restore scripts run in CI on PostgreSQL 16 | `docs/deployment.md`, `database/scripts/`, `.github/workflows/ci.yml` | FACT, CI scope |
| CI records optimized unsigned APK bytes and tests API 27 phone/API 35 tablet emulators | `.github/workflows/ci.yml`, `docs/android/device-qualification-v1.md` | FACT, source/CI scope |
| Production alert destination, off-host backup, external audit anchor, real RTO/RPO, physical performance | no qualifying production/device evidence found in current repository | UNKNOWN / BLOCKED |

## Change

- `docs/operations/OBSERVABILITY.md`: proposed signals, privacy-safe labels, alerts, redaction, and qualification gate.
- `docs/operations/DISASTER_RECOVERY.md`: existing test evidence, proposed objectives, safe restore flow, limitations of rollback.
- `docs/architecture/PERFORMANCE_BUDGETS.md`: candidate budgets and physical measurement method, with no invented PASS or premature CI gate.
- Update the canonical roadmap for the completed legal/security PR and this round.

## Validation

- Local Markdown shape, referenced-path existence, `git diff --check`, changed-file scope, and secret-pattern review: PASS on the task branch before PR creation.
- Exact PR-head CI: PENDING.
- Production restore, physical performance, signed release, and deployment: NOT RUN.

## Remaining decisions

Owner/operations approval of topology, RTO/RPO, retention, monitoring ownership, alert thresholds, reference hardware, and actual production deployment remains OPEN. Merge is a separate protected action under `AGENTS.md`.
