# MASTER ENGINEERING BASELINE REPORT — TYFINO

Date: 2026-09-26
Controller: Engineering Controller
Repository: `n923760-rgb/tyfino-platform`
Official branch: `main`
Verified official HEAD: `bcfa27ed15fc5b074303d9bbefd53f8a67e2ed1a`
Verified execution mode: authenticated GitHub repository connector/API
First-round inspection mode: READ-ONLY
Persistence note: this report was written only in the subsequently authorized governance-mutation round.

## A. PROJECT IDENTITY

FACT — TYFINO is an Android IPTV player, licensing/backend platform, and Admin surface in one repository.

FACT — official/default branch is `main`.

## B. LIVE REPOSITORY STATE

FACT — baseline `main` HEAD was `bcfa27ed15fc5b074303d9bbefd53f8a67e2ed1a`.

FACT — open PR #159 existed at baseline and was scoped to Movies/Series sorting/history UI changes.

FACT — GitHub reported `main` as unprotected at baseline.

## C. CURRENT CANONICAL SOURCE

FACT — live repository source on `main` is authoritative over historical chats, prior PRs, screenshots, archived SHAs, and AI memory.

FACT — no `/ENGINEERING/` directory existed before this migration.

## D. EXISTING REPOSITORY GOVERNANCE

FACT — root `AGENTS.md` existed.

FACT — TYFINO already had project-specific governance covering project profile, environment, task/result protocol, AI executor qualification, CI qualification, governance qualification, repository foundation, resource map, and historical adoption evidence.

FACT — this migration preserves those controls and adds the central Master Engineering System's canonical roadmap/report/evidence model.

Central governance migration reference inspected: `n923760-rgb/engineering-governance@641e4f9e45da109257ba1f38752b94604c2e4531`.

## E. BUILD / RELEASE CONFIGURATION

FACT — repository workflows include:
- `.github/workflows/ci.yml`;
- `.github/workflows/device-test-apk.yml`;
- `.github/workflows/release-apk.yml`.

FACT — Android uses JDK 17 and committed Gradle/toolchain declarations according to the project environment contract.

FACT — protected signed APK generation is distinct from ordinary CI and remains owner-controlled.

## F. APPLICATION / SYSTEM ARCHITECTURE MAP

FACT — major surfaces:
- `apps/android/` — native Android player;
- `apps/api/` — licensing/backend API;
- `apps/admin/` — Admin web UI;
- `database/` — schema/backup/restore controls;
- `infrastructure/` + `docker-compose.yml` — deployment/runtime definitions.

FACT — authoritative architecture details live in `docs/architecture.md`, `docs/data-ownership.md`, `docs/security.md`, and subsystem contracts.

## G. AUTHORITATIVE STATE / OWNERSHIP MAP

FACT — Android IPTV provider credentials remain device/provider-owned and must not cross the licensing boundary.

FACT — repository contracts explicitly separate account-scoped catalog/history/favorites/playback state and licensing/admin state.

INFERENCE — future changes must continue to prove stale asynchronous results cannot overwrite newer account/destination/generation ownership.

## H. FEATURE / SUBSYSTEM INVENTORY

FACT — current V1 gap audit records most core V1 IPTV and licensing capabilities as implemented.

FACT — current active product work includes Android UI refinement.

BLOCKED / separately gated — physical-device/provider/media/accessibility/performance and production-operations qualification.

## I. PLATFORM / RUNTIME CONTRACT

FACT — Android baseline includes phone/tablet/foldable/TV-capable adaptive behavior with D-pad requirements.

FACT — managed-device CI is not equivalent to physical TV, low-memory, foldable, provider/media, RTL/accessibility, or performance evidence.

## J. TEST INVENTORY

FACT — repository contains Android unit/instrumentation coverage and CI jobs for API, Admin, database/backup, Android build/lint/unit tests, and managed-device instrumentation.

UNKNOWN — exact present test count was not treated as a stable governance fact and must be queried from the exact source/task when needed.

## K. CI / AUTOMATION INVENTORY

FACT — CI and manual APK workflows are source-controlled in `.github/workflows/`.

FACT — release/signing workflow existence does not authorize signing or publication.

## L. SECURITY / PRIVACY BOUNDARIES

FACT — project governance prohibits secret/signing-material exposure.

FACT — Android licensing/provider boundary is documented and tested.

FACT — production deployment and credential operations remain protected actions.

## M. CURRENT EVIDENCE COVERAGE

FACT — prior governance, CI, signing-workflow, and product implementation evidence exists.

FACT — historical evidence is attributable to historical exact sources only.

BLOCKED / UNKNOWN — current physical-device and production-environment evidence must be separately qualified.

## N. RISK / GAP LEDGER

RISK — `main` was unprotected at baseline; policy therefore relies on disciplined PR/protected-action enforcement unless repository rules are later enabled.

RISK — concurrent PR #159 means governance work must remain isolated and non-overlapping.

RISK — signing custody/recovery, production operations, and physical qualification remain release blockers.

RISK — old governance files could become a competing authority unless explicitly subordinated to the central Master Engineering System and one Master Roadmap.

## O. PROPOSED REPOSITORY AUTHORITY MODEL

Current owner instruction
→ nearest `AGENTS.md`
→ central Master Engineering System
→ `/ENGINEERING/MASTER_ROADMAP.md`
→ TYFINO architecture/security/data-ownership docs and scoped contracts
→ project environment/task-result controls.

## P. PROPOSED PROJECT-SOURCES MODEL

Use live repository source for project truth.
Use central `engineering-governance` only for reusable engineering policy.
Keep project-specific facts, decisions, reports, and evidence in TYFINO.

## Q. PROPOSED MASTER ENGINEERING ROADMAP STATE

Establish exactly one roadmap at `/ENGINEERING/MASTER_ROADMAP.md`.

Seed it with:
- current architecture map;
- historical closed work;
- current V1/release gaps;
- runtime/release gates;
- explicit protected owner decisions;
- exact next engineering round.

## R. ENGINEERING LAB PLAN

Use GitHub CI for deterministic repository build/test evidence where supported.

Use qualified physical devices/environments for behavior that CI cannot establish.

Do not infer production or physical-device readiness from source alone.

## S. REQUIRED LAB TOOLCHAIN FOR THIS EXACT REPOSITORY

Repository-declared selectors include Node.js, JDK/Gradle/Android tooling, PostgreSQL/Docker, and CI runner capabilities as defined by the live project environment contract and workflows.

Every execution round must verify the actual available environment before claiming execution.

## T. CONTROLLER / EXECUTOR OPERATING MODEL

Controller verifies live source, scope, authority, protected actions, evidence, and stop conditions.

Executor performs only the bounded authorized task and returns attributable evidence.

Technical access is not owner authorization.

## U. EVIDENCE / REPORT STORAGE MODEL

- Roadmap: `/ENGINEERING/MASTER_ROADMAP.md`
- Reports: `/ENGINEERING/REPORTS/`
- Evidence indexes/metadata: `/ENGINEERING/EVIDENCE/`
- Existing detailed task records may remain under `governance/task-records/` where already authoritative, linked rather than duplicated.

## V. OWNER-PROTECTED DECISIONS

Merge, release, tag, signing, publication, production deployment, destructive database/infrastructure/credential actions, history rewrite/force-push, repository deletion, and permanent release-artifact deletion.

## W. EXACT NEXT ENGINEERING ROUND

Create/review the isolated governance-migration PR, verify exact-head CI and full diff, then obtain an explicit owner merge decision.

No product code, release, signing, or production deployment belongs in this governance migration.
