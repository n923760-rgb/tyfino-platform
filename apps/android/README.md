# TYFINO Android foundation

This directory contains the native Android client foundation and the first approved licensing-client slice. It intentionally contains no IPTV, Xtream, player, catalog, EPG, media, analytics, or provider-backend implementation.

## Implemented licensing slice

- Explicit **Start 7-Day Free Trial** and **Activate Now** entry points; opening or installing the app never starts the trial.
- Random opaque installation identity and entitlement/session storage encrypted with Android Keystore and excluded from backup.
- HTTPS-only calls to the approved `/v1/licensing/*` contract, with bounded timeouts, response size, retry count, exponential delay, and jitter.
- One authoritative operation generation per action so stale network results cannot overwrite newer intent.
- Server-time entitlement evaluation, 12-hour refresh scheduling, and a maximum 72-hour offline window. Offline access fails closed after a reboot until the server can verify the session, preventing device-clock rollback from extending access.
- Activation Codes are normalized locally, sent only to the activation endpoint, cleared from UI state immediately, and never persisted or logged.
- No IPTV Host, Username, Password, catalog, stream URL, or viewing data crosses the licensing boundary.

The production licensing origin remains OPEN. Development builds accept it only as an explicit Gradle property:

```shell
./gradlew :app:assembleDebug -PTYFINO_LICENSING_API_BASE_URL=https://approved-origin.example
```

Without that property, the UI remains usable but licensing network actions fail safely as unavailable.

## Foundation decisions

| Item | Value | Status | Reason |
| --- | --- | --- | --- |
| Language | Kotlin 2.4.10 | DECIDED for this foundation | Stable Kotlin/Compose compiler plugin compatible with the selected Android build toolchain. |
| UI | Jetpack Compose, stable BOM 2026.08.00 | DECIDED | Approved native UI direction and official compatibility management. |
| Build plugin | Android Gradle Plugin 9.4.0 | DECIDED for this foundation | Current stable AGP supporting API 37; no preview tooling. |
| Gradle | 9.6.0 | DECIDED for this foundation | Required/default version for AGP 9.4.0. The checked-in wrapper pins and verifies the distribution. |
| JDK | 17 | DECIDED | Required/default JDK for AGP 9.4.0. |
| compileSdk / targetSdk | 37 / 37 | DECIDED for this foundation | Uses the current Android platform and exceeds the Google Play API 36 minimum. |
| minSdk | 24 | PROVISIONAL | Current stable Navigation requires API 24; target-device coverage must be approved before product release. |
| Module count | One `app` module | DECIDED | Minimum maintainable structure; feature modules are not justified yet. |
| Adaptive API | Material 3 Adaptive 1.3.0 | DECIDED | Official window-size API for resizing, foldables, multi-window, and large screens. |
| Navigation | Navigation Compose 2.10.0 | DECIDED | Current stable AndroidX navigation for the minimal shell. |

Official references checked on 2026-09-08:

- <https://developer.android.com/google/play/requirements/target-sdk>
- <https://developer.android.com/build/releases/agp-9-4-0-release-notes>
- <https://developer.android.com/build/migrate-to-built-in-kotlin>
- <https://developer.android.com/develop/ui/compose/bom>
- <https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive>
- <https://developer.android.com/jetpack/androidx/releases/navigation>
- <https://developer.android.com/jetpack/androidx/releases/activity>

## OPEN temporary values

Android requires identifiers before the protected production values have been approved. The following values are development-only and must not be treated as final:

- Namespace and application ID: `dev.tyfino.foundation`
- App label: `TYFINO Dev`
- Version code/name: `1` / `0.1.0-foundation`
- Neutral icon, TV banner, and theme colors

There is no production signing configuration, production endpoint, ABI policy, or final branding in this project.

## Build and checks

The repository CI provisions Gradle 9.6.0 and runs:

```shell
./gradlew --no-daemon :app:assembleDebug
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:lintDebug
```

An Android SDK and JDK 17 are required. Instrumentation smoke tests live under `app/src/androidTest` and should be run on a qualified emulator or device; they are not part of the initial non-emulator CI job.

## Deferred contracts

Player, Xtream, IPTV-account persistence/security, full TV focus qualification, release, and full testing contracts remain deferred until their respective scoped work begins. This foundation must not be used to infer those designs.
