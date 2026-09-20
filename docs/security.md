# TYFINO Security & Privacy Baseline

Status: APPROVED BASELINE

Last reviewed: 2026-09-19

## Mandatory boundaries

The following are **DECIDED**:

- Keep production credentials and secrets out of source control.
- Never log IPTV passwords, authentication tokens, Authorization headers, full activation codes, or secret-bearing URLs.
- IPTV credentials must remain between the Android client and IPTV provider and must not be sent to TYFINO services.
- The licensing backend must receive only data required for application licensing and device enforcement.
- Administrative Customer Name, Phone Number, External Reference, Admin Label, and Internal Note must not be exposed to Android without an explicit future requirement.
- Do not invent custom cryptography.
- Do not bypass TLS validation, hostname validation, or certificate errors.
- Logout and account removal must not expose one account's state to another account.
- Every administrative authorization check must be enforced server-side; hiding UI is insufficient.
- Audit records must not contain plaintext credentials or secrets.

## Android credential handling

Host, Username, and Password are sensitive. The approved implementation uses a versioned AES-GCM portfolio protected by Android Keystore and excluded from backup.

- no plaintext persistence is authorized;
- no credential backup or cloud sync is authorized;
- no support export may contain credentials;
- no crash or analytics SDK may receive credential-bearing context;
- a user-entered HTTP provider requires explicit interception-risk confirmation;
- automatic HTTPS downgrade, global unscoped cleartext enablement, custom trust stores, and TLS bypass are forbidden.

## Licensing security

The following are **DECIDED**:

- Trial authority is server-side and must not rely only on the Android clock.
- Activation Codes must be handled as sensitive values and redacted in ordinary diagnostics.
- Application licensing state must remain independent from IPTV subscription state.
- The backend must not infer, validate, store, or administer IPTV subscription credentials.

Activation-code HMAC storage, opaque random installation identity, one active installation per code, audited device reset, 12-hour refresh, and maximum 72-hour offline use are approved by `docs/api.md` and `docs/licensing-trial-contract-v1.md`. Exact production rate thresholds, retention, token rotation, and additional anti-abuse signals remain **OPEN**.

## Administrative data

Administrative-only customer fields require purpose limitation, access control, auditability, and an approved retention policy. Retention and deletion periods are **OPEN**.

The dashboard must not add IPTV subscription administration merely because legacy tables or routes exist.

The OWNER can revoke all other active administrative sessions without invalidating the current recovery session. The server enforces the OWNER role, identifies the preserved session by its hashed opaque token, performs revocation and audit insertion in one transaction, and returns only the number of sessions revoked.

Fresh PostgreSQL initialization separates the schema-owner credential from the API credential. `tyfino_app`-style roles are non-superuser, cannot create roles or databases, cannot alter tables or triggers, receive only the table/column operations used by the licensing service, and have `SELECT` plus business-field `INSERT` access to `audit_logs` without `UPDATE` or `DELETE`. CI runs the full API integration suite through this restricted role and separately proves that audit mutation and trigger disabling are denied. The schema-owner credential must not be supplied to the API container.

## Network and logs

The following are **DECIDED**:

- Production TYFINO APIs use HTTPS.
- Network retries and recovery are finite, explicit, and testable.
- Sensitive query parameters and paths are redacted before logging.
- Cancellation is propagated and is not logged as an application failure.
- Security failures must not be hidden behind generic success or retry loops.

The API disables Fastify's raw request logging and emits one bounded completion event containing only the HTTP method, matched route template, status, duration, and request ID. Query strings, concrete path identifiers, headers, cookies, and bodies are not included. Pino redaction additionally censors authorization/cookie headers and known password, activation-code, challenge, session-token, TOTP, pepper, and database-URL fields if future structured logs include them. Error logs retain only the error type and non-secret machine code; error messages are not emitted.

Certificate pinning, custom trust stores, provider exceptions, analytics SDKs, and crash SDKs are **DEFERRED**.

## Current implementation evidence and deployment limit

The checked-in fresh-install schema and registered API routes contain licensing concepts only. Automated integration tests reject provider fields and prove the former player-configuration route is absent.

This repository evidence is not proof about an existing deployed database or server. Production remains blocked until deployment state, secrets, backups, restore, rollback, logs, and any pre-existing data are inspected under `docs/deployment.md`.
