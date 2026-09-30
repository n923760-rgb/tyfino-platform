# Android Settings sections — 2026-09-30

## Task packet

- Repository: `n923760-rgb/tyfino-platform`; official branch: `main`.
- Starting official SHA: `904f74621e342adb50075757345999ff9fdfd649`.
- Branch: `ui/android-settings-sections-2026-09-30`.
- Owner authority: merge all prepared changes and continue improving the application with full authority. This round stays within the native Settings presentation and its qualification.
- Execution: GitHub repository connector/API, JavaScript source preflight and existing exact-head GitHub Actions. No local shell, native renderer, browser or physical device is available.
- One coherent requirement: replace the flat Settings list with clear, accessible sections and comfortable adaptive width.
- Expected files: Settings screen and instrumentation, English/Arabic strings, Android README, canonical roadmap and this report.
- Stop conditions: unexpected source/target movement, conflicting edits, credential exposure, preference/account/transport behavior changes or unsupported runtime claims.

## Integrated baseline

The six earlier PRs were squash-merged on explicit owner instruction:

| PR | Change | Merge SHA |
| --- | --- | --- |
| #166 | First-use activation expiry | `3990c481b2470fdf9abcc0fe77ca5e47bd883b78` |
| #167 | Admin business/audit transactions | `89a8d98d642e9cd60e0ee8d28739b9b7cea08c41` |
| #168 | Clipboard result handling | `2d1899fa3e6e7e5bc80aa12014dae69ebcf37bc4` |
| #169 | Full-table dashboard counts | `fb6f900b30f7a32617388d09cece038917f77920` |
| #170 | Android theme and adaptive menus | `63ea3f212276709e43fdbab3e76998023dd0b87d` |
| #171 | Catalog font/width readability | `904f74621e342adb50075757345999ff9fdfd649` |

PASS: [official-main Validate 36769206816](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36769206816), exact source `904f74621e342adb50075757345999ff9fdfd649`, push event, seven successful jobs. Phone API 27 and tablet API 35 each ran 62 cases, zero failures/skips. Source composition matched all seven shared files after deduplicating the identical CI summary.

## Change and state ownership

Account actions appear first in a tonal, bordered section; provider compatibility is a separate section. Page/section headings expose heading semantics, section controls form focus groups, and account management uses the existing quiet button style. Content remains scrollable, uses 16dp horizontal padding below 600dp of available pane width and 24dp otherwise, and is centered with a maximum 840dp content width.

The original initial focus target and all four control tags are preserved. The same account callbacks and remembered ProviderUserAgent owner/selection/persistence are used. The account-removal explanation remains visible inside the account section. No account deletion, network request, new preference, endpoint, dependency, app identity or signing change is introduced.

## Verification at preparation

- PASS: source inspection of unchanged preference owner, preset handlers, account callbacks and initial focus.
- PASS: new resource is referenced and present once in both English/Arabic; original strings/tags remain.
- PASS: source-derived content-width/padding preflight. This is JavaScript/source arithmetic, not native layout execution.
- Existing native tests remain: dedicated account actions, initial D-pad focus in a dialog and preference persistence.
- New native coverage: Arabic resources/RTL at 160% font size in a 280×360dp pane, headings/order, scroll access, account actions and selected persisted VLC preset; 960dp synthetic pane with the actual production content capped at 840dp and centered. Density is controlled in the wide fixture so both managed devices can exercise that pane; it is not physical tablet/foldable evidence.
- NOT RUN locally: Gradle build/unit/lint/instrumentation, due to no local shell/Android runtime. Exact new-head results are recorded on the task PR.
- NOT RUN: physical TV/D-pad, TalkBack, screenshot review, actual fold/resize hardware, providers/media, low-memory and performance qualification.

## Next gate

Review the full exact-head diff and CI, merge within the owner's current application-improvement authority, and verify official-main CI after integration. Remaining Admin modal browser accessibility is a separate scoped round; physical and production release gates remain open.
