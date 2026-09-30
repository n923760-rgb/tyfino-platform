# Android catalog readability — 2026-09-30

## Task packet

- Repository: `n923760-rgb/tyfino-platform`; official branch: `main`.
- Starting official SHA: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`.
- Branch: `ui/android-catalog-readability-2026-09-30`.
- Owner authorization: improve the full application and its interface/menus, followed by "continue".
- Execution: authenticated GitHub connector for source, branch and PR operations; JavaScript source preflight; existing GitHub Actions for native build/test/lint. No local shell, native renderer or physical device is available.
- One problem: catalog cards become crowded in very narrow panes or with larger fonts; fixed 38dp title space makes equal-width artwork cards uneven at larger font sizes.
- Expected files: catalog screen/shared layout, column unit tests, layout instrumentation tests, Android README, this report and canonical roadmap.
- Stop conditions: source movement, conflicting edits, unrelated state/network/identity changes, secret exposure, or unqualified runtime claims.
- Protected actions: merge, signing, release and deployment are not included.

## Change and ownership

The preferred 3/4/5/6-column breakpoints remain. The shared lazy grid additionally caps columns by the space required for an 88dp card allocation scaled by the current font size plus the existing 8dp gaps. Extremely narrow panes can use one or two columns; small fonts do not increase baseline density. Both catalog/search/favorite/history items and Series episode history use this same production layout.

Artwork titles use two minimum and maximum lines measured at the current font size instead of a fixed 38dp minimum. Compact category labels retain one minimum and two maximum lines. The full card accessibility description retains the original title and metadata.

Provider and episode stable keys, item actions, favorite overlays, sorting ownership, artwork loading/cache/ratios, account/generation ownership and loading/empty/error states are preserved. No provider requests or new caches are introduced.

## Verification record at commit preparation

- PASS: source-derived JavaScript preflight of normal-width expectations, narrow panes, larger fonts and the one-to-six cap. This is arithmetic/source validation, not Kotlin execution.
- PASS: source inspection of both shared grid call sites, stable IDs, callbacks and unchanged catalog states.
- PASS: source composition review against open PRs #166–#170; non-overlapping changes in shared documents and catalog screen.
- NOT RUN locally: Gradle debug/unsigned optimized Release builds, Kotlin unit tests, lint and managed-device tests; this session has no shell/Android runtime.
- Native qualification: existing Validate workflow runs the exact PR head, including Android build/unit/lint and phone API 27/tablet API 35 instrumentation. The PR carries its exact head SHA, run URL and final results; this preparation record does not claim future CI results.
- New unit coverage: original normal-size breakpoints, narrow-pane transitions, larger font sizes and density caps.
- New instrumentation coverage: equal-height short/long Arabic poster cards at 200% font size with full descriptions and clicks; the actual shared grid reflows 3→2→3 columns as font scale changes and preserves the selected item's click binding.
- NOT RUN: physical TV/D-pad, TalkBack, foldable hardware, low-memory/provider/media, screenshots and visual/performance qualification.

## Review and next gate

Inspect the full exact-head PR diff and Validate results. Compose this change with open interface PR #170 before integration; its navigation/theme/category changes are separate. Physical accessibility, TV and visual evidence remain required for release qualification. Merge requires explicit owner authorization.
