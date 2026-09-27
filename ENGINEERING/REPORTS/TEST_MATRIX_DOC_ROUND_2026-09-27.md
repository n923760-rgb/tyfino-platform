# V1 Test Matrix Documentation Round — 2026-09-27

Status: DRAFT — exact PR-head CI pending
Repository: `n923760-rgb/tyfino-platform`
Starting official `main`: `bb71bbb7d75ab5f083e63d1c263e07be6e318821`
Task branch: `docs/device-scenario-test-matrix`

## Problem and scope

The owner-supplied test matrix listed devices, provider variants and user journeys, but exact hardware names and a PASS status without device evidence would conflict with TYFINO's existing qualification contract. This round makes an executable V1 scenario plan and points to the canonical Android evidence ledger/manual record. Documentation only; no Android/API/Admin/CI behavior changes.

## Source reconciliation

| Finding | Evidence | Classification |
| --- | --- | --- |
| Android CI builds and exercises API 27 phone and API 35 tablet managed devices | `.github/workflows/ci.yml`, `docs/android/device-qualification-v1.md` | FACT, emulator/source scope |
| Physical TV, API 24-class, foldable, real-provider/media and accessibility qualification remain open | `docs/android/device-qualification-v1.md`, `ENGINEERING/MASTER_ROADMAP.md` | BLOCKED |
| Validate #441 at PR #162 head passed 7/7 after one tablet focus-test rerun | GitHub Actions run `36304289366`, tablet report | FACT, that head only; stability finding |
| Exact owner devices, licensed media/provider fixtures and production-like environment | no attributable current session evidence | UNKNOWN / BLOCKED |

## Change

- `docs/testing/TEST_MATRIX.md`: profiles, fixtures, V1 journey IDs, evidence scope, run order and result rules.
- Canonical roadmap: record the merged operations round and the current test-planning round.

## Validation

- Local changed-file, Markdown/path, `git diff --check` and secret-pattern review: PASS on the task branch before PR creation.
- Exact PR-head CI: PENDING.
- Physical devices, real provider/media, signed release and staging restore: NOT RUN.

## Remaining work

Fix or diagnose the recurring tablet initial-focus test in a separate Android task. Select owner-approved physical devices and authorized fixtures, then record per-device results in the existing ledger. Merge of this documentation PR requires explicit owner authorization under `AGENTS.md`.
