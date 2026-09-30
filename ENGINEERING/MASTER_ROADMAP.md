# TYFINO Master Engineering Roadmap

Canonical path: `/ENGINEERING/MASTER_ROADMAP.md`

Status: ACTIVE — RECONCILE WITH LIVE EVIDENCE

2026-09-30 clipboard round: a bounded Admin correction awaits clipboard completion before success, handles denied/unavailable writes with manual-copy guidance, and disables duplicate clicks while pending. Source baseline is `main@ef420e7e9c0d968f271ada14bce1ab3076a655c7`; exact-head CI evidence belongs to the task PR. Details: [Admin clipboard result](REPORTS/ADMIN_CLIPBOARD_RESULT_2026-09-30.md). PRs #166 and #167 remain separate, unmerged changes.

This is the single permanent engineering roadmap for TYFINO. Detailed reports belong under `/ENGINEERING/REPORTS/`; evidence indexes/metadata belong under `/ENGINEERING/EVIDENCE/`.

## 1. Current Verified State

Repository: `n923760-rgb/tyfino-platform`
Official branch: `main`
Baseline official HEAD: `cd83a1c23819ae972185c748e7487156890a7dbf`
Baseline verified: 2026-09-27
Available execution mode: authenticated GitHub repository connector/API plus local source checkout; no physical-device or production-runtime proof in this round.
Active product PR at baseline: #159 — Android Movies/Series sorting/history UI refinement.
Current engineering phase: V1 product refinement + release/physical qualification.

The baseline SHA is historical evidence for the re-baseline only. Re-query live `main` before every mutation.

### 2026-09-30 live reconciliation

Inspected official source: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`.
Execution mode for this round: authenticated GitHub API/MCP plus JavaScript source-level checks; existing GitHub Actions provides build/integration validation. Local shell/PostgreSQL/Android/browser/physical-device execution is unavailable.
No open PRs were returned at this checkpoint. PRs #159, #164 and #165 are merged.
Validate run 36665625417 and device-test APK run 36666243511 succeeded on this official source. These are source/build/emulator evidence only.
The earlier baseline SHA and active-PR entries above are historical context.

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
- The canonical `/ENGINEERING/` structure is now present after governance migration PR #160.
- Legal/security documentation PR #161 was merged after exact-head Validate #439 succeeded on its second attempt; the first attempt had one tablet emulator focus assertion failure, so test stability remains a separate observation.
- Operations/performance documentation PR #162 was merged after exact-head Validate #441 succeeded on its second attempt; the same tablet focus assertion failed in the first attempt and remains a separate test-stability finding.
- V1 test-matrix documentation PR #163 was merged after exact-head Validate #443 passed all seven jobs on its first attempt; physical/device qualification remains open.
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

### 2026-09-30 review findings

- FACT: First-use activation expiry was incorrectly applied to already activated grants. A bounded correction and PostgreSQL integration regression coverage are prepared on `fix/licensing-first-use-expiry-2026-09-30`; this is not yet merged into official source.
- FACT: Admin code creation/settings changes and their audit events use separate commits.
- FACT: Clipboard success is shown before the write promise resolves.
- FACT: Dashboard counts use at most 500 newest codes and persisted status without grant-expiry classification.
- FACT: Admin modal source lacks Tab containment/background inertness/focus restoration; browser evidence is NOT RUN.
- INFERENCE: Separate foreground/background catalog repository instances need a deterministic same-account stale-write diagnosis.
- UNKNOWN / BLOCKED: Physical-device, real-provider/media, browser-accessibility and production qualification have not been performed in this round.

Details: [2026-09-30 source review and correction](REPORTS/PROJECT_REVIEW_FIRST_USE_EXPIRY_2026-09-30.md).

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

Admin dashboard review (2026-09-30): a bounded follow-up on official base `ef420e7e9c0d968f271ada14bce1ab3076a655c7` replaces counts inferred from 500 rows with a full-table authenticated summary and serial PostgreSQL regression coverage. Detailed scope, validation limits and backend-first rollout are recorded in `/ENGINEERING/REPORTS/ADMIN_DASHBOARD_STATISTICS_2026-09-30.md`. Exact-head CI evidence belongs to the PR; merge/deployment and runtime qualification remain separate.

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

Task: complete exact-head validation and review of the first-use activation-expiry correction, then address transactional Admin business/audit writes in a separate bounded round.
Why now: source inspection and isolated handler checks reproduce rejection of valid activated grants after the original first-use deadline. The remaining Admin transaction finding can lose auditability or a one-time activation code when audit insertion fails.
Required authority: the owner's current request authorizes review and improvements to the whole project. Merge remains separately protected.
Required execution capability: GitHub repository API/MCP and existing exact-head Validate workflow for the licensing correction; PostgreSQL failure-injection tests for the later transaction correction.
Expected evidence: isolated handler proof, integration coverage for expired unused/activated one-year/lifetime/device/reset/revocation/paid-expiry behavior, full scoped diff, and exact-head CI run results.
Stop conditions: unexpected official/head movement, conflicting PR, failing affected tests, secret exposure, unsupported runtime claims or unrelated changes.

## Linked Reports

- `/ENGINEERING/REPORTS/MASTER_ENGINEERING_BASELINE_REPORT.md`
- `/ENGINEERING/REPORTS/OPERATIONS_PERFORMANCE_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/TEST_MATRIX_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/API_ADR_CONVENTIONS_UX_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/PROJECT_REVIEW_FIRST_USE_EXPIRY_2026-09-30.md`

## Linked Evidence

- `/ENGINEERING/EVIDENCE/README.md`

## 2026-09-30 Admin audit atomicity round

Official source inspected: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`. PR #166 remains open for the earlier first-use activation-expiry correction; its source is not part of official `main`.
Owner authorization: review/improvement and continued engineering work. No protected merge/release/deployment authority is granted.

FACT: Admin activation-code creation and settings changes used separate commits for the business write and audit insertion. A bounded correction on `fix/admin-audit-atomicity-2026-09-30` places each pair in one transaction and rolls back failed code-generation attempts.
PASS: extracted-handler JavaScript checks reproduce partial writes on old source and verify rollback/retry/release behavior on the correction.
Exact-head PostgreSQL/Fastify integration and build results belong to the task PR/CI record; local shell/PostgreSQL checks are NOT RUN.
Report: [Admin audit atomicity](REPORTS/ADMIN_AUDIT_ATOMICITY_2026-09-30.md).

Immediate next round: finish exact-head validation/review of this correction, then address Admin clipboard success/error handling in a separate bounded change.
