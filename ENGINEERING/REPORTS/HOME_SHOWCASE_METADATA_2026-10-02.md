# Cached Home showcase metadata

## Task and authority

Task ID: `home-showcase-metadata-2026-10-02`; date: 2026-10-02 UTC.
Repository: `n923760-rgb/tyfino-platform`; official branch: `main`.
Starting official SHA: `75efab30b0fff61c3fce931715226a5afd6346f6`.
Task branch: `feat/home-showcase-metadata-2026-10-02`; isolated scratch worktree: `tyfino-metadata`.
Controller/executor: Codex; type: IMPLEMENTATION.

Owner explicitly requests #180 merge and continued improvements following the Home/playlist redesign. #180 was merged through its exact qualified head guard; the new official tree equals the qualified PR tree. Continued scope is one bounded Home presentation enhancement. No protected future merge, signing, release, deployment, identity change or provider access is authorized/performed by this enhancement task.
Live task packet was recorded in execution context before source edits. Repo/default/main/open PRs, applicable root/central authority, profile/roadmap, Android README/catalog/account/series/device/environment contracts, source and actual execution capabilities were inspected.

## Requirement and ownership

Selected Movie/Series should expose useful optional rating/year context without fetching additional metadata or scanning new catalogs. Account-owned catalog snapshots and the existing bounded Home summary remain authoritative; showcase selection owns only rendering. There is no new asynchronous operation, store or cache.

## Changes

- Pure metadata projection accepts finite 0–10 provider ratings; normalizes negative zero; accepts only trimmed four-digit ASCII years from 1000–9999. Invalid/missing fields disappear independently. Year shape validation is not verification of release accuracy; no current-year assumption or title parsing.
- Existing hero displays noninteractive dark outlined badges beneath the title. Provider rating is explicitly attributed (not IMDb/TYFINO), numbers use the current UI locale and at most one fractional digit, years disable thousands grouping. Two badges wrap naturally with available width and can grow at large font size; no fixed-height or extra focus targets.
- Two JVM tests cover valid, partial/missing, non-finite/out-of-range ratings and malformed year text. Two added managed-device cases verify metadata follows Movie/Series selection and invalid/partial fields do not block Details. Existing compact Arabic/font1.6 test additionally reaches both metadata badges before Next/Details; directional, resume, playlist and other existing cases remain (82 total expected).
- Paired Arabic/English strings; README and canonical roadmap reconciliation.

No network, dependencies, workflows, catalog ranking, persistence, licensing, playback callbacks, provider transport, package/SDK/ABI, signing policy or timers change.
Official [FlowRow reference](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/FlowRow.composable) confirms the stable no-overflow overload and width-based LTR/RTL wrapping; native layout proof is still separate from API documentation.

## Validation at source checkpoint

- PASS: full scoped source/fixture/API review and `git diff --check`.
- PASS: parsed both XML resource sets; equal keys, new positional-format parity and all Kotlin string references resolve.
- PASS: `bash governance/scripts/validate-engineering-environment.sh` and `bash governance/scripts/validate-task-result-protocol.sh`.
- BLOCKED: `./gradlew --no-daemon :app:testDebugUnitTest`, exit1 before compilation/testing, pinned Gradle9.6.0 download fails `Network is unreachable`. Local Android SDK/Gradle cache/Kotlin compiler are unavailable. No local JVM/native PASS claimed.
- NOT RUN at this checkpoint: exact new-head native build/lint/units/phone/tablet; existing seven-job Actions is final confirmation after reviewed source publication. Exact head/tree/run/job outcomes and any causal failure will be retained on the task PR.

Baseline [#180 Validate37075277135](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37075277135) passed7 jobs/80 tests per device0fail0skip/API33 exact configured artifact install/launch on qualified head `88a3a9b1aed3f90dee22c452ebdc0a19c95e32a1`; integrated tree `7d5eb748b3026f5e23ebc5e665654ad91d927a11` is identical. Baseline is not a metadata-enhancement PASS. Official-main Validate37076738964 was in progress and is recorded separately on #180; no passing workflow was rerun.

## Evidence boundary and next action

Physical LG Velvet/TV/older/16KB devices, real provider/media, TalkBack, RTL traversal and performance/operations remain NOT RUN. Synthetic Compose phone/tablet assertions and source/API documentation cannot qualify those gates.
Next: exact-head CI and diff review, then owner exact merge decision and UI trial. Any configured development APK is not a signed commercial release; no signing/publication/deployment was performed.
