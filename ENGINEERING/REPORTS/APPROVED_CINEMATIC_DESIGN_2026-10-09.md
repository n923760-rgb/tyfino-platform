# Owner-approved cinematic presentation — 2026-10-09

Task ID: approved-cinematic-design-20261009
Type: IMPLEMENTATION
Controller/executor: current authorized engineering session; self-review is not independent review.
Repository: n923760-rgb/tyfino-platform
Official base: main@06f37f62caf1d3b33b4b5d8f28c5c10925026ece
Base tree:308ce3b347b1097d9011729db9a2e36c002ee8f4
Isolated branch/worktree: feat/approved-cinematic-design-20261009 / tyfino-approved-design.

## Authority and live gate

The owner explicitly approved the displayed proposed app design with «تمام اعتمد هذا», following ongoing implementation and standing reviewed-merge authorization. Apply its visual direction to existing native presentation; this does not authorize fake content, new provider access, signing or commercial release. Root AGENTS.md, project profile, canonical roadmap, Android README, relevant catalog/account/playback contracts and exact-source CI contract were inspected in this continuing session. Live official/default main identity and source, empty open-PR set and clean local baseline were reverified before the isolated task. Baseline official-main Validate37961457475 passed all seven jobs. Baseline evidence is not changed-source proof.

Capabilities: real Git/shell/JDK17, repository connector and existing Actions. Local Android SDK/emulator/usable Gradle remain unavailable; changed-source native build/tests/lint are BLOCKED locally and require exact-source Actions. Physical devices, provider/media, TalkBack and performance are NOT RUN. Stop for source drift, unrelated conflict, secrets, causal required failures, protected scope expansion or missing evidence. No force-push or direct-main edit.

## Design translation and scope

Existing cinematic foundation already provides cached showcase/resume shelves, adaptive navigation, responsive details and Settings subscription/license sections. Refine it rather than introduce alternate state owners or a second interface framework:

- Match midnight navy background #091321, layered panel #132236 and turquoise #36D6C0; retain existing readable foreground/error pairs and restrained accents.
- Shared actions become pill-shaped with quieter secondary surfaces/borders. Existing52dp minimum, enabled states, callbacks, focus observer and3dp high-contrast focused border remain.
- Calm panel gradients and turquoise selected bottom-menu labels apply consistently to existing screens, including Settings/activation/account/series presentation through shared components.
- Home uses the existing TYFINO name as one compact heading, retains account/settings/notification controls and initial focus. The hero title wraps fully instead of ellipsizing at two lines, and its primary details action fills its bounded content width. All showcase selectors, cached metadata, content order, focus targets and owners remain.
- Compact grids prefer two posters below480dp; existing4/5/6 wider breakpoints and88dp font-scaled readability minimum remain, allowing one column for very large fonts/narrow panes. Shared Live/catalog/search/favorites/history use the same policy.
- Movie Play/Resume occupies available width within1040dp; existing Back/Refresh wrapping, focus order, loading/owner checks and playback authority remain. No new favorite action or progress model is fabricated from the conceptual board.

No generated board/posters enter app assets. Images remain optional provider artwork from existing bounded loader paths. No new dependencies, requests, automatic carousel, blur/shadow animations, provider scans, persistence, backend/licensing, IDs, SDK/ABI/version/origin/signing/CI changes. The board illustrates direction, not pixel-perfect native geometry, real content availability, screenshots or performance acceptance.

First exact-source Validate37966336167 at6780668832d210e918a8e1b7fd699f6b283047f2 passed debug/release compilation and unit tests, but lint correctly failed on the now-unused home_title and product_home_description resources. The complete causal lint report identified only these two errors; three pre-existing allocation hints are not failures. Remove both obsolete resources from default and Arabic translations after confirming there are no remaining source references. Do not suppress UnusedResources, change its gate, or rerun unchanged source. Native jobs were skipped because their Android dependency failed; new-source qualification is required.

## Verification plan and evidence boundary

Smallest deterministic checks: full intended diff/whitespace review, exact compact-grid preference change with retained wider thresholds/readability arithmetic, retained callbacks/state owner/loading paths and source/test case counts; static color-pair contrast calculation and six governance validators. Local executed outcomes and frozen source belong to the PR result. Do not claim planned commands as PASS.

Update existing CatalogGridColumnsTest and gridReflowsWithFontScaleAndKeepsItemActions expectations for the explicit new two-column compact contract. Retain width/font restoration and real item callback assertions; use200% to prove actual reflow to one column. No existing test is deleted. Add one real renderer HomeShowcase scenario at280dp, Arabic RTL/font2, asserting the full long title, more than two lines, no visual text overflow, no implicit navigation and explicit exact-item details activation. Expected104 native cases per device; all prior103 still present.

Required before integration: all seven exact-source Validate jobs, debug/optimized unsigned-release/unit/lint,104 cases each phone/tablet with zero failures/skips, nine existing Chromium scenarios and same configured owner APK offline API33 installation/launch. Record final SHA/tree/run/jobs/artifact/merge evidence on the PR without a bookkeeping source commit. Physical TV/LG/foldable, provider artwork/media, TalkBack, visual screenshots and startup/scroll/performance remain separate. productionReady=false.

PASS locally before remote publication: full scoped production/test diff and whitespace review; six governance validators including the adversarial suite;104 native@Test declarations; static color contrast primary/onPrimary7.18:1, background/onBackground17.43:1, panel/onSurface15.00:1, secondary-button/onSurface17.02:1 and surface/onSurfaceVariant10.95:1. These measured solid-color pairs exceed4.5:1; they do not certify every image overlay, disabled state or TalkBack behavior. Native density coverage explicitly scrolls the one-column grid to activate the third item and restores index0 before checking restored two-column geometry. Source regression counts, callback/focus/state/CI identity boundaries are retained; runtime PASS remains pending exact-source Actions.
