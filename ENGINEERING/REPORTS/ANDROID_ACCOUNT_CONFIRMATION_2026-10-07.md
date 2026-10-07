# Android account removal confirmation — 2026-10-07

Task: android-account-confirmation-2026-10-07
Type: IMPLEMENTATION
Issued: 2026-10-07 04:54:39 UTC
Repository: n923760-rgb/tyfino-platform
Official base: main@df1250b9c544716bbcbc32a59504b160af00e383
Branch: feat/android-account-confirmation-2026-10-07
Execution: GitHub MCP + JavaScript source checks; existing GitHub Actions for native build/runtime. Local shell/SDK/worktree unavailable.

## Authority and scope

The owner requests continued professional application refinement and previously explicitly authorized reviewed merges. This round changes only removal-confirmation presentation and adds two actual-dialog and one controlled-renderer regressions. Production signing/release/tag/deploy, identity/version/SDK/ABI/origin, provider access, persistence, removal policy and other screens remain out of scope.

## Source finding and requirement

FACT: the main account manager already bounds its surface to720dp and scrolls vertically. Its nested removal confirmation uses a plain unscrolled Column with title, long localized message and two buttons. It also uses an elevated surface unlike the surrounding native account presentation.
INFERENCE: lengthy account summaries, Arabic text and larger fonts can exceed available confirmation height. No physical clipping failure or old-source runtime reproduction is claimed.
Authorized feature: apply the manager's existing maximum-height/vertical-scroll pattern to the actual confirmation and reuse the shared branded heading and tonal border. No fixed content height or truncation is introduced.

## State and action ownership

The existing confirmation account remains the only payload owner. Initial focus remains Keep; dismissal and buttons retain the existing busy guard. Keep only closes the confirmation. Remove closes it and emits the selected stable Account ID exactly once. The controller, encrypted portfolio, async lifecycle, provider and deletion implementation are unchanged. No real account is removed by qualification.

## Changes

- XtreamAccountManager.kt: actual confirmation renderer with surface maximum720dp, scrollable content, shared ProductHeader and existing account-manager tonal surface/border. Existing resources and callbacks unchanged.
- XtreamRemovalConfirmationTest.kt: two synthetic actual-dialog scenarios using the device density and a third Arabic RTL/font2 controlled-renderer scenario. Identical long usernames on two distinct IDs ensure callbacks use selected stable identity. Touch scroll/cancel/reopen/confirm and keyboard Keep-first/Down/explicit Enter are asserted. Actual scroll range and positive offset are required. assertIsDisplayed establishes at least partial clipped visibility, not physical screenshot acceptance.
- AndroidREADME and canonical roadmap record scope and qualification boundary; this report is the detailed record.

## Validation

PASS: complete proposed source diff, exact untouched manager prefix and structurally retained confirmation callback mapping/focus/dismiss guards, existing resource references, test/import/delimiter/whitespace checks and five-file scope.
All96 prior native cases remain untouched. Three new scenarios require99 per managed device.
Exact new-source qualification is pending until recorded on the task PR: seven jobs, debug/optimized unsigned release/unit/lint, API27/API35 each99/zero failures/skips and same configured APK offline API33 clean-install/launch.
Local native validation: BLOCKED (no shell/SDK). CI results must be attributed to Actions and exact tested source.
Physical LG Velvet/TV/OEM IME/TalkBack/provider/media/performance: NOT RUN.

## Baseline and delivery

Official base Validate37271857310 was qualified in the preceding task:7 jobs,96 cases/device and same configured ownerAPK offline API33 install/launch. Its current source/run identity was reread. Old delivery artifact11329510196 remains unexpired through2026-10-12; the owner's old signed download URL expired, and a new ZIP attachment was issued. No new GitHub Release or production distribution was created.
Temporary development signing is not stable across runners. In-place upgrade continuity is not asserted; preserve credentials/data. Final task/main proof belongs to the PR without a bookkeeping source change that invalidates its own CI.

## Remaining gates

Owner physical visual/input trial, real provider/media/accessibility/performance and commercial signing custody/recovery/operations remain separate. productionReady=false.

## Initial qualification and causal correction

Initial exact head1c70a099b7fa845e25961ef7d4b524bc5da7d118/treec168aaa4d7b4bb40ed53fba791778199fc27e08e, [Validate37573883377](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37573883377): five build/service jobs PASS; API27 job112640112098 and API35 job112640111989 each98 tests/2 failed/zero skips. All96 previous tests PASS. Both new cases failed at the first assertion requiring a positive maximum scroll range; the actual confirmation content fit its viewport. API33 installer SKIPPED after native failure. No merge/delivery of failed source.

This is a test-fixture/qualification failure, not evidence that the bounded renderer failed to scroll overflowing content. The initial outer font2 provider did not prove the real Dialog's text density. [AndroidX common locals source](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/CompositionLocals.kt) provides each owner density and [Dialog source](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidDialog.android.kt) creates a separate root; these moving references explain the inference, not the pinned runtime dependency. No native actual-font2 evidence existed for the initial two tests.

Correction retains every original visible/focus/directional/cancel/exact-ID/positive-scroll assertion, uses a genuinely lengthy1200-character synthetic username in the two real Dialog cases, and additionally checks the actual Arabic title. Their density is the device/root density, not a falsely inferred custom font2.
The same actual bounded Surface renderer is extracted within XtreamAccountManager.kt without changing its layout/style, dialog effects, busy/focus/dismiss guards or callback authority. A third test renders that exact component inside280x320dp Arabic/font2 constraints, reads the real TextLayoutResult density and asserts fontScale2, positive overflow/scroll, visible controls and exact callback counts. Expected99 cases per device, all96 previous cases untouched. No source policy change, blind rerun, workflow change, disabled tests or weakened scrolling assertion.
Exact corrected-source validation is pending until recorded on the PR. Earlier98/two-case/byte-identical suffix notes are initial-source observations, not current qualification. Current controller prefix is retained byte-for-byte; confirmation callback mapping and guards are retained structurally through the actual renderer call.

## Wide-window qualification correction

Corrected head60108f791847757821e4221d6ed358128b669027/tree7d4903a2ae32fbe65812f74887a164c8f1705b98, [Validate37575428948](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37575428948): five build/service jobs PASS; API27 job112645005623 completed99/zero failures/skips and same configured ownerAPK offline API33 install/launch with source/hash/size/certificate matched. API35 job112645005635 completed99/2 failures/zero skips; only the two real-dialog overflow assertions failed. All96 prior cases and the new controlled-renderer/font2 case PASS on both. No merge or delivery of this partially qualified source.

FACT: even the longer synthetic summary fit the tablet's actual viewport (maximum scroll range0). INFERENCE: variable real window dimensions make fixture length an inadequate overflow precondition. Final test-driver correction constrains the focused real Compose Dialog window to280x320dp using already-declared Espresso and the public DialogWindowProvider. It then verifies actual rendered width/height bounds. This affects only transient test Dialog windows, not production code, system settings, application/device data or workflows. Username fixture returns to the original120 characters. Every original visible/focus/cancel/exact-ID/positive-scroll assertion and the actual Arabic title remains. The controlled actual renderer still directly attests fontScale2. Expected99 cases per device, all96 prior cases retained. Final source-specific qualification remains pending on the PR; no blind rerun or test weakening.
