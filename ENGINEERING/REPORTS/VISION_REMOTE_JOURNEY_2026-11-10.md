# Vision laboratory remote journey

## Task and source

Task `TYFINO-20261110-vision-remote-journey`, IMPLEMENTATION, 2026-11-10 UTC. Controller/executor Codex; sequential self-review, not independent review. Current owner continuation and standing reviewed-merge-after-success authority apply within TYFINO. No signing/release/deployment.

Verified repository `n923760-rgb/tyfino-platform`, official/default branch `main`, HEAD `619854fc59bf940c82568f7ba227269014e11d5c`, tree `d2c20f255dd0cd4ca6c9e94c91a7ada2d90c52a4`. No open task PRs. Official push Validate42793011441 passed all seven jobs; tablet129752146404 records165 cases,0 failures,0 skips. This supersedes the prior pending post-merge observation. Work branch `test/vision-remote-journey-20261110` starts from the clean exact official source.

Applicable root AGENTS, project profile, Android README, engineering environment/task-result contracts, canonical roadmap and Vision SDR laboratory contract were inspected. Local Git/source/shell are available; qualified local JDK17/SDK/emulator/Gradle distribution access remain unavailable. Existing hosted CI supplies Android runtime proof.

## Gap and scope

Existing coverage tests directional controls outside the actual Dialog, actual decoded pixels with touch interactions, Back release, Close/stop disposal and fresh session state. It does not qualify a continuous remote-only entry/Dialog/media journey or return to the original entry focus after explicit Close and native Back. This is a coverage gap, not a demonstrated product defect.

Add English/LTR and Arabic/RTL journeys through the actual `VisionLabEntry` and Dialog. Establish only initial launcher focus, open with Enter, require real native keyboard Dialog readiness and the product's own Original initial focus, then require a rendered first frame without focus theft. One horizontal arrow reaches Balanced without selecting it; Enter selects it. Down reaches More without changing strength; Enter changes50 to75 once. Down reaches comparison; Enter toggles original comparison on/off while preserving75. Down/Enter closes; require Dialog removal and restored original entry focus without a new focus request. Reopen with Enter, require Original/strength50/comparison off and a rendered frame, then native Back must restore entry without propagating to the host BackHandler.

Only bounded initial/window-restoration readiness waits are used. Original directional verdicts are immediate after one key; no post-direction waits, injected touch clicks, extra keys, focus forcing after open/close, swallowed assertions or retries. All165 existing cases remain byte-for-byte unchanged;167/device expected. No product, effect, decoder, provider/account/licensing, dependency, CI or identity change.

## Validation plan and results

- PASS: full three-file diff/secret review, whitespace and all six local governance validators. All165 baseline native cases remain unchanged; two new test methods are added.
- Require existing exact-head seven-job Validate,167 cases/device and configured API33 install/launch before acceptance/merge. Results belong to the task PR; pending is not PASS.
- Local Android execution: BLOCKED by previously verified Gradle distribution network failure and missing qualified SDK/JDK17/emulator. No redundant unchanged-environment provisioning attempt is claimed.
- Stop dependent mutation for drift, conflicts, secrets or required failure. Inspect complete attributable failure evidence before any bounded correction. Do not claim an existing intermittent failure is repaired without causal evidence.

Scope: this report, canonical roadmap and one new native test file. Controlled keyboard events on software-rendered phone/tablet emulators are not physical TV/OEM input, TalkBack or real-media/performance qualification. Physical TCL remains owner-deferred. Live Vision integration still requires the independent subjective quality, performance, direct-decoder Off/recovery and HDR/DRM gates in its contract.
