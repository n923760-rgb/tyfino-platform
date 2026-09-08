# TYFINO Licensing & Trial Contract v1

Status: PROPOSED — awaiting explicit approval  
Last reviewed: 2026-09-08  
Scope: TYFINO application licensing, trial authority, activation boundaries, and administration

## 1. Purpose and authority

This contract defines the minimum product and engineering boundary for TYFINO application licensing. It does not define IPTV authentication, IPTV subscription state, catalog access, media playback, deployment, or billing.

Once explicitly approved, this scoped contract overrides general documentation and the legacy API/backend implementation for licensing behavior. Until approval, it is a proposal and does not authorize production implementation or deployment.

Decision labels used here are:

- **DECIDED**: mandatory project rule.
- **PROPOSED**: recommended but awaiting explicit approval.
- **OPEN**: unresolved and must not be silently chosen.
- **DEFERRED**: intentionally outside this contract or V1.

## 2. Mandatory product boundary

The following are **DECIDED**:

- `APPLICATION LICENSE != IPTV SUBSCRIPTION`.
- TYFINO manages access to the TYFINO application only.
- IPTV subscriptions remain owned and managed externally by the user's IPTV provider.
- The Android app communicates directly with the IPTV provider for Xtream authentication, catalog data, EPG, and media.
- The TYFINO licensing service must not receive, validate, store, proxy, or return IPTV Host, Username, Password, playlist URLs, portal data, catalog data, stream URLs, or playback history.
- Application-license validity must never depend on IPTV account validity, expiry, package, provider host, or provider status.
- Encryption of IPTV credentials on the TYFINO server does not make server-side collection compliant.
- A device MAC address is not a user-facing activation method.
- The licensing service must not proxy IPTV requests or streams.

## 3. V1 licensing experience

The following are **DECIDED**:

- On the first licensing experience, the user can choose:
  - **Start 7-Day Free Trial**, or
  - **Activate Now**.
- The user may activate immediately without starting the trial.
- Activation Code is the only paid activation mechanism exposed to the user in V1.
- An administrator can configure an Activation Code for:
  - **1 Year**, or
  - **Lifetime**.
- Trial authority is server-side and must not rely only on the Android device clock.
- Exact anti-trial-abuse behavior must not be invented before approval.

The exact event that begins the seven-day trial is **OPEN**. Candidate events include explicit confirmation of **Start 7-Day Free Trial** or a later successful licensing transaction. Installation time, app process start, and Xtream login must not be silently selected as the authoritative start event.

## 4. Conceptual ownership model

The licensing domain must eventually own concepts equivalent to:

- a server-authoritative trial record;
- an Activation Code record with an administrator-selected period;
- an application-license grant;
- revocation and expiry state;
- only the minimum device or installation evidence approved for enforcement;
- auditable administrative actions that do not contain secrets.

These are conceptual responsibilities, not an approved database schema.

The following are **DECIDED**:

- Licensing records must not reference IPTV provider accounts or provider hosts.
- Activation Codes must not be assigned to, validated against, or invalidated by IPTV subscriptions.
- Licensing state and IPTV account state have separate authoritative owners.
- A stale activation, trial, refresh, or revocation result must prove current ownership before committing Android state; cancellation alone is insufficient.
- Administrative metadata is not application-license authority and must not alter IPTV behavior.

Exact identifiers, tables, relationships, enums, transitions, and persistence technology are **OPEN**.

## 5. Time and lifecycle rules

The following are **DECIDED**:

- Server time is authoritative for trial and paid-license expiry.
- The trial duration is seven days.
- A one-year code and a lifetime code are distinct administrator-selected license periods.
- Revoked or expired authority must not be reported as active.
- Recovery and retries must be finite, explicit, and testable.

The following are **OPEN**:

- the exact trial start event;
- whether seven days means 168 hours or a calendar-based interval;
- the exact one-year calendar calculation;
- the product/legal definition of Lifetime;
- activation-code pre-activation expiry, if any;
- reactivation, transfer, reset, and replacement rules;
- cached entitlement and offline grace behavior;
- refresh cadence and session duration;
- behavior after reinstall, factory reset, device replacement, or app-data deletion;
- revocation propagation timing.

No implementation may disguise these choices as technical defaults.

## 6. Device and trial-abuse boundary

The following are **DECIDED**:

- The trial must not rely only on locally stored state or the Android clock.
- Device MAC address is not an activation credential.
- Device information collected by TYFINO must be limited to an approved licensing purpose.
- Sensitive identifiers must not be written to ordinary logs or exposed to the dashboard without need.
- Custom cryptography is not authorized.

The following are **OPEN**:

- device or installation identity mechanism;
- whether a paid license has a device limit;
- device replacement and administrator reset policy;
- signals used to reduce repeated-trial abuse;
- attestation use, if any;
- privacy retention and deletion periods;
- handling of rooted, cloned, or restored installations.

These decisions require a privacy and operational trade-off review before implementation.

## 7. Activation Code boundary

The following are **DECIDED**:

- Activation Code is the only V1 paid activation input shown to the user.
- Codes are created and administered by authorized TYFINO administrators.
- A code represents application licensing only.
- Codes must be treated as sensitive and redacted in ordinary logs and diagnostics.
- The Android app must never receive administrative Customer Name, Phone Number, External Reference, Admin Label, or Internal Note.
- Administrative authorization is enforced server-side on every action; hiding controls is insufficient.

The following are **OPEN**:

- code format, entropy, normalization, and storage representation;
- single-use versus controlled reuse semantics;
- delivery and recovery procedures;
- rate limits and lockout behavior;
- administrator role permissions;
- whether optional administrative metadata attaches to a code, a license grant, or another administrative record.

## 8. Licensing API boundary

Exact routes, request schemas, response schemas, status codes, tokens, and endpoints are **OPEN**.

Any future API design must satisfy these **DECIDED** rules:

- accept only data required for application licensing and approved device enforcement;
- never accept or return IPTV credentials or provider connection data;
- never require an IPTV account, provider host, or provider subscription identifier;
- use HTTPS in production;
- authenticate and authorize administrative operations server-side;
- redact Activation Codes, tokens, Authorization headers, sensitive identifiers, and secret-bearing URLs;
- distinguish meaningful invalid, expired, revoked, ineligible, rate-limited, unavailable, and malformed-request outcomes where applicable;
- use bounded timeouts and retries;
- make server authority explicit rather than trusting client time;
- avoid uncontrolled polling;
- support commit-time ownership validation in the Android client.

The current `/v1/player/config` provider-connection response and provider-account administration routes are legacy behavior and are not an approved basis for the licensing API.

## 9. Android integration boundary

This contract does not authorize Android licensing implementation yet.

When separately approved, Android integration must:

- render the first usable licensing UI with minimal startup work;
- avoid artificial splash delays;
- avoid eager player, catalog, EPG, database, or IPTV initialization;
- keep licensing refresh bounded and lifecycle-aware;
- avoid navigation or state mutation during Compose composition;
- protect state from stale asynchronous completion;
- handle Arabic RTL, English LTR, phone, tablet, foldable, Android TV, Google TV, touch, and D-pad input;
- preserve visible focus and predictable Back behavior;
- avoid exposing full Activation Codes or sensitive licensing evidence in logs.

The licensing UI presentation, navigation destinations, storage mechanism, and offline UX remain **OPEN**.

## 10. Administration boundary

The V1 administration areas remain **PROPOSED** as:

- Dashboard;
- Activation Codes;
- App Settings;
- Audit Log.

Optional administrative-only fields may include:

- Customer Name;
- Phone Number;
- External Reference;
- Admin Label;
- Internal Note.

The following are **DECIDED**:

- those fields must not be returned to Android without a future explicit requirement;
- the dashboard must not manage IPTV usernames, passwords, hosts, packages, expiry, channels, streams, reseller accounts, M3U, or MAC Portal;
- audit records must not contain plaintext codes, credentials, tokens, or secret-bearing URLs.

Exact App Settings, administrator roles, metadata retention, exports, deletion flows, and audit retention are **OPEN**.

## 11. Performance and availability

The following are **DECIDED**:

- licensing work must remain small and bounded;
- no uncontrolled polling or infinite retry loop is permitted;
- non-essential refresh must not block the first usable UI;
- network recovery must be explicit, bounded, and testable;
- failure must not silently grant or silently revoke authority;
- dependencies require a current concrete need;
- performance and failure behavior must be measured rather than guessed.

Offline authority, grace periods, refresh intervals, and server-outage behavior are **OPEN** and must be resolved before implementation.

## 12. Security and privacy qualification

Before production use, the licensing implementation must prove:

- IPTV credentials cannot enter or leave TYFINO licensing services;
- administrative metadata cannot leak to Android;
- full Activation Codes and session tokens are absent from ordinary logs;
- authorization is enforced on every administrative route;
- trial and expiry decisions use server authority;
- invalid, expired, and revoked grants cannot become active through stale results;
- rate limits and abuse controls are bounded and testable;
- account, installation, and device removal follow the approved retention policy.

Exact hashing, token, session, secret-storage, attestation, backup, and deletion designs remain **OPEN**.

## 13. Legacy remediation gate

The repository currently contains provider hosts, provider accounts, encrypted IPTV credentials, M3U/Stalker values, activation-to-provider-account relationships, and endpoints that return provider connection data.

The following are **DECIDED**:

- legacy behavior is not approved for production or Android integration;
- it must not be extended;
- removal must be handled in a separate atomic implementation task;
- no destructive database change may be made without inspecting live data, deployment state, backup requirements, and rollback;
- rewriting only the initial schema is insufficient if any database has already been initialized;
- implementation remediation must include negative data-boundary tests.

A migration design remains **OPEN** until the deployment and data state are known.

## 14. Required future tests

Implementation qualification must eventually include:

- seven-day server-authoritative trial lifecycle;
- direct activation without trial;
- one-year activation;
- lifetime activation;
- expiry and revocation;
- stale licensing completion after a newer owner or generation exists;
- device limit and replacement behavior once approved;
- offline and server-outage behavior once approved;
- administrative authorization;
- activation brute-force and rate-limit behavior;
- redaction and privacy-boundary tests;
- proof that no IPTV credential or provider data is accepted, persisted, logged, or returned;
- migration and rollback tests for legacy removal.

Unexecuted tests are `SKIPPED` or `BLOCKED`, never `PASS`.

## 15. Explicitly deferred

The following are **DEFERRED** from this contract:

- in-app purchases and additional paid activation mechanisms;
- subscriptions billed through app stores;
- cloud sync;
- user and kids profiles;
- social features;
- downloads;
- M3U and MAC Portal;
- production deployment, DNS, signing, releases, and distribution;
- final Android UI design;
- Player Contract and IPTV feature implementation.

## 16. Approval gates

Before backend remediation implementation:

1. Approve this contract or explicitly resolve requested changes.
2. Resolve only the OPEN decisions required by the first remediation task.
3. Inspect whether any database or environment contains live data.
4. Approve a non-destructive migration and rollback plan.
5. Define the smallest licensing API slice to implement.
6. Define its required boundary and lifecycle tests.

Approval of this document does not authorize deployment, signing, release, destructive migration, or merge.
