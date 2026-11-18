# TYFINO physical TV owner-test preflight

Status: **PREPARED TOOL / PHYSICAL DEVICE NOT TESTED**. This runbook supplements [device qualification](device-qualification-v1.md) and [manual test record](manual-test-record-template.md). Source of truth remains the exact reviewed official `main`.

## Why this exists

The current `Validate` workflow tests managed **phone** and **tablet** emulators; those successes cannot establish a real Android TV/Google TV remote, OEM decoder, HLS/TS provider, accessibility or performance result. Before the owner tests on a physical television, confirm that the downloaded development APK matches the exact official build. Do not mistake an old artifact or a self-consistent modified ZIP for a trusted build.

The offline verifier in `apps/android/scripts/tv-bundle-preflight.py` verifies ZIP membership and safe paths, embedded source/package/policy metadata, SHA-256 of the APK bytes, and **two independent expected values** supplied from verified official CI. It does **not** authenticate the GitHub download, independently verify APK signing, start the app, read device information, call the licensing API, handle IPTV credentials, or declare TV acceptance. It never extracts the ZIP.

## Current reviewed example (2026-11-18)

- Official source: `d240642b4943ba9bb0f63205cffecbca94d0b29f`.
- Official [Validate run 44262185211](https://github.com/n923760-rgb/tyfino-platform/actions/runs/44262185211): **7 of 7 PASS**; 172 native emulator cases each for API27 phone and API35 tablet, no failed/skipped cases. API33 configured development APK installation/launch PASS.
- Download the **owner-test** [artifact 14407984751](https://github.com/n923760-rgb/tyfino-platform/actions/runs/44262185211/artifacts/14407984751), not the disconnected generic Debug artifact.
- CI APK SHA-256: `e76152e06372d81472510326cacdbf4c8c7995bdc67bb89b2fce7d1d992c5ecb`.
- CI APK size: 17,673,925 bytes.
- The GitHub artifact ZIP is short-lived. On expiry, select a **new** official build and read its **new** source/digest from the matching run; do not reuse the example as proof of a different build.

From a local checkout of this repository, after downloading the ZIP, run:

```bash
python3 apps/android/scripts/tv-bundle-preflight.py \
  "/path/to/downloaded-owner-test-artifact.zip" \
  --expected-source d240642b4943ba9bb0f63205cffecbca94d0b29f \
  --expected-apk-sha256 e76152e06372d81472510326cacdbf4c8c7995bdc67bb89b2fce7d1d992c5ecb \
  --record "./tyfino-tv-owner-local-record.md"
```

This produces a **local-only** 0600 permission worksheet with all scenarios initially marked BLOCKED. The tool refuses existing report paths; use a fresh output name per device/run. To install on a TV, extract the trusted ZIP, transfer only `TYFINO-device-test.apk`, and follow the device's normal installer flow. The development key can differ across builds; **do not uninstall an existing customer copy** to overcome an update-signature mismatch. Do not call the archive itself an APK.

## Physical TV acceptance sequence

Use the TV's **actual remote only**. Record sanitized device model/API, date, exact APK identity and PASS/FAIL/BLOCKED/SKIPPED for each worksheet row. Test first-launch trial/activation choices; RTL and LTR Home/catalog/search; D-pad initial and restored focus; nested dialogs and Back; Live and movies/series playback, audio/subtitles, EPG, seek/resume; offline and decoder errors; and repeated playback exit and recovery. Only use streams you are authorized to access. Capture no IPTV hostname, username, password, activation code, stream URL, cookie, request header, device serial or signing material.

If one scenario fails, record reproducible steps and a credential-free evidence ID; correct the exact source in a separate reviewed PR and qualify it again before retesting the TV. No test result may be inferred from emulator CI or a filled worksheet alone.

## Automated proof limits

`python3 apps/android/scripts/test-tv-bundle-preflight.py` tests synthetic ZIP acceptance/rejection (tampered hashes, altered identity, malicious archive members and report overwrite controls) without any genuine APK, signing keys or Android equipment. CI must run these tests. These checks only qualify the **offline verifier**, not customer release.

Physical Android TV/phone, TalkBack, low-memory, real-media behavior, two recoverable production signing-key backups, production monitoring/recovery, signed APK upgrade continuity and customer distribution remain independent release gates. Follow [physical TV qualification issue #234](https://github.com/n923760-rgb/tyfino-platform/issues/234).