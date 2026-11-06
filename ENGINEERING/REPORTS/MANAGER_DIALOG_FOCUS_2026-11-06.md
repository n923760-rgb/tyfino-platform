# Account-manager dialog focus

Task TYFINO-20261106-manager-focus — IMPLEMENTATION. Controller/executor Codex, sequential self-review. Owner requests continued development; standing merge authority applies after successful required checks. Physical TCL qualification remains deferred.

## Verified source and scope

Repository n923760-rgb/tyfino-platform, official/default main34e7d5f3a517211e604319e5076163d60445b45d, tree55af4c5dad13b586810f2efed6bd93626117b85c. No open task PRs at the initial gate. Local clean main was fast-forwarded to independently verified official source. Isolated worktree/branch improve/manager-focus-20261106.

Confirmed source hazard: both account-manager dialogs use a private two-frame focus routine that unconditionally requests focus twice without checking window focus or stopping after success. The requesters are remembered outside their Dialog roots. A later initialization request can undo directional navigation; two frame requests alone do not handle a window that gains focus later. Existing shared helper implements window readiness and stop-after-success, with controlled-frame regression coverage.

Bounded change: create the shared initial-focus requester inside each actual Dialog. Manager entry remains Back; confirmation entry remains Keep, keyed by stable account ID. Remove only the redundant private helper. No account persistence, removal authority, busy/dismissal semantics, network, credentials, endpoints, player lifecycle, layout, dependency, CI, app identity, signing, release or deployment change.

## Acceptance and evidence

Two new real nested-dialog LTR/RTL journeys enter the manager at Back without test-forced focus, move Up to the second account, explicitly open confirmation, require Keep focus, move to Remove without callbacks, return to Keep and cancel, reopen the same account with fresh Keep focus, then explicitly remove that exact stable ID once. Back callback remains zero. Retain all151 existing native cases,153/device expected; existing Arabic/large-text/short-window and generic controlled-frame coverage remain.

Local Git, source review and six governance validators PASS. git diff --check PASS. Local Android build/runtime BLOCKED: Gradle9.6 distribution fetch reports Network is unreachable, Android SDK/emulator not provisioned; available Java is21 rather than required17. Existing hosted Validate must supply exact-head Android builds/JVM/lint, seven successful jobs,153 native cases/device and configured APK API33 installation smoke. Full final diff and accidental/secret-bearing files must be reviewed before commit/merge. Stop dependent actions for source drift, conflicts, missing evidence or required failure. Exact-source hosted results belong to the PR. Physical TV/provider/TalkBack/performance NOT RUN.


## 2026-11-06 combined-source requalification

Original PR head84c8b9ab403e95bc96013011a3bd12666459deef did not qualify: phone153/0/0 and API33 smoke PASS; tablet initial attempt37 failures, one bounded retry127622715060 had153/1/0. Full artifact13466272927,905033 bytes, SHA25674265a6cce25a952105ee5bd09f0538dd2aefdab032f5aeba607c41d10e5eb11 locates the baseline switcher failure at first Down before metadata mutation. All manager cases including both new journeys passed; cause remains UNKNOWN, no waiver granted.

Separate diagnostic PR222 is merged as official main28056cf2d348cda9b17971edcf22df277b768dfb with qualified tree13ff6d6f98fbc995f7ac48522f82ae9478ca63cb: Validate42192457744 all seven checks PASS,151 cases/device and API33 smoke. Failure NOT REPRODUCED; no causal correction claimed. Integrate that main with a normal two-parent merge into the manager branch, retain both roadmap records and all diagnostic assertions. No force push/history rewrite. Current task remains only manager focus and its two journeys; diagnostic source is qualified base content. Require a fresh combined-source run153 cases/device plus seven checks and API33 smoke before merge. Original results remain attributable only to their own heads.
