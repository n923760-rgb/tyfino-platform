# TYFINO Platform

TYFINO is a native Android IPTV player/client with a separate application-licensing service and a small administration dashboard.

Status: development foundation implemented; production deployment and release remain unqualified.

## Product boundary

The following are **DECIDED**:

- Users bring their own IPTV subscription.
- Android V1 supports Xtream Codes only using Host, Username, and Password.
- The Android app connects directly to the IPTV provider.
- The TYFINO backend manages application licensing/control only.
- `APPLICATION LICENSE != IPTV SUBSCRIPTION`.
- The backend must not proxy, cache, restream, sell, or administer IPTV subscriptions.
- IPTV credentials must not be sent to the TYFINO backend.

## V1 direction

The following direction is **DECIDED**: the Android application is Kotlin and Jetpack Compose, performance-first, adaptive, RTL-safe, and designed for phones, tablets, foldables, Android TV, and Google TV from one codebase.

V1 includes Live TV, Movies/VOD, Series, categories, search, favorites, EPG when available, details, seasons and episodes, media playback, audio tracks, subtitles, resume, Continue Watching, history, and previous/last live channel.

M3U, MAC Portal, profiles, downloads, cloud sync, social features, and a custom recommendation engine are **DEFERRED**.

## Repository map

```text
apps/android/              Native Android client foundation
apps/api/                  Application-licensing service implementation
apps/admin/                Licensing administration interface
database/                  Fresh-install licensing schema
docs/                      Authoritative scoped contracts and project guidance
infrastructure/            Existing deployment preparation
sites/                     Existing web assets
```

## Current implementation and release boundary

The current repository source registers licensing and administration routes only. The fresh-install schema contains installations, trials, activation codes, licensing sessions, administrators, settings, and audit records; it does not define IPTV provider-account tables. Integration tests reject a provider Host in a licensing request, verify that the former `/v1/player/config` route returns 404, and check that the fresh test database has no provider-host, provider-account, or player-session tables.

This describes the checked-in implementation and fresh-test-database evidence, not an audit of any existing deployed database or server. Production deployment, migration of any pre-existing data, final Android identity/signing, licensing origin, and physical-device/media qualification remain unapproved or BLOCKED. Follow [`docs/deployment.md`](docs/deployment.md) before planning a deployment.

## Decision vocabulary

- **DECIDED**: mandatory and authoritative.
- **PROPOSED**: awaiting explicit approval.
- **OPEN**: unresolved; must not be silently chosen.
- **DEFERRED**: intentionally outside the current phase.

## Authority

Scoped approved contracts override general documents for their scope. Architecture and security documents override component READMEs. When documentation and implementation disagree, report the mismatch; do not silently change production behavior.

Read `AGENTS.md`, then read the relevant files under `docs/` before repository work.
