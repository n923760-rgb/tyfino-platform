# Admin modal keyboard and focus round — 2026-09-30

## Task packet

Repository: `n923760-rgb/tyfino-platform`; default/official branch: `main`.
Inspected source: `a21342df6ba3ca8465b5ef6b179bb145c8f7f5d6` after authorized Settings PR #172 merge.
Owner instruction: merge all prepared application changes and continue improvements with full authority. This round covers the shared Admin modal and its browser proof, followed by exact-head review/merge and official-main validation.
Nearest authority: root `AGENTS.md`; project profile, canonical roadmap, approved architecture/security/data-ownership and current UI state map read before mutation. Only root AGENTS is present in the inspected recursive tree.
Execution: authenticated repository API and JavaScript source checks; no local filesystem/shell/browser. Existing exact-source Validate CI executes Node/Chromium and regression checks.
Live preflight: repository/default/main identity verified, no open PRs, branch absence verified; main branch reports unprotected, rulesets empty. No protection policy is changed.

Expected files (seven): shared `apps/admin/src/components.tsx`, its `styles.css`, browser test and README under `apps/admin/tests/`, existing Validate Admin-job browser steps, canonical roadmap and this report. No business handler, role, transport, dependency/lockfile, identity, signing or production change.

## Confirmed problem and correction

The old modal focused close on every `onClose` callback change, listened globally for Escape, and declared `aria-modal` without enforcing background inertness, Tab containment or focus restoration. Inline callbacks change when the actual parent rerenders (for example toast expiry), so typing focus could also move.

The shared surface now uses native `<dialog>` with `showModal()`. The browser supplies modal keyboard/background behavior. A mount-only effect focuses close once, closes the native dialog on cleanup and restores the captured opener when still connected. React StrictMode setup/cleanup is exercised by the real app. Escape uses the dialog cancel event; the existing callback controls dismissal. Pointer dismissal requires a target on the dialog and coordinates outside its bounds, so clicks on empty interior padding or content do not close it. `useId()` gives each heading its own accessible label.

Native `::backdrop` retains the existing dim/blur treatment. The framed scrollable dialog retains normal/wide widths with 20px viewport clearance and no browser-default padding/border. Existing child forms and business callbacks are unchanged. No manual inert flags or document-wide keyboard listeners are introduced.

## Validation and evidence boundaries

PASS at preparation: complete source diff review; untouched business/security owners; seven expected paths; test-module JavaScript syntax; exactly six existing checkout/SHA-verification job definitions; pinned test-tool version; no production dependency or secret-bearing diagnostics.
NOT RUN locally: TypeScript/build, actual DOM focus/layout, Chromium and full repository regressions. These must pass the exact task head in existing Validate before merge. PR body records attributable run/head/results, including any failures and their fixes.
Settings parent head: `2950bb03a8225a6a1d45d22152ed4962f81f4cb5`; [Validate 36772162343](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36772162343) passed all seven jobs, phone/tablet 64 cases each, zero failures/skips. Attempt 1 phone emulator ZIP download failed before tests; only that job was rerun on unchanged source and passed on attempt 2. Settings official-main [Validate 36779000481](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36779000481) was in progress at this source checkpoint.

The browser fixture serves the actual development-mode app on loopback, including StrictMode, with pinned Playwright 1.58.2 Chromium. Every API route is intercepted with synthetic fixture data; non-local origins and unexpected routes fail. No production API/provider is contacted. No trace, screenshot, credentials, storage state or full fixture code is printed or retained.

Eight logged browser scenarios cover:
- forward/reverse traversal past all controls and background programmatic focus exclusion;
- Escape and opener restoration;
- backdrop and form cancellation;
- reset/revoke cancellation without mutation;
- create-to-result transition through the existing mocked handler;
- typing preserved across the actual toast-expiry parent rerender, interior clicks and close restoration;
- session-revocation cancellation without mutation;
- compact Arabic RTL viewport bounds, scroll access and cancellation.

NOT RUN: screen readers, Safari/Firefox, physical input, production API and visual-design qualification. Browser CI evidence must not be generalized to these surfaces.

## Next gate

Qualify/review the exact new head, merge under the current explicit owner instruction, then check official-main Validate and record results on the task PR. No production deployment/signing/release in this round. Remaining catalog concurrency finding requires a separate deterministic ownership diagnosis before any fix.
