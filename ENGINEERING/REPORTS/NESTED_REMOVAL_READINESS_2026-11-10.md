# Nested removal-dialog keyboard readiness

## Task / live source

Task `TYFINO-20261110-nested-removal-readiness`, IMPLEMENTATION, 2026-11-10 UTC. Owner requests testing, confirming operation and continuing. Standing TYFINO reviewed-merge-after-success authority applies; no release/signing/deployment. Controller/executor Codex; sequential self-review, not independent review.

Verified repository `n923760-rgb/tyfino-platform`, official/default `main`, HEAD `a5b44ba987a6ce97cb6d03007868061161e5ddac`, tree `27a730471afaecb4781cfb5a39d78cb0c0d2e003`; no open/conflicting PRs. Clean local main matched official source. Task branch `test/nested-removal-readiness-20261110`.

Applicable root AGENTS, project profile, task/environment contracts, Android README, multiple-account/device contracts and canonical roadmap govern this task. Local Git/source/shell are available; qualified local Android SDK/JDK17/emulator and Gradle download remain unavailable. Hosted existing CI supplies Android runtime proof.

## Original failure evidence

Fresh official-main Validate42875007616 failed one tablet case after #229. Five non-native jobs and phone130037057860 PASS; phone167/0/0 and exact configured APK API33 install/launch PASS, APK SHA256 `647da50e96b5d34d583e21af0ae3d9e1fef23107778d966853cc59c53af299b2`.

Tablet130037057767:167 cases,1 failure,0 skips. Complete failure artifact13791443455,1024413 bytes, SHA256 `1071f747ef8bbb857e2922a8fc948ac131312bb452a56cc5a3ec67a6ca021667`, downloaded and its JUnit/per-case log reviewed. The first failed assertion is `XtreamRemovalConfirmationTest.nestedDialogs:196`: immediately after the first Down from Keep, `xtream-confirm-remove` is not focused. The prior Keep-focus assertion passed. No exact native pre-key window/input-mode snapshot exists; product/dispatch cause remains UNKNOWN. The failure is retained, not relabelled PASS. No unchanged-source rerun was requested.

Both devices' actual JUnit records show all eight Vision cases PASS: actual decoded-pixel change/identity endpoints/Back release, isolated keyboard controls, Arabic font2 failure exit, HDR rejection, Close/ON_STOP/new-session disposal, and actual LTR/RTL remote entry/comparison/Close/Back restoration. This qualifies the debug synthetic sample only, not production media or physical/OEM quality. Phone artifact13792371213 SHA256 `1e83aefa2fadd5b046efe1c05821a290da483d3a975ef7c95f8d6f5c9998ce5c` also reviewed.

## Confirmed fixture gap and bounded correction

The two nested journeys used only pre-mount `setInTouchMode(false)` and Compose target-focus readiness, unlike the already-qualified mounted InputMode/native Dialog prerequisites used by other directional fixtures. Establish real focused host readiness and successful observed Keyboard mode before mounting the actual manager. Require the existing `awaitKeyboardDialogWindow` precondition after the product's genuine entry focus for manager and each confirmation, before the original first-direction chains. That helper checks native Dialog window/child focus outside touch mode; it never forces target focus or injects keys.

All existing Up/Down/Enter/Keep/cancel/reopen/exact-ID/no-early-delete assertions remain verbatim and in order. No wait after an arrow, extra key, forced destination focus, assertion timeout relaxation, product helper/decoder change, dependency or workflow change. Count remains167/device. This qualifies previously missing fixture prerequisites; it does not prove a historical pre-key condition or claim a causal product fix.

## Validation / stop conditions

PASS: full three-file diff/secret/whitespace review, programmatic verbatim nested action/assertion retention check and all six local governance validators. Require seven exact-head hosted jobs,167 cases/device, actual original nested cases and configured API33 install/launch before acceptance/merge. Task PR records resulting exact source/tree and runtime evidence; pending is not PASS.

Local Android execution remains BLOCKED by previously verified unavailable toolchain/distribution access; no redundant identical provisioning attempt is claimed. Stop for drift, conflicts, secrets or required failure, inspect full attributable evidence and preserve the first failure. No blind reruns or weakened directional verdicts. Physical TCL remains owner-deferred; real-media/OEM/TalkBack/performance and release qualification remain separate.
