# TYFINO repository instructions

Read the nearest `AGENTS.md` and the authoritative documentation for the area being changed before editing.

- Keep work atomic: one confirmed problem or scoped feature per branch and pull request.
- Audit, review, and diagnosis are read-only unless implementation is explicitly requested.
- Do not make speculative fixes or unrelated refactors.
- Never merge, force-push, amend, rewrite history, destructively reset, delete valid commits, tag, release, or change signing without explicit user instruction.
- Verify the live official branch, remote HEAD, current branch and PR, related PRs, status, diff, and applicable instructions before repository work.
- Do not log credentials, tokens, authorization headers, activation codes, or secret-bearing URLs.
- Every asynchronous result must validate its authoritative owner at commit time; cancellation alone is not sufficient.
- Android work must remain performance-first, adaptive by available window and input mode, RTL-safe, accessible, and usable with TV D-pad input. Images and background work must never block navigation.
- Do not change package/application identity, app name, version policy, signing, endpoints, branding, colors, logo, or ABI policy unless the value is explicitly approved.
- Qualification statuses are exactly `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED`; `BLOCKED` is not `PASS`.

Android foundation decisions and provisional values are documented in `apps/android/README.md`. Existing backend documentation under `docs/` applies to the backend only and must not be used to infer Android client architecture where it conflicts with approved Android scope.
