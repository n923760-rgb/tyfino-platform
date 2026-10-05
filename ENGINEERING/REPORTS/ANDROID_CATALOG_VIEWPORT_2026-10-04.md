# Android catalog viewport and search controls — 2026-10-04

Task: android-catalog-viewport-2026-10-04. Repository: n923760-rgb/tyfino-platform.
Verified source: main@1269935f6b77813b5da7fda005c37bd97c1cc8b9; tree 80aa390fb580422bbf0c4ae9a6bfc592a3c1785d.
Owner: continued professional app refinement and earlier explicit reviewed-merge authority.

## Live baseline and capability

No open PRs; only root AGENTS.md applies. Official-main Validate37176003196 completed all seven jobs successfully on this source. Its source-bound phone/tablet suites report 93 cases each, zero failures/skips, and configured owner APK installation/launch on API33. This round uses authenticated GitHub MCP plus JavaScript source review; no local shell/SDK/Gradle or physical device is available. New-head checks belong to this PR, not this frozen source checkpoint.

## Requirement and causal boundary

The original whole-screen Column measures title/filter/search/sort/error/scope and optional poster-sized resume rows before weighted search/browse results. Those unweighted controls can consume the available height; the weighted child receives only the remainder. The earlier short-height/IME concern was an unmeasured inference, and this report does not claim an observed failure on physical LG hardware or a baseline native reproduction.

The owner requests further professional refinement. One bounded catalog usability feature separates controls from results: controls retain their natural height when they fit, otherwise scroll within half the usable viewport (after a 12dp separation); the results region receives the remaining height. Existing category/status/grid scrolling remains authoritative inside the results region. The controls scroll state is keyed by section. Horizontal filters/categories/resume rails remain horizontal, with stable IDs and focus; no automatic request, category choice or play action is introduced.

Optional Movies/Series Continue Watching stays available in the controls region only in the original browse branch. Search, favorites and history keep their existing eligibility and ordering. No resume record is synthesized or fetched differently. The Movie rail becomes internal solely to exercise the actual renderer in native regression coverage.

## Search interaction

The shared existing query field now exposes a 48dp visible-focus clear control with paired English/Arabic labels, only when input is nonempty (including whitespace). Clearing invokes the same existing empty-query callback. The owning screen still enforces 80 Unicode code points and resets the result. Search eligibility (two trimmed code points), 250ms debounce, cache scope and asynchronous lifecycle/account owners are unchanged.

A one-code-point query shows the existing minimum-length message; longer nonblank queries retain the existing downloaded-category scope message as supporting text. An emoji counts as one code point. Search IME only requests keyboard dismissal; it does not dispatch a provider request, choose a result or alter focus/ownership. This is not proof of a physical OEM IME.

## Validation and evidence

- Prepared source review: scoped diff/resource/import/whitespace/delimiter checks, controller prefix guard, exact existing query callback and request/play callbacks.
- All93 prior native test cases remain untouched. Three focused native cases cover actual shared controls/Movies resume plus browse selection/play in short Arabic/font1.6 windows and resize; code-point guidance/empty and whitespace clearing/IME dismissal callback; keyboard Down from controls to results and explicit Enter activation.
- Expected:96 native cases on each existing API27/API35 managed device, all seven existing jobs, Android debug/optimized unsigned release/unit/lint, configured exact artifact offline API33 install/launch.
- Local native execution BLOCKED. Exact new-head results are recorded in the PR; pending checks are not PASS.
- Physical LG Velvet/TV, actual OEM IME/insets, TalkBack, provider/media and performance NOT RUN.

## Scope and residual risks

Android presentation only, one decorative vector, paired resource label, focused new native test file and canonical engineering records. Application identity/name/version/SDK/ABI/signing/origin, licensing/provider contracts, repository owners, persistence, dependencies/workflows and backend/Admin/infra are unchanged.

No device/account removal, data wipe or license reset. Temporary development signing can reject an update over an earlier differently signed test build; preserve account data before any owner-chosen uninstall. Production signing/release/operations and full physical qualification remain separate gates before sale.

Smallest next action: qualify the exact implementation and official-main artifact, then owner visual/input trial on LG Velvet with saved account credentials. This source checkpoint contains no generated runtime screenshot or commercial readiness claim.

## First exact-source qualification and causal correction

Initial head 80a94712a17f745c35b4cb6fc111c7906dc9e947, [Validate37226945567](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37226945567): Android debug/optimized release/unit/lint and the other four service/governance jobs PASS; API27 phone job 111509850894 and API35 tablet job 111509850866 each ran 96 cases, failed 3, skipped 0. The failed cases were the three new cases; all 93 previous cases passed. The exact APK installer check was SKIPPED after the phone native failure.

1. Clear-button Down did not focus the result tile. The trailing button sits within an editable field whose keyboard/geometry behavior can retain focus. Only vertical key-downs on the focused clear action now explicitly traverse logical next/previous focus before bubbling; text editing, horizontal keys, Enter, touch and other controls retain their existing behavior. The same directional-result assertion remains.
2. Supporting-text assertions used the merged semantics tree; logs explicitly identified the existing tagged message in the unmerged tree. The test now reads that existing child tree, retaining exact Arabic text/code-point and callback assertions. No production label is weakened or removed.
3. Poster-sized resume cards did not display in the bounded controls region. Movies and Series catalog-only rails now use existing compact 240dp text cards with current title/progress, stable keys and exact resume callbacks. Home cinematic artwork and main catalog artwork stay unchanged. The same full-card visibility/touch/resize assertion remains; no scroll/oracle relaxation.

No blind rerun or reduced test coverage. Corrected exact source must separately qualify; the initial native FAIL remains bound to its original SHA. Physical/OEM IME and production gates remain separate.

## Nested-scroll test-driver diagnosis

Corrected production head 90e711e8d56b267e3d4aa7215fa7654b42a454be, [Validate37244422005](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37244422005): all five build/service jobs PASS; phone job 111560730782 ran 96 cases, failed 1, skipped 0. The directional and query/IME cases now PASS; only resume visibility remains failed. Tablet job 111560730774 is recorded separately in the PR when complete. Installation after the failed phone suite is SKIPPED.

The generic resume-card scroll action encounters the card's closest scrollable ancestor, the horizontal LazyRow. AndroidX's scrollToNode implementation computes deltas against that closest ancestor and only dispatches axes it supports; it does not advance the outer vertical controls. [Reference implementation](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-test/src/commonMain/kotlin/androidx/compose/ui/test/Actions.kt) is explanatory context, not exact dependency/source qualification.

The test now scrolls the actual tagged resume Column, whose closest scroll ancestor is the vertical controls, then requires positive vertical scroll offset, visible rail/card, ordinary touch activation, exact payload/callback count and resize bounds. This strengthens the scroll evidence; no visibility/callback assertion is removed. Production geometry is unchanged in this correction.

The earlier poster-height diagnosis was an inference, not a proven cause of the test failure. Compact catalog resume controls remain the scoped short-window presentation refinement; this nested-scroll driver issue explains why reducing card height alone did not fix the failed assertion. Also, assertIsDisplayed establishes at least partial visible bounds after clipping, not full-card physical screenshot acceptance; the earlier wording must not be read as stronger evidence.

All 96 cases and all seven exact-source jobs remain required. Earlier native FAILs remain tied to their SHAs; no blind rerun or hidden failure. Local/physical/OEM/provider/accessibility/performance gates remain unchanged.
