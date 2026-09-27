# TYFINO API Versioning and Compatibility

Status: PROPOSED CHANGE PROCESS — current `/v1` contract remains authoritative
Planning baseline: `main@cd83a1c23819ae972185c748e7487156890a7dbf` (2026-09-27)

## Current surface

The licensing and Admin routes are registered under `/v1/` in `apps/api/src/routes/`; `/healthz` and `/readyz` are unversioned service probes. [`docs/api.md`](../api.md) defines the approved V1 licensing behavior, privacy boundary and error codes. The repository has no `apps/api/openapi.yaml` or API changelog at this baseline. This document neither creates those artifacts nor declares an unimplemented `/v2` route.

The Android Release build uses the approved licensing origin and the `/v1/licensing/*` contract. Admin web calls the existing `/v1/admin/*` routes. A version-path change must coordinate both clients, authorization, database migrations and a rollback plan; a route prefix alone does not provide compatibility.

## Change classification

| Change | Default treatment | Required evidence |
| --- | --- | --- |
| Add an optional response field without changing meaning | Candidate compatible V1 change | Old Android/Admin client ignores it; tests for both shapes |
| Add a new route without changing existing routes | Candidate compatible V1 change | Auth/privacy review, route tests and documentation |
| Add an optional request field with identical old behavior | Candidate compatible V1 change | Old payload tests and strict validation/security review |
| Remove/rename a field, change its type or meaning, or make an optional field required | Breaking; propose new version or explicit migration | Contract, client compatibility, transition and rollback tests |
| Change trial/activation/expiry/device binding, auth scope, error code/status or idempotency semantics | Breaking/security-sensitive regardless of JSON shape | Owner-approved scoped contract and integration tests |
| Change what IPTV/provider data can cross TYFINO boundaries | Prohibited without an explicit approved privacy/security architecture change | Data-ownership review before implementation |

Do not assume a response is compatible merely because a field is added: clients may reject unknown fields, and request validators may intentionally reject unknown security-sensitive inputs. Test real shipped client behavior. Do not change the minimum supported Android client version by a server switch until version identification, update paths and fail-safe handling are designed and qualified.

## Version transition process

1. Document the exact behavior and affected Android/Admin clients in a scoped contract/ADR; review `docs/api.md`, licensing trial, data ownership and security.
2. For a breaking change, propose `/v2/...` alongside `/v1/...` only when an actual migration is approved. Define support duration from installed-client/update evidence and operations capacity. The supplied 90-day notice and two-version overlap are **proposals**, not approved promises.
3. Prove old V1 clients and new clients against the candidate server, including expiry, revocation, offline behavior, 429/`Retry-After`, malformed/unknown fields, and authorization. Do not expose provider credentials or admin-only metadata through any version.
4. Stage rollout with observable error/usage signals that do not identify viewing behavior. Preserve a compatible rollback path and verify schema changes before turning off old routes.
5. Publish a sunset date only after owner approval and a customer communication path. Removal is a separate protected production change with exact deployment authorization.

## Documentation and contract artifacts

`docs/api.md` remains the current human-reviewed contract. If OpenAPI is introduced later, generate or validate it against the registered Fastify routes and keep it alongside tested source; do not commit a speculative `openapi.yaml` labeled as implementation truth. A changelog may record exact behavioral changes and client requirements after they land. Note that `docs/api.md` describes a common envelope while some current Admin/probe responses use different shapes; reconcile that discrepancy in a bounded API contract task before claiming schema completeness or automatically generated conformance.
