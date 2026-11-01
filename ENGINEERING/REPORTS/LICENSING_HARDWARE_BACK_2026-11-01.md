# Consistent hardware Back during licensing

Task: TYFINO-20261101-licensing-hardware-back — IMPLEMENTATION.
Controller/executor: Codex, sequential implementation and self-review.
Owner requests continued development/improvements; standing reviewed-merge authority applies after successful requirements. No signing, release or deployment is authorized/performed by this task.

## Source and scope

Repository/default branch: n923760-rgb/tyfino-platform, main.
Verified official base: 78fae08632dba4d60263750bd46f75093d9dc5f3. Official-main Validate41289846997 PASS. No open/conflicting PRs at initial gate.
Isolated worktree/branch: improve/licensing-hardware-back-20261101, from freshly fetched official main.
Scope: LicensingScreen hardware Back, two native regressions, this report and canonical roadmap. Excluded: licensing repository/server authority, IPTV data, endpoints, application identity, signing/release/deployment. Stop for drift, conflicts, secrets or required failures pending diagnosis.

## Behavior

The visible activation-form Back button already routes to the appropriate owner callback: initial activation returns to license choice; Settings activation restores authoritative access. Only the app shell's Settings-specific hardware handler existed, leaving initial activation without that same explicit behavior.

Handle hardware Back in the licensing presentation for ActivationEntry and Working. Code entry invokes the existing onBack callback. Working consumes Back without issuing another action or dismissing the operation. Choice, Checking and Failure retain their existing behavior; the shell continues to own Settings return behavior. Platform keyboard dismissal and the purchase dialog retain their own Back handling. No codes are persisted and no licensing/network semantics change.

Native coverage: input a synthetic code, close the keyboard, press actual hardware Back, assert license choice and exactly one owner callback, then reopen entry and require empty editable text. A separate Working case presses actual Back and requires progress to remain with zero action callbacks. Retain all139 previous cases;141 expected on each managed device.

## Validation

PASS local: diff whitespace, full four-file self-review, engineering-environment, Task/Result protocol, AI executor policy, behavioral-trial, CI qualification and governance adversarial validators. Local Android compile/runtime is BLOCKED by unavailable SDK/Gradle; exact-source hosted seven-job Validate,141 cases/device and existing API33 install smoke are required before merge. Physical phone/TV/IME/TalkBack and real-code/provider acceptance are NOT RUN. Passing managed-device cases do not establish physical-device qualification.
