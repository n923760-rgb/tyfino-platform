# TYFINO Architecture Decision Records

Status: PROCESS GUIDANCE — existing approved contracts remain authoritative
Planning baseline: `main@cd83a1c23819ae972185c748e7487156890a7dbf` (2026-09-27)

Use an ADR for a durable technical choice that crosses components or changes an approved boundary: licensing semantics, state ownership, API compatibility, signing, data retention, deployment topology, or a significant Android architecture choice. A small implementation detail or a bug fix with an existing contract does not need a new ADR.

## Authority

An ADR records context and rationale. It does **not** authorize a protected action or supersede `AGENTS.md`, `docs/architecture.md`, `docs/data-ownership.md`, `docs/security.md`, `docs/api.md`, or a scoped approved contract. If a decision changes one of those documents, update the authoritative contract in the same bounded review and obtain the required owner decision. The repository and current source remain the live implementation truth.

## Format and lifecycle

- Path: `docs/architecture/adr/NNNN-short-title.md`, with the next unused four-digit number. Do not renumber historical records.
- Status: `PROPOSED`, `ACCEPTED`, `SUPERSEDED`, or `REJECTED`. A proposed record is not an implementation mandate.
- Include date, decision owner, related issue/PR, affected contracts and exact source baseline when the decision depends on current implementation.
- Explain context, decision, alternatives considered, consequences, migration/rollback, validation, and open questions. Link a superseding ADR in both directions.
- Review security, privacy, operations, accessibility and performance effects where applicable. Never include secrets or provider data in examples.

Starter structure:

```markdown
# ADR NNNN — Decision title

Status: PROPOSED
Date: YYYY-MM-DD
Decision owner: [role]
Source baseline: [SHA]
Related contracts and PR: [links]

## Context
## Decision
## Alternatives considered
## Consequences and risks
## Migration and rollback
## Validation and evidence
## Open questions
```

The index is the set of records in this directory. Do not backfill speculative "accepted" ADRs for decisions already maintained in approved contracts solely to populate the folder.
