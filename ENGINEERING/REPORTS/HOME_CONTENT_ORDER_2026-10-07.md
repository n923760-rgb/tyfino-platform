# Android Home content order — 2026-10-07

Task ID: android-home-content-order-20261007
Type: IMPLEMENTATION
Controller/executor: current authorized engineering session.
Repository: n923760-rgb/tyfino-platform
Official branch/base: main@a125ced5337c5304258f7ee81419817de5e8f97a
Base tree: 2c787ef3e92b1d5554f187f17e5ca9cc494e6921
Isolated branch/worktree: fix/home-content-order-20261007, tyfino-home-order.
Report is the task/result record; exact final source/CI/merge/artifact evidence belongs to the task PR to preserve the tested source.

## Authorization and live gate

The owner requests continued implementation, error correction and interface development without repeated pauses. Prior explicit reviewed-merge authorization continues. The existing owner request and Android README place the showcase above Continue Watching, then latest Movies/Series, browsing shortcuts and recent history.

Current owner instruction, root AGENTS.md, project profile, canonical roadmap, Android README and relevant catalog/account/playback ownership contracts were read. Main identity was reverified after #188 merge; its tree equals the qualified details source. Open-PR API returned no PRs. Baseline [PR Validate37662573296](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37662573296) passed all seven jobs,103 cases per managed device with zero failures/skips and exact configured owner APK offline API33 install/launch. Official-main push validation is a separate run. No baseline pass is presented as changed-source validation.

Execution: local Git/source/shell/JDK17 and authenticated GitHub connector/Actions. Local Android SDK/emulator and usable Gradle distribution remain unavailable. No new provisioning or CI policy change.

Scope: one existing Home LazyColumn item relocation and the matching documentation/roadmap/result record. Production deployment, release/tag/signing, secrets, application identity, branding, SDK/ABI/version/origin, provider transport, persistence, licensing and playback policy remain outside this task.
Stop: changed/conflicting source, secrets, causal required failures, protected scope expansion or unsupported runtime claims.

## Finding and change

FACT: HomeScreen.kt inserts the browse item immediately after featured, before continue. This contradicts the documented/requested order. The content exists and uses the correct callbacks; only its placement requires correction.

Move the same three-line browse item below latest-series and before recent-live. The result is showcase → eligible Continue Watching → eligible latest Movies/Series → browsing shortcuts → eligible recent history. Empty-content guidance still uses its existing condition, and shortcuts remain available on empty Home.

No item body, eligibility condition, callback, resource, input target, focus group, initial-focus request, remember state, owner, data-loading path or LazyColumn scrolling code changes. All103 native test cases remain byte-for-byte unchanged. No new test mirrors this reversible three-line item move; minimal source-order and full callback-preservation review provide the deterministic proof, followed by the existing native regression suite.

## Validation and result

PASS locally before remote creation: complete scoped diff/whitespace review; exact single unchanged browse-block relocation with all other Home source retained; intended four-file scope; byte-for-byte unchanged test directories with103 native cases; all six governance validators, including the six adversarial qualification checks. git diff --check and the test-directory comparison exited0. Exact final source/results are recorded on the PR.
Required changed-source CI: all seven Validate jobs; Android debug/optimized unsigned-release/unit/lint;103 cases each phone/tablet with zero failures/skips; exact configured owner APK offline API33 clean-install/launch.
Local native execution: BLOCKED by the previously established SDK/emulator/Gradle limitations; no local native PASS claimed. Physical LG Velvet/TV/foldable, TalkBack, real-provider/media and performance qualification: NOT RUN in this round.

Next: review and qualify the exact branch, integrate only after required checks pass, verify merged tree identity, then deliver the qualified configured owner-test Debug APK. This is a development artifact; production signing/custody/upgrade continuity and commercial release readiness are not established. productionReady=false.
