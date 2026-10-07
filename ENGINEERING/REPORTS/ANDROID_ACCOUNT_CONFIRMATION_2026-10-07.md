# Android account removal confirmation — 2026-10-07

Task: android-account-confirmation-2026-10-07
Type: IMPLEMENTATION
Issued: 2026-10-07 04:54:39 UTC
Repository: n923760-rgb/tyfino-platform
Official base: main@df1250b9c544716bbcbc32a59504b160af00e383
Branch: feat/android-account-confirmation-2026-10-07
Execution: GitHub MCP + JavaScript source checks; existing GitHub Actions for native build/runtime. Local shell/SDK/worktree unavailable.

## Authority and scope

The owner requests continued professional application refinement and previously explicitly authorized reviewed merges. This round changes only removal-confirmation presentation and adds two actual-dialog regressions. Production signing/release/tag/deploy, identity/version/SDK/ABI/origin, provider access, persistence, removal policy and other screens remain out of scope.

## Source finding and requirement

FACT: the main account manager already bounds its surface to720dp and scrolls vertically. Its nested removal confirmation uses a plain unscrolled Column with title, long localized message and two buttons. It also uses an elevated surface unlike the surrounding native account presentation.
INFERENCE: lengthy account summaries, Arabic text and larger fonts can exceed available confirmation height. No physical clipping failure or old-source runtime reproduction is claimed.
Authorized feature: apply the manager's existing maximum-height/vertical-scroll pattern to the actual confirmation and reuse the shared branded heading and tonal border. No fixed content height or truncation is introduced.

## State and action ownership

The existing confirmation account remains the only payload owner. Initial focus remains Keep; dismissal and buttons retain the existing busy guard. Keep only closes the confirmation. Remove closes it and emits the selected stable Account ID exactly once. The controller, encrypted portfolio, async lifecycle, provider and deletion implementation are unchanged. No real account is removed by qualification.

## Changes

- XtreamAccountManager.kt: confirmation surface maximum720dp, scrollable content, shared ProductHeader and existing account-manager tonal surface/border. Existing resources and callbacks unchanged.
- XtreamRemovalConfirmationTest.kt: two synthetic actual-dialog scenarios at Arabic RTL/font2. Identical long usernames on two distinct IDs ensure callbacks use selected stable identity. Touch scroll/cancel/reopen/confirm and keyboard Keep-first/Down/explicit Enter are asserted. Actual scroll range and positive offset are required. assertIsDisplayed establishes at least partial clipped visibility, not physical screenshot acceptance.
- AndroidREADME and canonical roadmap record scope and qualification boundary; this report is the detailed record.

## Validation

PASS: complete proposed source diff, exact untouched manager prefix and unchanged confirmation callbacks/focus/dismiss guards, existing resource references, test/import/delimiter/whitespace checks and five-file scope.
All96 prior native cases remain untouched. Two new scenarios require98 per managed device.
Exact new-source qualification is pending until recorded on the task PR: seven jobs, debug/optimized unsigned release/unit/lint, API27/API35 each98/zero failures/skips and same configured APK offline API33 clean-install/launch.
Local native validation: BLOCKED (no shell/SDK). CI results must be attributed to Actions and exact tested source.
Physical LG Velvet/TV/OEM IME/TalkBack/provider/media/performance: NOT RUN.

## Baseline and delivery

Official base Validate37271857310 was qualified in the preceding task:7 jobs,96 cases/device and same configured ownerAPK offline API33 install/launch. Its current source/run identity was reread. Old delivery artifact11329510196 remains unexpired through2026-10-12; the owner's old signed download URL expired, and a new ZIP attachment was issued. No new GitHub Release or production distribution was created.
Temporary development signing is not stable across runners. In-place upgrade continuity is not asserted; preserve credentials/data. Final task/main proof belongs to the PR without a bookkeeping source change that invalidates its own CI.

## Remaining gates

Owner physical visual/input trial, real provider/media/accessibility/performance and commercial signing custody/recovery/operations remain separate. productionReady=false.
