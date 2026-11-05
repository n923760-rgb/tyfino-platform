# Expose explicit icon selection state

Task TYFINO-20261105-icon-selected-state — IMPLEMENTATION. Controller/executor: Codex, sequential self-review. Owner requests continued improvements with standing reviewed-merge authority after successful gates; TCL remains deferred.

Verified official/local clean source85e780f05bf9e681bb17d98adf16da69b76e6645, n923760-rgb/tyfino-platform, main. No open/conflicting PRs. Isolated improve/icon-selected-state-20261105 worktree. Base tree0eca067faf68f28648c8a4876c9ed851eaf6643e matches qualified #218: seven checks PASS,149 native cases/device and API33 smoke PASS. Earlier tablet attempt failed34 window-focus cases; unchanged-source isolated retry passed. No zero-defect inference.

Confirmed source gap: FocusIconButton.selected affects icon color but exposes no Selected semantics. Scope: distinguish an ordinary action from an explicitly selectable icon using a nullable selected parameter; publish true/false only for explicit callers. Preserve geometry, focus, callbacks and current colors. Strengthen existing category-search open/clear and picture-size journeys with selection assertions; retain149 native cases. No new test mirroring the helper, no new dependencies or provider data.

Excluded: network/player/selection authority, persistence, endpoints/identity, CI profiles, signing/release/deployment. Execution: local Git/source/governance checks and authenticated repository API, hosted CI; local Android SDK/Gradle/emulator unavailable. Require six governance validators, full diff/self-review, seven exact-head Validate jobs,149 native cases/device and API33 installation smoke. Stop on drift, conflicts, secrets or required failure. Physical TV/TalkBack/provider/performance remain NOT RUN. Exact-source results belong to the PR.
