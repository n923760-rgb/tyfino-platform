# Home and playlist input/lifecycle qualification

## Task and authority

Task ID: `home-playlist-input-lifecycle-2026-10-02`. Date: 2026-10-02 UTC.
Repository: `n923760-rgb/tyfino-platform`; official branch: `main`.
Starting official SHA: `f8534011a15d1167383995abd6a42b8f58fbc4fd`.
Task branch: `test/home-playlist-input-lifecycle-2026-10-02`; isolated scratch worktree: `tyfino-qualification`.
Controller/executor: Codex. Task type: IMPLEMENTATION (tests and engineering record only).

Current owner asks to continue after explicitly authorizing #179 merge. Scope is bounded automated qualification of that newly integrated Home/Settings feature. No new merge, signing, release, deployment, provider access or identity change is authorized/performed in this continuation.
The task packet was recorded in the execution context before source edits. Live repo/default/main, open PRs (none), clean worktree, root authority, central governance, project profile, roadmap, Android README, environment/task protocol, device/series/account contracts and relevant source were inspected.

## Verified baseline

FACT: #179 is merged. Its qualified PR head `25b8b18a996fc1ef0b770ce46f6a0e75d990b80f` and official squash commit have the identical tree `4cbe83e9127272d12666baff92199ca4659da0a9`.
PASS: [PR Validate 37040552329](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37040552329) and [official-main Validate 37043350131](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37043350131), all seven jobs. Official-main phone job 110961504901/tablet job 110961504860 each ran 76 tests, failed=0/skipped=0. Phone also passed exact configured APK clean-install and offline launch on API33 without test-only flags.
Earlier showcase sizing failure and correction remain in the original [feature report](HOME_SHOWCASE_PLAYLIST_2026-10-02.md); they are not hidden or attributed to this test-only source.

## Changes

- Showcase keyboard test establishes actual window focus and keyboard input mode, moves between Previous/Next using directional keys, selects Series without playback, returns to its Details action and opens the correct Series with Enter. Touch mode is restored in `finally`.
- Mixed resume rail test uses matching Movie/episode provider IDs and verifies independent actions, episode progress semantics and the exact unchanged episode record, including account/Series generations and resume position.
- Playlist lifecycle test provides an actual `LifecycleRegistry` owner. STARTED causes no query, RESUMED shows metadata, pause clears identity/status, a delayed non-cancellable old provider operation cannot replace a later resumed Active result. Deferred barriers determine ordering; no arbitrary sleeps.
- Disposal test removes the actual summary from composition while provider work is pending, releases that work and verifies the destination/metadata remain absent.

No production code, persistence, licensing, provider transport, resources, dependencies, workflows, package/SDK/ABI or signing policy changed. All 76 original native cases remain; four additions target a total of 80 per managed device. Synthetic fixtures use reserved `.example` origins and non-production credentials; no real provider is contacted.

## Execution and validation

Execution mode: local shell/Git/JDK17 source work plus authenticated GitHub API and existing exact-source Actions. Git fetch is available; GitHub API is used for branch/PR publication. Android SDK and local Gradle distribution/cache are unavailable.

- PASS: `git diff --check` after source edits.
- PASS: `bash governance/scripts/validate-engineering-environment.sh`.
- PASS: `bash governance/scripts/validate-task-result-protocol.sh`.
- BLOCKED: `./gradlew --no-daemon :app:testDebugUnitTest`, exit 1 before build/test execution: pinned Gradle 9.6.0 distribution download fails `Network is unreachable`. This is environment provisioning, not a test failure; no local compile/native PASS is claimed.
- Exact new-head seven-job validation, four focused scenarios, total counts and first causal failure if any are recorded on the task PR after publication. This document is a source checkpoint, not a claim that pending CI passed.

PASS: full staged diff/source fixture consistency review and final whitespace check; four scoped files only. Resulting exact source/tree/PR evidence is attached to that PR. No successful baseline workflow was rerun and no CI configuration changed.

## Evidence boundary and next action

### First native result and test-oracle correction

FAIL: initial head `fc5595480b061748f92107bb9b372ac9bbd1d399`, [Validate 37074530554](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37074530554), phone job 111061742412: 80 cases, one failure, zero skips; all five non-native jobs passed. Tablet results belong to the same run record. Full causal build/test logs and report artifact 11256199646 were inspected; XML failure is at `PlaylistLifecycleTest.kt:57`, the first Active assertion.

FACT: the actual semantics text is `Provider status: Active`, correct for the fixture. The test used `assertTextContains("Active")` without `substring = true`; this Compose assertion defaults to comparing a complete text entry, not a substring. Thus the new test oracle failed before exercising the subsequent lifecycle steps. This is a test defect, not evidence of a product subscription defect.
Correction uses `assertTextEquals(targetContext.getString(R.string.playlist_active))` for all three Active checks, matching the complete localized resource. No production fix, weakened substring oracle, test removal/skip, or workflow change was made. All four new cases and lifecycle barriers remain. Corrected exact-head outcomes are retained on the PR; initial FAIL is not replaced with PASS.

These tests qualify only synthetic phone/tablet Compose input and destination lifecycle behavior. Non-cancellable fixture work models delayed completion; it does not prove that a real blocked socket immediately disconnects on cancellation. Cancellation plus repository/UI ownership remain production safeguards; this round does not independently bypass cancellation to test a UI generation guard in isolation.
Physical LG Velvet/TV/older/16KB hardware, actual provider/media, TalkBack, RTL traversal, memory/performance and production operations are NOT RUN. Source-only tests do not change that status.
Next: review exact-head CI and request owner authorization for this test-only PR merge when qualified. Continue owner interface trial/physical qualification on the already delivered configured #179 development APK; production signing and release remain separate gates.
