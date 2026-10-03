# Catalog guidance and status placement

## Task / authority

Task ID: catalog-status-placement-2026-10-03; UTC date: 2026-10-03.
Type: IMPLEMENTATION. Controller/executor: Codex through authenticated GitHub MCP and JavaScript source checks.
Repository: n923760-rgb/tyfino-platform; official/default branch: main.
Starting official source: 354ae15063c27217e35a9865a02f99e0019dbfec, tree 94bf04d9c8c5d951ba82b81a8c77da882b7b0a3e.
Task branch: fix/catalog-status-placement-2026-10-03. Local shell/worktree/Android SDK/Gradle execution is unavailable; exact-source files were staged in execution memory before publication to an isolated remote branch.

The owner asks to continue, fix the placement of the category-selection guidance in Live/Movies/Series and review further needed adjustments. This task implements that coherent catalog presentation concern under continuing improvement and reviewed-merge authority. Commercial signing, release/tag/publication, production deployment, identity/version/ABI and licensing/provider-policy changes are excluded.

Live repository/default/main/open PRs, actual capabilities, root AGENTS, central governance, project profile, canonical roadmap, environment/Task-Result instructions, Android README and catalog/device contracts were inspected. No open/conflicting PR at the source gate. Full Task Packet was recorded in execution context before mutation.

## Requirement / diagnosis

FACT (source): EmptyState was a full-width ProductPanel at the top of the item pane; its choose-category message depended only on item state/selection, so it appeared even while category discovery was loading, failed or empty.
FACT (source): item refresh/stale and limited-search notices were aligned over an item grid, allowing text to obscure the first row.
The owner confirms that the placement needs correction. Exact original-device screenshot/geometry is NOT RUN here; diagnosis distinguishes that report from source evidence.

## Changes and ownership

- CatalogBrowsePane renders existing immutable category/item snapshots and existing selection/refresh/play callbacks. It uses the original 800dp sidebar threshold and 200dp sidebar; no automatic category selection, fetch or provider scan.
- One bounded, centered status area occupies the remaining item space below compact categories or beside the expanded sidebar. It caps content at 480dp and can scroll vertically when a short pane/large text outgrows its height.
- The ready-category selection guide has a decorative icon for the current Live/Movies/Series destination and centered readable text. It adds no focus target or navigation action.
- Initial loading/error/no-categories show their own single state without a misleading selection hint. After a selection, category notices stay height-wrapping so saved selected content retains its space. Valid empty category/item states expose explicit existing retry callbacks; refreshing empty states show progress instead of idle retry.
- Refresh/stale/search-limit notices reserve a row above the lazy item grid. Saved items and their play/favorite actions remain available during refresh/failure.
- Search/favorites/history empty messages use the same centered presentation. Existing local-search limits, ownership, safe URI/transport, cache, repository refresh ordering, lifecycle, initial focus, package/version/signing/ABI and dependencies are unchanged.

Six focused native scenarios cover all three sections' compact selection/play callbacks, expanded Arabic/font1.6 geometry and resize, unavailable/empty/stale categories, short Arabic item-state retry/keyboard access, refresh/stale notice geometry and limited-search result reachability. All 84 existing native cases are retained; 90 expected per managed device.
Wide tests explicitly use a synthetic density-constrained viewport that fits both managed-device profiles; this is layout evidence, not physical TV qualification.

## Validation checkpoint

- PASS: guarded source transformation, complete scoped source review, JavaScript whitespace/delimiter and paired Arabic/English string-reference checks. Existing controller prefix and all original Android tests are byte-identical; only shared catalog presentation, six focused scenarios and linked engineering records changed.
- Local native build/test/lint: BLOCKED; no shell/SDK/Gradle execution tools. No local native PASS is claimed.
- Exact new-source Validate: NOT RUN at this source checkpoint; all seven jobs, build/unit/lint, phone API27/tablet API35 and same-artifact configured API33 clean install/launch are required. Exact source/tree/run/job proof and causal failures belong to this PR, without changing qualified source for bookkeeping alone.
- Baseline official-main Validate37122045133: PASS on 354ae15063c27217e35a9865a02f99e0019dbfec. Its qualification belongs to that source, not this implementation.
- Physical LG Velvet/TV/older/16KB/foldable, real providers/media, TalkBack and performance: NOT RUN.

## Review / remaining work

This round reviewed the shared catalog selection, empty, loading, error, refresh, stale and search-limit presentation plus callback routing. It is not a whole-repository security/runtime audit.
INFERENCE for a separately measured follow-up: fixed controls and optional resume rails can consume much of a short full-screen catalog viewport; this task makes the status region scrollable when it has space, but does not claim a complete landscape/IME/whole-screen short-height redesign.
Physical/device/provider/performance, owner-controlled signing backup/recovery and production operations remain release gates.
No production signing, release/publication or deployment was performed. Next: exact-source qualification and reviewed integration, then owner UI/device trial.
