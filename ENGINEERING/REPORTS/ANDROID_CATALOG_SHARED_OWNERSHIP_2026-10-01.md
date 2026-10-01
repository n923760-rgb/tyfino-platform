# Android catalog shared operation ownership — 2026-10-01

## Task packet
IMPLEMENTATION with deterministic diagnosis first. Repository n923760-rgb/tyfino-platform; official main; starting SHA cd3f9a8c219e188f9d51c6d2c6502ffb730aef2d; work branch fix/catalog-shared-ownership-2026-10-01; PR #175.
Owner requested review/continuation and earlier explicitly authorized improvements and reviewed merges. Root AGENTS, central governance, project profile, canonical roadmap, architecture/security/data ownership, Android README, catalog/account and task/environment contracts read. Live identity/default/main/open-PR/branch/ruleset/capability gates passed.
Execution: authenticated GitHub API isolated branch; no local shell/JVM/Android runtime. Existing exact-source Validate executes tests.
Five files: CatalogRepository.kt, CatalogRepositoryTest.kt, Android README, canonical roadmap and this report.
Stop on unexpected source movement/conflicts, unrelated scope, secret exposure or missing required evidence. No signing, release, production or identity/version/ABI/endpoint change.

## Diagnosis
TyfinoApp and NewContentCheckJob construct separate CatalogRepository/SQLiteCatalogStore instances for the same database in the default app process. Replacement is transactional, but mutex/latest-operation map were instance-scoped.
Test-only head 7b3114b3501eeb2765fc7c1f7ea2eb51653b381a changed only CatalogRepositoryTest.kt, preserving product source.
[Validate 36798162851](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36798162851) completed FAILURE. Android job 110166263879 built Debug/optimized unsigned Release and executed 181 JVM tests, exactly two failures:
- separateRepositoriesDiscardOlderCategoryCompletion, comparison failure line 186;
- separateRepositoriesDiscardOlderItemsAndMovieAlert, assertion failure line 210.
Each starts an old request, waits for its start, commits newer work through another repository, then releases the old response. Snapshot assertions demonstrate older records replacing newer categories/items. Different-key control and the other six jobs passed. CompletableDeferred controls ordering; no sleeps/provider/production data.
This diagnostic red head is not a merge candidate. First causal failure is the deterministic JVM snapshot assertion.

## Correction
Share the catalog operation registry and mutex in the class's default-process owner. Stable Account ID/section/category keys remain. Local preparation, commits and publication checks serialize; network fetches remain concurrent outside the lock.
Unique object tokens replace resettable per-key counters so clearing a key cannot resurrect pending work by reusing its ID.
Preserve account/revision checks. Cache-first/loading/final publication and movie alerts recheck ownership under the shared lock. Rejected completions silently produce neither replacement nor obsolete state/alert.
SQLite schema/transactions, transport/freshness/resource limits, provider boundary and callbacks remain. No dependencies, backend/Admin, Series, signing or deployment changes.

## Validation
PASS preparation source review: original ten cases preserved, three diagnostic cases retained, fourth covers cross-instance clear/new work/old completion. Fourteen CatalogRepository cases expected.
Corrected-head Kotlin/JVM/build/lint and seven-job exact-source Validate evidence is recorded on PR #175; merge uses expected head, followed by official-main CI. Local execution NOT RUN.
Physical LG Velvet/API33 install, provider/media, TV/TalkBack/low-memory/performance and alternate multi-process configuration NOT RUN.
Two large tree-save requests were interrupted without moving the branch. Smaller sequential tree writes prepared the same reviewed five-file change; no ordinary direct main push.

## Integrated state and following work
#166–#174 are merged; official-main Validate 36781939130 passed all seven jobs.
Owner reports LG Velvet/Android13/no existing TYFINO and "App not installed". Exact filename/installer cause UNKNOWN. Standard CI Debug has empty licensing origin, so it cannot qualify end-to-end activation. This correction does not claim to fix installation. Inspect exact APK metadata/signature and prepare configured owner-test artifact separately.
Analogous Series cross-instance concern remains INFERENCE needing its own deterministic diagnosis.
