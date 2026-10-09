# TYFINO Master Engineering Roadmap

Canonical path: `/ENGINEERING/MASTER_ROADMAP.md`

Status: ACTIVE — RECONCILE WITH LIVE EVIDENCE

2026-09-30 clipboard round: a bounded Admin correction awaits clipboard completion before success, handles denied/unavailable writes with manual-copy guidance, and disables duplicate clicks while pending. Source baseline is `main@ef420e7e9c0d968f271ada14bce1ab3076a655c7`; exact-head CI evidence belongs to the task PR. Details: [Admin clipboard result](REPORTS/ADMIN_CLIPBOARD_RESULT_2026-09-30.md). At that round's source checkpoint, PRs #166 and #167 were separate, unmerged changes.

This is the single permanent engineering roadmap for TYFINO. Detailed reports belong under `/ENGINEERING/REPORTS/`; evidence indexes/metadata belong under `/ENGINEERING/EVIDENCE/`.

## 1. Current Verified State

Repository: `n923760-rgb/tyfino-platform`
Official branch: `main`
Baseline official HEAD: `cd83a1c23819ae972185c748e7487156890a7dbf`
Baseline verified: 2026-09-27
Available execution mode: authenticated GitHub repository connector/API plus local source checkout; no physical-device or production-runtime proof in this round.
Historical active product PR at baseline: #159 — Android Movies/Series sorting/history UI refinement.
Current engineering phase: V1 product refinement + release/physical qualification.

The baseline SHA is historical evidence for the re-baseline only. Re-query live `main` before every mutation.

### 2026-09-30 live reconciliation

Inspected official source: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`.
Execution mode for this round: authenticated GitHub API/MCP plus JavaScript source-level checks; existing GitHub Actions provides build/integration validation. Local shell/PostgreSQL/Android/browser/physical-device execution is unavailable.
No open PRs were returned at this checkpoint. PRs #159, #164 and #165 are merged.
Validate run 36665625417 and device-test APK run 36666243511 succeeded on this official source. These are source/build/emulator evidence only.
The earlier baseline SHA and active-PR entries above are historical context.

### 2026-09-30 integrated source checkpoint

Owner instruction: merge all prepared changes and continue application improvements with full authority. PRs #166–#171 were squash-merged through their expected heads into official `main@904f74621e342adb50075757345999ff9fdfd649`. This is the current source checkpoint for the Settings refinement; earlier round notes describe their original source/authority snapshots.

PASS: [official-main Validate 36769206816](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36769206816), push event, all seven jobs. Both managed devices ran 62 cases with zero failures or skips, including the combined navigation and catalog changes. Exact source composition matched all seven shared files; no prepared change was lost.

Current bounded UI round: organize Settings into account and provider sections with heading semantics, adaptive centered width and preserved actions/focus/preferences. Report: `/ENGINEERING/REPORTS/ANDROID_SETTINGS_SECTIONS_2026-09-30.md`. Exact new-head qualification is recorded on its PR. No production deployment, signing or release has occurred.


### 2026-09-30 Admin modal source checkpoint

Settings PR #172 is merged into official `main@a21342df6ba3ca8465b5ef6b179bb145c8f7f5d6`. Exact-head [Validate 36772162343](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36772162343) passed all seven jobs with 64 cases per managed device, zero failures/skips. Its first phone job failed during emulator ZIP installation before tests; retrying only that job on unchanged source passed. Official-main [Validate 36779000481](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36779000481) was in progress at this checkpoint.

Current bounded round: correct shared Admin modal keyboard containment, background inertness, opener restoration and unwanted refocus across parent rerenders. Native browser proof runs the actual development-mode app with synthetic intercepted API fixtures in the existing exact-source Admin CI job. Report: [Admin modal keyboard](REPORTS/ADMIN_MODAL_KEYBOARD_2026-09-30.md). Exact new-head results belong to its PR; screen-reader/other-browser/physical/production proof is separate.


### 2026-09-30 Admin sidebar source checkpoint

The scoped modal correction #173 is merged into official `main@0f7720d26c17c2235df9e7661efe92285cce97ba`. Exact-head [Validate 36780321130](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36780321130) passed all seven jobs with eight Chromium dialog scenarios and 64 cases per managed Android device, zero failures/skips. Earlier browser failures and their product corrections are preserved in its report/PR. Official-main [Validate 36781046104](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36781046104) was in progress at this checkpoint.

Current bounded round: fix the confirmed RTL sidebar/content overlap found by an ordinary browser click, keep navigation visible during document scroll and expose account controls on short viewports. The sidebar now occupies its existing grid track with sticky positioning and internal vertical overflow. The existing browser suite adds actual geometry and pointer reachability on a short RTL desktop viewport; its eight modal cases and compact coverage remain. Report: [Admin sidebar RTL](REPORTS/ADMIN_SIDEBAR_RTL_2026-09-30.md). Exact-head/official-main evidence is recorded on the task PR.

### 2026-10-01 live reconciliation and catalog ownership

Official source inspected: `cd3f9a8c219e188f9d51c6d2c6502ffb730aef2d`. #166–#174 are merged, initial open PRs: none. [Official-main Validate 36781939130](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36781939130): PASS all seven jobs. Older pending/authority notes are historical snapshots; subsequent explicit owner authority covers continued application improvements and reviewed merges.

FACT: separate foreground/background catalog repositories allowed an older same-key response to replace newer data. Test-only source `7b3114b3501eeb2765fc7c1f7ea2eb51653b381a`, [Validate 36798162851](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36798162851), Android job 110166263879: 181 tests executed, two cross-instance categories/items failures, different-key control passed; other six jobs passed. Product source unchanged. This is expected diagnostic FAIL evidence.
Current correction #175 shares process operation ownership/local lock, uses unique tokens through clear/new-request ordering, and rechecks publication/alerts. Four regression scenarios; [report](REPORTS/ANDROID_CATALOG_SHARED_OWNERSHIP_2026-10-01.md). Exact corrected-head proof belongs to the PR.
Installation: user reports LG Velvet/Android13/no existing TYFINO and "App not installed"; exact installer cause UNKNOWN. Standard CI Debug has empty licensing origin. Physical install NOT RUN; configured owner-test build and exact metadata/signature evidence need a separate round.

### 2026-10-01 configured owner-test APK checkpoint

Catalog ownership #175 is merged at `ab799ea006a032cfd7c119abdfe52e34847d3fec`. Exact-head [Validate 36822547067](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36822547067) passed all seven jobs, nine Chromium cases and 64 cases per managed device with zero failures/skips. Diagnostic failure remains recorded; official-main Validate 36855923633 was in progress at this checkpoint and is recorded on #175.

Current bounded task: create a separately named configured owner-test Debug APK with actual package/signature/ARM64/SDK/standalone/testOnly/CRC/alignment and exact source/hash/certificate evidence in the existing Android CI job. Ordinary Debug remains disconnected by default. [Report](REPORTS/ANDROID_OWNER_TEST_APK_2026-10-01.md).
User-reported LG Velvet Android13 installation failure remains UNKNOWN in cause. APK static/signature evidence is not physical/emulator install proof; no speculative SDK/ABI/signing change is authorized by a generic error.

### 2026-10-01 universal compatibility checkpoint

Official source inspected: `28df1d451281d82aba8f2d3c0a51bc5490db4d1b`; #175/#176 are merged. [Official-main Validate36859421813](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36859421813) passed all seven jobs; no open PRs at the live gate. The source/hash/certificate-bound owner APK contains ARM64/ARMv7/x86/x86_64 and is min24/target37, standalone/no testOnly; earlier signature inspection ended at API33.
Owner now requests a version across Android devices. Current bounded scope strengthens universal package/16KB/optional-feature/signature checks and adds an actual exact-artifact clean install/launch on API33. Existing minimum remains Android7/API24; Android5/6 requires a separate dependency/security/API compatibility decision, not an unsupported minSdk edit. [Report](REPORTS/ANDROID_UNIVERSAL_COMPATIBILITY_2026-10-01.md).
Physical OEM/TV/16KB/provider behavior remains independent; generic LG Velvet installer cause UNKNOWN.


### 2026-10-01 commercial experience checkpoint

Official base: `8f80fb8fb0c8846fc5e164d7755f7831c4ead024`; #166–#177 are merged. [Official-main Validate 36868608118](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36868608118) passed all seven jobs. #177 provides all-four-ABI/16KB/optional-feature/signatureAPI24–37 checks and a same-source, hash/certificate-bound clean API33 installation and launch. This is emulator/package evidence, not all-device qualification.

The owner tried the development build and now requests a comprehensive professional interface with paid licensing per device, continuing existing implementation and reviewed-merge authority. Current coherent task: shared native presentation across activation, account entry, Home, navigation, catalog, movie/series details, Settings license metadata and playback recovery/track selection. Existing one-device activation-code backend is reused; no price, store billing, new device identity or subscription is invented. [Report](REPORTS/ANDROID_COMMERCIAL_EXPERIENCE_2026-10-01.md). Exact implementation source and CI results are recorded on the task PR. Commercial signing, physical/provider/performance and operations gates remain open.

### 2026-10-02 Home showcase and playlist checkpoint

Official base: `e1b2ba9ee5baad712f48e068d5852c87c427838e`; #178 is merged, no open PRs at the live gate. Official-main Validate 36965457394 passed all seven jobs with 71 cases per managed device. Current owner explicitly requests a better Home with Movies/Series highlights above Continue watching, and playlist subscription/expiry in Settings.

Bounded implementation: cached manually selectable showcase, dedicated progress/resume rail and provider-only subscription metadata with account/generation/request/lifecycle checks. No provider-wide discovery, licensing/backend, package, signing or release-policy change. [Report](REPORTS/HOME_SHOWCASE_PLAYLIST_2026-10-02.md). Local Gradle provisioning is BLOCKED by the restricted network; exact-head native qualification uses existing Actions. Physical/provider/performance proof remains separate. Protected merge/release/deployment requires current exact-action authorization.

### 2026-10-02 Home/playlist integrated qualification checkpoint

Owner explicitly authorized #179 merge and continued work. #179 is squash-merged at official `main@f8534011a15d1167383995abd6a42b8f58fbc4fd`; its tree `4cbe83e9127272d12666baff92199ca4659da0a9` equals the qualified PR head `25b8b18a996fc1ef0b770ce46f6a0e75d990b80f`. PR [Validate 37040552329](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37040552329) and official-main [Validate 37043350131](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37043350131) passed all seven jobs. Each managed device ran 76 cases with zero failures/skips; phone also clean-installed/launched the exact configured owner APK offline on API33 without test-only flags. The configured development APK was provided for owner feedback; production signing/release/deployment were not performed.

Current bounded continuation is test-only qualification of showcase directional input, episode resume identity/progress and playlist pause/resume/destination disposal. All existing scenarios and production behavior are retained. [Report](REPORTS/HOME_PLAYLIST_INPUT_LIFECYCLE_2026-10-02.md). Native local execution remains BLOCKED by unavailable Gradle distribution/network/Android SDK; exact new-source runtime evidence belongs to the isolated PR. Physical TV/LG Velvet/provider/TalkBack/performance qualification remains separate.

### 2026-10-02 cached showcase metadata continuation

Owner explicitly authorized #180 merge and continued app improvements. #180 is squash-merged at official `main@75efab30b0fff61c3fce931715226a5afd6346f6`, tree `7d5eb748b3026f5e23ebc5e665654ad91d927a11`, identical to qualified PR head `88a3a9b1aed3f90dee22c452ebdc0a19c95e32a1`. [PR Validate 37075277135](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37075277135) passed all seven jobs, 80 cases per managed device, zero failures/skips, and exact configured APK API33 install/launch. Initial test-oracle FAIL remains recorded. Official-main push [Validate 37076738964](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37076738964) was in progress at this source checkpoint; its final evidence belongs to #180.

Current bounded UI enhancement: optional localized provider-rating and release-year badges for the selected Home showcase item, using existing cached metadata only. Invalid fields are omitted independently; wrapping badges add no input targets, network calls or inferred values. [Report](REPORTS/HOME_SHOWCASE_METADATA_2026-10-02.md). Local Android provisioning remains BLOCKED; exact-head build/managed-device proof belongs to this new isolated PR. Protected future merge/release/signing/deployment and physical/provider/performance qualification remain separate.

### 2026-10-03 playlist presentation continuation

Owner confirmed #181 merge and continued work. #181 is squash-merged at official `main@8c1918a7d567a8be4d75ee12796d519e3a21370d`, tree `04baeaea143a92ccbda3434c5d153f2b93858dfc`, identical to qualified head `e1b0f89a76c12070becd45513ea7ce836a15fb3b`. [PR Validate37077618687](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37077618687) passed all seven jobs, 82 cases per managed device, zero failures/skips and exact configured APK API33 install/launch. #180 official-main Validate37076738964 also passed all seven jobs/80 cases per device. #181 official-main [Validate37092878214](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37092878214) is tracked separately on #181.

Current bounded enhancement makes provider-reported playlist status and expiry prominent tonal facts and uses the active resource locale for dates/connection counts. Missing fields remain unknown; no inferred license, expiry/countdown, provider access or ownership change. [Report](REPORTS/PLAYLIST_SUMMARY_PRESENTATION_2026-10-03.md). All 82 previous cases retained, two new compact-Arabic/state-replacement scenarios (84 expected). Exact-head qualification belongs to its PR; local native provisioning remains BLOCKED. Future protected actions and physical/provider/accessibility/performance acceptance remain separate.

### 2026-10-03 catalog presentation continuation

Live official source: main@354ae15063c27217e35a9865a02f99e0019dbfec; #182 is merged and there are no open PRs at this gate. [Official-main Validate37122045133](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37122045133) passed all seven jobs, 84 cases per managed device with zero failures/skips and the exact configured owner APK API33 clean-install/launch. Earlier pending checkpoint/next-round notes are historical; the live source and run establish their completion.

The owner reports misplaced category-selection guidance in Live/Movies/Series and requests continued review/improvement. Current bounded correction centers and constrains states in the available content pane, shows selection guidance only when categories exist, retains valid retry callbacks, and moves grid notices out of overlays. [Report](REPORTS/CATALOG_STATUS_PLACEMENT_2026-10-03.md). Six new native scenarios join all previous 84. Local native execution is BLOCKED by absent shell/SDK/Gradle tools; exact-source Actions qualifies the new task. Physical/provider/visual/accessibility/performance and production signing/operations remain separate.

### 2026-10-03 details width continuation

Catalog placement #183 is squash-merged at main@d4e3204a5d3700878a9a6f374c6d4e08d448387b; its tree 7e60ee5a6b2b0d87fb291d3a167ed552a3507868 equals qualified head 1344edd23c5ac1aaddd8b4d56ef8679937ef7ff1. [PR Validate37157049507](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37157049507) passed all seven jobs, 90 cases per managed device with zero failures/skips and exact configured owner APK API33 installation/launch. No retries or new-source failures. Official-main [Validate37158569337](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37158569337) is tracked separately on #183.

The additional source review found fillMaxWidth before widthIn at Movie/Series story and episode-card sites, preventing the existing maximums from narrowing wider-window content. Current separate follow-up reverses only those three modifier pairs; existing caps, callbacks, ownership, resources and all 90 scenarios remain. [Report](REPORTS/DETAILS_WIDTH_BOUNDS_2026-10-03.md). Local native tools remain BLOCKED; exact-source qualification belongs to this PR. Physical wide-screen acceptance and production gates remain separate.

### 2026-10-04 cinematic Android design

Details width #184 is squash-merged at official main@56fcccb86693451a93056fa67b30711b3e07336c; tree 6f485adb63f1c6b64d4ef4293899591291948aab equals qualified PR head dff93abaddeb1bbd94f1635501bc879b44bfb0af. [Official-main Validate37159631461](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37159631461) passed all seven jobs, 90 cases each managed device with zero failures/skips and exact configured APK offline API33 install/launch. #183 official-main Validate37158569337 also passed. No open PRs at the new source gate.

The owner approves the proposed cinematic/navy/turquoise Home and requests the remaining sections. One coherent presentation round updates shared components, menus, responsive Home discovery/shortcuts/resume, catalog search/cards/filters, details/episodes and account forms/dialogs. Existing snapshots, owners, callbacks, transport and protected identity/release policy remain authoritative. Three focused adaptive/input/fallback cases join the unchanged 90. [Report](REPORTS/ANDROID_CINEMATIC_DESIGN_2026-10-04.md). Native local execution is BLOCKED; exact-head qualification belongs to this PR. Generated visual board is a concept, not a runtime screenshot. Physical/provider/TalkBack/performance, production signing and operations remain separate.

### 2026-10-04 catalog viewport and search continuation

Cinematic design #185 is squash-merged at official main@1269935f6b77813b5da7fda005c37bd97c1cc8b9; tree 80aa390fb580422bbf0c4ae9a6bfc592a3c1785d equals qualified PR head eb2cf724bbacd99c7cbceb34b539d21ffe9d8b32. [Official-main Validate37176003196](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37176003196) passed all seven jobs, 93 cases per managed device with zero failures/skips and the same configured owner APK offline API33 installation/launch. No open PRs at this new gate.

The owner requests continued professional refinement. One bounded catalog viewport/search round keeps overflowing title/filter/query/resume controls independently scrollable and reserves a positive results region on controlled short windows; clear-search and Search IME remain explicit local actions. Existing controller/state owners and all 93 previous cases are retained, with three targeted scenarios (96 expected each). [Report](REPORTS/ANDROID_CATALOG_VIEWPORT_2026-10-04.md). The original short-height/IME concern was an unmeasured inference; no physical baseline failure or actual OEM IME resolution is asserted. Exact-source CI belongs to this task PR; physical/provider/accessibility/performance and production gates remain separate.

### 2026-10-07 account confirmation continuation

Catalog viewport/search #186 is squash-merged at official main@df1250b9c544716bbcbc32a59504b160af00e383; tree f2502fe9d8c1e22e8070d8bbadd1c3ef7477e9d5 equals its qualified PR source. [Official-main Validate37271857310](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37271857310) passed all seven jobs,96 cases each managed device with zero failures/skips and same configured owner APK offline API33 install/launch. No open PRs at this new gate; old pending catalog notes are historical.

The owner requests a fresh installation link and continued refinement. A fresh authorized download of unexpired owner artifact11329510196 was issued; no release/signing-policy change. Current bounded presentation round makes lengthy account-removal confirmation vertically scrollable within its existing manager's720dp maximum and matches the shared branded tonal appearance. Safe Keep-first focus, busy/dismiss guards and exact-ID callback authority remain unchanged. Two real-dialog280x320dp overflow/touch/directional cases and one280x320dp Arabic/font2 actual-renderer case join all unchanged96 cases (99 expected). Initial98-case qualification failed two new overflow-fixture assertions per device. The next99-case source passed phone and actualfont2 renderer but the tablet's two real-dialog fixtures still fit; the final test-driver correction constrains the actual window without changing production geometry. Full causal history is retained in the report. [Report](REPORTS/ANDROID_ACCOUNT_CONFIRMATION_2026-10-07.md). Original overflow concern is an unmeasured source inference; physical/provider/accessibility/performance and commercial signing/operations remain separate.

### 2026-10-07 adaptive details continuation

Live official source: main@62bb365a99a4ab043c032c0325f7ab5831a67c61, including merged account-confirmation #187. [Official-main Validate 37578813615](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37578813615) passed all seven jobs. No open PRs at this gate. Previous task/qualification notes remain historical snapshots.

Current owner instruction: continue development, fix errors and refine the interface without repeated pauses. Existing reviewed-merge authorization continues within its scope. One coherent presentation round adapts shared Movie/Series headings/posters to available width and font size, and allows Back/Refresh controls to wrap using their measured labels. State, provider, playback, selection, identity and release policies are unchanged. The narrow-title concern is a source/layout inference; no physical baseline failure is asserted. Four focused native scenarios join all 99 prior cases (103 expected per managed device). Local native execution is BLOCKED by unavailable Gradle distribution/Android SDK; authenticated connector access recovered after transient errors and existing Actions will qualify the exact source. [Report](REPORTS/ANDROID_DETAILS_ADAPTIVE_2026-10-07.md). Physical/provider/TalkBack/performance and production qualification remain separate.

### 2026-10-07 Chromium provisioning deadline

Adaptive details #188 is merged at main@a125ced5337c5304258f7ee81419817de5e8f97a, tree2c787ef3e92b1d5554f187f17e5ca9cc494e6921. [Official-main Validate37664582379](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37664582379) passed all seven jobs,103 cases each managed device with zero failures/skips and exact configured APK offline API33 installation/launch.

Home ordering #189 retains head70e9fb867baa0ab873590737f59b7594f4ce59b0 and its isolated branch. [Validate37665154418](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37665154418) passed all six other jobs,103 cases on each device and exact APK API33 installation, but Admin provisioning remained pending for over35 minutes. Its typecheck/build passed; the running log archive returned BlobNotFound, so the internal installation cause is UNKNOWN. #189 is temporarily closed to keep one reviewable PR at a time, pending this separate prerequisite and controlled base/documentation reconciliation.

Confirmed workflow concern: pinned Chromium installation has no step deadline and inherits the job's much longer default budget. Add only a15-minute step deadline, preserving the pinned command, browser tests, all seven jobs, source/artifact attribution and failure semantics. This bounds setup rather than claiming a network-root-cause repair. Continued error-correction and reviewed-merge authority applies. [Report](REPORTS/CI_CHROMIUM_PROVISION_TIMEOUT_2026-10-07.md). Exact-source qualification belongs to the task PR; runtime/production gates remain separate.

### 2026-10-09 Home content-order resumption

CI deadline #190 is squash-merged at official main@98cfebad91de3c85e10a8f1cb1b3f72ab71581a6; tree cf619777d2b6d9d12f67e2ad8101d7f63a4692cc equals qualified source7edda7e9e2fcf8a6ce35b1909f5060cab7ac8611. [Validate37670195821](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37670195821) attempt2 passed all seven jobs. The complete failed Admin log located the first failure in apt package-mirror provisioning; one unchanged-source Admin-only retry passed all nine Chromium scenarios. Native103 cases per device and same configured APK API33 installation passed. Official-main [Validate37947443877](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37947443877) is tracked separately on #190. Earlier pending/closed notes remain historical.

Reconcile retained Home #189 against this official baseline without rewriting its published history: keep the identical three-line browsing-item relocation, README alignment and task report, and retain the qualified CI prerequisite. Home now presents showcase → eligible Continue Watching → eligible latest Movies/Series → browsing shortcuts → eligible recent history. All callbacks, conditions, focus/state/data owners and103 native cases remain unchanged. Continued owner implementation and reviewed-merge authority applies. [Report](REPORTS/HOME_CONTENT_ORDER_2026-10-07.md). New exact-source qualification is required; physical/provider/accessibility/performance and production signing/operations remain separate.

### 2026-10-09 owner-approved visual concept

Home order #189 is merged at main@06f37f62caf1d3b33b4b5d8f28c5c10925026ece, tree308ce3b347b1097d9011729db9a2e36c002ee8f4. [Official-main Validate37961457475](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37961457475) passed all seven jobs; no open PRs at this task gate. Earlier pending and next-task notes are historical.

Current owner explicitly approves the displayed navy/turquoise cinematic concept. One bounded native presentation implementation aligns shared colors/panels/actions, Home branding/title/action geometry, selected bottom-navigation labels and compact two-column catalog density. Existing details/Settings/TV patterns, functional callbacks, content ordering and state/data owners remain. Update the two affected density oracles to the newly approved policy, retain all103 existing native cases and add one actual Arabic/font2 long-title/callback case (104 expected). Source/local evidence and exact-head qualification belong to [the report](REPORTS/APPROVED_CINEMATIC_DESIGN_2026-10-09.md) and isolated task PR. The generated board is a concept, not runtime or shipped media content; no pixel-perfect/physical/provider/performance/commercial readiness is claimed.

## 2. Architecture

TYFINO is a multi-surface repository containing:

- native Android IPTV player under `apps/android/`;
- licensing/backend API under `apps/api/`;
- Admin web application under `apps/admin/`;
- PostgreSQL/database controls under `database/`;
- deployment/infrastructure assets under `infrastructure/` and `docker-compose.yml`.

Authoritative architecture, data-ownership, security, deployment, Android decisions, and subsystem contracts remain in existing project documentation and are not duplicated here.

## 3. Closed Historical Work

- Repository foundation and previous governance qualification completed under the older governance baseline.
- Core Xtream authentication, multiple accounts, categories, favorites, recent history, resume/Continue Watching, Movies/Series details, playback, track selection, previous-live behavior, and on-demand Live EPG are implemented with repository evidence.
- Activation-code licensing with seven-day trial, one-year/lifetime codes, installation binding/reset, refresh/offline constraints, and Admin licensing controls are implemented.
- CI covers API/Admin/database/Android build and managed-device validation surfaces.

Historical PASS does not replace current-task verification.

Current Android catalog refinement: item grids now account for both pane width and system font size, and artwork titles reserve two font-relative lines. See `/ENGINEERING/REPORTS/ANDROID_CATALOG_READABILITY_2026-09-30.md`; exact-head CI is recorded on the task PR, while physical-device and visual qualification remain separate gates.

## 4. Current Findings

FACT:
- The canonical `/ENGINEERING/` structure is now present after governance migration PR #160.
- Legal/security documentation PR #161 was merged after exact-head Validate #439 succeeded on its second attempt; the first attempt had one tablet emulator focus assertion failure, so test stability remains a separate observation.
- Operations/performance documentation PR #162 was merged after exact-head Validate #441 succeeded on its second attempt; the same tablet focus assertion failed in the first attempt and remains a separate test-stability finding.
- V1 test-matrix documentation PR #163 was merged after exact-head Validate #443 passed all seven jobs on its first attempt; physical/device qualification remains open.
- Root `AGENTS.md` and a substantial TYFINO-specific `governance/` layer already existed.
- `main` was not branch-protected at the 2026-09-26 baseline query.
- PR #159 was open and scoped to Android Movies/Series UI behavior.
- Physical TV/foldable/low-memory/real-provider/accessibility/performance qualification remains separate from source/emulator evidence.

UNKNOWN / MUST VERIFY LIVE:
- Current branch-protection/ruleset state after this baseline.
- Current production host/deployment state.
- Current off-host backup freshness and restore proof.
- Current signing-key custody/recovery evidence.
- Exact physical-device qualification state unless a newer attributable report exists.

### 2026-09-30 review findings (historical source checkpoint)

These are historical findings. #166–#169 corrected the first four, #173 qualified the modal correction in Chromium, and #174 qualified the sidebar pointer correction. The catalog concern is confirmed by cross-instance tests and being corrected in #175. Physical/accessibility qualification remains independent.

- FACT: First-use activation expiry was incorrectly applied to already activated grants. A bounded correction and PostgreSQL integration regression coverage are prepared on `fix/licensing-first-use-expiry-2026-09-30`; this is not yet merged into official source.
- FACT: Admin code creation/settings changes and their audit events use separate commits.
- FACT: Clipboard success is shown before the write promise resolves.
- FACT: Dashboard counts use at most 500 newest codes and persisted status without grant-expiry classification.
- FACT: Admin modal source lacks Tab containment/background inertness/focus restoration; browser evidence is NOT RUN.
- INFERENCE: Separate foreground/background catalog repository instances need a deterministic same-account stale-write diagnosis.
- UNKNOWN / BLOCKED: Physical-device, real-provider/media, browser-accessibility and production qualification have not been performed in this round.

Details: [2026-09-30 source review and correction](REPORTS/PROJECT_REVIEW_FIRST_USE_EXPIRY_2026-09-30.md).

## 5. Release Blocker Map

1. Exact-release signing and owner-controlled signing-key backup/recovery evidence.
2. Production operations qualification: secrets, monitoring, external audit anchoring, backup/restore, rollback, log retention/rotation, production thresholds.
3. Physical-device/provider/media qualification across required device/input/RTL/accessibility/performance surfaces.
4. Any OPEN product/retention decisions that affect production behavior.

## 6. Qualification Gaps

- Physical Android TV / Google TV evidence.
- Low-RAM/API-24-class evidence.
- Foldable/resizing hardware evidence.
- Real-provider/media error and lifecycle evidence.
- RTL/TalkBack and mixed-language evidence.
- Performance/startup/scrolling/playback/memory measurements on target hardware.
- Production operational evidence.

Admin dashboard review (2026-09-30): a bounded follow-up on official base `ef420e7e9c0d968f271ada14bce1ab3076a655c7` replaces counts inferred from 500 rows with a full-table authenticated summary and serial PostgreSQL regression coverage. Detailed scope, validation limits and backend-first rollout are recorded in `/ENGINEERING/REPORTS/ADMIN_DASHBOARD_STATISTICS_2026-09-30.md`. Exact-head CI evidence belongs to the PR; merge/deployment and runtime qualification remain separate.

## 7. Ordered Engineering Gates

1. Repository/governance live gate.
2. Bounded product task with isolated branch/PR.
3. Smallest deterministic source/test proof.
4. Relevant CI regression proof.
5. Runtime/device proof when behavior cannot be established by source/CI.
6. Exact-release candidate freeze.
7. Signing/release evidence.
8. Owner release/deployment decision.

## 8. Runtime Gates

Runtime claims require attributable device/environment evidence. Emulator CI does not qualify physical TV, real providers/media, low-memory behavior, accessibility, RTL, or performance.

Android interface/menu refinement (2026-09-30): the owner requested professional interface and menu improvements. Scope is a consistent native dark palette, adaptive bottom/compact/expanded menus, explicit selection/focus, quieter Home actions and a framed category sidebar. Report: `/ENGINEERING/REPORTS/ANDROID_INTERFACE_MENUS_2026-09-30.md`. Exact-head CI is recorded in the isolated PR; visual screenshot review and physical TV/RTL/TalkBack/performance qualification remain open. Merge, signing and deployment are not authorized by this round.

## 9. Release Gates

No release or production deployment is authorized by this roadmap.

Before release:
- exact candidate SHA must be identified;
- required CI must pass on exact source;
- required physical/runtime qualification must be complete;
- signing custody/recovery must be proven;
- protected signing must be explicitly authorized;
- release artifact checksum/certificate must be captured;
- owner must explicitly authorize release/publication/deployment.

## 10. Owner Decisions

Protected decisions remain owner-controlled:
- merge;
- signing;
- release/tag/publication;
- production deployment;
- destructive infrastructure/database/security operations;
- identity/branding/signing-policy changes.

## 11. Deferred Post-Release Work

Existing deferred scope such as M3U, Stalker/MAC Portal, downloads, cloud sync, user/kids profiles, social features, provider-wide search, full EPG synchronization, CRM, analytics, and Kubernetes remains deferred unless separately approved.

## 12. Exact Immediate Next Round

Task: qualify the owner-approved cinematic presentation from main@06f37f62caf1d3b33b4b5d8f28c5c10925026ece. Continued owner implementation and reviewed-merge authority applies.
Required: complete scoped presentation/callback/focus/ownership review; affected compact-density unit/native oracles and full long-Arabic showcase renderer; all104 native cases each device; seven exact-source jobs, Android build/unit/lint and same configured APK offline API33 installation/launch. Record final results on the task PR without a bookkeeping source change invalidating its checks; integrate only after qualification.
Stop: changed baseline/conflicting source, secrets, causal required failures, protected scope expansion or unproved runtime claims.
Following gate: owner LG Velvet/TV visual/input trial and separate physical/provider/TalkBack/performance, signing continuity/custody/recovery and production operations before sale.

## Linked Reports

- `/ENGINEERING/REPORTS/MASTER_ENGINEERING_BASELINE_REPORT.md`
- `/ENGINEERING/REPORTS/OPERATIONS_PERFORMANCE_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/TEST_MATRIX_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/API_ADR_CONVENTIONS_UX_DOC_ROUND_2026-09-27.md`
- `/ENGINEERING/REPORTS/PROJECT_REVIEW_FIRST_USE_EXPIRY_2026-09-30.md`

## Linked Evidence

- `/ENGINEERING/EVIDENCE/README.md`

## 2026-09-30 Admin audit atomicity round

Official source inspected: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`. PR #166 remains open for the earlier first-use activation-expiry correction; its source is not part of official `main`.
Owner authorization: review/improvement and continued engineering work. No protected merge/release/deployment authority is granted.

FACT: Admin activation-code creation and settings changes used separate commits for the business write and audit insertion. A bounded correction on `fix/admin-audit-atomicity-2026-09-30` places each pair in one transaction and rolls back failed code-generation attempts.
PASS: extracted-handler JavaScript checks reproduce partial writes on old source and verify rollback/retry/release behavior on the correction.
Exact-head PostgreSQL/Fastify integration and build results belong to the task PR/CI record; local shell/PostgreSQL checks are NOT RUN.
Report: [Admin audit atomicity](REPORTS/ADMIN_AUDIT_ATOMICITY_2026-09-30.md).

Immediate next round: finish exact-head validation/review of this correction, then address Admin clipboard success/error handling in a separate bounded change.
