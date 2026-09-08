# Xtream Authentication & Account Ownership Contract v1

Status: PROPOSED — awaiting approval  
Scope: TYFINO Android client  
Version: 1.0  
Last reviewed: 2026-09-08

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

A monotonically changing ownership version associated with the authoritative active-account context. This is a behavioral concept; its concrete Kotlin representation remains **OPEN**.

### Operation identity

An identity or version that distinguishes a currently authoritative operation from an older operation for the same account and generation. Its concrete representation remains **OPEN**.

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

Multiple saved accounts in the user interface are **PROPOSED**. The isolation model is mandatory regardless of when that UI is introduced.

## 5. Authentication input

The following are **DECIDED**:

- Authentication input consists only of Host, Username, and Password.
- Inputs are collected by UI and submitted to an authoritative non-Composition state owner.
- Composables must not perform validation IO, network requests, persistence, or navigation during Composition.
- Empty required values must be rejected before network work.
- Passwords must not be trimmed, normalized, reformatted, or silently modified.
- User-visible errors must not echo the password or a secret-bearing full URL.
- Authentication begins only from an explicit user action; it must not run during application startup merely because the screen is displayed.

Exact host canonicalization, accepted schemes, path handling, internationalized hosts, and duplicate matching are **OPEN**.

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

Exact Xtream response-field mappings and provider compatibility exceptions are **OPEN** pending an evidence-based integration task.

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

The exact implementation type—generation counter, immutable owner token, request token, reducer guard, or equivalent—is **OPEN**. The implementation must be explicit, inspectable, and race-testable.

## 8. State ownership

The following are **DECIDED**:

- Each mutable state value must have one authoritative owner.
- UI may render state and emit user intents; it must not own network or credential persistence.
- Navigation must occur from handled events/effects, never as a side effect of Composition.
- Repository abstractions may be introduced where they enforce a real data boundary; generic base repositories and one-use-case-per-action patterns are not required.
- Authentication success is not committed until the response is classified and ownership is revalidated.
- Persisted active-account selection must never expose another account's scoped state during restoration.

The exact ViewModel, reducer, repository, and persistence types remain **OPEN** until implementation design is approved.

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

The credential storage mechanism is **OPEN** and blocks persisted-account implementation.

## 10. Transport policy

TLS preference and the prohibition on silently weakening transport security are **DECIDED**. Whether TYFINO will support provider Hosts using cleartext HTTP is **OPEN**.

This decision is a blocking product/security decision because:

- many legacy Xtream providers may expose HTTP endpoints;
- globally enabling cleartext weakens the application security boundary;
- the Android foundation currently disables cleartext traffic;
- per-domain exceptions cannot be known before the user supplies a Host.

Implementation must not globally enable cleartext, add a permissive network-security configuration, or silently upgrade/downgrade schemes until this decision is approved.

Certificate pinning, custom trust stores, hostname-verification bypasses, and acceptance of invalid certificates are **DEFERRED** and must not be introduced implicitly.

## 11. Bounded network behavior

The following are **DECIDED**:

- Authentication must have explicit finite timeouts.
- Infinite retries, arbitrary delays, hidden fallback loops, uncontrolled polling, and delay-based synchronization are prohibited.
- Automatic retry must not repeat known invalid credentials, expired accounts, disabled accounts, malformed responses, or unsupported responses.
- Network loss and timeout must remain distinguishable where evidence permits.
- Work must run off the main thread without blocking first usable UI.

Exact timeout durations, redirect policy, retry count, backoff, HTTP client, serializer, and connection-pool policy are **OPEN** and require official dependency review.

## 12. Account switching and removal

The following are **DECIDED**:

- Switching is an authoritative state transition, not only a navigation action.
- The outgoing account must become unable to commit new state before the incoming account is exposed as active.
- UI must not temporarily show Account A data under Account B identity.
- In-memory account-scoped state must be cleared or replaced deterministically.
- Pending work belonging to the outgoing account should be cancelled and must fail commit-time validation.
- Removal must target an exact stable Account ID.
- Removal must not delete or alter another account whose username or host happens to match.

Removal retention, undo, re-authentication, and partial-cache cleanup policies are **OPEN**.

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

## 15. Decision register

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

### PROPOSED

- Multiple saved accounts exposed in the V1 user interface.

### OPEN — must be resolved before implementation

- Cleartext HTTP provider compatibility policy.
- Android credential-storage mechanism and recovery behavior.
- Host validation and canonicalization rules.
- Duplicate-account matching and update policy.
- Exact Xtream authentication response mapping and compatibility evidence.
- HTTP client, serializer, timeout, redirect, and retry policy.
- Concrete account-generation and operation-identity representation.
- Account removal retention and cleanup semantics.

### DEFERRED

- M3U and MAC/Stalker Portal.
- Catalog, category, EPG, search, favorites, history, and resume implementations.
- Stream URL construction and playback behavior.
- Player Contract and Media3 implementation.
- Licensing/trial implementation and anti-abuse design.
- Final TV screen focus contracts.
- Certificate pinning or provider-specific trust exceptions.
- Cloud sync, profiles, downloads, and recommendations.

## 16. Implementation entry gate

Once this contract is approved, the following entry gate is **DECIDED**.

Xtream authentication implementation must not begin until:

1. This contract is approved.
2. Blocking OPEN decisions required by the selected implementation slice are resolved explicitly.
3. Dependencies and their stable versions are verified against official sources.
4. A single atomic implementation scope and branch are defined.
5. Tests for ownership races and error classification are designed before production code is committed.

This contract authorizes no production code, backend change, deployment, signing change, merge, tag, or release.
