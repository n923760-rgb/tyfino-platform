# TYFINO Threat Model

Status: ENGINEERING SECURITY BASELINE — REVIEW WITH IMPLEMENTATION CHANGES  
Last reviewed: 2026-09-27  
Method: STRIDE-oriented analysis with explicit TYFINO trust boundaries

## 1. System context

```text
[Android TYFINO App] ── HTTPS ──> [TYFINO Licensing API] ──> [PostgreSQL]
        │                               ▲
        │                               │ HTTPS
        └── direct provider traffic ──> [Third-party IPTV Provider]

[Authorized Admin Browser] ── HTTPS ──> [Admin UI / Licensing API]
```

Critical product boundary:

`APPLICATION LICENSE != IPTV SUBSCRIPTION`

The Android app talks directly to IPTV providers. TYFINO licensing must not receive provider credentials, catalogs, streams, or viewing history.

## 2. Trust boundaries

1. Android app ↔ TYFINO licensing API.
2. Android app ↔ third-party IPTV provider.
3. Admin browser ↔ Admin UI/API.
4. Licensing API ↔ PostgreSQL.
5. CI/release environment ↔ signing/release artifacts.
6. Production operators/secrets ↔ deployed services.

## 3. Sensitive assets

| Asset | Sensitivity | Expected location |
| --- | --- | --- |
| IPTV Host / Username / Password | High | Android device only |
| Activation Code plaintext | High | transient creation/redemption only |
| Activation Code HMAC digest | High | PostgreSQL |
| Licensing session token | High | server + protected Android storage |
| Admin password/TOTP secret/session | Critical | approved secret/auth stores |
| Installation ID | Medium | Android + licensing DB |
| Optional customer metadata | Medium/High | Admin/licensing DB |
| Audit chain | High integrity | PostgreSQL + future external anchor |
| Android signing key | Critical | owner-controlled signing custody; production qualification pending |

## 4. Non-negotiable privacy/security boundaries

- Do not transmit IPTV Host/Username/Password to TYFINO services.
- Do not store IPTV catalogs, streams, or viewing history server-side.
- Do not log IPTV stream URLs or provider credentials.
- Do not use Advertising ID for licensing identity.
- Do not use MAC address as the user-facing activation credential.
- Do not bypass TLS validation or hostname verification.
- Do not place production secrets, TOTP seeds, signing keys, or full Activation Codes in source, reports, screenshots, or normal logs.

## 5. STRIDE analysis

### Spoofing

#### S1 — Fake licensing API / man-in-the-middle

Risk:
A compromised network could attempt to impersonate the licensing API.

Existing controls:

- production licensing uses HTTPS;
- Android does not allow TLS bypass;
- production licensing origin is fixed in Release builds;
- hostname/certificate validation must remain enabled.

Residual:

- certificate pinning is currently **DEFERRED** by `docs/security.md`, not an approved requirement.
- production DNS/certificate/operations qualification remains required.

#### S2 — Stolen licensing session

Risk:
A stolen token could be replayed.

Existing/required controls:

- opaque tokens;
- no token logging;
- revocation support;
- one-installation binding;
- bounded refresh/offline behavior;
- secure local storage.

Further token rotation policy remains OPEN where not fixed by the API contract.

#### S3 — Admin impersonation

Existing controls include:

- password authentication;
- OWNER TOTP challenge;
- bounded challenge attempts/lifetime;
- server-side authorization;
- session revocation;
- audit logging.

Production secret provisioning remains a release gate.

### Tampering

#### T1 — Modified Android APK

Risk:
An attacker modifies the application to bypass client behavior.

Controls / limits:

- production signing and release provenance are required;
- server-side licensing authority must not trust client-only assertions;
- trial/expiry decisions use server time.

Production signing custody/recovery is not yet qualified.

Play Integrity is currently **DEFERRED** until an approved measured abuse case justifies it.

#### T2 — Database tampering

Existing controls:

- restricted API DB role;
- runtime privilege checks;
- audit mutation protections;
- audit SHA-256 chain;
- CI tests for privilege boundaries.

Residual:

A privileged schema owner could still alter database state or recompute an in-database chain. External integrity anchoring and production owner-access controls remain operations work.

#### T3 — Request manipulation

Controls:

- schema validation;
- security-sensitive unknown-field rejection;
- bounded request size;
- server-side authorization;
- typed/stable error contracts;
- SQL parameterization must remain enforced by implementation.

### Repudiation

#### R1 — Administrator denies an action

Controls:

- redacted administrative audit records;
- actor/time/action/target evidence;
- append-only restrictions for runtime role;
- audit integrity verification.

Residual:

External integrity anchoring remains unqualified.

#### R2 — Customer disputes activation/reset

Controls:

- server-authoritative licensing records;
- activation/reset timestamps;
- audited reset actions;
- no plaintext code in routine logs.

Commercial dispute/refund handling remains a legal/operations decision.

### Information Disclosure

#### I1 — IPTV credential leakage

Highest-priority invariant.

Controls:

- credentials stored only on Android;
- Keystore-backed encrypted account portfolio;
- backup exclusion;
- direct Android-to-provider network path;
- licensing API rejects provider fields;
- logs must not include secrets.

#### I2 — Activation Code or token leakage

Controls:

- Activation Code returned plaintext once at creation;
- server stores HMAC digest + non-secret suffix;
- UI state clears code after activation;
- logging redaction;
- bearer/session secrets excluded from normal diagnostics.

#### I3 — Admin metadata exposure to Android

Control:
The API boundary forbids returning optional administrative customer metadata to Android.

#### I4 — Sensitive logs

Controls:
Current API logging is bounded and redacts known secret-bearing fields.

Production log destination, access, retention, and external monitoring remain release work.

### Denial of Service

#### D1 — Licensing endpoint flooding / guessing

Existing controls:

- route-specific rate-limit configuration;
- stable HTTP 429 / Retry-After behavior;
- bounded request sizes/timeouts.

Production rate-limit counts remain OPEN until supported by traffic/abuse evidence. Do not hard-code the recommendations document's proposed 60/min or 300/min as approved production values without evidence.

#### D2 — Database exhaustion

Controls:

- restricted role;
- statement timeout;
- bounded API behavior;
- readiness checks.

Residual:

Production capacity, alert thresholds, connection-pool limits, and load evidence remain to be qualified.

#### D3 — Provider slowness affects Android

The provider network path is independent from TYFINO licensing.

Android must enforce bounded provider timeouts, response sizes, and retries so a slow provider does not freeze navigation.

### Elevation of Privilege

#### E1 — IDOR / unauthorized Admin object action

Required controls:

- authenticated Admin session;
- server-side role/authorization checks on every administrative route;
- validated stable object identifiers;
- tests for unauthorized cross-role actions.

Hiding a button is not authorization.

#### E2 — SQL injection

Required controls:

- parameterized database operations;
- validated input;
- least-privilege DB role;
- integration tests for security boundaries.

#### E3 — OWNER-only action used by lower role

Current server contract must enforce OWNER role for OWNER-only operations such as revoking other admin sessions.

## 6. Abuse / trial replay

Risk:
Repeated installations may attempt to obtain repeated trials.

Current boundary:

- trial is server-authoritative;
- installation identity is opaque/random;
- exact anti-abuse signals remain OPEN;
- hardware attestation / Play Integrity are DEFERRED pending an approved measured need.

Do not silently introduce invasive device identifiers.

## 7. Security decisions that remain OPEN or DEFERRED

### OPEN / production evidence needed

- exact production rate-limit counts;
- final retention schedule;
- session/token rotation details where not fixed;
- external audit integrity anchoring;
- production secret delivery/storage;
- production backup encryption/location;
- operational monitoring and alert thresholds;
- signing-key custody/recovery evidence.

### DEFERRED under current contracts

- Certificate pinning;
- Play Integrity / hardware attestation;
- analytics/crash SDK introduction;
- custom provider trust exceptions.

Changing a DEFERRED item into a mandatory architecture requirement requires an explicit approved contract change.

## 8. Required security verification

Before production release, obtain attributable evidence for:

- licensing API rejects IPTV/provider fields;
- no provider credentials are logged or persisted server-side;
- Activation Codes/tokens are redacted;
- Admin authorization is server-side;
- OWNER TOTP challenge behavior;
- rate limiting and anti-enumeration;
- DB runtime least privilege;
- audit-chain verification;
- backup/restore protections;
- Android backup exclusion;
- exact release signing identity;
- production TLS/DNS;
- secrets management.

## 9. Threat-model maintenance

Update this document when a change:

- adds a new trust boundary;
- changes data ownership;
- adds a third-party SDK;
- introduces a new authentication method;
- changes licensing;
- changes production infrastructure;
- changes signing/release;
- adds analytics/telemetry;
- introduces a new IPTV protocol;
- makes an OPEN/DEFERRED security mechanism mandatory.

Security claims require exact implementation/runtime evidence. Documentation alone is not PASS.
