# TYFINO Android

This directory contains the native TYFINO Android client and the licensing, multiple-account Xtream authentication, catalog, artwork, playback, Movie details, Series episodes, resume, cached-catalog search, favorites, Live/Movie/episode recent-history, and on-demand Live EPG slices. The account portfolio follows [the multiple Xtream accounts contract](../../docs/android/multiple-xtream-accounts-contract-v1.md). Provider-wide search and production release configuration remain separate work.

## Implemented licensing slice

- Explicit **Start 7-Day Free Trial** and **Activate Now** entry points; opening or installing the app never starts the trial.
- Random opaque installation identity and entitlement/session storage encrypted with Android Keystore and excluded from backup.
- HTTPS-only calls to the approved `/v1/licensing/*` contract, with bounded timeouts, response size, retry count, exponential delay, and jitter.
- One authoritative operation generation per action so stale network results cannot overwrite newer intent.
- Server-time entitlement evaluation, 12-hour refresh scheduling, and a maximum 72-hour offline window. Offline access fails closed after a reboot until the server can verify the session, preventing device-clock rollback from extending access.
- Activation Codes are normalized locally, sent only to the activation endpoint, cleared from UI state immediately, and never persisted or logged.
- No IPTV Host, Username, Password, catalog, stream URL, or viewing data crosses the licensing boundary.

Release builds use the approved production licensing origin `https://api.tyfino.online`. It is fixed in the Release build type so a local development property cannot silently redirect a production binary. Development builds remain disconnected by default and accept a test origin only as an explicit Gradle property:

```shell
./gradlew :app:assembleDebug -PTYFINO_LICENSING_API_BASE_URL=https://approved-origin.example
```

Without that property, a Debug build's UI remains usable but licensing network actions fail safely as unavailable.

For owner-run physical-device qualification, the manual [Build device test APK](../../.github/workflows/device-test-apk.yml) workflow builds a Debug APK from exact official `main` with the approved production licensing origin explicitly configured. It uses temporary debug signing, includes source SHA and checksum evidence, expires after seven days, and does not authorize customer distribution. Use the [manual test record](../../docs/android/manual-test-record-template.md) per build and physical device.

## Implemented Xtream authentication slice

- Explicit Host, Username, and Password entry after TYFINO licensing succeeds.
- Direct Android-to-provider authentication through the provider's `player_api.php` endpoint; IPTV credentials never enter TYFINO licensing.
- HTTPS preferred. A user-entered HTTP provider requires an explicit interception-risk confirmation, with no automatic downgrade, redirect, or certificate bypass.
- Strict host canonicalization, 8-second connect timeout, 12-second read timeout, 256 KiB response limit, and no automatic authentication retry.
- Distinct invalid-host, offline, timeout, provider-unavailable, invalid-credential, expired, disabled, malformed, and unsupported result states.
- Up to eight saved IPTV accounts with one explicitly selected active account. Credentials, active selection, and HTTP consent are encrypted in a versioned AES-GCM portfolio protected by Android Keystore and excluded from backup.
- Account ID, generation, and operation identity are checked at commit time so a stale completion cannot overwrite a newer login, logout, removal, or destination owner.
- Switching is local and explicit. Removing the active account leaves remaining accounts unselected until the user chooses one.
- Account removal targets one stable Account ID, invalidates ownership first, and clears only that account's implemented local data partitions.

Authentication fetches no catalog, EPG, playback URLs, or streams. Those features operate only after sign-in and explicit destination actions.

## Foundation decisions

| Item | Value | Status | Reason |
| --- | --- | --- | --- |
| Language | Kotlin 2.4.10 | DECIDED for TYFINO V1 | Stable Kotlin/Compose compiler plugin compatible with the selected Android build toolchain. |
| UI | Jetpack Compose, stable BOM 2026.08.00 | DECIDED | Approved native UI direction and official compatibility management. |
| Build plugin | Android Gradle Plugin 9.4.0 | DECIDED for TYFINO V1 | Current stable AGP supporting API 37; no preview tooling. |
| Gradle | 9.6.0 | DECIDED for TYFINO V1 | Required/default version for AGP 9.4.0. The checked-in wrapper pins and verifies the distribution. |
| JDK | 17 | DECIDED | Required/default JDK for AGP 9.4.0. |
| compileSdk / targetSdk | 37 / 37 | DECIDED for TYFINO V1 | Uses the current Android platform and exceeds the Google Play API 36 minimum. |
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

## Application identity

- Stable application ID: `com.tyfino.player`
- Internal source namespace: `dev.tyfino.foundation` (not part of the installed or Play Store identity)
- App label: `TYFINO`
- Initial version code/name: `1` / `1.0.0`
- TYFINO vector icon, Android TV banner, and dark cyan/violet theme

The application identity, production licensing endpoint, and Direct APK distribution path are approved and must not be changed after distribution. The protected signing workflow is defined by [`../../docs/android/direct-apk-release-v1.md`](../../docs/android/direct-apk-release-v1.md); key generation, verified backup custody, and release approval remain separate gates.

The former `dev.tyfino.foundation` development application ID and `com.tyfino.player` are separate Android applications. Pre-release test devices must remove the old development install; no production customer data migration is implied or required.

## Build and checks

The repository CI provisions Gradle 9.6.0 and runs:

```shell
./gradlew --no-daemon :app:assembleDebug
./gradlew --no-daemon :app:assembleRelease
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:lintDebug
./gradlew --no-daemon :app:phoneApi27DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
./gradlew --no-daemon :app:tabletApi35DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

An Android SDK, emulator acceleration, and JDK 17 are required. Instrumentation smoke tests run on clean API 27 phone and API 35 tablet managed devices. This automated baseline does not replace the physical phone, Android TV/Google TV, API 24-class, media, RTL, accessibility, and performance checks in [`../../docs/android/device-qualification-v1.md`](../../docs/android/device-qualification-v1.md).

The ordinary Release variant runs R8 code optimization and resource shrinking in CI and embeds only the approved `https://api.tyfino.online` licensing origin. It remains unsigned by default and is build evidence only. The manual protected workflow signs only when explicitly requested with provisioned environment secrets, verifies the approved certificate fingerprint, and produces an owner-review artifact without publishing it. Production signing material must never be committed.

## TV and D-pad focus scope

Primary screens request a deterministic first focus target after attachment. Navigation destinations, horizontal content rails, season selectors, and adaptive catalog grids are focus groups so directional input visits related controls coherently; buttons and cards retain a visible focus border. Managed-device tests assert representative Settings, Movie details, and Series details entry targets. This is emulator evidence only: physical Android TV/Google TV traversal, overscan, Back behavior, playback controls, RTL, accessibility, and performance qualification remains `BLOCKED` until recorded under the device-qualification contract.

## Cached search scope

Each Live, Movies, and Series destination searches only the active account's already-downloaded category snapshots. The search is local (no network fetch or background full-catalog synchronization), starts after two characters and a short debounce, and shows at most 50 matches. The UI states this limitation. Provider-wide search remains deferred.

## Favorites scope

Live, Movies, and Series items can be added to an account-scoped local favorites list. Only account ID, section, provider item ID, and a timestamp are stored; titles, artwork, playback URLs, and credentials are never written to this store. A list contains at most 200 favorites per account and displays only items still present in current downloaded catalog categories. Account removal clears local favorites. The per-section Favorites filter and per-item add/remove controls support touch, keyboard, and TV D-pad input.

## Recent-history scope

Live channels, Movies, and Series episodes are recorded only after the foreground player reaches actual playback. Separate account-scoped stores retain at most 100 recent identities and store no titles, artwork, playback URLs, or credentials. Live/Movie history rejoins current downloaded catalog metadata; episode history rejoins the current published Series generation and is shown independently from resume-based Continue Watching. Missing or stale items stay hidden.

Home displays only shelves with current items in the active account's opened catalog and recent history. Once its account-scoped summary finishes loading, an empty Home shows one invitation to open a section instead of five empty shelves. Account switching and Settings remain available.

The foreground player uses a shorter 15–30 second, 16 MiB target buffer for Live streams or low-RAM devices and a 30–60 second buffer for Movie and Series playback on other devices. These are tuning defaults; real provider streams and lower-memory devices still need physical playback qualification.

Only Live playback retries a lost connection, timeout, or expired live window: up to three attempts after 1, 2, and 4 seconds, then the existing manual Retry/Back error state. When the default network returns during a pending delay, playback resumes that attempt immediately without adding an attempt. Each attempt rechecks the active account, foreground lifecycle, player identity, and destination exit state; leaving the player cancels pending retry work and unregisters the network callback. Access, format, and decoder failures do not automatically retry.

## Live EPG scope

The player opens the selected Live channel's program guide only when requested. A direct per-channel provider request updates an account-owned, bounded cache; playback does not wait for guide data. The dialog shows current/next programs and a lazy schedule with localized loading, empty, stale, and error states. Closing it or changing channels invalidates older results, and account removal clears the EPG cache. Physical TV D-pad, RTL, accessibility, media, and performance qualification remains BLOCKED until recorded under the device-qualification contract.

## Catalog artwork scope

Catalog cards render already-validated artwork references lazily through one application-owned image loader. Live uses a landscape frame; Movies and Series use a poster frame. A local placeholder remains visible for missing, rejected, slow, redirected, oversized, or failed images, and the card stays navigable without waiting for artwork.

The shared card uses 12dp corners, a two-character label on a stable tonal gradient when artwork is unavailable, and a short image crossfade when artwork loads. Coil's `AsyncImage` requests the image at the rendered card constraints; title and supporting text remain bounded to two and one lines, respectively.

The shared Live, Movies, Series, search, favorites, and history item grids show three cards in narrow content panes, then four from 480dp, five from 600dp, and six from 900dp of available grid width. The number follows the content pane after navigation and padding, so it also adapts when a foldable or multi-window view changes size.

When the full catalog content pane is at least 800dp wide, its category picker becomes a 200dp vertical list next to the selected item's grid. Narrower panes retain horizontal categories above the grid. The three section filters and local search remain above both layouts; grid columns continue to follow the remaining pane width.

Artwork networking has bounded concurrency and timeouts, rejects redirects, limits a response to 8 MiB even when Content-Length is absent, and uses a 20% memory-cache ceiling (10% on low-RAM devices or devices with less than 128 MiB memory class) plus a 64 MiB disk-cache ceiling. Artwork is decorative: the card label and supporting metadata remain the accessibility description. Physical low-memory TV, provider/CDN, RTL, and D-pad qualification remains BLOCKED.

## Movie details scope

Selecting a Movie opens an explicit details destination instead of starting playback. The client requests only that Movie through `get_vod_info`, with an 8 MiB response ceiling, bounded timeouts, no redirects, defensive scalar parsing, safe artwork validation, account/generation ownership checks, and a six-hour account-scoped cache. Catalog metadata remains the fallback when optional provider details are absent. The Play action uses the original validated catalog item; provider detail metadata never rewrites playback authority. Existing eligible progress changes the action label to Resume, and account removal clears the details partition.

## Deferred contracts

Full physical-device/TV qualification, release identity/signing, provider-wide search, and other deferred playback capabilities require their own approved work. Movie details and Series episode playback, history, resume, and Continue Watching are implemented; do not infer release qualification from build and emulator checks.
