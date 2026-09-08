# TYFINO Security & Privacy Baseline

Status: PROPOSED — awaiting approval  
Last reviewed: 2026-09-08

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

Host, Username, and Password are sensitive. The Android credential-storage mechanism is **OPEN** pending a platform-supported design and recovery requirements.

Until approved:

- no plaintext persistence is authorized;
- no credential backup or cloud sync is authorized;
- no custom encryption wrapper is authorized;
- no support export may contain credentials;
- no crash or analytics SDK may receive credential-bearing context.

Whether user-supplied cleartext HTTP provider Hosts are supported is **OPEN**. Global cleartext enablement is not authorized.

## Licensing security

The following are **DECIDED**:

- Trial authority is server-side and must not rely only on the Android clock.
- Activation Codes must be handled as sensitive values and redacted in ordinary diagnostics.
- Application licensing state must remain independent from IPTV subscription state.
- The backend must not infer, validate, store, or administer IPTV subscription credentials.

Exact activation-code hashing, device identity, rate limits, session design, and anti-trial-abuse controls remain **OPEN** until the licensing contract is approved.

## Administrative data

Administrative-only customer fields require purpose limitation, access control, auditability, and an approved retention policy. Retention and deletion periods are **OPEN**.

The dashboard must not add IPTV subscription administration merely because legacy tables or routes exist.

## Network and logs

The following are **DECIDED**:

- Production TYFINO APIs use HTTPS.
- Network retries and recovery are finite, explicit, and testable.
- Sensitive query parameters and paths are redacted before logging.
- Cancellation is propagated and is not logged as an application failure.
- Security failures must not be hidden behind generic success or retry loops.

Certificate pinning, custom trust stores, provider exceptions, analytics SDKs, and crash SDKs are **DEFERRED**.

## Known legacy implementation mismatch

The repository currently contains encrypted IPTV credential columns and provider-account administration from an older scope. Encryption does not make this compliant with the new data boundary.

A separate read-only security/data-flow audit must identify routes, database fields, admin screens, migrations, and deletion risks before any remediation. No production deployment should rely on the legacy provider-account path.
