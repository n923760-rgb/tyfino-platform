# Picture-size focus: post-merge window diagnosis

Official baseline: `df7201054a22d96501077872aa194b618d162750`.
Owner instruction: continue building TYFINO; preserve release-readiness focus.
Task type: CI INVESTIGATION. Work branch: `test/picture-focus-window-diagnosis-20261023`.

## Verified failure

Official-main [Validate39717316309](https://github.com/n923760-rgb/tyfino-platform/actions/runs/39717316309) passed six jobs but tablet job119623041738 failed:138 cases,1 failure,0 skips. Full job log and artifact12400061948 reviewed; ZIP SHA256 `459bb445a8b05d1bdea04898e3c078756e0bd4210829c968a364e7414fb5e8f4`. PlaybackAspectTest fails after Down from Fit at line110; the initial selected/first-focused assertions passed. The full original per-case log contains no Compose focus-transition history. The controlled helper regression passed in the same suite. Prior successful runs do not establish that #205 is fully resolved.

## Bounded diagnostic change

Only the existing test changes. Passive listeners observe the actual production dialog window/native-focus/touch-mode transitions. Before/after Down snapshots record fixture focus tags and bounds. The existing Down event duration, focus assertions, Enter-only selection, native resize assertions, reopening and Back/cancel assertions remain. No production helper/dialog/layout/selection changes, retries, waits, sleeps or expected-target forcing. Observers are removed on teardown. Trace data uses only static fixture tags, geometry, input state, native view class names and monotonic timestamps.

This adds observation checkpoints and can affect scheduling. A passing diagnostic run is not a causal fix or proof of intermittent-failure absence. A failing run needs its complete trace examined before any production correction. All138 cases and seven Validate gates remain required.

## Capability and validation

PASS: exact Git baseline/default branch verified; no open PRs at start; full failure artifact SHA verified; local diff whitespace check.
BLOCKED: local native compilation/runtime: JDK17 available, Gradle distribution fetch fails Network is unreachable; Android SDK/emulator unavailable. Existing exact-head GitHub Actions is the execution path.
NOT RUN at this checkpoint: exact new-head CI; physical TV/phone/provider/accessibility/performance; signing/release/deployment. Native CI results belong to the isolated diagnostic PR.

## Next gate

Inspect exact diagnostic head logs and trace; reproduce or isolate the failed event/window/focus sequence before altering product code. Keep #205 and release acceptance open until causal correction and required regression evidence exist. No production release is created by this task.

## Diagnostic fixture compilation correction

Diagnostic head `fa461dbcfadd5df4467eb88c84252fbbf5a1c383`, Validate39952535729: both native jobs failed before any test (0 cases) because an unnecessary top-level `onAllNodes` import is unresolved. The API is a member of the test rule. Remove only the import; this is a test compilation correction, not product/focus evidence. Android build/unit/lint and API/database/config gates passed; Admin provisioning was still running at this checkpoint. Require fresh exact-head native execution.
