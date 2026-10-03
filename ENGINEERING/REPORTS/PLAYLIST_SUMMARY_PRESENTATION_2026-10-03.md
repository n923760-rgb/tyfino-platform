# Playlist subscription presentation

## Task / authority

Task: `playlist-summary-presentation-2026-10-03`; UTC date 2026-10-03.
Type: IMPLEMENTATION. Controller/executor: Codex.
Repository: `n923760-rgb/tyfino-platform`; official/default branch: `main`.
Starting official source: `8c1918a7d567a8be4d75ee12796d519e3a21370d`.
Task branch: `feat/playlist-summary-presentation-2026-10-03`; isolated worktree: `tyfino-playlist-summary`.

Owner confirms #181 merge and continued improvements ("اي استمر بدون توقف"). #181 was merged using its exact qualified head guard; integrated tree equals the tested tree. Current bounded continuation improves the previously requested playlist information in Settings. Future protected merges, signing, release and deployment remain exact owner decisions.
Live repository/default/main/open PRs, exact #181 source/CI, AGENTS, profile, canonical roadmap and central governance were verified. Applicable Android README, account/catalog/device contracts and actual execution capability were inspected. Task packet was recorded in execution context before source edits. No conflicting product PR at source checkpoint.

## Requirement / owners

Make reported subscription state and expiration easier to find and read while preserving direct-provider provenance and the separate application license. PlaylistRepository, account/generation/request checks and destination lifecycle remain authoritative. This change only renders the existing immutable PlaylistState.

## Changes

- Full-width tonal provider-status and expiry blocks with readable growing text. Active uses the existing primary colors; reported Expired/Disabled uses existing error colors with an explicit localized status label. Color is never the sole indication; blocks are noninteractive.
- Date and connection integers follow the active string-resource locale. Counts disable grouping; both positional connection placeholders now accept preformatted strings. Unknown active count uses the existing Not provided fallback, while missing maximum keeps the optional row absent.
- No clock-based expiry calculation, countdown, inferred lifetime, extra network call, timer, cache, persistence, dependency or changed entitlement. Existing refresh, loading/failure/no-account states and semantics tags remain.
- Two added native cases: 280dp Arabic RTL/font1.6 reachability and horizontal containment for facts/refresh with localized date/counts; replacing reported expired/known metadata with another ready state removes prior expiry and optional counts. All 82 prior cases remain; 84 expected per managed device.

## Validation checkpoint

- PASS: full scoped source/diff review and `git diff --check`; paired XML keys/positional-format parity, formatted connection placeholder contract and Kotlin string references.
- PASS: environment validator and Task/Result protocol validator.
- Local native build/test/lint: BLOCKED by unavailable Android SDK/Gradle distribution and restricted provisioning network. No local compile/test PASS claimed.
- New exact-source Actions: NOT RUN at this checkpoint; existing seven-job Validate is required after reviewed publication. Exact head/tree/run/job results and any causal failure are retained on the PR without changing the qualified source for documentation-only bookkeeping.
- Baseline #181: [Validate37077618687](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37077618687) PASS all seven jobs, 82 cases per device/zero failures/skips/API33 exact configured APK clean-install/launch on head `e1b0f89a76c12070becd45513ea7ce836a15fb3b`. Official merged tree `04baeaea143a92ccbda3434c5d153f2b93858dfc` is identical. Official-main [Validate37092878214](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37092878214) is separately tracked on #181. Baseline success does not qualify this change.

## Result boundary / next action

Physical LG Velvet/TV/foldable/older/16KB devices, real provider/media, TalkBack and performance acceptance remain NOT RUN for this task. Synthetic native UI geometry/string checks are narrower than physical/visual/accessibility qualification.
Next: exact-head qualification and source review; owner exact merge decision, then owner device/UI trial. No commercial signing, release/publication or production deployment performed.
