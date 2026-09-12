# Android Device Qualification v1

Status: **IN PROGRESS — release qualification is not yet granted**

This document is the evidence ledger and compatibility backlog for the approved Android playback V1 contract. A row is `PASS` only when the exact build, device profile, result, and evidence are available. Build success is not a substitute for physical playback qualification.

## Automated baseline

| Surface | Configuration | Required evidence | Status |
| --- | --- | --- | --- |
| Compile and package | minSdk 24, target/compile SDK 37 | `assembleDebug` on CI | PASS |
| Repository unit suite | JVM tests for ownership, URI policy, resume, catalog, and previous channel | `testDebugUnitTest` on CI | PASS |
| Static Android checks | Debug variant with warnings as errors | `lintDebug` on CI | PASS |
| Compact phone runtime | Pixel 2 profile, AOSP API 27 | Managed-device `androidTest` | PASS |
| Large-screen runtime | Nexus 9 profile, AOSP API 35 | Managed-device `androidTest` | PASS |

The two managed devices run from clean emulator state in GitHub Actions. API 27 is the lowest API supported by Android build-managed devices; it does not replace the required API 24-class physical-device check. The CI uses software rendering as required for headless servers.

A passing Android CI job attaches a short-lived `tyfino-debug-*` APK artifact for manual development testing. It uses temporary debug signing and the CI default of an empty licensing-service origin; it is not a production release or a configured end-to-end IPTV test build. Record the artifact's commit and any approved local build configuration in manual evidence. Reinstalling a different CI build may require removing the prior debug app and its local state because the signing key is not stable across runners.

Recorded automated evidence: [Validate run 81](https://github.com/n923760-rgb/tyfino-platform/actions/runs/34392935134) on commit `04fcd50a8e5cafe039bfa9201d20268f67a8e07a`. All five automated rows above completed successfully. This evidence does not qualify any physical-device or real-media row below.

## Required physical and media qualification

| Area | Minimum evidence before release | Current status |
| --- | --- | --- |
| Representative phone | Live HLS/TS, progressive Movie seek/resume, pause/background/exit, audio/subtitle controls | BLOCKED |
| Android TV or Google TV | D-pad traversal, visible focus, Back behavior, non-touch launch, Live and Movie playback | BLOCKED |
| Lower-performance API 24-class device | startup, catalog scroll, player creation/release, decoder behavior, memory pressure | BLOCKED |
| Foldable or resizable large screen | fold/unfold and configuration changes during catalog and playback | BLOCKED |
| RTL and accessibility | Arabic layout, TalkBack labels/order, controls without credential/provider-ID exposure | BLOCKED |
| Media failure matrix | offline, timeout, rejected HTTP/redirect, unsupported format, decoder failure, non-seekable stream | BLOCKED |
| Lifecycle stress | rapid Back, navigation, logout, account replacement, and repeated channel switching | BLOCKED |

These rows remain `BLOCKED` because no approved provider test fixture and no physical-device evidence are attached. They must not be reported as `PASS` from emulator smoke tests.

## Evidence protocol

Use the [manual test record template](manual-test-record-template.md) for each build/device pair, including the CI artifact identity and a separate status for every exercised scenario.

Every manual run records:

- date and tester;
- commit SHA and APK variant;
- manufacturer, model, form factor, Android/API version, RAM class, and input type;
- media type and credential-free fixture identifier;
- each scenario result using only `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED`;
- credential-free logs, screenshots, or video when a failure needs investigation.

Evidence must never contain a provider host, username, password, activation code, playback URI, redirect target, authorization header, cookie, or manifest excerpt.

## Compatibility backlog

### P0 — required before release qualification

- Run the full physical-device and media matrix above.
- Verify that one player and surface are released without leaked Activity or continuing network/audio work.
- Measure cold start, catalog scrolling, player startup, dropped frames, memory, and CPU on the API 24-class device.
- Confirm TV overscan/safe areas and deterministic D-pad focus for Back, Previous channel, Audio, Subtitles, Retry, and Media3 controls.
- Confirm Arabic RTL and TalkBack do not expose provider IDs or secret-bearing values.
- Record device-specific codec failures without promising universal codec support.

### P1 — compatibility follow-up

- Add approved credential-free local Live HLS/TS and progressive Movie fixtures for repeatable player integration tests.
- Add lifecycle leak detection and a bounded playback startup benchmark.
- Expand emulator coverage to a TV profile once player entry can use a deterministic local fixture.
- Add fold/unfold automation when a stable CI device profile is available.

### Deferred by the playback contract

DRM, provider-specific headers/cookies, external subtitles, software codec bundles, background/PiP playback, Series episodes, Cast, downloads, and alternate Xtream URL forms require separate approved decisions.
