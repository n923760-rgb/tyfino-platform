# Settings Initial Focus Test Stability — 2026-09-27

Status: DRAFT — exact-head CI pending
Repository: `n923760-rgb/tyfino-platform`
Starting official `main`: `cd83a1c23819ae972185c748e7487156890a7dbf`
Branch: `fix/android-settings-initial-focus-test`

## Confirmed failure

The tablet API 35 managed-device test `SettingsScreenTest.accountSwitcherReceivesInitialDpadFocus` failed with `Focused = false` while the target node existed. It failed on the first attempts of Validate #439, #441 and #445, and again on #445 attempt 2. #439 and #441 subsequently passed on a job rerun; #443 passed on the first attempt. These heads changed documentation only, so the repeated failure is not caused by those documentation diffs.

## Bounded diagnosis and change

`SettingsScreenTest` called `waitForIdle()` then asserted focus inside a Compose `Dialog`. The focus helper requests focus after attachment/frames and upon the dialog window gaining focus. Compose idleness does not itself guarantee the separate dialog window's focus callback has arrived. This is a source-supported timing hypothesis, not a physical TV qualification claim.

The test now waits up to five seconds for the target's actual focused semantics state, then keeps the existing focus assertion. It does not synthesize focus or change product UI code. If focus never arrives, the test still fails and the failure must be diagnosed as a runtime behavior issue.

## Validation

- Local diff/whitespace and changed-file review: PASS before PR creation.
- Initial PR head `e8dfdffa6742edb49ea325075a7e3224d00cea47`: FAIL at `compileDebugAndroidTestKotlin` on phone and tablet because `SemanticsConfiguration.getOrNull` is unavailable in the pinned Compose API. Corrected to the supported `SemanticsConfiguration[SemanticsProperties.Focused]` operator; exact corrected-head CI: PENDING.
- Physical TV/Google TV initial focus: NOT RUN; remains BLOCKED under `docs/android/device-qualification-v1.md`.

Merge and any release action remain owner-protected under `AGENTS.md`.
