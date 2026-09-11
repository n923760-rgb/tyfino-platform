# Android Series Episodes Contract v1

Status: **PROPOSED — awaiting explicit approval**  
Scope: Android Series details, seasons, episodes, episode playback, and episode resume for the single active Xtream account  
Authoritative owners: active Account ID, credential generation, selected Series ID, published Series generation, foreground playback session, and episode resume repository

## 1. Purpose

This contract defines the next Android V1 slice after catalog and Live/Movie playback. It turns a cached Series catalog item into an on-demand Series details destination with seasons and playable episodes while preserving TYFINO's account isolation, bounded-resource, secret-handling, lifecycle, TV input, RTL, and accessibility rules.

The goals are:

- load only the Series the user opens, never every provider Series at startup;
- tolerate common Xtream scalar and optional-field variation without accepting unbounded data;
- derive a stable season/episode presentation even when the optional provider season list is empty;
- construct credential-bearing episode references only in memory;
- resume an episode only for the account, Series, episode, and credential generation that created the request;
- remain responsive on phones, tablets, foldables, Android TV, and Google TV.

## 2. Product boundary

### 2.1 In scope

- Open a Series details destination from a Series catalog item.
- Fetch Series metadata, seasons, and episodes on demand.
- Render a bounded Series summary when safe metadata exists.
- Render seasons and episodes lazily with deterministic ordering.
- Play an episode through the approved foreground Media3 player.
- Use embedded audio and subtitle controls already approved by the playback contract.
- Save and automatically resume eligible episode progress.
- Expose account-scoped Continue Watching episodes in the Series destination.
- Cache canonical Series metadata and episode records for cache-first reopen.
- Independent loading, empty, stale, refresh, and safe error states.
- Touch, keyboard, and TV D-pad operation.

### 2.2 Deferred

- Automatic next-episode playback and binge countdown.
- Mark watched/unwatched, season completion, and manual progress editing.
- Series-level progress percentages and recommendations.
- Trailers, cast pages, external metadata enrichment, and provider-specific detail endpoints.
- External subtitle discovery, provider-specific headers/cookies, DRM, and software codecs.
- Episode downloads, Cast, Picture-in-Picture, and background playback.
- Search, favorites, EPG, catch-up, broader history, and cross-account aggregation.
- M3U and Stalker/MAC Series.

## 3. Approved Xtream operation proposal

The Android client requests one selected Series directly from the active provider:

```text
GET {baseUrl}/player_api.php
    ?username={username}
    &password={password}
    &action=get_series_info
    &series_id={seriesId}
```

Rules:

- Credentials come only from the active encrypted Xtream account.
- Query values are UTF-8 encoded as individual values.
- Series ID is non-empty and at most 256 Unicode code points.
- The request uses the active account's normalized scheme, host, port, and approved base path.
- HTTPS is preferred. HTTP requires the active account's stored cleartext consent.
- Redirects, automatic retry, cookies, authorization headers, and implicit HTTPS/HTTP fallback are disabled.
- Connect timeout is 8 seconds; read timeout is 60 seconds.
- The decompressed response limit is 32 MiB.
- The request starts only when the Series destination is visible or the user explicitly refreshes it.
- Opening one Series never starts detail requests for other Series.

The response baseline is an object containing optional `info` and `seasons` members plus an `episodes` object whose keys identify seasons and whose values are episode arrays. The `episodes` object is the playable authority. An absent or empty `seasons` array does not invalidate otherwise usable episodes.

### 3.1 Compatibility basis

The operation, response baseline, scalar variation, and episode path were cross-checked on 2026-09-11 against independent open-source Xtream clients and fixtures:

- [better-iptv request, response models, variation tests, and episode URL builder](https://github.com/mewset/better-iptv/blob/b7441255aec5b38d32dcc75ce739e815bddb62fb/src-tauri/src/playlist/xtream.rs);
- [pyxtream lazy Series loading and episode URL construction](https://github.com/superolmo/pyxtream/blob/a759d6e93d797bcf808f29c4d4674aafc59a7b86/pyxtream/pyxtream.py);
- [netv Series flattening and episode URL construction](https://github.com/jvdillon/netv/blob/bbd64f154bae88aa5f9e17b5f291cc6c3d9430bf/tools/xtream2m3u.py);
- [IPTVnator Xtream mock-server operation and route fixtures](https://github.com/4gray/iptvnator/blob/fadcbb467347c4941b107c0c0c66e1a808270d0d/apps/xtream-mock-server/README.md).

These implementations are compatibility evidence, not authority to expand TYFINO's scope or weaken this contract's limits.

## 4. Provider-variation rules

Xtream panels commonly vary scalar representations. For bounded recognized fields, TYFINO accepts:

- a string or finite integral JSON number for identifiers;
- a string or finite integral JSON number for season and episode numbers;
- missing, `null`, or an empty string for optional scalar metadata.

TYFINO does not coerce arrays, objects, booleans, fractional numbers, non-finite values, or out-of-range integers into scalar identities or ordering values.

Rules:

1. Unknown fields are skipped.
2. A malformed individual season or episode is skipped only while the top-level response remains structurally valid.
3. Duplicate episode identities within the same Series are rejected from the candidate generation after the first valid occurrence.
4. A season entry is advisory metadata. Episode grouping remains derived from valid episode records.
5. If a valid episode's explicit season number conflicts with its enclosing season key, the enclosing key wins and a credential-free aggregate mismatch count may be recorded.
6. A response without a valid `episodes` object is unsupported and never replaces a valid cached generation.

## 5. Canonical Series metadata

The candidate Series summary may contain:

- Account ID;
- provider Series ID;
- bounded display name;
- optional plot/overview;
- optional genre;
- optional release date/year;
- optional rating text;
- optional cast and director display text;
- optional accepted cover or backdrop artwork references;
- generation ID and refresh timestamp.

Limits:

| Field | Limit |
| --- | ---: |
| Series name | 512 Unicode code points |
| Plot/overview | 4,096 Unicode code points |
| Genre, cast, or director | 1,024 Unicode code points each |
| Rating or release text | 128 Unicode code points |
| Artwork reference | 2,048 characters |

Metadata beyond a limit is truncated only for display text. An identifier or artwork reference beyond its limit is rejected. Artwork uses the catalog artwork policy: only approved HTTP(S), no user-info or fragment, no credential-bearing values, and cleartext only with stored consent.

The cached catalog Series item remains the fallback source for name, rating, year, and cover. A missing optional `info` object does not block valid episode browsing.

## 6. Canonical season and episode records

### 6.1 Season

A season presentation record contains:

- Account ID and provider Series ID;
- normalized season number;
- bounded display label;
- optional bounded overview and air-date text;
- optional accepted cover reference;
- provider order when available;
- episode count derived from accepted episodes.

Season numbers are integers from 0 through 10,000. Season 0 is allowed for specials. A season without accepted episodes is not displayed in V1.

### 6.2 Episode

An episode record contains only:

- Account ID and provider Series ID;
- provider episode ID;
- normalized season number;
- normalized episode number when valid;
- bounded display title;
- bounded container extension;
- provider order within its season;
- optional bounded plot, duration text, release text, and rating text;
- generation ID and refresh timestamp.

Provider episode ID follows the 256-code-point identifier limit. Episode number is an integer from 0 through 100,000. A missing title falls back to a localized neutral label using the accepted episode number or provider order; raw provider identity is not exposed as the title.

Container extension is required for playback and follows the existing playback rule: 1–12 lowercase ASCII letters or digits after normalization, with no dot, slash, whitespace, percent escape, query, or fragment. An episode with a missing or unsafe extension remains visible but disabled with a safe playback-unavailable explanation.

The implementation must not persist raw JSON, credentials, authenticated request URLs, constructed playback references, manifests, redirect targets, activation data, or raw provider errors.

## 7. Limits and atomic publication

| Limit | Value | Result when exceeded |
| --- | ---: | --- |
| Decompressed response | 32 MiB | Reject candidate generation |
| Recognized season groups | 1,000 | Reject candidate generation |
| Episode entries | 10,000 | Reject candidate generation |
| Skipped malformed entries | Report aggregate count only | Publish remaining valid records |
| Automatic retries | 0 | Preserve cache and wait for user action |

The parser reads incrementally and does not create a second complete response copy.

Only one Account ID + Series ID generation is published atomically. A failed, cancelled, malformed, oversized, stale-owner, or storage-failed candidate leaves the previous generation unchanged.

Before publication, the repository verifies:

1. expected Account ID still equals the active Account ID;
2. expected credential generation still equals the active generation;
3. expected Series ID still owns the destination operation;
4. the destination epoch remains current;
5. no newer refresh generation for that Account ID + Series ID committed;
6. parsing and all hard-limit checks completed successfully.

Cancellation alone is never authority to commit.

## 8. Cache and repository state

Series detail generations are fresh for 6 hours, matching catalog item freshness. There is no polling or startup prefetch. Explicit refresh bypasses freshness.

The repository exposes:

- `Empty`;
- `Loading`;
- `Content` with freshness and refresh state;
- `EmptyContent`;
- `Error` when no cache exists;
- `StaleContent` with the latest safe refresh failure.

Stale cached episodes remain usable after a refresh failure as long as the active account owner still matches. Future or invalid wall-clock timestamps are stale. Logout or account replacement makes the old account's Series data inaccessible before new UI publication and then removes its records.

## 9. Episode playback identity and reference

An episode selection contains:

- Account ID;
- credential generation;
- provider Series ID;
- provider episode ID;
- container extension;
- random in-memory playback operation ID;
- current playback-destination epoch.

The approved baseline episode reference is:

```text
{baseUrl}/series/{username}/{password}/{episodeId}.{containerExtension}
```

Every component is encoded as one URI path segment. The reference is built only after reloading the active encrypted account and revalidating the selected cached episode against the current published Series generation.

The reference inherits all playback-contract rules for:

- in-memory secret handling and redacted string representation;
- approved cleartext consent;
- at most three safe playback redirects;
- no HTTPS downgrade, user-info, fragment, credential forwarding, or logging;
- one foreground player;
- Media3 error mapping, embedded tracks, lifecycle pause, clear, and release.

A Series catalog ID is never substituted for an episode ID.

## 10. Episode resume and Continue Watching

Episode progress uses a separate repository namespace from Movie resume. Its key is:

```text
Account ID + provider Series ID + provider episode ID
```

A record stores only the key, position milliseconds, observed duration milliseconds when known, and update timestamp. It never stores a title, artwork, season label, host, credentials, playback reference, or raw error.

The existing Movie thresholds are reused:

- do not save below 30 seconds;
- checkpoint no more often than every 10 seconds;
- save on pause, foreground loss, episode replacement, and player exit;
- delete at 95% completion or when less than 60 seconds remain;
- include in Series Continue Watching at 60 seconds or later while not completed;
- reject negative, future, or greater-than-duration positions;
- seek only after the timeline is ready and ownership still matches.

Continue Watching joins progress to the current account's currently published Series/episode metadata. Orphaned progress is not shown. Records are ordered by most recent update and bounded to 200 candidates.

No automatic next episode starts after completion in V1.

## 11. UI, TV input, RTL, and accessibility

- Selecting a Series opens a details destination; it never attempts to play the Series catalog ID.
- Cached content renders before a refresh completes.
- The summary collapses gracefully when optional metadata is absent.
- Seasons use a lazy, horizontally scrollable selector with visible deterministic focus.
- Episodes use a lazy list or adaptive grid keyed by Series ID + episode ID.
- The selected season and focused episode are preserved across same-owner generation replacement when their stable keys remain.
- A loading indicator never steals focus.
- Disabled episodes explain that playback information is unavailable.
- Back exits player to the same Series and season, then exits Series details.
- TalkBack labels use bounded display title, season, and episode number without provider IDs or URLs.
- RTL mirrors layout and navigation placement without reversing season or episode numeric order.
- Touch targets and D-pad focus remain usable on compact and expanded windows.

## 12. Safe failure taxonomy

UI and diagnostics use bounded credential-free categories:

- active account changed or signed out;
- invalid Series identity;
- offline;
- connection timeout;
- provider unavailable or authentication rejected;
- malformed or unsupported Series response;
- response too large;
- no episodes;
- episode playback metadata unavailable;
- redirect rejected;
- unsupported media format;
- decoder unavailable or failed;
- local Series cache unavailable;
- local resume storage unavailable;
- unknown safe fallback.

No error may contain a host, username, password, activation code, authenticated URL, path, query, redirect target, response body, episode manifest, cookie, or authorization header.

## 13. Required verification

### 13.1 Protocol and parser tests

- Build the encoded `get_series_info&series_id` request.
- Reject blank/oversized Series IDs, redirects, unapproved HTTP, and over-limit bodies.
- Parse missing/empty optional `seasons` while retaining valid `episodes`.
- Accept bounded quoted or integral-number identifiers and ordering fields.
- Reject fractional, object, array, boolean, non-finite, and out-of-range scalar substitutions.
- Enforce season, episode, text, artwork, and extension limits.
- Verify conflicting season metadata cannot move an episode outside its enclosing group.
- Verify duplicate identities and malformed-entry aggregate behavior.

### 13.2 Repository and ownership tests

- Publish one complete atomic generation and preserve the previous generation on every failure.
- Reject stale network, parse, transaction, destination, account, and credential-generation results.
- Verify Account A Series and progress cannot be queried or changed by Account B.
- Verify explicit refresh, stale cache, invalid/future timestamps, logout, and replacement cleanup.
- Verify credentials, raw JSON, and playback references are absent from stored records and safe diagnostics.

### 13.3 Playback and resume tests

- Construct the encoded `/series/.../{episodeId}.{extension}` reference.
- Reject Series catalog IDs and unsafe/missing episode extensions.
- Verify one foreground player, lifecycle release, safe redirects, and embedded track controls.
- Verify thresholds, checkpoint cadence, clamping, completion deletion, and 200-record bound.
- Verify Movie and episode resume namespaces cannot collide.
- Verify no automatic next episode starts.

### 13.4 UI and qualification tests

- Cache-first open, empty seasons metadata, multiple seasons, specials, disabled episode, stale refresh, and safe errors.
- Rapid Series switching, refresh, Back, logout, and account replacement.
- Compact touch, expanded tablet/foldable, TV D-pad focus, Arabic RTL, and TalkBack.
- Episode playback/resume on managed API 27 phone and API 35 tablet.
- Representative physical phone, Android TV/Google TV, and lower-performance API 24-class checks remain required before release qualification.

## 14. Implementation sequence after approval

1. Bounded Series request, incremental parser, canonical models, and protocol tests.
2. Account-scoped atomic Series cache repository and cleanup.
3. Adaptive Series details, season selector, and episode list.
4. Episode playback identity/reference and Media3 integration.
5. Episode resume repository and Series Continue Watching.
6. Managed-device coverage and physical compatibility backlog update.

Each step is an atomic pull request. Search, favorites, EPG, broader history, artwork loading, automatic next episode, and final branding remain separate.

## 15. Decisions made by approval

Approval makes the following Android V1 decisions binding:

- on-demand `get_series_info` for one selected Series;
- the `episodes` object as the playable authority when optional season metadata is absent;
- bounded lenient scalar handling without unbounded coercion;
- 6-hour cache freshness with no polling;
- atomic Account ID + credential generation + Series ID publication;
- `/series/{username}/{password}/{episodeId}.{extension}` in-memory playback references;
- episode progress keyed by Account ID + Series ID + episode ID;
- Movie-equivalent resume thresholds and a Series Continue Watching source;
- no automatic next episode in V1;
- adaptive, RTL-safe, accessible, D-pad-first Series navigation.

Any alternate Series endpoint, provider-specific headers/cookies, external metadata, autoplay, downloads, DRM, software codec, or background-playback expansion requires a separate explicit decision.
