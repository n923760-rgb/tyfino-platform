# Configured owner-test APK and installation evidence — 2026-10-01

## Task packet
IMPLEMENTATION; repository n923760-rgb/tyfino-platform; official main baseline ab799ea006a032cfd7c119abdfe52e34847d3fec; task branch build/verified-owner-test-apk-2026-10-01.
User requested install link, reported LG Velvet Android13/no existing TYFINO/generic "App not installed", then requested continued review. Prior explicit authority covers reviewed improvements/merges and owner development testing. Temporary Debug model only; no protected production key, release, publication or deployment.
Root/central/project/profile/roadmap, Android README, device/direct-APK and task/environment/CI contracts read; live gates precede mutation. GitHub API/source review; local shell/SDK/device unavailable.
Six files: existing ci.yml, new Android verifier script, Android README, device qualification ledger, canonical roadmap and this report.
Stop on source movement/conflicting work, secret exposure, unrelated policy change or unavailable required evidence.

## Confirmed gap and unknown cause
Ordinary Validate Debug builds use empty licensing origin, as documented. The previous artifact link therefore cannot support a complete activation/device journey. Existing manual device-test workflow explicitly configures the approved origin; no callable dispatch tool exists here.
The owner's generic installer error does not identify a cause. Exact filename/extended installer result are pending. minSdk24 source compatibility with Android13 does not prove OEM installation. No SDK/ABI/signing identity change is inferred.

## Implementation
After normal Debug build/tests/lint/artifact upload, the existing Android job explicitly rebuilds Debug with the approved https://api.tyfino.online property. A separate seven-day tyfino-owner-test source/attempt artifact contains TYFINO-device-test.apk, source SHA, APK checksum/size, public certificate and installation metadata.
The script inspects actual APK bytes: ZIP CRC, standalone manifest, exact application/version/min24/target37 policy, no testOnly, ARM64 support when native libraries exist, approved origin presence in DEX, signature verification for API24–33, native ZIP alignment. It writes only bounded package metadata and public Debug certificate output, without manifest excerpts/secrets. No device/provider/backend request occurs.
All six checkout/exact-source job definitions and seven job surfaces remain. Ordinary disconnected Debug is uploaded before the configured rebuild. Default product configuration, identity, version, ABI policy, production signing/endpoint, dependencies and optimized unsigned Release are unchanged.

## Evidence and limits
Source checks at preparation: six-file coherent scope; existing CI contract markers/counts preserved; verifier uses Python standard library and installed Android SDK tools. Actual script/build/qualification results belong to this task PR.
Required exact-head validation: actual APK inspection plus all seven Validate jobs, then reviewed merge and official-main artifact verification.
NOT RUN: local SDK/Python runtime, API33 emulator install, physical LG Velvet installation, real activation/provider/media, customer distribution, protected release signing or production.
Signature verification across API24–33 and ARM64 metadata are package evidence, not an OEM install PASS or a demonstrated root-cause fix.

## Owner instructions
Download the artifact ZIP; extract it; open TYFINO-device-test.apk. Do not rename the ZIP into APK. Permit installation from the file manager if Android requests it. Identify results by the exact source/checksum metadata.
Record the actual installer result on LG Velvet Android13 before claiming physical compatibility. Use the existing manual test record for later activation/provider journeys.

## Parent state
#175 catalog correction is merged. Exact head a79e7ca0771e7f969d06284b35558c1bcf3d6d98, Validate36822547067 all seven PASS; phone/tablet64 each, zero failures/skips; Chromium nine PASS.
Parent official-main Validate36855923633 completion is recorded on #175 and verified before this task merge. Earlier #166–#174 remain integrated.
