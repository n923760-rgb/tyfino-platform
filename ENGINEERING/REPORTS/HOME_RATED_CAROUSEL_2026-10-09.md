# Highest-rated Home carousel — 2026-10-09

Task: home-rated-carousel-20261009; IMPLEMENTATION, scoped owner-requested feature.
Repository: n923760-rgb/tyfino-platform; official base/mainf9b1555385fabc7cbf3a193dd04189bf8a39cf84; treea25d13dbea31830ba5e97334815f4ae6abb3409b.
Branch/worktree: feat/home-rated-carousel-20261009 / tyfino-home-carousel.
Controller/executor/self-review: current authorized session; self-review is not independent.

## Authority, live state and capability

Owner explicitly requests highest-rated Spotlight with automatic rotation and around ten works, removing latest Movies only; «الباقي تمام» excludes an unrelated redesign. Continuing implementation and standing reviewed-merge authority apply after successful requirements. Main/default identity, source, clean local baseline, no open PRs and unprotected main/empty rulesets were reverified. Official-main Validate37968908256 succeeded; that is baseline evidence, not changed-source proof.

Read root AGENTS.md, central governance reference, project profile, roadmap, Android README, relevant catalog contracts/source and existing exact-source CI. Git/shell/JDK and authenticated repository API/Actions available. No local usable Android SDK/emulator/Gradle distribution: native build/unit/lint/UI execution BLOCKED locally and requires exact-source Actions. Physical devices/provider/media/TalkBack/performance NOT RUN. No signing/release/production operations authorized here. Stop for source drift/conflict, secrets, causal required failures, protected scope expansion, missing authority/evidence.

## Implementation

Use existing account-owned snapshots, not the previous eight newest works. SQLite reads only current snapshot generations/categories for the selected account and Movies or Series. Stream rows with non-null scores on IO; parse finite0–10 scores rather than SQLite's permissive CAST, retain at most ten plus the current candidate, deduplicate section-local IDs and use score/date/ID/category tie-breaks. Old and undated high ratings qualify. No whole-result Kotlin materialization, network/provider-wide fetch, schema/index migration or new dependencies. Query CPU/latency is not measured physical performance; cache reads still inspect eligible local rows.

Repository uses its existing mutex/IO context and revalidates active account ID plus credential generation before returning. Home retains existing request-generation and final account-generation publication checks. Merge two bounded section summaries globally into at most ten; missing/malformed/nonfinite/out-of-range ratings are excluded, not guessed. Same provider ID in Movies and Series remains distinct.

Remove the latest-Movies shelf, its summary read and now-unused default/Arabic label. Keep latest Series, Continue Watching, shortcuts, history, header/account/settings/alerts, adaptive layout and playback/details actions unchanged.

Showcase advances every7000ms while RESUMED. Its effect restarts after a selection/list change, giving manual navigation a fresh interval, and cancels below RESUMED, on composition disposal, explicit pause, touch exploration or keyboard/D-pad focus within the focus group. One cached artwork at a time, no new timer-driven media callback or focus request. A localized pause/resume icon is reachable alongside retained wrapping Previous/Next controls. The500ms minimum/internal interval parameter supports actual-renderer qualification; production callers retain7000ms. No media autoplay or network polling is introduced. Physical accessibility and offscreen/OEM behavior are not inferred from source.

## Qualification plan and limits

Retain all104 prior native cases. Add one real SQLite case for old/undated highest ratings, ten-item bound, duplicates, invalid scores, account/section isolation, replacement and removed categories; two actual Showcase cases for automatic advancement, explicit pause/resume, lifecycle stop/resume, keyboard-focus pause and exact-item manual callback/no implicit opening. Expected107 cases each managed device. Add/update pure ranking/global-order/bound/malformed/ID tests and a repository generation/account-change/no-fetch test. Do not weaken gates or suppress lint.

PASS locally before publication: scoped full-diff/callback/ownership self-review, whitespace, XML parsing/locale parity, no remaining obsolete production references, all104 native case identities retained/107 total and six governance validators including adversarial qualification. These source/static results do not claim native execution. Exact frozen head/tree and executed CI results belong to the PR. Before merge require all seven exact-source Validate jobs, debug/optimized unsigned-release/unit/lint,107 native cases per device with0 failures/0 skips, all nine retained Chromium scenarios and exact configured owner APK offline API33 installation/launch. Record CI/merge/artifact results on the PR without a bookkeeping source commit. productionReady=false; physical/provider/TalkBack/performance, commercial signing and production remain separate.
