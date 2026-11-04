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

## 2026-11-03 continuation: asynchronous initial-focus observation

Owner requests continued development from the latest checkpoint. Live official main remains ed7237fc6c379cd98315819c75797695d2e93ce6; the only open PR is #216, head07249b8cc5ee884849fef9194a9ce963691d905f. Local clean main matched the official source, then an isolated worktree checked out that existing task branch. Execution: Git/source inspection and authenticated repository API, hosted CI; local Android SDK/Gradle/emulator unavailable. Continue the same bounded task, with test synchronization and its report/roadmap only. Required evidence: unchanged144 cases/device, all seven exact-source gates, retained selected/visible/directional/single-callback assertions and API33 install smoke. Stop on source drift, conflicts, secrets or required failures; physical/release/deployment boundaries above remain.

FAIL evidence: Validate41603821012 on head07249b8, tablet job125811824736,144 cases/1 failure/0 skipped. Six other jobs succeeded. Full tablet log and retained artifact13216693324 reviewed; ZIP854079 bytes, SHA25611499cf51c458a3ab2bb25828e04ab924296e94f4dcf02bf0b3bbcf728997b2b. JUnit records Off selected=true/focused=false at the original assertion, while the failure-only later observation records Off focused=true, Automatic/Close=false and dialog window focused=true. This directly establishes that focus arrived between observations without any test focus request; exact arrival latency is not measured. Inference: row composition/selection was incorrectly treated as a synchronous barrier for native dialog focus. This evidence does not establish a production focus-helper defect.

Correction: observe the expected initial target with a bounded five-second condition wait in the three new selected-row scenarios, then retain explicit focus/visibility, selection, D-pad and exact callback assertions. No sleep, forced focus, scrolling, selection action, assertion removal, workflow retry or production-helper change. Timeout still fails with a synthetic focus/window snapshot. Existing141 cases, including controlled-frame no-refocus coverage, remain unchanged. A successful bounded wait is functional evidence, not a TV startup-latency/performance qualification. Exact corrected-source CI results belong to #216.

PASS local continuation checks: all six repository governance validators and full scoped diff/whitespace review. Production changes remain the previously reviewed selected-row initialization only. New native execution is BLOCKED locally by unavailable Gradle/SDK/emulator; hosted exact-head results are pending.

## 2026-11-04 continuation: Arabic large-text touch diagnosis

Owner requests continuation. Verified live main remains ed7237fc6c379cd98315819c75797695d2e93ce6 and PR216 head a96aa47f5019233eaca51b119f842ee98dca0b01. Continue the same dialog task with a bounded diagnostic of its existing Arabic/font2/280x320dp regression, retaining all144 cases and callback assertions. No production fix is justified yet. Local execution capability and physical/release boundaries above remain. Stop acceptance/merge on required failures.

Validate41679652057: six jobs PASS, including phone144 cases and API33 exact-APK install/launch; tablet job126072840238 FAIL144 cases/1 failure/0 skipped. The three new selected-row focus cases passed. The different failure is ArabicLargeTextContentScrollsAndRetainsSubtitleOffSelection: its first final assertion reports Off callback expected1, actual0 at line233. This establishes a missed Off activation, but not its cause. Full tablet log and artifact13254205595 reviewed; ZIP SHA25656da05c5ff37fdd8352a823bc637d9e8b1f55b896dc0ceb0c9683eb7912a5cb0 verified.

Diagnostic change: after the existing scroll-to/selection assertion, require the Off button's clipped bounds to cover its measured size before the existing touch click. Record synthetic target/outer geometry in failures; assert Off callback exactly once immediately after its click, and retain final Off/Close counts with explicit labels. This distinguishes clipping from a missed touch on a fully visible target and identifies the first failing action. It does not add scrolling, forced focus, sleep, retries, or a production workaround. The additional semantics observations can affect scheduling: a passing diagnostic run alone does not prove a causal product correction. Exact-source native results are pending; no zero-defect or physical-TV acceptance is established.
