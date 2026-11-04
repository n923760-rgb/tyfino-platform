# Keep EPG refresh state clear and navigation available

Task TYFINO-20261104-epg-refresh-busy — IMPLEMENTATION.
Controller/executor: Codex, sequential implementation and self-review.
Owner explicitly requests continued development while deferring the physical TCL check. Standing reviewed-merge authority applies after successful requirements. Physical TV qualification is deferred, not passed.

## Source and work order

Repository/default branch: n923760-rgb/tyfino-platform, main.
Verified official/local clean HEAD: eba2a3b5a1ba2174e51b27e9bf3e8d108dee6858.
Qualified tree e99c116c6f3bb71d399849b4fbf6933f9f37ad7a matches #216 head bf49f95: Validate41828523283 PASS seven jobs,144 native cases/device and API33 installation smoke. Official-main Validate41831581956 was still running at the initial gate; it is not reported as PASS. No open/conflicting PRs. Isolated branch/worktree: improve/epg-refresh-busy-20261104.
Execution capabilities: local Git/source checks and authenticated repository API, hosted Android CI; local Gradle/SDK/emulator unavailable.

Scope: EPG dialog busy presentation/actions, three actual-dialog native regressions, this report and canonical roadmap. Source inspection confirms Refresh has no disabled state during Loading/Content.refreshing/Empty.refreshing, and Empty.refreshing omits loading status. Preserve repository/request-generation authority, player lifecycle and callbacks. Exclude provider networking, cache, credentials/endpoints, dependencies/CI profiles, identity/signing/release/publication/deployment and speculative repair of earlier focus/touch failures.

## Intended behavior and validation

Disable Refresh only while the authoritative presentation is loading/refreshing. Show loading status for cached-empty refresh as for cached-content refresh. A dialog initially busy gives its initial directional focus to Close; normal ready/error/stale entry retains Refresh. Completion must not reset focus to Refresh. Keep Close and hardware Back available during loading. This is a UI action guard after busy state is published, not repository-level request deduplication or a latency claim.

Three native cases: LTR/RTL busy entry and state transitions through loading/content-refresh/empty-refresh/error, Close focus retained on completion, D-pad retry and Back dismissal; touch attempts while disabled invoke no callback and a ready-state retry invokes exactly once. Retain all144 existing cases,147 expected/device. Synthetic states only; no provider or media requests.

Require full scoped diff/self-review and six local governance validators, then seven exact-head Validate jobs,147 native cases/device and API33 exact-APK installation smoke. Stop for source drift, conflicts, secrets, required failures or scope/authority changes. Physical TCL/TV/provider/accessibility/performance and commercial release gates remain NOT RUN. Exact-source CI results belong to the task PR.

PASS local: six governance validators, whitespace check and full four-file scoped self-review. Local native execution BLOCKED by unavailable toolchain; exact-head hosted runtime validation is pending.
