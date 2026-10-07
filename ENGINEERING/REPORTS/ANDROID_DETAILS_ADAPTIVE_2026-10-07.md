# Adaptive Android Movie/Series details — 2026-10-07

## Task identity and role

Task ID: android-details-adaptive-20261007
Type: IMPLEMENTATION
Controller/executor: current authorized engineering session.
Official repository: n923760-rgb/tyfino-platform
Official branch/base: main@62bb365a99a4ab043c032c0325f7ab5831a67c61
Base tree: c7967fae38b67dc4ecd586591e9c3502419f32c8
Isolated branch/worktree: feat/details-adaptive-layout-20261007, tyfino-details.
Execution: local Git/source/shell, JDK17, authenticated GitHub connector and existing Actions. Local Android SDK/emulator and usable Gradle distribution are unavailable.

## Authority, scope and live gate

The owner explicitly requests continued implementation, error correction and interface development without repeated pauses. Prior explicit reviewed-merge authorization remains in scope. This task is one coherent adaptive Movie/Series details presentation change. Production deployment, release/tag/signing, secrets, application identity, branding, SDK/ABI/version/origin, provider transport, persistence, licensing and playback policy are outside scope.

Before implementation, current AGENTS.md, project profile, Android README, relevant Movie/Series source and ownership contracts, environment/task protocols, canonical roadmap and live central MASTER_GOVERNANCE.md were read. Live main/default branch identity matched the base; the open-PR API returned no PRs. Official-main Validate37578813615 passed all seven jobs. Shell clone/source reads worked. CLI write dry-run lacked credentials; initial connector errors were transient and a subsequent authenticated source read succeeded. Work was prepared in an isolated worktree, with the task packet recorded in the authorized execution context before source mutation.

Stop conditions: unexpected official/head source, overlapping PR, unrelated scope, secrets, causal required test failures, unqualified protected actions or unsupported runtime claims. If a required qualification cannot run, report BLOCKED and do not treat this change as accepted.

## Source finding and implementation

FACT: the shared poster hero always used a horizontal Row. In a 240dp details content area, 40dp panel padding, an88dp poster and a16dp gap left96dp for the heading, irrespective of font scale. Both detail screens put Back and eligible Refresh in a fixed Row. Localized button labels and title font size can grow.
INFERENCE: compact windows and larger Arabic text can make the heading unnecessarily narrow and press the second action into the remaining space. No old-source physical clipping failure or measured baseline runtime failure is claimed.

Authorized improvement:

- Preserve the existing poster beside the heading when at least200dp, scaled up with system font size, remains for text. Otherwise center a bounded160dp poster above the full-width heading/facts. Landscape artwork keeps the existing full-width16:9 frame and320dp cap.
- Replace the two fixed action rows with FlowRow, retaining label measurements, spacing, focus grouping, initial Back focus and exact existing callback eligibility. Existing LazyColumn scrolling and Movie Play/Resume remain unchanged.
- Keep artwork decorative and optional. The same bounded image loader, provider metadata, selection and lifecycle/account owners remain authoritative. No async/persistence/network code changes.
- Add four actual-renderer scenarios: RTL poster reflow on font/window changes; compact landscape fallback geometry/readability; compact Arabic Movie callback reachability; wrapped Series directional focus/explicit activation. Tests attest the TextLayoutResult's actual font scale and unclipped bounds, not an inferred external density.

## Validation and result

Required local review: complete diff, whitespace, scoped file set, retained callbacks/owners, resource references and existing native test preservation.
Required native regression: seven existing Validate jobs; debug/optimized unsigned-release build, unit tests and lint; all99 existing cases plus four new cases (103 expected per managed device); same configured owner-test APK offline API33 installation/launch.

PASS locally: git diff --check; complete scoped diff review; all existing instrumentation files compared byte-for-byte with official base and retained unchanged (99 existing Test annotations, four added); validate-engineering-environment.sh; validate-task-result-protocol.sh; validate-ai-executor-qualification.sh; validate-ai-executor-behavioral-trials.sh; validate-ci-qualification.sh; validate-governance-qualification.sh and its six adversarial checks. These are source/governance checks, not a native compile/runtime result.

BLOCKED locally: ./gradlew --version failed while fetching the pinned Gradle9.6.0 distribution with Network is unreachable. Direct distribution probes redirected to a GitHub release but timed out; Android SDK/emulator are absent. No local build/unit/lint/native runtime PASS is claimed. This existing environment limitation is not a source compile failure.
Exact changed-source CI results, commit/tree identity, PR review and delivery belong to the task PR. A successful baseline run is not changed-source validation. Final source bookkeeping is kept on that PR to preserve the tested source.
Physical phone/TV/foldable, TalkBack, real-provider/media, performance and commercial release qualification: NOT RUN in this round.

## Artifacts and next action

No secret-bearing artifact is created. Ordinary configured Debug owner-test output is a development artifact; commercial signing/custody/upgrade continuity is not established by it.
Next action: qualify and review this exact branch in the existing CI, resolve any first causal failure, and integrate only after required checks succeed within owner authorization. Then deliver the qualified configured owner-test APK for visual/input feedback. productionReady=false.

## Initial native build and causal correction

Initial head eb121d81cc140527c351d53a06673d82f7c864b9, tree68f15b502b62352567b1f6077e7389dffd0d5165, [Validate37657274286](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37657274286): four service/governance jobs PASS; Android job112915458976 FAIL during compileDebugKotlin. First causal error: ProductPresentation.kt:130 could not access BoxWithConstraints.maxWidth as an implicit receiver within the nested Column scope. Native instrumentation and APK qualification were SKIPPED, not PASS.

Correction calculates the bounded stackedPosterWidth in the BoxWithConstraints scope before entering Column. Presentation decisions, geometry, callbacks and every test/assertion are retained. Corrected-source qualification is pending on the same PR. No merge or artifact delivery occurred on the failed source; no blind rerun or workflow/test-policy change is used.

## Instrumentation compile and measurement correction

Corrected production head7122c6b6f7fac7aeb5b69705a3ce6c87731a8873/tree df2a0d1a836833ab0057481935a93e59980452cf, [Validate37657607844](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37657607844): all five build/service jobs PASS, including debug/optimized unsigned-release/unit/lint/configured APK package verification. Both native jobs FAIL during compileDebugAndroidTestKotlin, with zero tests executed. First causal error: DpRect width/height are extension properties, and the new test did not import them. The initial interpretation of secondary unresolved-value diagnostics as Float dimensions was incorrect.

The attempted pixel-only correction at head03a60db6fbdaa52edf538bb7a66d11b383f275d5/tree f545fc09f4e419182e27a682f14e7c52e27c5eb6, [Validate37659021176](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37659021176), did not correct those unresolved extension accesses. Five build/service jobs PASS; tablet job112924596740 FAIL at test compilation, zero native cases. Complete compiler diagnostics and official AndroidX Dp.kt source (blob142c5afcc29f0e57c3c99d0420dc1cda59fb2c50) establish DpRect's Dp coordinate members and separate width/height extensions. This is a diagnosis mistake in the previous correction, not a change of the dependency API.

The final test-only correction calculates width and height directly from right-left and bottom-top, using the Dp coordinate members, and retains the exact200dp width/16:9 ratio assertions. All four scenarios, every geometry/readability/callback/focus assertion and all99 previous tests remain. Production source is unchanged. New-source qualification remains pending; no partially qualified APK is delivered or merged.

## First actual native readability result

Head e6a0a7bdd698022b40fe5f49896668a994583405/tree960aecd58e9298cd5b0160ec3a08e9708b570601, [Validate37660644631](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37660644631): all five build/service jobs PASS. Phone job112929702411 compiled and executed103 cases: two failures, zero skips. The XML stacks identify the title hasVisualOverflow assertion in the compact landscape case and the font2 transition of the poster case. Both Movie callback/readability and wrapped Series directional cases PASS; all99 previous cases PASS. API33 install/launch was SKIPPED after the test failure. Tablet qualification was still running at this checkpoint.

FACT: DetailsHeading's Column fills available width, while both Text fields still used natural wrap-content width. The failing fixtures use a short Arabic title; the longer Movie title passes. INFERENCE: giving the text fields the full heading width avoids the short-title measurement concern while matching the intended full-width heading presentation. The precise overflow dimension is not yet captured, so no rounding or physical clipping cause is asserted.

Correction makes the title and facts Text fields fill the heading's existing width. No font, text, height, truncation, scrolling or callback changes. The existing no-visual-overflow assertion is retained and gains actual size/paragraph/constraint/overflow diagnostics. Exact corrected-source qualification is required; no failed-source merge/delivery or blind retry is performed.
