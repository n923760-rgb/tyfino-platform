# TYFINO Contribution and Review Conventions

Status: REPOSITORY GUIDANCE — subordinate to `AGENTS.md` and approved contracts
Planning baseline: `main@cd83a1c23819ae972185c748e7487156890a7dbf` (2026-09-27)

## Scope and source identity

Make one confirmed problem or coherent governance change per branch and PR. Verify live `main`, open/conflicting PRs, the nearest `AGENTS.md`, `governance/PROJECT_PROFILE.md`, the canonical roadmap, and the relevant scoped contract before mutation. Do not push ordinary changes directly to `main`. The checked-in [PR template](../.github/pull_request_template.md) and `governance/` task/result protocol supply the detailed review checklist; this document summarizes naming and evidence conventions, not a competing approval process.

## Names and descriptions

Prefer descriptive branches such as `feat/android-resume`, `fix/api-trial-race`, `docs/api-versioning`, or `chore/ci-artifact-metadata`. Prefer commit subjects in the form `type(scope): concise outcome` (`feat`, `fix`, `docs`, `test`, `chore`), but do not rewrite history solely to conform to a subject pattern. Preserve the actual source identity and reviewable diff.

A PR description should state the problem, scope, source base/head, implementation, validation with exact SHA and result, limitations, runtime qualification, and protected actions. Reconcile any rerun failure and link its evidence; a later PASS does not erase the first result. Use `PASS`, `FAIL`, `BLOCKED`, `UNKNOWN`, `NOT RUN`, or `SKIPPED` truthfully. The repository's more specific evidence protocols may use a narrower vocabulary for a particular manual run.

## Code and tests

- Choose the smallest deterministic test for the changed contract, then relevant CI regression. Android source/build and emulator results do not prove physical TV, real provider/media, accessibility or performance.
- Changes to async Android state must revalidate the current account/destination/generation owner at commit time; cancellation alone is insufficient. Protect D-pad focus, RTL, low-memory navigation and media lifecycle.
- API/Admin changes preserve server-side authorization, data ownership, error semantics and redacted logging. Unknown security-sensitive fields are rejected according to the API contract.
- Database and operations changes require a recovery/rollback plan proportionate to their impact. No production migration, signing or deployment is authorized by a passing CI job.
- Tests should assert externally meaningful behavior or security boundaries. No blanket 70% API / 60% Android coverage gate is approved; establish useful targets from the measured suite and risk before adding CI thresholds.

## Security review

Inspect diffs for credentials, Activation Codes, tokens, signing material, private keys, provider URLs, and customer data. Never log or commit them. Keep licensing separate from IPTV provider traffic; retain HTTPS validation and strict scoped HTTP consent for user providers. Verify secret-bearing fields are excluded from diagnostics and backup as required by current contracts. Review dependency and CI additions for maintenance cost and permissions before enabling new gates.

Merge, release, tag, signing, store publication, production deployment, DNS, destructive migration, credential rotation, force-push and other protected actions listed in `AGENTS.md` require explicit current owner authorization for the exact action.
