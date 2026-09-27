# TYFINO V1 Test Matrix

Status: EXECUTION PLAN — physical/provider qualification pending
Planning baseline: `main@bb71bbb7d75ab5f083e63d1c263e07be6e318821` (2026-09-27)

## Authority and evidence

This matrix selects journeys, environments, and fixtures for V1 qualification. The authoritative Android result ledger is [`docs/android/device-qualification-v1.md`](../android/device-qualification-v1.md); record each physical build/device run using the [manual test record](../android/manual-test-record-template.md). Do not mark this planning table PASS in place of a run record. The current V1 scope and data boundaries remain in `apps/android/README.md`, `docs/licensing-trial-contract-v1.md`, `docs/data-ownership.md`, and the scoped Android contracts.

The repository CI builds Debug and optimized unsigned Release APKs, runs Android JVM/lint and API/Admin/database checks, and exercises API 27 phone and API 35 tablet **managed emulators**. It does not prove physical Android TV, Google TV, API 24-class, foldable, real-provider/media, TalkBack, or release performance. Validate #441 on PR #162 head `baee5418b889fc209ee2268968a2f3f6a75c05ad` finished 7/7 jobs after one tablet rerun; the first attempt failed `SettingsScreenTest.accountSwitcherReceivesInitialDpadFocus` (55/56). Treat this as a test-stability finding for a separate Android diagnosis, not as physical focus evidence.

## Device and input coverage

Select actual owned/available devices before execution. Model names below are examples of classes, not procurement commitments or claims of existing test hardware. A single device may cover several rows if its profile is recorded.

| ID | Profile | Required interaction and observation | Current qualification |
| --- | --- | --- | --- |
| D1 | Lower-performance API 24-class phone or TV, low RAM | cold start, scrolling, player lifecycle, decoder and memory pressure | BLOCKED — physical device/evidence |
| D2 | Representative current Android phone | touch, portrait/landscape, media, background/return | BLOCKED — physical device/evidence |
| D3 | Tablet / large display | expanded layout, rotation, touch and keyboard if available | BLOCKED — physical device/evidence |
| D4 | Foldable or resizable window | fold/unfold, resize, multi-window while browsing and playing | BLOCKED — physical device/evidence |
| D5 | Android TV or Google TV with remote | non-touch launch, D-pad focus, Back, overscan/safe area, media controls | BLOCKED — physical device/evidence |
| D6 | Managed emulators: API 27 phone and API 35 tablet | repeatable synthetic smoke and instrumentation | Automated CI evidence only; not D1–D5 |

Record Android/API, model, RAM class, ABI, display, language, input device, source SHA, APK variant/hash and signing identity for each run. Coverage must include Arabic RTL and mixed Arabic/English text; TalkBack belongs on a device with accessibility services enabled. Do not infer D1 coverage from the API 27 emulator.

## Test accounts, network, and media fixtures

- Licensing: owner-controlled test trial/code and Admin actions with no production code in reports. Use the approved owner device-test APK for network journeys; ordinary CI Debug artifacts have no licensing origin and cannot qualify activation or IPTV sign-in.
- Provider: an authorized Xtream test account with licensed fixture content, plus controlled HTTPS/EPG, HTTP-only with explicit warning, slow/timeout, missing EPG, malformed artwork, and large-catalog cases. Record a non-secret fixture label only. Never publish a provider host, username, password, stream URL, token, or redirect target.
- Media: representative Live HLS/TS, progressive Movie seek, Series episode, selectable audio/embedded subtitles where supplied, missing/unsupported media, non-seekable content, and decoder error. A provider feature that is absent should be marked SKIPPED for that fixture and covered using another suitable fixture where required.
- Network: good connection, offline before request, loss during playback, delayed response, invalid TLS, provider 4xx/5xx, and rate-limited licensing. Never disable TLS or alter a real provider to simulate failure.

## Journey matrix

Run each applicable scenario on at least the profiles specified. Record observed behavior and sanitized screenshot/video/log evidence under the manual record. `BLOCKED` below means evidence is still required; it is not a judgment that the implementation fails.

| ID | Journey and expected boundary | Minimum profiles | Current physical result |
| --- | --- | --- | --- |
| L1 | First launch offers explicit trial/activation; install/open does not start trial | D2, D5 | BLOCKED |
| L2 | Trial begins only by action, expires by server time; activation binds one installation; revoke/reset and 12-hour refresh/72-hour offline behavior follow contract | D2, D5; backend tests for races | BLOCKED |
| L3 | Reboot, clock rollback, offline expiry, failed refresh and recovery never extend authorization; errors remain actionable | D2, D1 | BLOCKED |
| A1 | Valid/invalid Xtream login, slow/unavailable provider, HTTP consent, invalid TLS, no catalog fetch during authentication | D2, D5 | BLOCKED |
| A2 | Add/switch/remove up to eight accounts; pending responses cannot overwrite active account; removal clears only that account's local partitions | D2, D5, D1 | BLOCKED |
| C1 | Live/Movie/Series categories, all/favorites/recent filters, local cached search, empty/stale/error states and manual refresh | D2, D3, D5 | BLOCKED |
| C2 | Large catalog and slow/malformed artwork retain responsive D-pad/touch navigation and safe placeholders | D1, D5, D2 | BLOCKED |
| P1 | Live HLS/TS starts, channel change/previous channel works after actual playback, EPG current/next and absent/failed/stale guide do not block playback | D2, D5 | BLOCKED |
| P2 | Movie details and Play/Resume; Series details, seasons/episodes and Continue Watching resume from an eligible checkpoint | D2, D5 | BLOCKED |
| P3 | Audio/subtitle selection and Off/Automatic states, seek/pause/Back, background/foreground, repeated entry/exit release resources | D2, D5, D1 | BLOCKED |
| P4 | Offline/timeout, invalid redirect, unsupported/non-seekable media, decoder failure and recovery display safe states without leaked playback/network work | D2, D5, D1 | BLOCKED |
| U1 | Arabic RTL and mixed-language labels, 200% font, TalkBack order/labels, no secrets or raw provider IDs in announcements | D2, D5, D3 | BLOCKED |
| U2 | D-pad first focus, visible focus, directional travel, dialogs, Back and focus restoration across navigation and player | D5, D3 with keyboard | BLOCKED |
| U3 | Rotate/fold/resize while browsing or playing; maintain usable layout, state ownership and safe Back behavior | D4, D3, D2 | BLOCKED |
| R1 | Long playback (at least two hours), rapid account/channel changes, process death/return, low-memory eviction and repeated player release | D1, D5, D2 | BLOCKED |
| S1 | No IPTV credentials/catalog/viewing data sent to TYFINO API, logs, backups or Admin; local sensitive data excluded from backup/data extraction | source tests plus D2 release-like build | BLOCKED for release-device verification |
| O1 | Production-like backup/restore, audit integrity, readiness, alerts, rollback and actual RTO/RPO | approved staging target | BLOCKED — operations environment |

The current `main` UI is the baseline for screenshots. PR #159 proposes changes to Movie/Series sort and episode history presentation; if it merges, update affected C1/P2 assertions and retest against its new exact source before claiming those controls qualified.

## Execution order and result rules

1. Record the exact candidate SHA and artifact hash, provision authorized fixtures, then run automated CI on that exact source. A red job is FAIL until diagnosed; a rerun success must retain the first-attempt failure in evidence.
2. Run entry/licensing and account-isolation journeys before provider media. Then exercise basic content/media, negative network cases, accessibility/RTL, TV/foldable/low-memory, and sustained playback.
3. Measure startup, frame pacing, memory, account switch, and API latency as described in [`docs/architecture/PERFORMANCE_BUDGETS.md`](../architecture/PERFORMANCE_BUDGETS.md). Candidate budgets are not release gates until measured and approved.
4. Use `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED` per executed manual scenario. `SKIPPED` needs a reason and cannot close a required V1 gap. Include reproduction steps and sanitized evidence on FAIL; retest on a new SHA when changed code affects the journey.
5. Promote the relevant rows in the authoritative device ledger only when exact build/device/fixture evidence exists. CI and source reviews retain their own result scope. Release remains blocked until signing custody, production operations, physical/media, and required owner decisions are satisfied.

This plan authorizes no signing, production deployment, purchase, or collection of customer viewing data.
