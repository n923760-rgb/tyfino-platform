# TYFINO development APK: manual test record

Use this form for a **single build on one physical Android device**. Save a separate copy per device and run. It records evidence; completing the form does not automatically grant release qualification.

## Build identity

- Date (UTC) and tester:
- GitHub Validate or Build device test APK run URL, commit SHA, artifact name and expiry:
- APK variant: development `app-debug.apk` / owner `TYFINO-device-test.apk`
- Licensing origin in this build: unset / approved production origin / separately approved test origin (record a **label only**, never a URL)
- APK signing: temporary debug key / approved local debug build
- Existing install removed before this run? Yes / No; if yes, local app state was lost

## Device identity

- Manufacturer and model:
- Phone / tablet / foldable / Android TV / Google TV:
- Android version and API level:
- RAM class and input: touch / keyboard / D-pad / remote:
- Display mode and language: compact / expanded / resized; Arabic RTL / English LTR

## Install and entry

1. For entry/UI-only checks, download the `tyfino-debug-*` artifact from the matching **Validate** run. Its `app-debug.apk` has no licensing origin; mark activation, IPTV login, catalog, and media scenarios `BLOCKED`.
2. For full owner testing, manually trigger **Build device test APK** on official `main`. Download its `tyfino-device-test-*` artifact, verify `source-sha.txt` and `TYFINO-device-test.apk.sha256`, then install `TYFINO-device-test.apk`. This Debug build has the already approved production licensing origin configured; it is not a customer release. Both artifact types expire after seven days.
3. Confirm the device shows **TYFINO** with the approved icon/banner and explicit trial/activation choices. Installing or opening it must not start a trial. Use only an owner-controlled test activation or deliberately start a test trial on that device; keep any code private.
4. The CI Debug signing key is temporary and may differ between runs. If an update is rejected, remove the previous test installation only after recording that its local state will be lost. Never attach real activation codes, provider credentials, host URLs, playback links, authorization headers, cookies, or manifest excerpts to this record.

## Scenario ledger

For every row use only `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED`. Record brief steps and sanitized evidence. A run on one device cannot qualify other device profiles.

| Scenario | Status | Steps, observed result, sanitized evidence |
| --- | --- | --- |
| Explicit licensing choices at first launch; no automatic trial | BLOCKED | |
| Arabic RTL and English LTR layout, clipping and TalkBack reading order | BLOCKED | |
| Phone touch navigation and Back behavior | BLOCKED | |
| TV D-pad: safe initial focus, visible focus, dialog cancel, Back and player controls | BLOCKED | |
| Fold/unfold or resize during catalog and playback | BLOCKED | |
| Live HLS/TS playback and channel switching | BLOCKED | |
| Movie seek/resume and Series episode resume | BLOCKED | |
| Audio, subtitles, pause/background/exit and release | BLOCKED | |
| Offline, timeout, invalid credentials, unsupported media and decoder failure | BLOCKED | |
| Cold start, catalog scroll, player start, CPU and memory on API 24-class hardware | BLOCKED | |

The `BLOCKED` entries are placeholders, not test results. Change a row to `PASS` or `FAIL` only after that exact scenario was executed on the recorded build and device. Use `SKIPPED` only when the scenario was deliberately out of scope for this run. Link credential-free screenshots or logs by evidence ID, without including secrets or provider identifiers.

## Outcome

- Failures and reproducible steps:
- Sanitized evidence IDs:
- Outstanding `BLOCKED` rows and required fixture/device:
- Retest commit and device:
