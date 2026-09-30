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

## Evidence and completion gates

Review the full diff, source identity, shared roadmap composition and exact-head Actions. Fix reproducible failures before readiness; record any unresolved/flaky runtime failures honestly. Stop for unexpected base/head movement, overlapping product work, authority conflict, secrets or unrelated source changes. Signing and deployment remain outside this task.
