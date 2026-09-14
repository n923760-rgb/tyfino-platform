# Xtream Authentication & Account Ownership Contract v1

Status: DECIDED — approved for Android V1  
Scope: TYFINO Android client  
Version: 1.0  
Last reviewed: 2026-09-09

## 1. Purpose and authority

This contract defines the behavioral boundaries that must exist before TYFINO implements Xtream authentication or persisted IPTV accounts on Android.

Only rules marked **DECIDED** are mandatory. **PROPOSED** items require approval, **OPEN** items must not be silently resolved, and **DEFERRED** items must not influence the authentication implementation prematurely.

Within Android IPTV authentication and account ownership, this contract overrides older repository text that says:

- the TYFINO backend assigns or returns IPTV provider credentials;
- administrators manage IPTV provider accounts for the Android client;
- Android V1 supports M3U or Stalker/MAC authentication;
- Flutter is the Android client technology.

The general repository documents are aligned in the same documentation task. The existing backend and database implementation remain a known legacy mismatch; this contract does not modify or migrate them.

## 2. Product boundary

The following are **DECIDED**:

- TYFINO is an IPTV player/client, not an IPTV provider, reseller panel, stream proxy, or content host.
- Android V1 supports Xtream Codes only.
- The user supplies Host, Username, and Password.
- The Android app communicates directly with the IPTV provider.
- The TYFINO backend provides application licensing/control only and must not proxy IPTV traffic.
- `APPLICATION LICENSE != IPTV SUBSCRIPTION`.
- IPTV credentials must never be sent to the TYFINO licensing backend.
- Activation state must not be used as the ownership identity for IPTV account data.

## 3. Terminology

### IPTV account

A saved Xtream connection owned locally by the Android client. It contains a stable internal account identity and may reference sensitive credentials.

### Account ID

An opaque, stable, app-generated internal identifier. It must not be derived solely from username, host, activation code, device identifier, or a mutable display label.

### Active account

The single IPTV account whose data may currently drive account-scoped UI and work.

### Account generation

A monotonically changing ownership version associated with the authoritative active-account context. Android V1 represents it as a `Long` inside the immutable owner token defined in section 15.

### Operation identity

An identity or version that distinguishes a currently authoritative operation from an older operation for the same account and generation. Android V1 represents it as a monotonically increasing operation ID defined in section 15.

## 4. Account model

The following are **DECIDED**:

- The architecture must support isolated saved-account identities even if the first UI exposes only one account.
- At most one IPTV account is authoritative and active at a time.
- Username alone must never be used as an ownership key.
- Host plus username must not be assumed globally unique without an approved canonicalization and duplicate policy.
- Account-scoped data must be partitioned by stable Account ID.
- Switching accounts must change the authoritative ownership context before new account work can commit.
- Removing an account must invalidate its active and pending ownership contexts before account data is removed.

Account-scoped data includes, when implemented:

- credentials and provider metadata;
- catalog and category metadata;
- favorites;
- history and recently watched state;
- resume and completion state;
- EPG cache;
- search state and results;
- pending catalog, EPG, detail, and playback-related work;
- last and previous live channel state.

This original Android V1 boundary exposed exactly one saved active IPTV account.
The subsequently approved
`multiple-xtream-accounts-contract-v1.md` supersedes that portfolio limit and now
allows up to eight saved accounts with exactly one or zero active selections. Its
stable Account ID, isolation, explicit switching, and no-automatic-fallback rules
are mandatory; all ownership requirements in this contract continue to apply.

## 5. Authentication input

The following are **DECIDED**:

- Authentication input consists only of Host, Username, and Password.
- Inputs are collected by UI and submitted to an authoritative non-Composition state owner.
- Composables must not perform validation IO, network requests, persistence, or navigation during Composition.
- Empty required values must be rejected before network work.
- Passwords must not be trimmed, normalized, reformatted, or silently modified.
- User-visible errors must not echo the password or a secret-bearing full URL.
- Authentication begins only from an explicit user action; it must not run during application startup merely because the screen is displayed.

The approved V1 host canonicalization and duplicate rules are defined in section 15.

## 6. Authentication result taxonomy

The result taxonomy and the rule against collapsing failures are **DECIDED**. Authentication failures must remain distinguishable and must not all become a generic network error.

| Result | Required meaning | Status |
| --- | --- | --- |
| Invalid host | Input cannot identify a permitted provider endpoint before or during connection setup | DECIDED |
| Network unavailable | The platform reports that usable connectivity is unavailable | DECIDED |
| Timeout | The bounded request exceeded its approved deadline | DECIDED |
| Invalid credentials | The provider rejects or does not authenticate the supplied credentials | DECIDED |
| Account expired | The provider response identifies an expired IPTV subscription | DECIDED |
| Account disabled | The provider response identifies a disabled or inactive account | DECIDED |
| Malformed provider response | A response was received but cannot be parsed safely as the expected structure | DECIDED |
| Unsupported provider response | A valid response shape or behavior is outside the compatibility contract | DECIDED |

Additional internal diagnostic detail may exist, but UI mapping must preserve the meaningful category and must redact sensitive values.

The approved V1 response mapping is defined in section 15. Unknown provider variants must return `Unsupported provider response`; they must not be guessed into a successful login.

## 7. Asynchronous ownership invariant

The following rule is **DECIDED** and mandatory across the Android application:

> Any asynchronous completion must prove at commit time that it still belongs to the current authoritative owner. Cancellation alone is insufficient.

An account-scoped result may commit only when all applicable checks succeed:

1. Its captured Account ID still exists.
2. Its Account ID is still the authoritative account for the destination state.
3. Its captured account generation still equals the authoritative generation.
4. Its operation identity is still current when newer operations supersede older ones.
5. The destination owner is still active and permitted to accept the result.

If any check fails, the result must be discarded without mutating authoritative UI, repository, cache, history, resume, EPG, or playback state.

Required behavior:

- Cancellation should be used to stop unnecessary work, but never as the sole correctness mechanism.
- `CancellationException` must not be swallowed or converted into an authentication failure.
- Account switching must invalidate prior generations synchronously from the perspective of state commitment.
- Account removal and logout must invalidate pending work before destructive cleanup begins.
- A newer login attempt must be able to supersede an older attempt for the same candidate account.
- Callbacks and player events introduced later must follow the same commit-time rule.
- Licensing refreshes must validate their own licensing owner/version and must never use or mutate IPTV credential ownership.

Android V1 uses the immutable ownership token defined in section 15. It must remain explicit, inspectable, and race-testable.

## 8. State ownership

The following are **DECIDED**:

- Each mutable state value must have one authoritative owner.
- UI may render state and emit user intents; it must not own network or credential persistence.
- Navigation must occur from handled events/effects, never as a side effect of Composition.
- Repository abstractions may be introduced where they enforce a real data boundary; generic base repositories and one-use-case-per-action patterns are not required.
- Authentication success is not committed until the response is classified and ownership is revalidated.
- Persisted active-account selection must never expose another account's scoped state during restoration.

Concrete class names may follow the existing small Android module, but the ownership and persistence behavior in section 15 is mandatory.

## 9. Credential security and privacy

The following are **DECIDED**:

- Host, Username, and Password are sensitive data.
- Never log the IPTV password, authentication token, Authorization header, complete activation code, or secret-bearing stream/playlist URL.
- Diagnostic URLs must be reduced to a non-sensitive origin or otherwise redacted.
- IPTV credentials must not be included in analytics, crash metadata, audit events, licensing requests, or support exports.
- Custom cryptography is prohibited.
- Credential protection must use an approved Android/platform-supported mechanism after requirements are understood.
- Account removal must prevent credentials and account-scoped state from leaking into another account.
- Production endpoints and secrets must not be embedded in source control.

The approved Android V1 credential storage and recovery behavior is defined in section 15.

## 10. Transport policy

TLS preference and the prohibition on silently weakening transport security are **DECIDED**. Android V1 may support a user-entered cleartext HTTP provider only under the explicit-consent policy in section 15.

This decision is a blocking product/security decision because:

- many legacy Xtream providers may expose HTTP endpoints;
- globally enabling cleartext weakens the application security boundary;
- the Android foundation currently disables cleartext traffic;
- per-domain exceptions cannot be known before the user supplies a Host.

Implementation must never silently upgrade or downgrade schemes. Enabling platform cleartext capability is permitted only for the isolated provider client, while application code must continue rejecting cleartext for licensing and every non-provider destination.

Certificate pinning, custom trust stores, hostname-verification bypasses, and acceptance of invalid certificates are **DEFERRED** and must not be introduced implicitly.

## 11. Bounded network behavior

The following are **DECIDED**:

- Authentication must have explicit finite timeouts.
- Infinite retries, arbitrary delays, hidden fallback loops, uncontrolled polling, and delay-based synchronization are prohibited.
- Automatic retry must not repeat known invalid credentials, expired accounts, disabled accounts, malformed responses, or unsupported responses.
- Network loss and timeout must remain distinguishable where evidence permits.
- Work must run off the main thread without blocking first usable UI.

The Android V1 network profile is defined in section 15 and introduces no new networking or serialization dependency.

## 12. Account switching and removal

The following are **DECIDED**:

- Switching is an authoritative state transition, not only a navigation action.
- The outgoing account must become unable to commit new state before the incoming account is exposed as active.
- UI must not temporarily show Account A data under Account B identity.
- In-memory account-scoped state must be cleared or replaced deterministically.
- Pending work belonging to the outgoing account should be cancelled and must fail commit-time validation.
- Removal must target an exact stable Account ID.
- Removal must not delete or alter another account whose username or host happens to match.

Android V1 logout/removal invalidates ownership synchronously, then deletes the encrypted credential payload and all implemented data for the exact Account ID. There is no undo; re-entry and authentication are required.

## 13. Performance requirements

The following are **DECIDED**:

- No authentication request, credential-store initialization, catalog sync, EPG sync, artwork work, or player initialization may block application startup.
- Successful authentication must not wait for a full catalog or EPG synchronization.
- Authentication must not introduce background polling.
- Dependencies require a concrete present-tense need and size/startup/memory review.
- Large-catalog processing belongs to later progressive catalog work, not the login transaction.

## 14. Required tests before authentication implementation can qualify

The following qualification coverage is **DECIDED** for the authentication implementation.

Qualification must use only `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED`.

Required automated coverage:

- validation of empty and structurally invalid input;
- every defined authentication result category;
- redaction of credentials and secret-bearing URLs;
- Account A completion after switching to Account B is rejected;
- older login completion after a newer login attempt is rejected;
- completion after logout or account removal is rejected;
- cancellation propagates without becoming a user-visible authentication error;
- equal usernames on different accounts do not share ownership;
- account-scoped persistence never reads or writes another Account ID partition;
- success returns usable UI before catalog/EPG work completes;
- retry and timeout behavior is finite and deterministic.

Device/instrumentation coverage for lifecycle recreation, RTL, TV focus, and process restoration is required when the corresponding implementation exists.

## 15. Approved Android V1 implementation profile

The following profile is **DECIDED**:

### Transport and explicit HTTP consent

- `https://` is preferred and requires no transport warning.
- `http://` is accepted only when the user entered that scheme and explicitly confirms a warning that the provider credentials and IPTV traffic are not encrypted in transit.
- Consent is scoped to the exact canonical provider base URL and is stored inside the encrypted IPTV account payload.
- No implicit scheme insertion, HTTP fallback, HTTPS downgrade, certificate-verification bypass, redirect, or provider-specific trust exception is allowed.
- TYFINO licensing remains HTTPS-only. HTTP capability must never permit IPTV credentials to enter licensing, analytics, crash metadata, logs, or support exports.

### Host validation and canonicalization

- Surrounding whitespace is removed from the Host field only. Username and Password are preserved exactly.
- The Host must be an absolute `http://` or `https://` URI with a non-empty DNS name, IPv4 address, or bracketed IPv6 address.
- User-info, query, fragment, control characters, backslashes, and dot-segment paths are rejected.
- Scheme and DNS host are lowercased; an internationalized DNS host is converted with the platform IDN ASCII conversion; default ports are removed.
- A non-root provider base path is permitted, normalized without a trailing slash, and `player_api.php` is appended to that base path by the client.
- Duplicate matching uses canonical provider base URL plus exact case-sensitive Username.

### One-account update policy

- A successful login matching the current canonical base URL and exact Username keeps its stable Account ID, replaces its encrypted credentials, and increments its generation.
- A successful login for a different pair creates a new random Account ID, invalidates the prior ownership context, removes the prior account and implemented account-scoped state, then commits the new account.
- Failed authentication never replaces the currently saved account.

### Credential persistence and recovery

- Host, Username, Password, HTTP consent, and Account ID are stored together in an AES-GCM payload protected by a non-exportable Android Keystore key.
- The IPTV key alias and preferences namespace are separate from TYFINO licensing storage.
- The payload is excluded from Android backup and must never be copied to another installation.
- If the key is invalidated or the payload fails authenticated decryption or parsing, the payload is deleted and the user must sign in again. No plaintext recovery copy is retained.

### Request and response profile

- Authentication calls the canonical base path's `player_api.php` endpoint directly from Android.
- Xtream Username and Password are percent-encoded as request parameters only for that provider request. The complete request URL is treated as secret and never logged, persisted, redirected, or surfaced in errors.
- The platform `HttpURLConnection` is used off the main thread with an 8-second connect timeout, 12-second read timeout, redirects disabled, and a 256 KiB response limit.
- Authentication has no automatic retry. The user may explicitly retry after a network-unavailable or timeout result.
- HTTP `401` or `403`, or `user_info.auth` equal to numeric/string zero, maps to `Invalid credentials`.
- `user_info.auth` must equal numeric/string one for a successful authentication candidate.
- A case-insensitive `user_info.status` of `Active` maps to success; `Expired` maps to `Account expired`; and `Disabled`, `Banned`, or `Inactive` maps to `Account disabled`.
- Invalid JSON, a non-object root, or missing required `user_info`, `auth`, or `status` maps to `Malformed provider response`.
- A syntactically valid authenticated response with an unknown status or incompatible field type maps to `Unsupported provider response`.
- HTTP transport failure with platform-confirmed no usable network maps to `Network unavailable`; bounded deadline exhaustion maps to `Timeout`; invalid URI or prohibited transport maps to `Invalid host`.
- No catalog, categories, EPG, playlist, artwork, or stream request occurs inside the authentication transaction.

### Ownership representation

- The authoritative account owner contains `{accountId, generation}` where generation is a monotonically increasing `Long`.
- Each login attempt also captures a monotonically increasing operation ID.
- A result commits only when Account ID, generation, operation ID, and destination-active state still match under one serialized commit section.
- Starting a newer attempt invalidates the prior operation ID. Logout, replacement, or removal invalidates generation before cancellation or cleanup.

## 16. Decision register

### DECIDED

- Xtream Codes only for V1.
- User-provided Host, Username, and Password.
- Direct Android-to-provider communication.
- Licensing and IPTV subscriptions are separate.
- Stable internal Account ID and per-account isolation.
- At most one authoritative active account.
- Cancellation plus commit-time ownership validation.
- Distinct authentication error categories.
- Sensitive-data logging prohibitions.
- Bounded, explicit network behavior.
- No startup-blocking authentication or catalog work.
- HTTPS preference with explicit per-provider consent before HTTP.
- One saved active account in the V1 UI.
- Keystore-backed AES-GCM credential storage with fail-closed recovery.
- Canonical host plus exact Username duplicate matching.
- Platform networking with fixed timeouts, no redirects, and no automatic retry.
- Immutable Account ID, generation, and operation-ID commit guard.

### OPEN

- No implementation-blocking decisions remain for the scoped Android V1 authentication slice.
- Provider-specific compatibility exceptions require future evidence and separate approval.

### DEFERRED

- M3U and MAC/Stalker Portal.
- Catalog, category, EPG, search, favorites, history, and resume implementations.
- Stream URL construction and playback behavior.
- Player Contract and Media3 implementation.
- Licensing/trial implementation and anti-abuse design.
- Final TV screen focus contracts.
- Certificate pinning or provider-specific trust exceptions.
- Cloud sync, profiles, downloads, and recommendations.

## 17. Implementation entry gate

Once this contract is approved, the following entry gate is **DECIDED**.

Xtream authentication implementation must not begin until:

1. This contract is approved.
2. No implementation-blocking OPEN decision remains for the selected slice.
3. Dependencies and their stable versions are verified against official sources.
4. A single atomic implementation scope and branch are defined.
5. Tests for ownership races and error classification are designed before production code is committed.

This contract authorizes no production code, backend change, deployment, signing change, merge, tag, or release.
