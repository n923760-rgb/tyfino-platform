# Home showcase and playlist subscription — 2026-10-02

## Task packet and authority

IMPLEMENTATION `home-showcase-playlist-2026-10-02`, repository `n923760-rgb/tyfino-platform`, default/official `main`, verified base `e1b2ba9ee5baad712f48e068d5852c87c427838e`. Work branch `feature/home-showcase-playlist-2026-10-02` in an isolated local worktree. Owner requests Home improvements, mixed Movie/Series highlights above Continue watching, and playlist subscription/expiry in Settings. One coherent Android discovery/account-presentation PR. No conflicting open PR at preparation. Root AGENTS, central governance, profile, roadmap, Android README and relevant ownership/catalog contracts govern the work.

Authorized: scoped source/tests/docs, feature branch and PR, existing non-production CI qualification. Protected merge requires current exact-action authorization; no release/tag, production signing/publication/deployment, identity/version/endpoint/ABI/minSDK change, billing/backend work, credentials migration or destructive operations are included. No new dependency or whole-provider scan.

Available capabilities: local git/source/shell and Java 17, authenticated GitHub connector, existing seven-job exact-source Actions. No configured local Android SDK/Gradle cache or physical/provider runtime. Local Gradle wrapper provisioning attempted once: network unreachable for the pinned distribution; native execution remains BLOCKED locally, not PASS.

Stop conditions: changed/conflicting source, credential exposure, required validation failure, contract conflict, unrelated scope or protected action without explicit authority. Result location is this report plus exact-source PR/Actions evidence. Success means original behavior retained, new focused/regression tests PASS, full diff reviewed and artifact tied to its exact source; physical/release claims excluded.

## Implementation and boundaries

- Showcase ranks finite 0–10 ratings then added dates within already bounded cached latest Movies/Series; interleaves at most three of each. Missing/invalid ratings remain deterministic. It is honestly labeled local cached discovery, not popularity across an unseen provider catalog.
- Manual previous/next wrap; no timer, network discovery or automatic focus movement. Showcase opens details. Continue watching separately uses existing account-scoped eligible movie/episode resume callbacks with accessible progress and progress bars. Latest shelves and existing browse/history follow.
- Settings displays provider origin/username, reported subscription status, optional expiry/trial/connections. Provider `user_info` metadata is separate from application license entitlements. Missing/zero/invalid expiry does not imply lifetime access; optional malformed values do not become invented information. Active/Expired/Disabled are provider reports, not client-clock authorization.
- One bounded direct provider query on Settings entry/resume or explicit refresh, no polling/retries/persistence/logging. Existing canonical host, HTTP consent, no redirect/downgrade/TLS bypass, 8-second connect/12-second read and 256 KiB response bounds are retained. The selected provider User-Agent is reused; licensing transport is untouched.
- Before publication, repository checks Account ID, credential generation and latest operation; destination checks lifecycle/request generation and cancels obsolete work. Account switch/removal/credential replacement and older refresh results are rejected. No password, raw JSON, authenticated URL, token or installation identifier enters the summary. No provider metadata enters TYFINO services.
- Arabic/English text, adaptive widths, visible-focus controls and decorative artwork retain native cyan/violet identity. No authentication, player, catalog-persistence or application licensing policy changes.

## Validation

Baseline PASS: official-main Validate 36965457394, seven jobs, 71 cases per managed device. This does not qualify the changed source.

Preparation PASS: `git diff --check`; XML parsing, identical Arabic/English string keys and format placeholders, production string-reference resolution; `validate-task-result-protocol.sh`; `validate-engineering-environment.sh`. These are source/governance checks only. Local native build/unit/lint/emulator execution is BLOCKED by unavailable Gradle distribution network/SDK, not qualified by these checks.

Focused JVM tests cover mixed ranking/ID collisions, invalid ratings/bounds/empty selection; provider parsing, unknown expiry, unsupported/auth-rejected responses; switch/removal/generation races; older refresh and redacted output. Five new instrumentation scenarios cover manual showcase/detail actions, accessible progress/resume, Arabic large-font controls, independent provider summary/unknown expiry/refresh, and reported expired status/known expiry. All previous tests retained.

Preparation source checks, exact tested head, all seven jobs, Debug/unsigned Release build, unit/lint, existing browser/API regression, phone/tablet counts and source/hash/certificate-bound configured APK/API33 clean install/launch outcomes are recorded on the task PR after execution. No prospective native result is labeled PASS.

Physical LG Velvet/TV/API24/16KB/foldable/low-RAM, TalkBack, real-provider subscription/media compatibility and measured performance: NOT RUN. Owner-test APK remains a debug development artifact, not a signed commercial release. Earlier OEM/Google protection installation cause remains UNKNOWN; this presentation task does not bypass device protection or claim to fix it.

## First exact-head UI failure and correction

Head `d807824cb23dd9f660bb32de13d300352258fbd2`, Validate 37038931937: all five non-instrumentation jobs PASS, including Debug/unsigned Release/unit/lint and configured package inspection. Phone job 110946761684 executed 76 cases, one failure, zero skips: `HomeShowcaseTest.spotlightMovesBetweenMovieAndSeriesWithoutStartingPlayback` cannot display the Series title after Next. Other 75 cases, including the original 71, pass. Tablet result was still pending at the correction checkpoint; no all-job PASS is claimed.

Source measurement review identifies the hero's decorative `fillMaxSize` children participating in the parent's size, consuming bounded vertical space and allowing intrinsic artwork sizing in unbounded rails. Corrected the image and gradient to Box-scoped `matchParentSize`: foreground content and adaptive minimum height determine the hero, not its decorations. Android's official Compose modifier guidance documents this exact distinction: https://developer.android.com/develop/ui/compose/modifiers#matchparentsize-in-box. The same standalone user-flow test is retained and strengthened to require Previous/Next actually displayed before clicking. No assertion is removed, scrolling workaround added, screenshot assumption or physical PASS invented. Corrected exact-head qualification is recorded on the PR; the first APK is not delivered as qualified.

## Next gate

Complete exact-head CI/review and request owner interface feedback with the configured test APK. Merge/release/signing/production deployment remain separately authorized; real-provider expiry and physical/input/accessibility qualification require their own evidence.
