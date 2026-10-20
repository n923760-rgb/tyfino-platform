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

Second diagnostic source `e0bce1fe67a5a5d67a53fbea8ea6213ddf947853`, run39514615433 phone job118944480009:138 cases,1 failure,0 skips. Full artifact12307961445, ZIP SHA256 `c23b07d4739dd65892d2cfc3e99a12843b5e7caf2ea6e1de3e17058d41ceb9b8`, resolves the failure to line65 immediately after Down, before the explicit extra frame. Input-mode setup and first-target assertion passed, but movement/retention timing remains ambiguous. KeyInjectionScope.pressKey advances event time between keyDown/keyUp; the diagnostic now uses a zero-duration press to isolate explicit frame advancement and logs actual focus transitions with test-clock times in distinct assertions. Production behavior remains unchanged; require new runtime evidence before a correction.

The original full artifact12296026794 (SHA256 `9f210051f389858c5254afd6e6bfc16c7c02a54d2f002af5f87da2c96e2472e3`) resolves #205 to PlaybackAspectTest.kt:110 after Down from Fit, before selecting Zoom. Its failure summary alone had omitted the line. Cause remains under investigation.
