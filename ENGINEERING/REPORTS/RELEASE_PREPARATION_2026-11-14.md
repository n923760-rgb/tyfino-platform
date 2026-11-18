# TYFINO release preparation — 2026-11-14 (reconciled 2026-11-18)

Status: PREPARED FOR OWNER REVIEW — CUSTOMER PUBLICATION BLOCKED.

## Work order

Task TYFINO-20261114-release-preparation, IMPLEMENTATION (documentation only).
Owner authorizes testing, preparation and proposed publication, then asks to continue.
That authorization does not establish missing custody or physical-runtime facts.
Controller/executor Codex, sequential self-review.
Repository n923760-rgb/tyfino-platform, official/default main.
Original documented base: main@4c7e019f7adbef4f7caf19fe498f4306a72973c6, tree d9388c85721aa3c7fb31d6181aec5aca14e7db42 (historical; superseded by #233).
Live official release-review source as checked on 2026-11-18: main@d240642b4943ba9bb0f63205cffecbca94d0b29f, tree 97fc46774b23aec9cad2d249ac51fc43d9a4beb8.
The new official tree equals the exact reviewed/tested #233 head tree; the source was squash-merged without product-tree differences.
At reconciliation #233 is merged; the only open PR is this documentation-only draft #232. No assertion is made about a live local clean worktree in this session.
Task branch docs/release-preparation-20261114.
Applicable AGENTS, governance/PROJECT_PROFILE.md, ENGINEERING/MASTER_ROADMAP.md,
docs/android/direct-apk-release-v1.md and device-qualification-v1.md remain authoritative.
Scope: this candidate evidence/owner execution record only. No code, workflow,
version, signing-key, endpoint, tag, release, deployment or publication change.
Keep this preparation on a draft branch: merging changes official source, and the
signing preflight requires a freshly reviewed exact official SHA.
Stop for candidate drift, secret exposure or missing mandatory gate evidence.

## Candidate identity and verified results

- Proposed **current** development-device qualification source: `d240642b4943ba9bb0f63205cffecbca94d0b29f` (official `main` on 2026-11-18; re-read before any later protected action).
- Reviewed product tree: `97fc46774b23aec9cad2d249ac51fc43d9a4beb8`, identical to #233's reviewed exact-head source tree.
- Package: `com.tyfino.player`; version policy remains `1.0.0` / `code1` (unchanged).
- Official [Validate 44262185211](https://github.com/n923760-rgb/tyfino-platform/actions/runs/44262185211), **one attempt**, seven jobs PASS on exact merged main.
- Phone AOSP API27: **172 tests, 0 failures, 0 skips**. Tablet AOSP API35: **172 tests, 0 failures, 0 skips**. These are managed emulators, not physical TV/phone proof.
- Same-source configured owner development APK clean-install and offline launch on API33: **PASS**.
- Exact owner development APK CI artifact: [`tyfino-owner-test-d240642b4943ba9bb0f63205cffecbca94d0b29f-1`](https://github.com/n923760-rgb/tyfino-platform/actions/runs/44262185211/artifacts/14407984751), artifact ID `14407984751`.
- APK SHA256 (recorded in CI owner package verifier and API33 installed-artifact result): `e76152e06372d81472510326cacdbf4c8c7995bdc67bb89b2fce7d1d992c5ecb`.
- APK size (CI verification): `17673925` bytes. CI Debug test signing certificate SHA256: `4b974c8223e5d7f1a172b5bad965a33ae4d96cd70896cc6ec516c9894b6908ad`; **not** an approved production signing certificate.
- Manifest/ABI/signature/16KB compatibility verifier PASS on this build; physical-install proof is explicitly NOT RUN. Bundle retention is time limited, so a vanished ZIP requires a new exact-source build/qualification rather than use of a different version.
- Source-level, no-key signing guard suite of 14 tests was recorded on the **older 4c7e candidate**; current run passed deployment configuration gates but this document does not claim that the older local guard execution was rerun against d240.
- Vision is a Debug-only synthetic SDR laboratory, not an applied HDR/AI enhancement to IPTV playback.
- This is **only a configured development APK** signed with a temporary Debug identity; no customer-signed Release APK or published customer build exists from the reconciled source.

Previous documented candidate `4c7e019f...`, [Validate 43584042490](https://github.com/n923760-rgb/tyfino-platform/actions/runs/43584042490), owner artifact `14109975635` and its 168/device tests remain historical evidence. They are **not** the latest reviewed source or the APK for current owner testing.

## Outstanding release gates

| Gate | Status | Evidence needed |
| --- | --- | --- |
| Two independent encrypted signing-key backups | UNKNOWN | Owner confirms two separate controlled copies |
| Recovery of both copies | UNKNOWN | Each opens successfully and exposes the same approved certificate fingerprint |
| Strong independent signing passwords | UNKNOWN | Owner-controlled confirmation; never disclose the values |
| Current physical phone qualification | NOT RUN | Device/build/scenario record, actual provider/media and lifecycle checks |
| Current physical Android TV/Google TV qualification | NOT RUN | Remote-only navigation, playback, Back and recovery; track acceptance in [issue #234](https://github.com/n923760-rgb/tyfino-platform/issues/234) |
| Low-memory/foldable/accessibility/performance gates | NOT RUN | Applicable device evidence under device-qualification-v1.md |
| Production operational recovery | UNKNOWN | Current off-site restore, monitoring, retention and rollback evidence |
| Exact-source signed Release build | NOT RUN | Protected workflow on final reviewed official SHA after prerequisites |
| Signed artifact install/upgrade and certificate continuity | NOT RUN | Exact signed APK/device result and public certificate/checksum |
| Customer publication | BLOCKED | All applicable gates complete, exact artifact and destination approved |

No owner evidence establishing recovery of both encrypted signing-key backups or current physical-phone/TV qualification has been supplied to this record. Historical signed-build success does not prove backup recoverability. Passing managed emulator tests is not physical/OEM/real-provider/performance qualification.

## Physical qualification execution record

Use docs/android/manual-test-record-template.md for each device. Keep credentials,
activation codes, provider URLs and keys out of records.

1. Download the exact configured owner bundle above; compare source-sha.txt and APK hash.
2. Extract the ZIP and install TYFINO-device-test.apk.
3. On a phone, test Arabic/English layout, explicit trial/activation, Xtream login,
   Live channels, Movie/Series seek/resume, subtitles/audio and background/Back.
4. On TV/Google TV, perform the same applicable journeys using only the remote;
   verify visible initial/restored focus and no inaccessible dialog action.
5. Check offline/reconnection/error recovery and repeated entry/exit.
6. Record model, API, build SHA, each scenario PASS/FAIL and sanitized evidence.
7. A debug install does not qualify production-signature upgrade continuity.
   Never remove a customer installation merely to make an incompatible test APK install.

## Signing and distribution sequence

1. Close the custody, runtime and operational gates above with attributable records.
2. Re-read official main; if it moved, review and qualify the new exact candidate.
3. Under the owner's signing authorization, dispatch Build signed APK on official main
   with expected_source_sha set to that final full40-character SHA.
4. Retain resulting APK, checksum, certificate report and source-sha.txt.
   Verify certificate matches the permanent approved production identity.
5. Qualify exact signed install/upgrade and data continuity. A new key is not an update path.
6. Approve exact release bytes and distribution destination; only then publish.
   Direct APK/Downloader distribution remains the approved direction; no customer
   hosting upload or Downloader listing was performed here.
7. Keep the previous accepted artifact and operational rollback/recovery procedure.

Never generate or retain a signing key on the public TYFINO VPS. Never send a key,
password, recovery file or secret value through chat or commit it to GitHub.

## Preparation result

PASS: 2026-11-18 official-main identity/tree, full seven-job Validate for current source, emulator 172/device, API33 offline clean-install, and CI-recorded exact owner APK hash/size.
PASS: documentation-only scope review, with key values, provider details and credentials excluded.
NOT RUN: separate new native tests for this documentation-only branch; official current-source tests above are attributable to the unchanged product tree.
BLOCKED: customer release pending recoverable key custody, exact signed final APK, physical phone/TV/low-resource/accessibility/media evidence, and operational recovery.
No signing workflow, tag, release, customer upload, infrastructure mutation or publication performed.
This record complements the canonical roadmap; it does not replace it.
