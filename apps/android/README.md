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

Validate also attaches a separately named `tyfino-owner-test-<source SHA>-<attempt>` artifact after the normal disconnected Debug artifact. This owner development APK is built with the approved licensing origin explicitly configured. Its verifier checks the actual APK's standalone manifest, API24/37 policy without a maximum SDK restriction, absence of test-only installation, complete ARM64/ARMv7/x86/x86_64 native coverage, ELF architecture, 16KB load alignment for 64-bit libraries, 16KB native ZIP alignment and signature compatibility for API24–37. Phone and TV launcher declarations and optional touchscreen/leanback features are checked. The bundle includes exact source, APK SHA-256/size and public debug certificate evidence; it does not claim a physical-device install.

Download the GitHub artifact ZIP, extract it, then open `TYFINO-device-test.apk`. Permit installation from the file manager if Android requests it. Renaming the ZIP to `.apk` does not create an Android package. Use this configured artifact for owner activation/device checks; the ordinary `tyfino-debug-*` artifact remains disconnected by default.

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

## Compatibility scope

One standalone APK contains the four supported native architectures: ARMv7 and x86 (32-bit), ARM64 and x86_64 (64-bit). The current minimum remains Android 7.0/API24 because the selected Navigation dependency requires it. Android 5/6 support needs a separate dependency/security/API review; lowering the manifest alone is insufficient.

Phone/tablet and Android TV/Google TV launcher support share one application. Touchscreen and leanback are optional features, allowing either device class; physical TV input/media qualification remains open. The package verifier checks 16KB native ELF/ZIP alignment for newer devices, but this is not a 16KB physical runtime or universal codec guarantee.

After the phone API27 suite, the same CI job downloads the exact configured owner APK from the Android job, checks its source/hash, clean-installs it without `-t`, and launches it offline on a separate Pixel 2/API33 x86_64 emulator. It requires a running process and resumed TYFINO activity after five seconds and uploads source/hash/certificate-bound evidence. It chooses the highest available artifact attempt from this run for the same SHA, so retrying a failed phone job does not require rebuilding a successful Android job. Existing phone/tablet instrumentation remains separate from this installer check.

Exact-task/official-main results are recorded on the compatibility PR and [report](../../ENGINEERING/REPORTS/ANDROID_UNIVERSAL_COMPATIBILITY_2026-10-01.md). LG Velvet, physical TV, API24 hardware, foldables, 16KB devices and real providers/media still need their own runtime records.

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

## Interface and adaptive menus

The dark cyan/violet palette defines tonal containers and surface levels across cards, menus, fields and dialogs. Headings use stronger weights and shared Material corner sizes; no external font or UI dependency is added.

Primary navigation follows full window width: a bottom menu below 600dp, a compact 96dp side menu from 600 to 839dp, and a labeled 232dp sidebar from 840dp. Both side menus scroll in short panes. The sidebar groups browsing and preferences, preserves the five existing destinations and uses start-relative layout for Arabic RTL. A cyan tonal surface marks selection; a separate light outline marks keyboard/D-pad focus. Home account/settings actions use a quieter surface treatment, and the vertical catalog category picker has its own heading and matching surface.

The scoped report is [Android interface and menu refinement](../../ENGINEERING/REPORTS/ANDROID_INTERFACE_MENUS_2026-09-30.md). Added managed-device tests cover selection, Arabic RTL at larger font scale, short-pane access to Settings and directional focus/Enter. Exact-head CI outcomes are attached to the task PR; screenshot review, physical TV, TalkBack, overscan and performance still require separate runtime evidence.

Settings presents account actions first, the active provider subscription summary, the separate application license summary, then provider compatibility. Headings expose accessibility semantics, related controls form focus groups, and account management uses the quieter button style. The page scrolls in compact panes and keeps content centered at a maximum 840dp width on larger windows; text and buttons can grow with the current font size. Initial D-pad focus remains on Switch IPTV account, and the existing account destinations and local provider presets are preserved.

## TV and D-pad focus scope

Primary screens request a deterministic first focus target after attachment. Navigation destinations, horizontal content rails, season selectors, and adaptive catalog grids are focus groups so directional input visits related controls coherently; buttons and cards retain a visible focus border. Managed-device tests assert representative Settings, Movie details, and Series details entry targets. This is emulator evidence only: physical Android TV/Google TV traversal, overscan, Back behavior, playback controls, RTL, accessibility, and performance qualification remains `BLOCKED` until recorded under the device-qualification contract.

## Cached search scope

Each Live, Movies, and Series destination searches only the active account's already-downloaded category snapshots. The search is local (no network fetch or background full-catalog synchronization), starts after two characters and a short debounce, and shows at most 50 matches. The UI states this limitation. Provider-wide search remains deferred.

## Favorites scope

Live, Movies, and Series items can be added to an account-scoped local favorites list. Only account ID, section, provider item ID, and a timestamp are stored; titles, artwork, playback URLs, and credentials are never written to this store. A list contains at most 200 favorites per account and displays only items still present in current downloaded catalog categories. Account removal clears local favorites. The per-section Favorites filter and per-item add/remove controls support touch, keyboard, and TV D-pad input.

## Recent-history scope

Live channels, Movies, and Series episodes are recorded only after the foreground player reaches actual playback. Separate account-scoped stores retain at most 100 recent identities and store no titles, artwork, playback URLs, or credentials. Live/Movie history rejoins current downloaded catalog metadata; episode history rejoins the current published Series generation and is shown independently from resume-based Continue Watching. Missing or stale items stay hidden.

Home displays only shelves with current items in the active account's opened catalog and recent history. Once its account-scoped summary finishes loading, an empty Home shows one invitation to open a section instead of five empty shelves. Account switching and Settings remain available.

Home starts with an In the spotlight showcase, then a dedicated Continue watching rail with movie/episode progress, followed by latest Movies/Series, browsing shortcuts and recent history. The showcase interleaves up to three Movies and three Series from existing bounded latest-cache summaries, ranking available finite 0–10 ratings then added dates. These are local discovery picks, not provider-wide popularity or a full catalog scan. Explicit previous/next controls wrap without timers or automatic focus movement; the showcase opens details while the separate resume cards retain eligible playback callbacks. Artwork is decorative and missing images never hide titles/actions. No new Home provider request is introduced.

The showcase also displays optional provider-rating and release-year badges from the selected cached item. Ratings must be finite and within 0–10, are localized with at most one fractional digit, and are explicitly labeled as provider ratings (not TYFINO/IMDb scores). Years must be four ASCII digits from 1000–9999; this is presentation-shape validation, not verification of the actual release. Missing/invalid fields disappear independently without guessed values or title parsing. Badges wrap with available width, are noninteractive and do not introduce focus stops. Existing Arabic/large-font, details and directional-input tests remain. See [showcase metadata report](../../ENGINEERING/REPORTS/HOME_SHOWCASE_METADATA_2026-10-02.md).

Settings reads optional playlist subscription metadata directly from the active provider's existing `player_api.php` endpoint on entry/resume or explicit refresh. It displays a reduced provider origin, username, reported Active/Expired/Disabled status, optional trial/connection information and optional expiry. Missing, zero or malformed expiry is Not provided, never a lifetime entitlement. This in-memory metadata is not persisted, logged or sent to TYFINO services, and cannot change application licensing or saved credentials. Account ID, credential generation, request identity and destination lifecycle guard publication. Transport retains explicit HTTP consent, no redirects/TLS bypass, 8/12-second timeouts, a 256 KiB limit and no automatic retry. See [Home showcase and playlist report](../../ENGINEERING/REPORTS/HOME_SHOWCASE_PLAYLIST_2026-10-02.md).

Provider status and expiry use separate full-width tonal facts with growing text; Active uses the primary theme pair, while provider-reported Expired/Disabled uses the error pair and an explicit text label. Color is not the only indication. The displayed date and integer connection counts use the active string-resource locale, with no grouping on counts. Missing expiry remains Not provided; no countdown or device-clock entitlement is inferred. Presentation adds no focus stops, fixed height, polling or state ownership. Compact Arabic/font1.6 and replacement-state regressions are recorded in [playlist presentation report](../../ENGINEERING/REPORTS/PLAYLIST_SUMMARY_PRESENTATION_2026-10-03.md).

The foreground player uses a shorter 15–30 second, 16 MiB target buffer for Live streams or low-RAM devices and a 30–60 second buffer for Movie and Series playback on other devices. These are tuning defaults; real provider streams and lower-memory devices still need physical playback qualification.

Only Live playback retries a lost connection, timeout, or expired live window: up to three attempts after 1, 2, and 4 seconds, then the existing manual Retry/Back error state. When the default network returns during a pending delay, playback resumes that attempt immediately without adding an attempt. Each attempt rechecks the active account, foreground lifecycle, player identity, and destination exit state; leaving the player cancels pending retry work and unregisters the network callback. Access, format, and decoder failures do not automatically retry.

Settings offers two bounded provider User-Agent presets: `TYFINO/<versionName> (Android)` by default, or `VLC/3.0.0` for provider compatibility. The local preference applies to new Xtream authentication, catalog, details, EPG, and background new-content requests; a new playback session snapshots the same value for media requests and approved redirects. It never changes the licensing transport or the account's HTTP consent and redirect limits.

## Live EPG scope

The player opens the selected Live channel's program guide only when requested. A direct per-channel provider request updates an account-owned, bounded cache; playback does not wait for guide data. The dialog shows current/next programs and a lazy schedule with localized loading, empty, stale, and error states. Closing it or changing channels invalidates older results, and account removal clears the EPG cache. Physical TV D-pad, RTL, accessibility, media, and performance qualification remains BLOCKED until recorded under the device-qualification contract.

## Catalog refresh ownership

Foreground screens and the opt-in background new-content job use separate repositories for one catalog database in the default application process. They share the account/section/category operation registry and serialize local preparation, replacement and publication checks; provider requests remain concurrent. Newer work invalidates older completions across instances, and clearing an account cannot reuse a pending operation's unique token. Cache publication and movie alerts recheck the same owner.

The [catalog ownership report](../../ENGINEERING/REPORTS/ANDROID_CATALOG_SHARED_OWNERSHIP_2026-10-01.md) records the failing test-only diagnosis and correction. JVM regressions cover delayed category/item results, stale alerts, independent keys and cross-instance clear. Physical/provider/multiple-process behavior remains separately qualified.

## Catalog artwork scope

Catalog cards render already-validated artwork references lazily through one application-owned image loader. Live uses a landscape frame; Movies and Series use a poster frame. A local placeholder remains visible for missing, rejected, slow, redirected, oversized, or failed images, and the card stays navigable without waiting for artwork.

The shared card uses 12dp corners, a two-character label on a stable tonal gradient when artwork is unavailable, and a short image crossfade when artwork loads. Coil's `AsyncImage` requests the image at the rendered card constraints; title and supporting text remain bounded to two and one lines, respectively. Artwork cards reserve two title lines with untrimmed 1.8em leading, leaving room for Arabic font metrics and keeping short and long titles aligned when the system font size increases.

The shared Live, Movies, Series, search, favorites, and history item grids prefer three cards in narrow content panes, then four from 480dp, five from 600dp, and six from 900dp of available grid width. A minimum 88dp allocation per card, scaled up with the system font size and separated by 8dp gaps, reduces that count when text would become crowded. For example, a 328dp pane shows three columns at normal font size and two at 130%; a 200dp pane shows two at normal size. The count stays between one and six and follows the content pane after navigation and padding, including window resizing.

When the full catalog content pane is at least 800dp wide, its category picker becomes a 200dp vertical list next to the selected item's grid. Narrower panes retain horizontal categories above the grid. The three section filters and local search remain above both layouts; grid columns continue to follow the remaining pane width.

Before a category is selected, the ready-category guidance and its decorative destination icon are centered within the remaining item pane, below compact categories or beside the expanded sidebar. The status area is bounded to 480dp and scrolls when its content outgrows a short pane. Loading, failed and empty category lists show their own states without an impossible selection hint; valid empty category/item states retain explicit refresh callbacks. Item refresh/stale and limited-search notices occupy a row above the grid instead of overlaying the first result. Existing section/category/account ownership, selection/play/favorite callbacks and initial focus remain authoritative. See [catalog placement report](../../ENGINEERING/REPORTS/CATALOG_STATUS_PLACEMENT_2026-10-03.md).

Movies and Series can locally sort an opened category or the bounded cached search results by newest provider add date, numeric rating, or name. Missing dates and invalid ratings appear last; equal values keep provider order. Larger category sorts run away from the UI thread. Recent viewing and Favorites retain their own ordering, and sorting never downloads unopened categories.

Artwork networking has bounded concurrency and timeouts, rejects redirects, limits a response to 8 MiB even when Content-Length is absent, and uses a 20% memory-cache ceiling (10% on low-RAM devices or devices with less than 128 MiB memory class) plus a 64 MiB disk-cache ceiling. Artwork is decorative: the card label and supporting metadata remain the accessibility description. Physical low-memory TV, provider/CDN, RTL, and D-pad qualification remains BLOCKED.

## Movie details scope

Selecting a Movie opens an explicit details destination instead of starting playback. The client requests only that Movie through `get_vod_info`, with an 8 MiB response ceiling, bounded timeouts, no redirects, defensive scalar parsing, safe artwork validation, account/generation ownership checks, and a six-hour account-scoped cache. Catalog metadata remains the fallback when optional provider details are absent. The Play action uses the original validated catalog item; provider detail metadata never rewrites playback authority. Existing eligible progress changes the action label to Resume, and account removal clears the details partition.

## Deferred contracts

Full physical-device/TV qualification, release identity/signing, provider-wide search, and other deferred playback capabilities require their own approved work. Movie details and Series episode playback, history, resume, and Continue Watching are implemented; do not infer release qualification from build and emulator checks.


## Commercial experience presentation (2026-10-01)

The native app uses shared headers, panels, readable typography and visible focus across activation, account entry, Home, catalog, details, Settings and playback recovery. Home shortcuts open the existing Live/Movies/Series destinations. Account entry adds an explicit password visibility toggle, IME actions and safe-drawing/keyboard insets; pending HTTP consent still uses its existing explicit callback.

The owner selected paid licensing per device. The activation screen presents existing annual/lifetime codes and the separately chosen seven-day trial. Settings receives only license kind, server-provided expiry and offline state; no session token or installation identifier is passed to its presentation. Application licensing does not include an IPTV subscription. Existing licensing, offline policy, provider ownership, Media3 lifecycle, package identity, minimum API, ABIs and signing policy remain authoritative.

See [commercial experience report](../../ENGINEERING/REPORTS/ANDROID_COMMERCIAL_EXPERIENCE_2026-10-01.md). Exact-source CI and configured owner APK evidence belong to the task PR. A debug owner-test APK is a development artifact, not a signed commercial release. Physical TV/LG Velvet, TalkBack, real-provider/media and performance qualification remain separate.
