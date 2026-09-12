# Android Live EPG Contract v1

Status: **PROPOSED — not approved for implementation**
Scope: On-demand guide for the selected Live channel in the native Android client
Owner: Active Xtream account ID, credential generation, selected channel ID, and EPG request generation

## Purpose and product boundary

Show the current and upcoming programs when a user opens a Live channel, if that provider supplies usable guide data. Empty or unsupported EPG must never prevent playback. The app requests data directly from the active provider; the TYFINO licensing service must never receive IPTV credentials, requests, guide data, or viewing activity. EPG is metadata only: catch-up playback, recording, reminders, and whole-provider XMLTV synchronization are outside this slice.

## Proposed V1 behavior

- Load guide entries only when the user opens a selected Live channel's guide or player guide panel. Do not prefetch every channel, poll in the background, or block the player while fetching.
- Show a compact current/next summary and a focusable, lazy list for that channel. Show localized Arabic/English loading, empty, stale, and safe error states. Do not infer a program title from the channel name when EPG is absent.
- Use a direct, per-channel `player_api.php` request with `action=get_short_epg`, an encoded `stream_id`, and a bounded optional `limit` as the initial compatibility path; never attempt a second endpoint automatically after a failure. Two independent client implementations agree on this action and parameter shape (see references below). Response field variants and timestamp semantics still require fixture-based validation before implementation.
- Preserve responsive touch and TV D-pad interaction, focus on the channel/player when the guide appears or disappears, RTL text order, and readable labels. Guide loading must never take away playback or navigation.
- On channel, account, credential, or request replacement, invalidate the old operation. Before storing or publishing data, recheck account ID, generation, selected channel ID and latest request generation. Cancellation alone is insufficient.

## Network and parsing proposal

- Reuse the account's normalized provider endpoint and explicit HTTP consent; no HTTPS downgrade, redirect following, certificate bypass, automatic retry, or TYFINO backend proxy.
- Start with the catalog transport limits: 8-second connect timeout, 60-second read timeout, decompressed response bounded to **1 MiB** for one channel, and at most **100** guide entries. Reject a response beyond either hard limit without replacing a valid cache.
- Parse incrementally; accept only a bounded top-level shape and bounded scalar fields. Propose 512 Unicode code points for title/description, and a duration/end time that is after start time. Skip malformed entries with a safe aggregate count, but reject malformed top-level data.
- Establish timestamp and timezone precedence with representative provider fixtures before coding. Do not silently assume a provider's local time is UTC. Preserve start/end instants in UTC; format them in the device's locale/timezone only for display. Decode program text, including base64 variants, only after validation and with strict decoded-size bounds.
- Never log or persist username, password, authenticated URL, provider response body, authorization headers, or raw malformed fields. Only credential-free failure categories may reach diagnostics.

## Local cache proposal

Cache only active-account ID, Live provider channel ID, bounded title/description, UTC start/end instants, generation, and fetched timestamp, in an EPG-specific store. No credentials or full response payload. One channel is fresh for **30 minutes**; explicit refresh bypasses freshness. Expired entries may be shown as stale after network failure if their time window is still relevant. Prune completed entries and bound the total number of channels stored to **20** with least-recently-used eviction. Purge account data on account removal and keep the cache isolated from Live catalog, favorites, history and resume databases.

All replacement of one channel's entries must be transactional. On partial parse, limit violation, network/storage failure, or owner change, keep the last valid generation. A fresh empty provider response is an explicit empty generation, not an invitation to reuse another channel's programs.

## Verification before marking DECIDED

1. Confirm the supported request and payload variants against at least two independent client implementations or safe, redacted provider fixtures; pin timezone/base64 behavior to tests.
2. Unit-test URL encoding and redirect refusal, decoded-byte and count boundaries, malformed fields, ambiguous timestamps, deterministic sorting, deduplication, and no secrets in stored/logged values.
3. Test cache isolation, atomic replacement, stale completion after account/channel switch, account removal, and failure fallback.
4. Run Android build, unit tests, lint, and managed phone/tablet instrumentation. Physical Android TV D-pad, actual provider/media, RTL, accessibility and performance qualification remain **BLOCKED** until performed; emulator success is not release qualification.

## Compatibility evidence

- [py-xtream-codes request builder](https://github.com/chazlarson/py-xtream-codes/blob/master/xtream.py) constructs `get_short_epg` with `stream_id` and an optional `limit`.
- [go.xtream-codes EPG client](https://github.com/ALiP61/go.xtream-codes/blob/master/xtream-codes.go) uses the same action and parameters; its [response model](https://github.com/ALiP61/go.xtream-codes/blob/master/structs.go) maps `epg_listings` and both textual and timestamp fields. These are compatibility evidence, not a formal provider specification.

## Explicitly deferred

Whole-provider XMLTV, bulk EPG sync, catch-up/timeshift, scheduling/recording, background polling, provider-specific headers/cookies, and EPG on Movies or Series require separate approval.
