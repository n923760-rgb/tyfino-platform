# Android Catalog Synchronization Contract v1

Status: **DECIDED — explicitly approved on 2026-09-09**  
Scope: Android client catalog metadata for the single active Xtream account  
Authoritative owners: active Account ID, catalog repository, and published cache generation

## 1. Purpose

This contract defines the smallest safe catalog slice that follows Xtream authentication. It covers discovery and local caching of Live TV, Movies, and Series categories and their items. It does not authorize playback or any backend handling of IPTV credentials or catalog data.

The goals are:

- keep application startup and navigation responsive on phones, tablets, foldables, Android TV, and Google TV;
- make useful cached content available before network refresh completes;
- prevent large provider catalogs from causing unbounded memory, storage, or network work;
- prevent results from a logged-out or replaced account from entering the active catalog;
- keep Xtream credentials and credential-bearing URLs out of catalog persistence, logs, analytics, and crash reports.

## 2. Product boundary

### 2.1 In scope

- Three catalog sections: Live TV, Movies, and Series.
- Section category lists.
- Item lists for the category the user opens.
- Cache-first rendering with background refresh.
- Explicit user refresh.
- Account-scoped local metadata snapshots.
- Independent loading, empty, stale, and error states.
- A deterministic compatibility result for malformed or unsupported provider responses.

### 2.2 Deferred

- Playback, stream URL construction, player selection, codecs, and DRM.
- EPG and catch-up.
- Movie details, series seasons, episodes, and trailers.
- Search, favorites, history, resume position, recommendations, and parental controls.
- Background polling, push refresh, and multi-account aggregation.
- M3U and Stalker/MAC.
- Server-side proxying, transformation, or storage of IPTV data.
- Final artwork downloading and disk-cache policy; V1 may expose validated artwork references to a later image layer, but navigation must work with placeholders.

## 3. Direct provider operations

The Android application communicates directly with the active Xtream provider through `player_api.php`, using the normalized host and encrypted credentials owned by the approved authentication slice.

| Section | Categories | Items for one category |
|---|---|---|
| Live TV | `action=get_live_categories` | `action=get_live_streams&category_id={id}` |
| Movies | `action=get_vod_categories` | `action=get_vod_streams&category_id={id}` |
| Series | `action=get_series_categories` | `action=get_series&category_id={id}` |

These operation names are treated as the V1 compatibility baseline because they recur across independent Xtream client implementations. Xtream is not treated as a stable formal standard. A provider that requires different names, redirects, transport fallback, or undocumented parameters is **unsupported** until a separate compatibility decision is approved.

Rules:

1. A request uses only the active account's normalized host, username, and password at execution time.
2. Credentials remain query parameters only where the provider protocol requires them.
3. Credential-bearing request URLs, query strings, response bodies, and headers must never be logged, persisted as catalog data, emitted to analytics, or attached to crash reports.
4. Redirects are rejected. The client must not forward credentials to a redirect target.
5. HTTPS remains the normal transport. HTTP is permitted only for an account that the user already saved through the explicit insecure-transport warning in the authentication contract.
6. No request is routed through the TYFINO licensing backend.

## 4. Progressive loading model

### 4.1 Application start

Application startup must not start a whole-catalog download. Authentication and licensing state remain independent owners. The catalog shell becomes usable immediately and reads only small account-scoped summary state needed for the visible destination.

### 4.2 Section entry

When a user opens Live TV, Movies, or Series:

1. Publish the latest valid cached category generation immediately, if present.
2. If no cache exists, show a non-blocking loading state for that section only.
3. Refresh categories in the background when missing, explicitly requested, or older than the freshness window.
4. Do not fetch item lists for every category.

### 4.3 Category entry

When a user opens one category:

1. Publish the latest valid cached item generation for that Account ID, section, and category ID.
2. Refresh only that category when missing, explicitly requested, or older than the freshness window.
3. Cancel obsolete work when the user changes category, section, or account, while still performing the commit-time ownership checks in Section 8.
4. Preserve D-pad focus and scroll state when cached rows are replaced by a newer generation, where the same stable item ID still exists.

### 4.4 Freshness proposal

Approval of this contract approves the following V1 defaults:

- category lists are fresh for **12 hours**;
- item lists are fresh for **6 hours**;
- there is **no automatic polling**;
- user-initiated refresh bypasses the freshness window;
- stale cached data may remain visible while refresh runs or while the provider is unreachable;
- the UI displays that the data is stale after a failed refresh without discarding it.

Freshness uses device elapsed time for in-process decisions and a persisted wall-clock timestamp for restart continuity. A clock rollback never makes a snapshot fresh indefinitely; invalid or future timestamps are treated as stale.

## 5. Network and resource limits

Approval of this contract approves these V1 limits per request:

| Limit | Value | Result when exceeded |
|---|---:|---|
| Connect timeout | 8 seconds | Preserve cache and publish timeout state |
| Read timeout | 60 seconds | Preserve cache and publish timeout state |
| Response body | 32 MiB decompressed | Reject new generation as too large |
| Categories | 10,000 | Reject new generation as too large |
| Items in one category | 50,000 | Reject new generation as too large |
| Automatic retries | 0 | Wait for explicit refresh or later navigation |

Implementation requirements:

- Parse JSON incrementally; do not require a second complete in-memory copy of the response.
- Ignore unknown fields and tolerate missing optional fields.
- Bound every text field before persistence: names at 512 Unicode code points and artwork references at 2,048 characters.
- Reject invalid top-level shapes and entries without a usable provider ID.
- Skip malformed individual entries only when the top-level response is valid; record a credential-free aggregate count for diagnostics, never raw entry content.
- Keep category/item rendering lazy and keyed by stable IDs. Images and refresh work never block focus movement or navigation.
- A response that hits any hard limit never replaces a previously valid cache generation.

## 6. Canonical metadata

### 6.1 Category record

A category snapshot stores only:

- Account ID;
- section type: `LIVE`, `MOVIES`, or `SERIES`;
- provider category ID as an opaque bounded string;
- display name as a bounded string;
- provider ordering position;
- published generation ID and refresh timestamp.

### 6.2 Item record

An item snapshot stores only metadata needed to render a catalog row:

- Account ID;
- section type and provider category ID;
- provider item ID as an opaque bounded string;
- display name;
- provider ordering position;
- optional validated artwork reference;
- optional rating text, release year, and container extension when supplied as bounded scalar values;
- published generation ID and refresh timestamp.

The implementation must not persist:

- username, password, activation code, authorization header, or authenticated request URL;
- raw response JSON;
- constructed live, movie, episode, or other playback URL;
- user-agent or provider session tokens discovered in arbitrary fields;
- EPG, episode lists, playback history, or personally entered profile data.

Artwork references are treated as potentially sensitive URLs: they are never logged or exposed to analytics. References containing the active username, active password, user-info query parameters, or unsupported schemes are discarded. `https` is accepted; `http` follows the active account's explicit insecure-transport choice. Other schemes are rejected.

## 7. Local persistence and isolation

V1 uses a structured Android database abstraction with transactional replacement; the exact dependency version is selected and verified during implementation. Repository callers must not depend directly on the database library.

All cache keys begin with the stable Account ID created by the authentication slice. Section and category are subordinate keys. Queries without an Account ID are forbidden.

Rules:

1. Saving a new account creates a new Account ID and cannot inherit another account's cache.
2. Logging out deletes the active account's catalog metadata in the same logical operation or marks it inaccessible before UI publication; encrypted credential deletion remains owned by the authentication slice.
3. Re-authentication for the same provider does not reuse old metadata unless the authentication slice explicitly preserves the same Account ID.
4. Database migrations must be forward-only and tested. Downgrade behavior remains unsupported.
5. Database corruption or migration failure must fail closed for catalog cache: recreate only catalog tables after reporting a credential-free local error. It must not delete licensing state or encrypted Xtream credentials.

## 8. Ownership, concurrency, and atomic publication

Cancellation alone is insufficient. Every network or parsing result must pass all commit-time checks:

1. the expected Account ID still equals the active Account ID;
2. the expected account revision still equals the current credential revision;
3. the request's section and category key still identify the repository operation being committed;
4. no newer refresh generation for the same key has already committed;
5. parsing completed successfully and all hard limits were respected.

Only then may the repository publish the new generation.

Each categories response and each single-category item response is an atomic snapshot:

- stage or parse the candidate generation separately;
- replace the previously published generation in one transaction;
- never expose a partially parsed generation;
- remove records absent from the successful replacement snapshot;
- keep the previous generation unchanged on cancellation, timeout, malformed response, hard-limit rejection, database error, or ownership mismatch.

A result rejected by an ownership check produces no user-visible error for the new owner. A stale result from Account A must never mutate Account B, even if the request cannot be physically cancelled.

## 9. Repository state contract

Each Account ID + catalog key exposes one immutable state:

- `Empty`: no published cache and no request;
- `Loading`: no cache and a request is active;
- `Content`: published items, freshness, last successful refresh time, and whether refresh is active;
- `EmptyContent`: successful published generation with zero records;
- `Error`: no cache and a categorized failure;
- `StaleContent`: published cache plus the latest categorized refresh failure.

Failures are categorized without embedding secrets:

- offline;
- timeout;
- authentication rejected or account expired;
- unsupported or malformed provider response;
- response too large;
- local storage failure;
- unknown safe fallback.

An authentication rejection asks the user to re-authenticate. It does not automatically delete credentials or cached content, and it never changes licensing state.

## 10. UI and accessibility requirements

- Live TV, Movies, and Series remain distinct destinations or clearly distinct adaptive navigation entries.
- Each destination renders independently; a Movies failure must not block Live TV or Settings.
- Cached text rows are interactive before artwork finishes.
- Loading indicators do not steal focus.
- Every category and item has a readable accessibility label derived from bounded display metadata.
- TV focus is visible, deterministic, and recoverable after refresh.
- Phone/tablet layouts use available window width rather than device model checks.
- RTL mirrors layout direction without reversing semantic ordering supplied by the provider.
- Empty and error states expose a focusable retry action where a retry is valid.

## 11. Required verification before implementation merge

### 11.1 Unit tests

- Parse valid, empty, missing-optional-field, unknown-field, malformed-entry, and invalid-top-level responses for all three sections.
- Enforce text, category-count, item-count, and decompressed-body limits.
- Verify each canonical request action and its category ID encoding.
- Verify no retry occurs automatically.
- Verify freshness behavior, explicit refresh, future timestamps, and clock rollback.
- Verify Account A data cannot be queried through Account B.
- Verify an older generation cannot overwrite a newer generation.
- Verify logout/account replacement during network, parse, and transaction phases prevents commit.
- Verify partial, cancelled, malformed, and over-limit generations leave the previous snapshot unchanged.
- Verify secrets and credential-bearing URLs are absent from persisted catalog records and diagnostic messages.

### 11.2 Integration and UI tests

- Cold start with and without cache.
- Offline start with stale cache.
- Independent loading and error states for all three sections.
- Open one category without starting requests for all other categories.
- Rapid section/category changes and rapid account logout/login.
- D-pad navigation and focus retention on TV dimensions.
- Touch navigation on compact and expanded phone/tablet windows.
- RTL rendering, accessibility labels, retry focus, and navigation while artwork is unavailable.
- Large synthetic catalog verification under a documented bounded-memory test budget.

### 11.3 Security checks

- Inspect database contents, logs, test reports, exceptions, and crash-safe diagnostics for credentials or authenticated URLs.
- Verify redirect rejection and no implicit HTTP/HTTPS fallback.
- Verify the Android client alone communicates with the provider and that the TYFINO licensing backend receives no IPTV catalog or account data.

## 12. Blocking decisions after this contract

Approval of this contract makes the scope, operations, freshness windows, limits, persistence boundary, and ownership model above **DECIDED** for the first catalog implementation.

The following remain intentionally open and do not block category/item browsing:

- playback URL and player contract;
- details, seasons, episodes, EPG, catch-up, search, favorites, and history;
- final image-loading library and image disk-cache policy;
- compatibility adapters for providers outside the six-operation baseline;
- whether later product versions support multiple stored accounts.

Any change to the approved limits or data boundary requires a separate contract change. Implementation must be split into reviewable atomic pull requests and must not mix catalog work with playback, EPG, backend, branding, signing, or release changes.
