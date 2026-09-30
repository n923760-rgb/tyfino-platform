# Android interface and menu refinement — 2026-09-30

## Task packet

- Repository: `n923760-rgb/tyfino-platform`; default/target branch: `main`.
- Live official base: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`.
- Owner instruction: continue improving TYFINO and make the application's interface and menus professional. Android is the current scope; use phone and TV together as the default priority while the optional preference is unanswered.
- Authority: root `AGENTS.md`, central Master Engineering System, project profile, canonical roadmap, Android README and approved architecture/catalog/device/UI-state contracts.
- Root is the only `AGENTS.md` in the inspected tree. Open PRs #166–#169 concern licensing/Admin; Android product files do not overlap. Roadmap edits need source-composition review.
- Execution: authenticated GitHub repository connector, in-memory JavaScript source checks and existing Actions. No local shell, native renderer, device, provider, screenshot review or production access is available.
- Deliver one coherent visual/navigation refinement on an isolated branch and draft PR. Merge/signing/release/deployment remain owner-controlled.

## Confirmed interface findings

The theme defined headline cyan/violet colors but inherited Material container/surface roles, so cards, selection backgrounds and dialog controls could use unrelated default tones. Wide windows used the same narrow icon rail as medium windows. Primary navigation did not add TYFINO's explicit focus outline, and its fixed vertical destination stack could run out of room in short panes. Home account/settings controls had the same bright treatment as primary viewing actions.

## Implemented scope

- Complete the existing dark cyan/violet palette across tonal containers, surface levels, outlines, inverse and error roles. Preserve primary/secondary identity colors, assets and app identity. Use stronger heading weights and consistent Material corner sizes without new fonts/dependencies.
- Adapt primary navigation by full available window width: bottom menu below 600dp; compact 96dp menu from 600 to 839dp; labeled 232dp sidebar from 840dp. Keep all five existing destinations, callbacks, selection semantics and navigation behavior.
- Group sidebar browsing and preferences with localized headings. Use a lazy, scrollable menu so Settings stays reachable in short panes. Highlight the current destination with a cyan tonal surface and keyboard/D-pad focus with a separate light outline. Use start-relative layouts for RTL.
- Give Home account/settings controls a quieter surface treatment while preserving tags, focus ownership and actions.
- Frame the vertical catalog category list with a matching surface and localized heading. Preserve the narrow horizontal picker, category selection, grids, loading/error/stale states and provider ownership.

No provider fetch, artwork preload, whole-catalog scan, animation framework, identity change or playback/account/licensing behavior is introduced. The AppNavHost route region is byte-for-byte unchanged; navigation composables are extracted for isolated UI tests.

## Validation

PASS — pre-publication source checks: no duplicate localized string keys and all three new labels in Arabic/English; unchanged route region; menu selection/focus/scroll structure; 13 palette foreground/background calculations above 4.5:1. This is token contrast, not rendered accessibility qualification.

Added four managed-device UI tests: all five bottom destinations and exclusive selection; Arabic RTL with 1.35x font scale and sidebar selection; Settings reachable in a 260dp compact pane; directional focus and Enter activation. Expanded existing navigation unit coverage at 599/600/839/840dp and for short/tall panes.

NOT RUN at commit preparation — native compilation/unit/lint/instrumentation. Exact-head Validate and native results must be attached to the PR before readiness.
NOT RUN — screenshot/visual review, physical phone/TV/Google TV, TalkBack, overscan, real-provider/media, low-RAM and measured performance. Managed phone/tablet fixtures do not qualify physical TV or full accessibility.

## First native verification and diagnostic follow-up

Validate run [36725882728](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36725882728) on `ecd9a833e75247cdcdba252fcdec0406270689e6`: API/Admin/database/deployment and tablet instrumentation PASS; Debug/unsigned Release builds and JVM unit tests PASS; Android lint FAIL for the obsolete `open_settings` string; phone instrumentation FAIL without a testcase reason in the console.

Remove the unused English/Arabic string pair and use the non-deprecated locale factory in the new test. Add an always-run instrumentation summary to the existing CI job: current XML results and new navigation cases are printed, and the most recent same-branch failed artifact may be inspected with bounded read-only Actions access. Previous results are explicitly labeled with run/head and never used as current proof. Reports are parsed in memory, URLs/secret-like message fields are redacted, ZIP/XML size is bounded, and original test failure status is preserved. Exact checkout, required checks, source/artifact attribution and failure uploads remain intact.

This diagnostic change is necessary because this session can download the report ZIP but has no shell/archive reader. The next exact-head run must establish the phone failure reason or record it as unresolved; no blind UI workaround or test weakening is authorized.

## Phone focus fixture diagnosis

Run [36728070934](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36728070934) on `2a62206fcae0f0bd2b513cafef6d1de747020c5f` printed 60 phone test cases, one failure, zero skipped. The same-branch archive confirmed the identical initial-head failure: `AppNavigationTest.sideMenuSupportsDirectionalFocusAndKeyboardActivation` failed at the first Home-focused assertion, before directional traversal. The other three new navigation cases (bottom selection, short pane and Arabic RTL/larger text) passed.

The test requested node focus immediately after composition without establishing window-focus and keyboard-input preconditions. Synchronize on the host window, explicitly request keyboard input through Compose's input-mode manager and verify that mode before the unchanged Home-focus, Down/Live-focus and Enter/action assertions. Use bounded event/state waits, not sleeps or retries of the suite.

Previous-artifact access was used only for diagnosis and is removed from the final workflow, along with Actions-read permissions and token environment. Keep the small always-run current-XML summary for attributable navigation results and failure details; it needs no network or token access. Final native CI remains required on the corrected source.

## Evidence and completion gates

Review the full diff, source identity, shared roadmap composition and exact-head Actions. Fix reproducible failures before readiness; record any unresolved/flaky runtime failures honestly. Stop for unexpected base/head movement, overlapping product work, authority conflict, secrets or unrelated source changes. Signing and deployment remain outside this task.
