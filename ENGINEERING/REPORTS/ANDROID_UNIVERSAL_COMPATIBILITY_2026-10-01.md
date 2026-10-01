# Universal Android package compatibility — 2026-10-01

## Task packet
Task type: IMPLEMENTATION, including bounded runtime validation for one coherent compatibility feature. Repository n923760-rgb/tyfino-platform; official main starting 28df1d451281d82aba8f2d3c0a51bc5490db4d1b; branch qualify/universal-android-apk-2026-10-01.
Owner requests a version across Android devices; prior explicit reviewed-merge authority persists. Optional oldest-version question offered; current min24 is unchanged while existing supported versions are independently qualified. Root AGENTS/central/project/profile/roadmap, architecture/security/data ownership, Android/device/direct-APK and task/environment/CI instructions read. Live identity/default/main/open-PR/branch/capability/ruleset gate before mutation.
Authenticated GitHub API/source review; no local shell/Python/SDK/physical devices. Existing exact-source Actions executes package and emulator verification. Seven jobs/six checkout definitions retained.
Eight files: ci.yml, existing APK verifier, new API33 installer script, Android README, device qualification, environment contract, canonical roadmap and this report.
Stop on source movement/conflicts, missing required evidence, secrets, speculative identity/version/dependency/signing/API changes or universal claims beyond evidence. No production keys/release/deployment/publication.

## Existing facts and scope
Official main #175/#176 integrated; Validate36859421813 all seven PASS, 64 cases per managed device zero failures/skips, Chromium nine scenarios PASS.
Previous APK already contains all four ABIs and min24/target37, with optional touchscreen/leanback and phone/TV launchers. Packaging supports many devices but that does not prove every OEM, older OS or codec.
Current Navigation requires min24; Android5/6 cannot be supported merely by editing minSdk. No minimum, dependency, ABI filter, identity/version or endpoint policy changes in this round.
The previous verifier only required ARM64 and API24–33 signatures/4KB ZIP alignment; API33 installation was NOT RUN. The owner's LG Velvet error cause remains UNKNOWN.

## Implementation
Strengthen real APK checks: no maximum SDK, all ARM64/ARMv7/x86/x86_64 with complete same-name native coverage, correct ELF class/machine, 16KB PT_LOAD alignment and offset congruence for 64-bit libraries, 16KB native ZIP alignment, signatureAPI24–37, phone/TV launcher/banner and optional touchscreen/leanback.
Add exact-artifact API33 clean installation: existing instrumentation job depends on Android's owner artifact. After preserved API27 tests, download only this run's same-SHA owner artifacts, select highest build attempt, verify source/checksum and use a separate wiped Pixel2/google_apis/x86_64 API33 emulator. Retries can reuse a successful prior-attempt Android artifact without wildcard source selection.
Disable network through airplane mode/Wi-Fi/data commands. Require no preinstalled package, adb install Success without -t/-r/grant/bypass flags, launcher Status ok, live process and resumed TYFINO activity after five seconds. Persist bounded source/hash/certificate/API/ABI/results JSON; always terminate this emulator. No raw dumpsys/UI/provider/logcat material uploaded; no activation/provider action invoked.
The serial installer check shares the existing phone runner after managed tests end. Seven-job/six-checkout exact-source contract retained; all existing assertions and ordinary/configured/optimized builds remain.

## Required proof
Actual verifier/build/unit/lint results, original managed64 cases per device, nine Chromium scenarios and all seven jobs on exact head. API33 installer result must bind the downloaded APK checksum and certificate to the same source; successful shell command alone is insufficient.
Full eight-file diff review before expected-head merge, official-main qualification afterward; task PR records exact heads/run IDs/artifact hashes/first causal failures.
Local commands and physical OEM/TV/API24/16KB/foldable/provider/performance qualification NOT RUN. Static16KB checks are not a 16KB device run. Emulator x86_64 is not ARM physical evidence or all-device guarantee.
Next: exact owner artifact download and LG Velvet retry, oldest-version decision if expanded, then appropriate physical/runtime matrix.

## First qualification failure
Exact head74d31188c87cde77734d956bb3d537eed1f7331e, Validate36861939450: Android package/ELF/ZIP16KB/signature checks PASS; API/Admin/database/governance/tablet jobs PASS. Phone job110368792713 ran original64 cases with zero failures/skips and downloaded/verified the exact artifact, then the API33 emulator exited before boot. No APK installation/launch occurred; this is not an installer rejection or OEM cause evidence.
The emulator startup log was retained only on the ephemeral runner, so the initial result lacked the cause. Add bounded redacted ERROR/FATAL/PANIC/WARNING startup diagnostics without weakening install assertions or speculative product changes. Subsequent exact-head results belong to PR #177.

## Confirmed harness root cause and correction
Diagnostic head f6cf14da5f76487117a32e5806ffe011b5b7e6fb, Validate36863203617 phone job110372841674: original64 cases PASS, then emulator exit1. Bounded startup error: Unknown AVD name tyfino_clean_install_api33; no matching .ini in emulator's searched directory. Creation and launch disagreed on AVD registry location, before any APK install.
Set task-local ANDROID_USER_HOME/ANDROID_AVD_HOME consistently for SDK manager, AVD manager and emulator, create an explicit AVD path, and require the .ini registry to exist before launch. HOME/production/user devices unchanged. Keep all package/install/offline/resume assertions and startup diagnostics. No speculative product fix or installer root cause claim. Subsequent exact-head results recorded on #177.
