# TYFINO Platform

TYFINO is a native Android IPTV player/client with a separate application-licensing service and a small administration dashboard.

Status: PROPOSED foundation alignment. No production deployment or release is authorized.

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
apps/api/                  Existing control-service implementation
apps/admin/                Existing administration interface
database/                  Existing database schema
docs/                      Authoritative scoped contracts and project guidance
infrastructure/            Existing deployment preparation
sites/                     Existing web assets
```

## Current implementation warning

The existing backend, admin application, and database were created before the current product boundary was approved. They include provider-account management and fields for IPTV credentials and protocols outside V1.

That legacy implementation is not approved for production use or Android integration. It must be audited and remediated in a separate atomic task. Documentation alignment does not prove implementation compliance.

## Decision vocabulary

- **DECIDED**: mandatory and authoritative.
- **PROPOSED**: awaiting explicit approval.
- **OPEN**: unresolved; must not be silently chosen.
- **DEFERRED**: intentionally outside the current phase.

## Authority

Scoped approved contracts override general documents for their scope. Architecture and security documents override component READMEs. When documentation and implementation disagree, report the mismatch; do not silently change production behavior.

Read `AGENTS.md`, then read the relevant files under `docs/` before repository work.
