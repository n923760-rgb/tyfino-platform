# Initial-focus controlled-frame diagnosis

Scope: test-only investigation of #205 on official `main@3359b4ddb206928af4374c55b95608b2594d59c5`. Owner requested continued engineering work. No production helper, selection behavior, dependency, workflow, identity, signing or deployment change.

## Evidence and hypothesis

FACT: main run39489812458, tablet job118860726245, failed the existing picture-size Zoom-focus assertion (136 cases,1 failure,0 skips). Full decoded job log reviewed; its summary does not distinguish the assertion after Down from the assertion after reopening. The same Android source passed the premerge suite. About #203 then passed137 cases/device and official-main run39494244653 passed all seven jobs. Later passes do not prove a correction.

FACT (source): `rememberInitialFocusRequester` requests focus on each of two successive frame callbacks and on window-focus gain. INFERENCE: a user move between frame callbacks could be overwritten. This is not yet a proven cause of #205.

## Diagnostic change

One new native test mounts the actual production helper in an already window-focused keyboard-mode host, disables automatic test-clock advancement, advances the attachment/first initialization frames, moves focus with a real Down event, asserts the second target, advances the next frame and asserts retention. It neither forces the expected second target nor changes the helper. All137 existing cases remain; expected total138/device. Existing playback assertions retain their predicates and gain distinct stage messages.

The controlled-frame case may intentionally expose a production failure. A failure before the demonstrated first focus/Down checkpoint is a diagnostic harness failure, not causal proof. A pass does not eliminate alternate window/dialog timing causes. Inspect full exact-head logs before any product change; no blind rerun, sleep, skip or weakened assertion.

## Qualification

Diagnostic source `f71d8bb6c7b9a0fa053094631de337f15c1e7fb6`, Validate39509534707: five jobs passed; both native jobs ran138 cases,1 failure,0 skips. All137 prior cases passed. The new case failed at input-mode setup, before mounting the helper or moving focus. The full tablet report ZIP (artifact12304784898, SHA256 `03a29880871b40583cf44b40f487094de7b890dd5402d7838f34ec35d5fcacec`) resolves the failure to InitialFocusTest.kt:51, `requestInputMode(Keyboard)` returning false. This is a harness failure, not evidence for the product hypothesis. The corrected fixture keeps a real focusable host button present while requesting keyboard mode; the production helper is still mounted afterward. The request has a distinct assertion message. A new source run is required.

Local native execution BLOCKED: Gradle/Android SDK/emulator unavailable. Existing exact-head Validate is the native execution environment. Runtime result belongs to the diagnostic PR; do not merge failing diagnostic source or claim a fix from source inspection. Physical TV/TalkBack/provider/performance NOT RUN. No release or customer artifact is produced.
