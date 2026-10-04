# Android cinematic design — 2026-10-04

Task: cinematic-android-design-2026-10-03
Type: IMPLEMENTATION
Owner requirement: implement the approved navy/turquoise cinematic Home and design the remaining existing sections.
Started: 2026-10-03 UTC; source preparation/checkpoint: 2026-10-04 UTC.
Repository: n923760-rgb/tyfino-platform
Official baseline: main@56fcccb86693451a93056fa67b30711b3e07336c
Baseline tree: 6f485adb63f1c6b64d4ef4293899591291948aab
Work branch: feat/cinematic-android-design-2026-10-03
Execution: authenticated GitHub MCP and JavaScript source staging/checks; no local shell/SDK/Gradle/emulator.
Authority: current owner design request plus earlier explicit implementation and reviewed-merge authority. Production signing/release/tag/deployment excluded.
Task/Result packet retained in the authorized execution context; exact new-head qualification and final artifact evidence belong to the task PR.

## Requirement and implementation

One coherent presentation feature applies the approved cinematic direction across existing Android destinations.

- Navy surfaces, turquoise primary actions and selected filters, cyan/violet secondary accents; existing TYFINO app mark/name remain the identity.
- Shared decorative brand headers, subtle tonal panels, consistent outlined field fills and rounded shapes. Phone bottom menu and compact/expanded side menus retain existing destinations and visible selection/focus.
- Home uses a responsive artwork-backed hero with direction-aware gradients, existing optional provider metadata and explicit details action. Three section shortcuts move above the remaining shelves. They share one row when readable widths fit, otherwise become full-width rows as width/font scale changes.
- Continue Watching uses wider landscape cards with its existing progress and resume callbacks. Existing account/settings/alert controls remain explicit; account/generation/lifecycle summaries are unchanged.
- Live/Movies/Series search gets a decorative search symbol and the shared field presentation. Solid selected filters use paired foreground/background colors; cards use quieter surfaces and continuous upper artwork corners.
- Movie/Series details use framed artwork or a decorative placeholder when artwork is absent. Wide backdrops crop within a height computed from available width, capped at 320dp with compatible constraints. Story widths and existing episode limits remain effective.
- Series uses its own placeholder symbol; episode rows have a decorative play affordance beside growing text. Published-generation/playability/disabled semantics and episode callback payloads remain authoritative.
- Account entry, activation, account dialogs, Settings, player recovery/options and other shared-panel users inherit the same presentation. Account dialog headers gain the shared brand treatment; scrolling, busy state and destructive-action confirmation are retained.

All items come from current owned snapshots. No fabricated popularity/resolution/audio badges, prices, global library destination, provider-wide search, Home provider fetch, new playback action, network/storage or account/license policy is added.
The generated ten-screen board is a conceptual illustration with fictional content, not captured runtime screenshots and not pixel-exact acceptance evidence.

## Source ownership and scope

FACT: controller/lifecycle/request segments of Home and Catalog are checked for preservation; auth/licensing field changes are presentation arguments; account selection/removal callbacks and Series generation/playability expressions are retained.
No change to package/application identity, name, version, supported SDK/ABIs, licensing origin, signing model, dependencies, workflows, backend/Admin/infrastructure, providers or persisted schemas.
Original 90 native cases are retained without edits. Three new cases cover consequential adaptive/input/fallback behavior; no color/constant mirror tests are added.

## Validation at this source checkpoint

- Baseline PASS: exact official-source [Validate37159631461](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37159631461), all seven jobs on 56fcccb86693451a93056fa67b30711b3e07336c; 90 cases each API27/API35 and same configured APK offline API33 install/launch recorded on #184.
- Source review: guarded transformations, full scoped diff, controller/callback preservation, resource references, Kotlin delimiters/whitespace and opaque text/selection color contrast checked before publication. Attributable actual results are recorded on the task PR.
- Local native build/unit/lint/instrumentation: BLOCKED; no shell/SDK/Gradle/device execution is available.
- Required new-source CI: exact-source seven-job Validate, Android debug/optimized unsigned release/unit/lint, 93 cases each API27/API35 (90 existing plus 3), and same configured owner APK offline API33 install/launch without test-only flags. Pending at this frozen source checkpoint; final proof belongs to the PR.
- Physical LG Velvet/TV/API24/foldable/16KB runtime, real-provider/media, TalkBack, OEM install-over and performance: NOT RUN. Emulator assertions do not establish those results.

## Residual risks and next action

Runtime screenshot/visual acceptance on representative hardware remains open. Whole-screen catalog short-height/IME control-space behavior was previously an unmeasured inference and is not claimed resolved by this presentation round.
The configured owner APK uses temporary development signing; a different installed test signature can block an update. Uninstall erases local data and may require device-license reactivation; no uninstall/reset is performed here.
After exact-head qualification, reviewed merge and official-main confirmation, deliver the same verified owner artifact for physical owner review. A commercial release still needs its signing, device/provider/accessibility/performance and production-operation gates.

## First qualification correction

FAIL: [Validate37174137385](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37174137385) on head 6b78a96c8101b50cd484469e87898e45d73b3820, Android job111353104977, stopped in compileDebugKotlin. The first causal diagnostic at ProductPresentation.kt:117 was that maxWidth could not be accessed through an implicit BoxWithConstraints receiver inside a nested Column scope. API/Admin/database/governance passed; downstream release/unit/lint/native/API33 tasks did not run and cannot be counted as PASS.

Correction: compute the same backdrop height in the immediate BoxWithConstraints scope and pass the local value into Column. No dimensions, policy or ownership change. The full causal log was read; this is a source correction, not a blind rerun. New exact-source qualification remains required and is recorded on the PR.
