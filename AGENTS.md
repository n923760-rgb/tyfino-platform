# TYFINO repository instructions

TYFINO adopts Engineering Governance v1.0.0 from `n923760-rgb/engineering-governance`.
Project-specific customization lives under `governance/`.

Authority order is: current owner instruction → this/nearest `AGENTS.md` → approved scoped subsystem contracts → engineering environment contract → advisory guides → atomic Task Packet. Lower layers may narrow but must not widen higher-layer authority.

Read the nearest `AGENTS.md`, `governance/PROJECT_PROFILE.md`, and the authoritative documentation for the area being changed before editing.

- Keep work atomic: one confirmed problem or scoped feature per branch and pull request.
- Audit, review, and diagnosis are read-only unless implementation is explicitly requested.
- Do not make speculative fixes or unrelated refactors.
- Never merge, force-push, amend, rewrite history, destructively reset, delete valid commits, tag, release, deploy production, or change signing without explicit current owner instruction.
- Verify the live repository identity, official branch, remote HEAD, current branch/worktree, current or conflicting PRs, relevant CI, and applicable instructions before repository work.
- If an expected SHA or PR state differs from live state, stop mutation and investigate the mismatch first.
- Do not log credentials, tokens, authorization headers, activation codes, signing material, or secret-bearing URLs.
- Every asynchronous result must validate its authoritative owner at commit time; cancellation alone is not sufficient.
- Android work must remain performance-first, adaptive by available window and input mode, RTL-safe, accessible, and usable with TV D-pad input. Images and background work must never block navigation.
- Do not change package/application identity, app name, version policy, signing, endpoints, branding, colors, logo, or ABI policy unless the value is explicitly approved.
- Validation statuses must be reported honestly as `PASS`, `FAIL`, `BLOCKED`, `SKIPPED`, or `NOT RUN`; `BLOCKED` and `NOT RUN` are never `PASS`.
- Important engineering conclusions require attributable evidence tied to the exact tested source.
- Production source remains authoritative over historical chats, old PRs, screenshots, archived SHAs, or AI memory.

Android foundation decisions and provisional values are documented in `apps/android/README.md`. Existing backend documentation under `docs/` applies to the backend only and must not be used to infer Android client architecture where it conflicts with approved Android scope.
