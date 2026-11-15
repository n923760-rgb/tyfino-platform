# Favorites and History cached-search filtering

## Task identity and authority

- Task: TYFINO-20261114-filtered-cached-search; IMPLEMENTATION.
- Owner instruction: “تمام نكمل”, following the review's prioritized fixes; subsequently reaffirmed “تمام كمل”.
- Authorized scope: one bounded search correction, regression coverage, task branch/commit, push, PR and existing CI qualification.
- Protected authority: NONE. No merge, signing, release, deployment or publication.
- Execution: one agent plans, implements and self-reviews sequentially; this is not independent review.

## Verified source and live gate

- Repository: `n923760-rgb/tyfino-platform`; default/official branch `main`.
- Baseline: `4c7e019f7adbef4f7caf19fe498f4306a72973c6`; local clean main and live origin/main matched before worktree creation and at follow-up checks.
- Task branch: `fix/filtered-cached-search-2026-11-14`, isolated worktree `/workspace/work/tyfino-filtered-search`.
- Open PR232 is separate documentation-only release preparation against the unchanged candidate. It does not overlap this implementation.
- Baseline official-source Validate43584042490: all seven jobs SUCCESS, verified from live job metadata. This is baseline evidence, not proof for this change.
- Read current AGENTS, project profile, canonical roadmap, Android README, catalog contract, environment/task protocols and central Master Governance. No source, ownership or contract stop condition applies to this bounded correction.

## Confirmed problem

`CatalogRepository.searchCached` requested 51 rows and retained at most 50 across the entire section. `CatalogScreen` then applied Favorites/History membership. A matching selected item alphabetically beyond those global rows was absent, even though it was in the downloaded catalog and selected filter. The UI could show an empty search incorrectly.

An extracted-SQL reproduction on the baseline used synthetic catalog entries and a favorite/history ID outside the first 50 global matches. It confirmed the missing result without provider requests. This is SQLite/logic evidence, not Android runtime proof.

## Correction and boundaries

- Optional selected IDs reach the existing account/section query; parameterized `IN` membership applies before ordering and LIMIT.
- Null means All. An empty selected set returns no matches, never All. Membership is bounded to 200 IDs, each at most 256 code points, matching existing Favorites/catalog lookup bounds and covering the smaller History scope.
- Screen effects requery when the selected membership changes. Each repository/section/query/membership snapshot owns its own result cell, containing delayed completions.
- Existing account-generation checks after reads, active snapshot/category joins, literal `instr` matching, 80-code-point query bound, two-character threshold, 250ms debounce and 50-result UI ceiling remain.
- No provider scan/network fetch, new persistence schema, layout, focus policy, player, licensing, backend, dependency, workflow, identity or release change.

## Regression coverage

- Three JVM cases: scope before limit for all three sections including empty/limited sets; invalid/oversized membership; account replacement during a filtered read. Existing search cases are retained.
- Two real Android SQLite cases: IDs outside the global limit, empty sets, limit/account/section/category isolation, and bound literal SQL-like IDs/wildcard characters.
- Two actual Compose screen journeys: English/LTR and Arabic/RTL with real isolated SQLite partitions and 120 matching titles. Enter one query, switch All/Favorites/History without editing it, return to All, then remove the only matching favorite and require empty results. Assert zero provider requests. Dispose before partition cleanup.
- All 168 baseline native cases retained; expected total 172 per managed device.

## Validation and result

- PASS: local Python/SQLite extraction of actual before/after query templates. Selected IDs beyond the global limit now appear; All matches are unchanged; empty/max-200/51-row scopes, account/section/removed-category/current-generation isolation and literal bound parameters pass.
- The first extraction-harness attempt failed because the Python interpolation did not expand the nested membership column name. Correcting the harness produced the PASS above without changing product SQL.
- PASS: all six existing governance validators (environment, task/result, executor policy, behavioral records, CI qualification, governance adversarial suite).
- PASS: local `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug` on the six changed product/test files. BUILD SUCCESSFUL; 207 JVM cases, zero failures/errors/skips, including all three new filtered-search cases. Native test source compilation and Debug lint passed. This command does not execute native UI tests.
- PASS: full nine-file source/test/document diff self-review, whitespace and accidental-file/secret review. Only isolated synthetic test data is added.
- Native execution: BLOCKED locally because `/dev/kvm` is unavailable. Hosted exact-task-head seven-job Validate, 172 cases per managed device and configured API33 smoke remain PENDING until recorded in the PR. Baseline results are not substituted.

## Environment and evidence limits

Shell/Git/Python/SQLite and the connected GitHub API are available. Initially only JDK21 was installed and no SDK/adb/emulator was present. Official Temurin17.0.21+9 and Android command-line tools were installed under the session's `work/android-tools/` with published archive checksum verification. The Adoptium API returned HTTP403; official GitHub release metadata/downloads worked. SDK37.0 and Build Tools36 (selected by AGP) were provisioned; Gradle9.6.0 used the wrapper's pinned checksum. Initial SDK setup hit an unwritable default user directory; setting the supported Android user path inside work resolved it. Initial wrapper fetch lacked Java proxy configuration and failed with Connection refused; preserving the inherited proxy route in user-owned Gradle properties and using the system CA trust store resolved it without TLS bypass. The build then succeeded on JDK17. Project dependencies/configuration remain unchanged. Exact task SHA and external native results belong to the PR.

Local reproduction and scripts are under the session's `work/` area and use synthetic, non-sensitive data. They do not qualify physical TV/phone input, actual TalkBack output, OEM codecs, real providers or performance. Those remain separate runtime gates. No protected operation is performed.

## Completion criteria and next action

The correction must make matching selected items visible regardless of their global-search position, keep an empty filter empty, retain All/local/owner/result bounds and pass applicable exact-source tests. Review every changed file before pushing. Use the existing seven-job Validate, phone/tablet instrumentation and configured API33 installation smoke; inspect any actual failure before a correction. Leave the PR unmerged for owner review.
