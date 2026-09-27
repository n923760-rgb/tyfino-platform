# TYFINO Performance Measurement and Budgets

Status: PROPOSED TARGETS — physical and production measurements pending
Baseline: `main@41c046ad313d447d8d5213f3a431cadf7cbf4df5` (2026-09-27)

## Measured versus proposed

CI builds an optimized unsigned Android release and records its exact byte size and checksum. It also runs JVM tests, lint, and API 27 phone/API 35 tablet managed-device instrumentation. Those jobs do not measure production startup, frame pacing, memory, provider artwork latency, playback stability, or API latency under load. Physical TV, API 24-class, foldable, RTL/accessibility, and real-provider qualification remains blocked under `docs/android/device-qualification-v1.md`.

The following numbers from the owner-supplied review are **evaluation targets**, not current PASS results or CI gates:

| Journey or resource | Candidate target | Measurement prerequisite |
| --- | --- | --- |
| Cold/warm launch to usable navigation | under 2,000 / 800 ms | repeated release-like starts on reference hardware, p50/p95 |
| UI frame time while scrolling/navigation | under 16 ms target frame, report missed frames | physical TV and lower-end phone traces |
| Idle/playing process memory | under 120 / 250 MiB | define PSS versus heap, foreground state, stream/decoder, repeated samples |
| Optimized APK size | under 20 MiB candidate | compare exact unsigned and signed APK bytes; review growth and ABI impact |
| Artwork visible or placeholder | under 500 ms candidate | local versus network/cache state and provider conditions |
| Local cached search response | under 100 ms candidate | active-account catalog sizes and debounce excluded from query timing |
| Account switch to usable destination | under 2 s candidate | 1/8 accounts, hot/cold cache, pending work, low-memory device |
| API p50/p95/p99 | under 100/300/800 ms candidate | production-like route classes and representative traffic |
| API 5xx share | under 0.5% candidate | exclude intentional 4xx, define window and minimum sample count |

The suggested EPG/cache 10/50 MiB per-account limits cannot be asserted from source without measuring the actual storage and memory definitions. The implemented protections instead include a 20-channel EPG cache, bounded entries/responses, account-scoped catalogs, artwork memory/disk ceilings, and bounded player buffers described in `apps/android/README.md` and scoped contracts. Do not turn proposed memory totals into hard limits without a realistic large-catalog fixture.

## Reference matrix and method

Use a representative low-RAM Android TV/Google TV, an API 24-class lower-end device, a current phone, tablet, and foldable/resizable surface. Record manufacturer/model, OS/API, RAM class, ABI, display/input, build SHA, APK hash and signing variant. Use the same credential-free fixture or authorized provider scenario across comparisons and record network conditions. At least one large catalog and one slow or malformed provider response should be included without storing IPTV credentials or stream URLs in evidence.

Measure cold and warm launch separately, the first usable interaction, scroll and D-pad traversal, account switch during pending requests, artwork fallback, Live/VOD start and two-hour playback memory, and API load under realistic concurrency. Report distributions and regressions against a measured baseline, not a single best run. Development Debug APK and emulator numbers must remain separate from optimized signed release and physical-device evidence.

## CI and decision rules

The current CI size metadata is observational. A hard 20 MiB APK gate or per-PR physical startup/memory gate is **not enabled** by this document. First establish repeatable reference hardware, baseline variance, and a size policy that distinguishes unsigned APK, signed APK, and any future AAB. Then approve tolerances and add the smallest reliable gate. Treat a regression that affects navigation, playback, or low-memory survival as a release concern even when it falls below a headline budget.

Backend metrics collection and alerts are proposed in `docs/operations/OBSERVABILITY.md`; no unverified p95 or error-rate result should be reported as production evidence. Record profiles/traces without customer data, credentials, provider URLs, or content history.
