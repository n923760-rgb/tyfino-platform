# TYFINO development APK: manual test record

Use this form for a **single build on one physical Android device**. Save a separate copy per device and run. It records evidence; completing the form does not automatically grant release qualification.

## Build identity

- Date (UTC) and tester:
- GitHub Validate run URL, commit SHA, artifact name and expiry:
- APK variant: development `app-debug.apk`
- Licensing origin in this build: unset / approved test origin (record the origin's **label only**, never its URL)
- APK signing: CI temporary debug / approved local debug build
- Existing install removed before this run? Yes / No; if yes, local app state was lost

## Device identity

- Manufacturer and model:
- Phone / tablet / foldable / Android TV / Google TV:
- Android version and API level:
- RAM class and input: touch / keyboard / D-pad / remote:
- Display mode and language: compact / expanded / resized; Arabic RTL / English LTR

## Install and entry

1. In the matching private GitHub Actions **Validate** run, download its `tyfino-debug-*` artifact. Extract the ZIP and install `app-debug.apk` on the test Android device. Record the exact run and commit above. This artifact expires after seven days.
2. Confirm the device shows **TYFINO** with the approved icon/banner and the explicit trial/activation choices. Installation or app launch must not start the trial automatically.
3. A CI APK has **no licensing service origin configured**. Mark activation, IPTV login, catalog, and media scenarios `BLOCKED` for that build. Only an approved, separately configured test build can run those scenarios.
4. Never attach real activation codes, provider credentials, host URLs, playback links, authorization headers, cookies, or manifest excerpts to this record.

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
