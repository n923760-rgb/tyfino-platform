# API, ADR, Conventions and UX Documentation Round — 2026-09-27

Status: DRAFT — exact PR-head CI pending
Repository: `n923760-rgb/tyfino-platform`
Starting official `main`: `cd83a1c23819ae972185c748e7487156890a7dbf`
Task branch: `docs/api-adr-conventions-ux-states`

## Scope and reason

The owner-supplied phase proposes API versioning, ADRs, conventions and UI state guidance. This round documents a change process and current user-facing states without implementing `/v2`, an OpenAPI specification, a 90-day sunset promise, fixed coverage quotas, skeleton loaders, a global offline banner, or runtime changes.

## Source reconciliation

| Finding | Evidence | Classification |
| --- | --- | --- |
| Licensing/Admin routes are registered under `/v1/`; liveness/readiness probes are unversioned | `apps/api/src/routes/`, `apps/api/src/app.ts` | FACT, source |
| Human-approved V1 API contract exists but no checked-in OpenAPI/changelog | `docs/api.md`, repository tree | FACT, source |
| PR template and task/result governance already prescribe source identity, evidence and protected actions | `.github/pull_request_template.md`, `AGENTS.md`, `governance/` | FACT, source |
| Android has localized loading/error/empty states, indicators, placeholders and focus behavior | resource strings and screen implementations | FACT, source |
| Physical TV focus/accessibility, global offline banner, skeleton migration and exact deprecation policy | no qualifying evidence/approved contract at baseline | BLOCKED or PROPOSED |

## Change

- `docs/architecture/API_VERSIONING.md`: V1 compatibility and future breaking-change process.
- `docs/architecture/adr/README.md`: narrow ADR format and authority.
- `docs/CONVENTIONS.md`: contribution and review guidance aligned with existing template.
- `docs/ux/STATES.md`: current state map and separately marked proposals.
- Advance the canonical roadmap for this round.

## Validation

- Local Markdown/links, changed-file, `git diff --check` and secret-pattern review: PASS on the task branch before PR creation.
- Exact PR-head CI: PENDING.
- Runtime/client compatibility, physical UX, and production rollout: NOT RUN.

Merge remains a separately protected owner decision under `AGENTS.md`.
