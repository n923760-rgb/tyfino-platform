# Admin sidebar RTL layout and pointer reachability — 2026-09-30

## Task packet

Repository: `n923760-rgb/tyfino-platform`; default/official branch: `main`.
Inspected source: 0f7720d26c17c2235df9e7661efe92285cce97ba after authorized modal PR #173 merge.
Owner instruction authorizes continued application improvements and reviewed merges. Scope is the confirmed Admin sidebar/content overlap, a browser regression scenario, documentation and canonical state.
Nearest authority: root `AGENTS.md`; project profile, canonical roadmap, architecture/security/data-ownership and UI guidance read. Live repository/default/main/open-PR/branch/source capability checks precede mutation.
Execution: authenticated repository API and JavaScript source checks; no local shell/browser. Existing Validate runs the pinned Chromium fixture in its exact-source Admin job.
Expected files (five): `apps/admin/src/styles.css`, existing `apps/admin/tests/modal-dialog.browser.mjs`, its README, canonical roadmap and this report. No business logic, dependency/lockfile, role, data/identity/transport/signing or deployment change.

## Confirmed defect

FAIL evidence: initial modal browser [Validate 36779314452](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36779314452), source `80c47851e8ac30114d3e641a64a479e345bf557d`, Chromium 145.0.7632.6, Arabic RTL desktop 1280×900. An ordinary click on the new-code opener timed out because `aside.sidebar` intercepted the pointer. No forced click was used. Subsequent scoped modal keyboard tests do not qualify this separate pointer defect.

Source explains the geometry: the RTL shell reserves its first 260px grid track on the right, but the fixed sidebar is anchored to physical left. Content is placed in grid column 2, so the fixed surface overlaps it. At short heights the fixed sidebar also lacks an internal overflow path for its account controls.

## Correction

The sidebar participates in its own existing grid track with `position: sticky`, `top: 0`, `align-self: start`, full viewport height and vertical overflow. Grid placement now follows the shell direction, so the sidebar aligns with the reserved RTL column; it stays visible during document scroll and its internal controls remain reachable on short viewports.

Width, colors, typography, content-column rule and compact breakpoint remain unchanged. Existing mobile layout hides the sidebar as before. Business callbacks and authorization are unchanged.

## Validation

PASS at preparation: full five-file diff review, exact unchanged business owners, existing eight modal scenarios preserved, no forced click/assertion exclusion, test-module JavaScript syntax and no new runtime dependency.
Runtime task results are recorded on the exact-head PR. Local TypeScript/browser/Gradle checks: NOT RUN.

The new ninth browser scenario uses the real development-mode app, Arabic RTL and a 1280×400 desktop viewport:
- compare actual sidebar/content bounds and require separation at the right edge;
- open/close the new-code dialog with an ordinary pointer;
- exercise real document scroll and require the sidebar to remain at the viewport top;
- scroll the sidebar account control into view, check its hit target with a non-mutating trial click and require it within the viewport;
- click navigation to Settings/Activations and exercise ordinary session/reset/revoke dialog opening, cancellation and opener restoration;
- require cancellation/navigation to add no API mutations.

The existing compact RTL modal scenario and all eight keyboard/dialog cases remain. All API calls use synthetic local intercepted fixtures; non-local requests/unexpected routes/browser errors fail. No production/provider traffic, real credentials, trace or screenshot is retained.
NOT RUN: other browsers, screen readers, physical input and visual-design qualification. Chromium geometry/hit-target evidence is not generalized to these surfaces.

## Integrated parent evidence

Modal exact head: `2e39c189273287c69fc578182d4ab76abda5c972`.
[Validate 36780321130](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36780321130): PASS all seven jobs; Chromium 145.0.7632.6 logged eight dialog scenarios PASS; phone API 27 and tablet API 35 each ran 64 tests, zero failures/skips.
All three earlier modal failures and their bounded corrections are attributable in `ADMIN_MODAL_KEYBOARD_2026-09-30.md` and PR #173. No assertion was removed.
Modal official-main validation: [Validate 36781046104](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36781046104), exact merged push head `0f7720d26c17c2235df9e7661efe92285cce97ba`, in progress at this checkpoint; completion is recorded on PR #173 and verified before this task's merge.

## Next gate

Qualify/review this exact task head, merge under current explicit owner instruction, then verify official-main Validate with all integrated changes. No production deployment/signing/release. Remaining catalog concurrency concern needs its own deterministic diagnosis before a fix; physical/provider/production gates remain separate.
