# Picture-size focus evidence retention

Task: TYFINO-20261023-picture-focus-evidence-retention (IMPLEMENTATION).
Official base: `8ddbde958d63f4392b4e43be254a613fc82c9703`, including merged #208.
Owner explicitly requested continued engineering and merge on 2026-10-23.

## Confirmed evidence gap

#208 exact head `d9639da922f09ce9adc17c56318d00a1d04b682e` passed all seven Validate gates in run39953214125:138 cases on each managed device,0 failures/skips, exact configured APK API33 clean install/launch PASS. This did not reproduce or fix #205. The existing full report uploader runs only on failure, so successful per-case window/focus traces were not retained.

## Bounded change

After instrumentation, always upload only the picture-size directional test's per-case logcat and native JUnit XML to a separate artifact named by device, exact source SHA and attempt. Use30-day retention, matching existing CI evidence retention. Keep the full failure uploader, source verification, all seven gates, test commands, assertions and success/failure behavior unchanged. Missing files warn rather than masking an earlier provisioning/compile/test failure; absence is not evidence of a successful test. No production input/layout/focus changes.

The retained trace contains static synthetic fixture tags, geometry, window/touch/native-focus state and timestamps. The separate XML records test identity/results. Inspect actual artifacts and their source attribution before making causal claims. Observation checkpoints in the existing test can still affect scheduling; retaining logs does not remove this limitation or prove a fix.

## Validation and acceptance

Local syntax/configuration checks and exact-head hosted results are recorded on the task PR. Local Android runtime remains BLOCKED by unavailable Gradle distribution network/SDK/emulator. Accept this change only as evidence retention after inspecting the new per-device artifacts. #205 remains open. Physical TV/provider/accessibility/performance, signing/release/deployment remain NOT RUN.

Next: compare retained successful and failing real-dialog traces, isolate initialization/window/input ordering, and make a separate causal correction only when supported by evidence.
