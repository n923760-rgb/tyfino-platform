# Television priority: focus the current audio/subtitle choice

Task: TYFINO-20261101-tv-selected-track-focus — IMPLEMENTATION.
Controller/executor: Codex, sequential implementation and self-review.
Owner explicitly prioritizes a professional television experience. Standing reviewed-merge authority applies after successful requirements; signing, release and deployment are excluded.

## Source and scope

Repository/default branch: n923760-rgb/tyfino-platform, main.
Verified official base: ed7237fc6c379cd98315819c75797695d2e93ce6; official-main Validate41319159216 PASS. Fresh origin/main and isolated worktree start from that source. No open/conflicting PRs at initial gate.
Branch: improve/tv-selected-track-focus-20261101.
Scope: track-picker initial focus/viewport, three native remote-input cases, this report and canonical roadmap. No media selection, player lifecycle, network/data authority, dependency, package, endpoint, signing or release changes. Stop for drift, conflicts, secrets or failed required checks pending diagnosis.

## Behavior

Source inspection: the dialog always attaches its initial FocusRequester to Automatic. This is independent of the selected embedded track or subtitle Off state. Opening a long list also starts at its beginning, requiring remote navigation away from the actual selected row.

Choose the initial row from the current authoritative presentation: Automatic when selected, Off when available and selected, otherwise the first selected embedded option, with Automatic as the safe fallback when no selection exists. Initialize the existing LazyColumn at that row so a deep selected option is composed and reachable by the existing initial-focus helper. Preserve the helper's no-refocus-after-success behavior and the existing callback/selection owners. Moving focus never selects a track; Enter remains the explicit action.

Three native cases exercise the actual dialog: Off initially selected/focused, Up to Automatic without selection, Enter invoking exactly one callback; embedded option72 of80 initially visible/selected/focused without test scrolling or forced target focus, Down to73 without selection and Enter selecting exactly73 once. The deep-list case runs in both LTR and RTL. Fixture labels/formats are synthetic; no provider requests or secrets are involved. Retain all141 prior cases,144 expected per managed device.

## Validation and boundary

PASS local: diff whitespace, full four-file self-review, engineering-environment, Task/Result protocol, AI executor policy, behavioral-trial, CI qualification and governance adversarial validators. Local Android SDK/Gradle/emulator are unavailable, so native qualification requires the existing exact-source seven-job Validate,144 cases/device and exact-APK API33 install smoke.

The current managed devices are phone/tablet profiles. Their directional-input checks qualify these bounded UI behaviors; they do not qualify Android TV firmware, a physical remote, Google TV launcher behavior, real-provider media or TV performance. Those remain NOT RUN and mandatory before television release acceptance. No zero-defect or universal-device claim is made.
