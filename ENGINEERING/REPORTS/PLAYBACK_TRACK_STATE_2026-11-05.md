# Audio/subtitle action state descriptions

Task TYFINO-20261105-track-action-state — IMPLEMENTATION. Controller/executor: Codex, sequential self-review.
Owner requests continued development; standing reviewed-merge authority applies after successful gates. Physical TCL check remains explicitly deferred.

Verified repository/default branch n923760-rgb/tyfino-platform, main; clean official/local base1fed87be68911f9d9d60a747738144b02dab8883. No open/conflicting PRs. Isolated branch improve/playback-track-state-20261104. Base treea78490795f6b1027038d51e62412f3831e522f51 matches qualified #217, Validate41832919053 PASS seven jobs,147 cases/device and API33 install smoke. Official-main run41835829891 was still running at the initial gate, not reported PASS.

Scope: audio/subtitle icon state descriptions from current player-owned policy and existing bounded display labels; two native presentation/input cases; report and canonical roadmap. No selection authority, player/network/lifecycle changes, new persistence/provider data, dependencies, CI profiles, identity/endpoints, signing/release/deployment. Stop on source drift, conflicts, secrets or required failures.

The icons previously exposed only their action names. The production PlaybackTrackAction now exposes localized Automatic/Off or the already-displayed selected label via stateDescription. Unknown manual selection omits the state rather than inventing Automatic. Existing action labels, layout and callbacks remain. The small presentation component follows the existing PlaybackAspectAction pattern. No effect requests focus or changes playback.

Two native LTR/RTL cases exercise the same production action through Automatic/manual/Off/unknown transitions, retained focus, no callback until Enter, exactly one callback on Enter, directional movement and no focus reset after later updates. These are semantics/input assertions, not audible TalkBack or physical TV qualification. Retain147 existing cases,149 expected/device.

Execution: local Git/source/governance checks and authenticated repository API; hosted CI supplies native execution. Local Gradle/SDK/emulator unavailable. Require six governance validators, full scoped self-review/diff check, seven exact-head CI jobs,149 native cases/device and API33 configured-APK install smoke. Physical TV/provider/accessibility/performance and release gates remain NOT RUN. Exact-source results belong to the task PR.
