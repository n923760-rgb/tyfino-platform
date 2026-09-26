# TYFINO Master Engineering Roadmap

Canonical path: `/ENGINEERING/MASTER_ROADMAP.md`

Status: ACTIVE — RECONCILE WITH LIVE EVIDENCE

This is the single permanent engineering roadmap for TYFINO. Detailed reports belong under `/ENGINEERING/REPORTS/`; evidence indexes/metadata belong under `/ENGINEERING/EVIDENCE/`.

## 1. Current Verified State

Repository: `n923760-rgb/tyfino-platform`
Official branch: `main`
Baseline official HEAD: `bcfa27ed15fc5b074303d9bbefd53f8a67e2ed1a`
Baseline verified: 2026-09-26
Available migration execution mode: authenticated GitHub repository connector/API; no local shell/runtime proof in this round.
Active PR at baseline: #159 — Android Movies/Series sorting/history UI refinement.
Current engineering phase: V1 product refinement + release/physical qualification.

The baseline SHA is historical evidence for the re-baseline only. Re-query live `main` before every mutation.

## 2. Architecture

TYFINO is a multi-surface repository containing:

- native Android IPTV player under `apps/android/`;
- licensing/backend API under `apps/api/`;
- Admin web application under `apps/admin/`;
- PostgreSQL/database controls under `database/`;
- deployment/infrastructure assets under `infrastructure/` and `docker-compose.yml`.

Authoritative architecture, data-ownership, security, deployment, Android decisions, and subsystem contracts remain in existing project documentation and are not duplicated here.

## 3. Closed Historical Work

- Repository foundation and previous governance qualification completed under the older governance baseline.
- Core Xtream authentication, multiple accounts, categories, favorites, recent history, resume/Continue Watching, Movies/Series details, playback, track selection, previous-live behavior, and on-demand Live EPG are implemented with repository evidence.
- Activation-code licensing with seven-day trial, one-year/lifetime codes, installation binding/reset, refresh/offline constraints, and Admin licensing controls are implemented.
- CI covers API/Admin/database/Android build and managed-device validation surfaces.

Historical PASS does not replace current-task verification.

## 4. Current Findings

FACT:
- The repository had no canonical `/ENGINEERING/` structure before this governance migration.
- Root `AGENTS.md` and a substantial TYFINO-specific `governance/` layer already existed.
- `main` was not branch-protected at the 2026-09-26 baseline query.
- PR #159 was open and scoped to Android Movies/Series UI behavior.
- Physical TV/foldable/low-memory/real-provider/accessibility/performance qualification remains separate from source/emulator evidence.

UNKNOWN / MUST VERIFY LIVE:
- Current branch-protection/ruleset state after this baseline.
- Current production host/deployment state.
- Current off-host backup freshness and restore proof.
- Current signing-key custody/recovery evidence.
- Exact physical-device qualification state unless a newer attributable report exists.

## 5. Release Blocker Map

1. Exact-release signing and owner-controlled signing-key backup/recovery evidence.
2. Production operations qualification: secrets, monitoring, external audit anchoring, backup/restore, rollback, log retention/rotation, production thresholds.
3. Physical-device/provider/media qualification across required device/input/RTL/accessibility/performance surfaces.
4. Any OPEN product/retention decisions that affect production behavior.

## 6. Qualification Gaps

- Physical Android TV / Google TV evidence.
- Low-RAM/API-24-class evidence.
- Foldable/resizing hardware evidence.
- Real-provider/media error and lifecycle evidence.
- RTL/TalkBack and mixed-language evidence.
- Performance/startup/scrolling/playback/memory measurements on target hardware.
- Production operational evidence.

## 7. Ordered Engineering Gates

1. Repository/governance live gate.
2. Bounded product task with isolated branch/PR.
3. Smallest deterministic source/test proof.
4. Relevant CI regression proof.
5. Runtime/device proof when behavior cannot be established by source/CI.
6. Exact-release candidate freeze.
7. Signing/release evidence.
8. Owner release/deployment decision.

## 8. Runtime Gates

Runtime claims require attributable device/environment evidence. Emulator CI does not qualify physical TV, real providers/media, low-memory behavior, accessibility, RTL, or performance.

## 9. Release Gates

No release or production deployment is authorized by this roadmap.

Before release:
- exact candidate SHA must be identified;
- required CI must pass on exact source;
- required physical/runtime qualification must be complete;
- signing custody/recovery must be proven;
- protected signing must be explicitly authorized;
- release artifact checksum/certificate must be captured;
- owner must explicitly authorize release/publication/deployment.

## 10. Owner Decisions

Protected decisions remain owner-controlled:
- merge;
- signing;
- release/tag/publication;
- production deployment;
- destructive infrastructure/database/security operations;
- identity/branding/signing-policy changes.

## 11. Deferred Post-Release Work

Existing deferred scope such as M3U, Stalker/MAC Portal, downloads, cloud sync, user/kids profiles, social features, provider-wide search, full EPG synchronization, CRM, analytics, and Kubernetes remains deferred unless separately approved.

## 12. Exact Immediate Next Round

Task: qualify the governance-migration Pull Request on its exact head, review its full diff, and make an owner-controlled merge decision.
Why now: the canonical roadmap/report/evidence structure must land before future governed engineering rounds rely on it.
Required authority: repository mutation already authorized for this governance change; merge requires explicit current owner authorization.
Required execution capability: GitHub PR/CI inspection.
Expected evidence: exact PR head, changed-file diff, CI results, no source/product behavior changes.
Stop conditions: unexpected base/head movement, overlap with unrelated PR scope, failing governance-related checks, secret exposure, or unreviewed unrelated files.

## Linked Reports

- `/ENGINEERING/REPORTS/MASTER_ENGINEERING_BASELINE_REPORT.md`

## Linked Evidence

- `/ENGINEERING/EVIDENCE/README.md`
