# Details maximum-width correction

## Task / authority

Task ID: details-width-bounds-2026-10-03; UTC date: 2026-10-03.
Type: IMPLEMENTATION. Controller/executor: Codex through authenticated GitHub MCP and JavaScript source review.
Repository: n923760-rgb/tyfino-platform; official/default branch: main.
Starting official source: d4e3204a5d3700878a9a6f374c6d4e08d448387b, tree 7e60ee5a6b2b0d87fb291d3a167ed552a3507868.
Task branch: fix/details-width-bounds-2026-10-03; no local shell/worktree/Android SDK/Gradle execution is available.
Full Task Packet recorded in execution context before mutation. Live repository/main/open PRs, source identity, applicable AGENTS/governance/profile/roadmap and Android presentation/device contracts were revalidated. No conflicting PR.

Owner requests continued improvements and review of other needed adjustments alongside the catalog guide correction. This is a separate bounded follow-up under continuing implementation/reviewed-merge authority. Commercial signing/release/deployment and product identity/version/ABI/licensing/network policy are excluded.

## Problem / correction

FACT (source): Movie and Series story panels used fillMaxWidth().widthIn(max=1040.dp), and EpisodeRow used fillMaxWidth() before widthIn(max=760.dp).
Compose applies incoming constraints in modifier order: filling first establishes a fixed width, so the subsequent maximum cannot reduce it on a wider window. This conclusion follows the standard sizing contract; no physical screenshot or measured width PASS is claimed here.

Only these three sites now constrain the existing maximum first, then fill the available constrained space. The existing 1040dp/760dp limits, compact-window behavior, alignment, text, state, generation checks, episode eligibility, play/back/refresh callbacks and focus remain unchanged. No dependency, resource, new UI abstraction or test-policy change.

## Validation checkpoint

- PASS: live-source three-site diff/whitespace review and exact reverse-transform equality to the original source; all remaining source and all 90 existing Android cases are untouched.
- Local native build/test/lint: BLOCKED by unavailable shell/SDK/Gradle tools; no local native PASS claimed.
- New-source existing seven-job Validate, 90 cases per API27 phone/API35 tablet and exact configured APK API33 install/launch: NOT RUN at this checkpoint, required before qualification/integration. Exact source/tree/run/job/artifact evidence belongs to its PR.
- Baseline #183 [Validate37157049507](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37157049507): PASS all seven jobs, 90 cases per device/zero failures/skips and same source/hash/certificate configured APK API33 clean-install/launch. Integrated main tree equals that qualified tree. Official-main Validate37158569337 is separately tracked on #183.
- No redundant implementation-mirroring tests added for this reversible modifier-order correction.
- Physical wide-window/TV/RTL/accessibility, real providers/media and performance: NOT RUN.

## Result boundary / next action

This source correction restores intended width limits; existing automated tests preserve broader details/catalog/focus regressions. It does not constitute measured physical wide-screen acceptance.
Next: exact-source qualification and reviewed integration, then owner UI/device trial. Catalog whole-screen short-height/IME reachability, physical LG Velvet/TV/older/16KB/provider/TalkBack/performance, signing custody/recovery and production operations remain separate gates.
No production signing, customer release/publication or deployment performed.
