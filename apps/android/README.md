# TYFINO Android foundation

This directory contains the native Android client foundation and the licensing, Xtream authentication, catalog, playback, Series episodes, resume, cached-catalog search, and favorites slices. EPG, provider-wide search, favorites, broader history, final branding, and release configuration remain separate work.

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

## Implemented Xtream authentication slice

- Explicit Host, Username, and Password entry after TYFINO licensing succeeds.
- Direct Android-to-provider authentication through the provider's `player_api.php` endpoint; IPTV credentials never enter TYFINO licensing.
- HTTPS preferred. A user-entered HTTP provider requires an explicit interception-risk confirmation, with no automatic downgrade, redirect, or certificate bypass.
- Strict host canonicalization, 8-second connect timeout, 12-second read timeout, 256 KiB response limit, and no automatic authentication retry.
- Distinct invalid-host, offline, timeout, provider-unavailable, invalid-credential, expired, disabled, malformed, and unsupported result states.
- One saved active IPTV account. Credentials and HTTP consent are encrypted in a separate AES-GCM payload protected by Android Keystore and excluded from backup.
- Account ID, generation, and operation identity are checked at commit time so a stale completion cannot overwrite a newer login, logout, removal, or destination owner.
- Account removal invalidates ownership before deleting the encrypted credential payload.

Catalog, categories, EPG, playback URLs, and streams are not fetched by authentication and remain deferred.

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
./gradlew --no-daemon :app:phoneApi27DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
./gradlew --no-daemon :app:tabletApi35DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

An Android SDK, emulator acceleration, and JDK 17 are required. Instrumentation smoke tests run on clean API 27 phone and API 35 tablet managed devices. This automated baseline does not replace the physical phone, Android TV/Google TV, API 24-class, media, RTL, accessibility, and performance checks in [`../../docs/android/device-qualification-v1.md`](../../docs/android/device-qualification-v1.md).

## Cached search scope

Each Live, Movies, and Series destination searches only the active account's already-downloaded category snapshots. The search is local (no network fetch or background full-catalog synchronization), starts after two characters and a short debounce, and shows at most 50 matches. The UI states this limitation. Provider-wide search remains deferred.

## Favorites scope

Live, Movies, and Series items can be added to an account-scoped local favorites list. Only account ID, section, provider item ID, and a timestamp are stored; titles, artwork, playback URLs, and credentials are never written to this store. A list contains at most 200 favorites per account and displays only items still present in current downloaded catalog categories. Account removal clears local favorites. The per-section Favorites filter and per-item add/remove controls support touch, keyboard, and TV D-pad input.

## Deferred contracts

Full physical-device/TV qualification, release identity/signing, EPG, provider-wide search, and other deferred playback capabilities require their own approved work. Series episode playback, resume, and Continue Watching are implemented; do not infer release qualification from build and emulator checks.
