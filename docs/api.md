# TYFINO Licensing API Contract v1

Status: DECIDED — approved 2026-09-08  
Last reviewed: 2026-09-08  
Scope: application trial, paid activation, entitlement refresh, and licensing administration

## 1. Authority and boundary

This document replaces the legacy provider-account API description. It defines the smallest V1 API for TYFINO application licensing.

The approved product rule remains:

`APPLICATION LICENSE != IPTV SUBSCRIPTION`

The licensing API must never accept, validate, store, proxy, or return:

- IPTV Host, Username, or Password;
- M3U URLs or content;
- Stalker/MAC Portal data;
- provider-account, package, catalog, EPG, stream, or playback data;
- IPTV subscription status or expiry.

The Android app communicates directly with the user's IPTV provider. Licensing authority belongs only to the TYFINO licensing service.

Approval of this contract does not authorize production deployment, DNS, database deletion, signing, release, or Android integration.

## 2. Common transport rules

All production requests use HTTPS and JSON.

Every response includes:

- `serverTime`: authoritative UTC timestamp;
- `requestId`: opaque diagnostic identifier safe to show to support.

The service must:

- reject unknown security-sensitive fields rather than silently persisting them;
- apply bounded request sizes, timeouts, and rate limits;
- redact Authorization headers, tokens, complete Activation Codes, and sensitive installation evidence;
- never place secrets in URLs or query strings;
- return explicit non-success HTTP status codes for failure;
- avoid revealing whether a guessed Activation Code exists.

Rate-limit windows are fixed by route class, while their request counts are environment configuration: `GLOBAL_RATE_LIMIT_PER_MINUTE`, `ADMIN_AUTH_RATE_LIMIT_PER_15_MINUTES`, `TRIAL_START_RATE_LIMIT_PER_HOUR`, `ACTIVATION_RATE_LIMIT_PER_15_MINUTES`, and `ENTITLEMENT_REFRESH_RATE_LIMIT_PER_HOUR`. Development/test use conservative checked defaults. Production refuses to start until every count is explicitly set to an integer from 1 through 10,000; the approved values must come from traffic and abuse evidence rather than source-code assumptions.

`HTTP_REQUEST_TIMEOUT_MS` bounds receipt of the complete inbound HTTP request and `DATABASE_STATEMENT_TIMEOUT_MS` asks PostgreSQL to cancel overlong statements. Development/test defaults are 15 seconds and 10 seconds respectively. Production refuses to start until both are explicitly configured between 1 and 120 seconds; approved values must be supported by production latency evidence.

## 3. Android licensing endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/v1/licensing/trials/start` | Start the seven-day trial after explicit user action |
| POST | `/v1/licensing/activations` | Redeem an Activation Code for the current installation |
| POST | `/v1/licensing/entitlements/refresh` | Refresh current server-authoritative entitlement |
| POST | `/v1/licensing/sessions/revoke` | Revoke the current installation session during local reset/logout |

There is no player configuration endpoint. In particular, the legacy `/v1/player/config` response is forbidden because it returns provider connection data.

### 3.1 Installation envelope

Each Android request contains only the minimum licensing envelope:

```json
{
  "installationId": "opaque-random-installation-identifier",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

The following rules are **DECIDED**:

- `installationId` is generated randomly by the app and is not a MAC address, advertising ID, serial number, IMEI, Android ID, IPTV username, or hardware fingerprint.
- It is stored with Android platform-supported protected storage and is excluded from backups.
- Reinstallation or app-data deletion may create a new installation identity.
- Raw identity values are not shown in routine Admin UI or ordinary logs.
- Hardware attestation and Play Integrity are deferred until a measured abuse case justifies them.



### 3.2 Start trial

`POST /v1/licensing/trials/start`

The endpoint is called only after the user explicitly selects **Start 7-Day Free Trial**.

Request:

```json
{
  "installationId": "opaque-random-installation-identifier",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

Success returns an entitlement envelope. The service commits the start exactly once for the eligible installation. A failed, timed-out, or uncommitted request does not consume trial time.

The following lifecycle rule is **DECIDED**: seven days means exactly 168 hours from the server-recorded `startsAt`.

### 3.3 Activate

`POST /v1/licensing/activations`

Request:

```json
{
  "activationCode": "TYF-XXXXX-XXXXX-XXXXX-XXXXX-XXXXXX",
  "installationId": "opaque-random-installation-identifier",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

The complete code is used only for this request, must not be logged, and must be discarded from Android UI state after the transaction finishes.

Activation must:

- work without starting a trial;
- enforce one active installation per code;
- remain independent of every IPTV account;
- be idempotent for the same valid code and installation;
- reject a different installation while the current binding remains active;
- revoke older sessions after an authorized Admin device reset.

The following duration rules are **DECIDED**:

- **1 Year** expires exactly 365 days after the server-recorded activation time.
- **Lifetime** has no time-based expiry but may be revoked by an authorized administrator for refund, fraud, abuse, or operational correction.
- An unused code has no automatic pre-activation expiry unless an administrator explicitly sets one when creating it.

### 3.4 Refresh entitlement

`POST /v1/licensing/entitlements/refresh`

Authentication: opaque bearer session token.

Request:

```json
{
  "installationId": "opaque-random-installation-identifier",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

Success:

```json
{
  "serverTime": "2026-09-08T15:00:00Z",
  "requestId": "opaque-request-id",
  "entitlement": {
    "status": "active",
    "kind": "trial",
    "startsAt": "2026-09-08T15:00:00Z",
    "expiresAt": "2026-09-15T15:00:00Z",
    "offlineValidUntil": "2026-09-11T15:00:00Z"
  },
  "session": {
    "token": "opaque-session-token",
    "refreshAfter": "2026-09-09T03:00:00Z"
  }
}
```

Rules:

- `status` is one of `active`, `expired`, or `revoked`.
- `kind` is one of `trial`, `one_year`, or `lifetime`.
- `expiresAt` is null only for Lifetime.
- `offlineValidUntil` never exceeds 72 hours after the latest successful server verification and never exceeds a finite entitlement expiry.
- Expired or revoked authority takes effect immediately when a successful server response reports it.
- Client clock changes and failed requests never extend offline use.
- Administrative metadata is never returned.

The following refresh behavior is **DECIDED**:

- refresh when the app becomes active if the last successful verification is older than 12 hours;
- while the app remains active, do not refresh more often than once every 12 hours unless a user explicitly retries after a failure;
- retry transient failures with bounded exponential backoff and jitter;
- never run an infinite retry loop or block first usable UI.

### 3.5 Revoke current session

`POST /v1/licensing/sessions/revoke`

Authentication: opaque bearer session token.

This endpoint revokes only the current session. It does not reset the paid device binding, extend a trial, or delete Admin audit history.

## 4. Entitlement ownership and stale results

Every Android licensing operation captures an operation owner consisting of the current installation identity and a monotonically increasing local operation generation.

Before committing a result, Android must verify that:

- the installation identity still matches;
- the operation generation is still current;
- the user has not selected a newer licensing action;
- the response belongs to the request that produced it.

Cancellation is an efficiency mechanism, not the correctness check.

## 5. Admin endpoints

### 5.0 Service probes

- `GET /healthz` is a process liveness probe. It does not touch PostgreSQL and remains successful while a database dependency is unavailable.
- `GET /readyz` is a traffic-readiness probe. It returns `200` only when PostgreSQL is reachable and the required licensing tables, audit-chain verification function, and both enabled audit protection triggers are present. Failure returns only `503 {"status":"not_ready"}` without exposing database details.

The readiness probe intentionally validates schema presence in constant time; full audit-chain verification remains on the authorized Admin audit endpoint so frequent orchestrator probes do not scan an unbounded audit table.

Admin authentication and authorization are enforced server-side on every route.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/v1/admin/auth/login` | Verify password; issue a short-lived challenge for OWNER or a bounded non-OWNER session |
| POST | `/v1/admin/auth/verify-totp` | Consume the OWNER TOTP challenge and start the bounded Admin session |
| POST | `/v1/admin/auth/logout` | Revoke the current Admin session |
| POST | `/v1/admin/auth/revoke-other-sessions` | OWNER only: revoke every other active Admin session while preserving the current session |
| GET | `/v1/admin/me` | Return the current authorized administrator |
| GET/POST | `/v1/admin/activation-codes` | List or create application Activation Codes |
| GET | `/v1/admin/activation-codes/:id` | Inspect redacted code and license state |
| POST | `/v1/admin/activation-codes/:id/revoke` | Revoke the code grant and active sessions |
| POST | `/v1/admin/activation-codes/:id/reset-device` | Audit and reset the active device binding |
| GET/PATCH | `/v1/admin/app-settings` | Read or update approved licensing settings |
| GET | `/v1/admin/audit-logs` | Read redacted administrative activity |

Forbidden Admin routes include provider hosts, provider accounts, IPTV credentials, M3U, Stalker/MAC Portal, packages, channels, streams, reseller operations, and IPTV expiry.

### 5.1 OWNER two-factor authentication

The single V1 bootstrap OWNER must complete RFC 6238 TOTP after a correct password. Password verification creates only an opaque five-minute challenge and no session cookie. The challenge permits at most five code attempts. Successful verification consumes the challenge, records the accepted 30-second counter to prevent replay, issues the 12-hour Admin session, and writes redacted audit events. At most one prior/current/next time step is checked for clock drift; a previously accepted counter is never accepted again.

`ADMIN_OWNER_TOTP_SECRET` is an RFC 4648 Base32 secret of at least 160 bits. Production configuration fails closed when it is absent or malformed. It is provisioned directly into the owner's authenticator and process secret store; it is never returned by an API, stored in PostgreSQL, logged, or committed. Admin and Support accounts remain password-authenticated in V1 and cannot perform OWNER-only operations.

Audit inserts are serialized into an append-only SHA-256 chain inside PostgreSQL. The API recomputes the chain before returning audit history and includes `integrityVerified`; the Admin UI raises an assertive warning when verification fails. The restricted API database role has no audit `UPDATE`, `DELETE`, or schema-alter privilege, while database triggers provide an additional mutation backstop. This detects and blocks application-role mutation, but it does not protect against a privileged schema owner who can disable triggers and recompute the chain; production owner-access controls, monitoring, and an external integrity anchor remain operations work.

The OWNER can revoke all other active Admin sessions from Settings. The current authenticated session is excluded by its token hash, the operation is transactional, and its redacted count is appended to the audit log. Admin and Support roles cannot invoke this operation.

### 5.2 Create Activation Code

Request:

```json
{
  "licenseKind": "one_year",
  "preActivationExpiresAt": null,
  "customerName": "optional admin-only value",
  "phoneNumber": "optional admin-only value",
  "externalReference": "optional admin-only value",
  "adminLabel": "optional admin-only value",
  "internalNote": "optional admin-only value"
}
```

`licenseKind` is `one_year` or `lifetime`.

The plaintext Activation Code is returned exactly once. The format encodes at least 128 bits from a cryptographically secure random generator using human-safe uppercase Base32 groups; normalization may remove separators and fold case but must reject all other transformations. Storage uses a keyed server-side HMAC-SHA-256 digest plus a non-secret suffix for Admin identification. The HMAC key lives outside source control and outside the database.

Administrative-only metadata must never enter Android responses.

### 5.2 Reset device

Resetting a device must be an explicit POST action and must:

- require an authenticated, authorized administrator;
- require a short reason;
- record actor, time, code/grant identifier, and redacted previous-binding reference;
- revoke all sessions for the previous binding;
- clear the current binding atomically;
- not alter paid duration or resurrect an expired/revoked license;
- be idempotent when no binding exists.

No fixed customer reset limit applies in V1; authorized administrators control resets and every reset is audited. Rate limiting still protects the endpoint from automated misuse.

## 6. Error contract

Responses use an error envelope:

```json
{
  "serverTime": "2026-09-08T15:00:00Z",
  "requestId": "opaque-request-id",
  "error": {
    "code": "LICENSE_UNAVAILABLE",
    "message": "localized-safe-message",
    "retryable": true
  }
}
```

Minimum stable codes:

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Malformed or unsupported fields |
| 401 | `SESSION_INVALID` | Missing, invalid, or revoked session |
| 403 | `ENTITLEMENT_EXPIRED` | Finite entitlement expired |
| 403 | `ENTITLEMENT_REVOKED` | Entitlement revoked |
| 409 | `TRIAL_ALREADY_USED` | Installation is not trial-eligible |
| 409 | `DEVICE_LIMIT_REACHED` | Code is bound to another active installation |
| 422 | `ACTIVATION_REJECTED` | Code cannot be accepted without revealing why |
| 429 | `RATE_LIMITED` | Too many attempts; honor `Retry-After` |
| 503 | `LICENSE_UNAVAILABLE` | Temporary licensing outage |

Admin authorization failures must not be mapped to success. Internal exception details, SQL errors, tokens, code hashes, and existence-oracle details must not be returned.

## 7. Persistence boundary

The future licensing schema may contain only concepts needed for:

- trial eligibility and lifecycle;
- Activation Code digest and redacted suffix;
- license grant, duration kind, expiry, and revocation;
- one current installation binding per paid code;
- opaque licensing sessions;
- approved Admin-only metadata;
- redacted audit events;
- approved application settings.

It must not contain or reference provider hosts, provider accounts, IPTV credentials, M3U, Stalker/MAC data, catalog, streams, or playback history.

The legacy schema is not an approved migration base. Destructive removal remains blocked until deployment and live-data state are inspected and a rollback plan exists.

## 8. Required qualification

Before implementation is merged, automated tests must prove:

- a trial starts only after explicit accepted action and starts once;
- activation works without a trial;
- one-device enforcement and audited reset work atomically;
- one-year, Lifetime, expiry, revocation, and 72-hour offline limits behave as approved;
- stale Android results cannot overwrite newer ownership;
- Admin authorization is enforced on every route;
- OWNER password success cannot create a session before a valid TOTP challenge is consumed;
- expired/exhausted challenges, invalid codes, and replayed TOTP counters are rejected;
- ordinary audit-row updates/deletes are rejected and the full hash chain verifies after administrative workflows;
- activation guessing is rate-limited without an existence oracle;
- complete codes, tokens, and sensitive identifiers are absent from ordinary logs;
- unknown IPTV/provider fields are rejected;
- no IPTV/provider value is accepted, persisted, logged, or returned;
- legacy provider routes are absent from the replacement service.

Unexecuted checks are `SKIPPED` or `BLOCKED`, never `PASS`.

## 9. Decision effects

Approval of this contract:

- makes these routes and payload boundaries authoritative for the first backend remediation slice;
- makes the duration, installation identity, refresh, Activation Code storage, and reset details in this document mandatory;
- authorizes a separate implementation PR with tests.

Approval does not authorize:

- deleting or migrating a live database;
- production deployment, DNS, secrets, signing, release, or distribution;
- IPTV feature implementation;
- merging any future implementation without explicit instruction.
