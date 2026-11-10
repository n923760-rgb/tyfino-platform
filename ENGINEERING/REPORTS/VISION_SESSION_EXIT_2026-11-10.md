# Vision laboratory session exit and re-entry

## Task / authority

- Task: `TYFINO-20261110-vision-session-exit`, IMPLEMENTATION, 2026-11-10 UTC.
- Controller/executor: Codex; sequential self-review, not independent review.
- Owner requests continuing TYFINO from the last checkpoint. Existing authority covers bounded development and conditional reviewed merges after successful checks. No signing, release, publication or deployment is performed here.
- Repository: `n923760-rgb/tyfino-platform`; official/default branch `main`.
- Verified official base: `3cfe7a462085e09fc1b7f1cdfb332625593f0303`, tree `4ca8a17ee56c7fe09b8a713b328fb7e8e64cd743`.
- Official push Validate `42561660102` completed SUCCESS on that exact base. No open PRs or issues were returned at the initial live gate.
- Work branch: `test/vision-session-exit-20261110`; clean clone matched official HEAD before branch creation.

Applicable instructions: root AGENTS, central governance, project profile, Android README, environment/task-result contracts, device qualification and `docs/android/vision-sdr-lab-v1.md`.

## Confirmed gap and bounded change

The laboratory contract requires dismissal on ON_STOP, player release on disposal and memory-only controls resetting on exit. Existing actual-media coverage proves hardware Back release but does not exercise lifecycle-stop dismissal or explicit Close followed by a new session. This is a qualification gap, not a reproduced product defect.

Add two actual `VisionLabEntry` / `VisionLabDialog` integration cases:

1. Open the bundled sample, require a rendered first frame, select Balanced, strength75 and original comparison. Close explicitly; require dialog removal and one real Media3 decoder-release event. Explicitly reopen and require a different player, the same fixed local sample and default Original/strength50/comparison-off controls. Close again and require its decoder release without a duplicate old release.
2. Perform the same changed session, transition a local LifecycleRegistry from RESUMED to CREATED to deliver actual ON_PAUSE/ON_STOP, and require the entry's own dismissal plus decoder release. Return to RESUMED and require the dialog remains absent until explicit reopen, then verify the same fresh-session assertions.

No test directly invokes the dismissal callback, releases the player or resets the profile. No player factory, fake decoder, retry, sleep, production change or weakened existing assertion. Lifecycle delivery is controlled locally; this is not a physical Activity/background or process-death qualification. All163 baseline cases remain unchanged;165/device expected.

## Validation and result

Local Git/source/shell are available. Local Android compilation is BLOCKED: `./gradlew --no-daemon :app:compileDebugAndroidTestKotlin` exits1 while fetching Gradle9.6.0 with `Network is unreachable`; only JDK21 and no qualified SDK/JDK17/emulator were found. Hosted existing CI must supply compilation/runtime proof.

PASS: all six local governance validators, whitespace check and full three-file diff/secret review. All163 existing test methods are retained byte-for-byte; the new class adds two test methods. Exact-head hosted seven-job Validate,165 cases/device and configured API33 install smoke are required before acceptance. Their results are recorded in the task PR; a pending run is not PASS.

Scope: one native integration test file, this report and the canonical roadmap. The normal player, laboratory production source/shader, provider/account/license/storage authority, dependencies, CI environment, application identity and release configuration are unchanged.

Stop dependent mutation for source drift, conflicting PRs, required failed checks, secrets or unrelated scope. Preserve failed evidence and inspect the first failing assertion before any correction. Physical TCL remains owner-deferred; real-media quality, OEM/low-power GL compatibility, accessibility and performance remain NOT RUN. Live Vision integration still requires subjective A/B evidence, direct-decoder Off/recovery and HDR/DRM bypass qualification under its contract.
