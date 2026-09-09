# Android Playback Contract v1

Status: **PROPOSED — awaiting explicit approval**  
Scope: Android playback for Live TV and Movies from the single active Xtream account  
Authoritative owners: active Account ID, credential generation, foreground playback session, and resume repository

## 1. Purpose

This contract defines the first safe playback slice for TYFINO Android after catalog browsing. It covers Live TV and Movie playback, embedded audio and subtitle selection, movie resume positions, Continue Watching eligibility, and the previous-live-channel action.

The goals are:

- start playback quickly without blocking catalog navigation;
- support phones, tablets, foldables, Android TV, and Google TV through one player contract;
- prevent credentials and credential-bearing playback URLs from entering persistence, logs, analytics, crash reports, or the TYFINO backend;
- keep playback owned by the active account and visible player destination;
- preserve useful movie progress without treating Live TV as resumable content;
- fail safely across provider, network, codec, and device differences.

## 2. Product boundary

### 2.1 In scope

- Live TV playback from a catalog Live item.
- Movie playback from a catalog Movies item.
- Play, pause, seek, mute, volume, and full-screen controls supplied by the player surface.
- Embedded audio-track selection.
- Embedded subtitle-track selection and subtitle-off.
- Automatic continuation of an eligible Movie from its saved position.
- A Continue Watching data source for eligible Movies.
- Previous live channel within the current signed-in application session.
- Playback loading, reconnect, unsupported-format, decoder, provider, and safe fallback error states.
- Touch, keyboard, and TV D-pad operation.

### 2.2 Deferred

- Series playback, seasons, and episodes. A Series catalog item identifies a series, not a playable episode.
- Movie details, trailers, external subtitle URLs, and provider-specific request headers.
- EPG, catch-up, timeshift, recording, and downloads.
- Picture-in-Picture and background video playback.
- Cast, AirPlay, Android Auto, and remote playback.
- DRM and provider-specific license acquisition.
- Software decoder bundles such as FFmpeg, AV1, or VP9 extensions.
- Multi-view, channel mosaics, and simultaneous players.
- Playback history beyond the minimum resume/Continue Watching record.
- M3U and Stalker/MAC playback.

## 3. Approved player stack proposal

The first implementation uses AndroidX Media3 **1.11.0**, with all Media3 modules pinned to the same version:

- `androidx.media3:media3-exoplayer:1.11.0`;
- `androidx.media3:media3-exoplayer-hls:1.11.0`;
- `androidx.media3:media3-ui:1.11.0`.

`PlayerView` is hosted inside the Compose destination. It is selected for the first implementation because it supplies a mature video surface, subtitle rendering, buffering state, and playback controls across touch and TV input. A custom Compose control surface remains deferred until the playback contract is qualified on target devices.

Only one `ExoPlayer` instance may own video playback at a time. The instance is created for the visible playback destination, detached from `PlayerView`, stopped, cleared, and released when that destination is destroyed. TYFINO does not keep video playing in the background in V1.

The screen remains awake only while playback is active.

## 4. Playback identity and ownership

Every playback request is identified by:

- Account ID;
- credential generation;
- section: `LIVE` or `MOVIES`;
- provider item ID;
- a random in-memory playback operation ID;
- the current playback-destination epoch.

Rules:

1. The selected catalog record is not sufficient authority by itself.
2. Immediately before constructing a playback request, the active encrypted Xtream account is loaded again.
3. The Account ID and credential generation must still match the catalog selection owner.
4. Logout, account replacement, a newer item selection, or destination disposal invalidates the previous operation before player state may be published.
5. Cancellation alone is insufficient; every asynchronous prepare, reconnect, track, and resume result validates ownership at commit time.
6. A stale result produces no player error for the new owner and must not start, resume, or alter playback.
7. Only Account-ID-scoped resume records may be queried. Cross-account queries are forbidden.

## 5. Xtream playback references

Playback URIs are constructed only in memory immediately before playback. They are never stored in the catalog database, resume database, saved instance state, navigation route, notification, clipboard, log, analytics event, exception message, or TYFINO backend request.

All path components are encoded as single URI path segments. A provider ID or extension that fails the bounds below is rejected before construction.

### 5.1 Live TV

Baseline form:

```text
{baseUrl}/live/{username}/{password}/{providerItemId}.{extension}
```

The extension is the catalog item's bounded `container_extension` when it matches the safe ASCII token rule. If absent, V1 uses `ts`, which is the baseline form repeated across independent Xtream client implementations. The client does not silently try a different provider endpoint after a failure.

Media3 may identify an HLS playlist by an accepted `m3u8` extension and use the HLS module. Other accepted Live extensions use normal Media3 content-type inference.

### 5.2 Movies

Baseline form:

```text
{baseUrl}/movie/{username}/{password}/{providerItemId}.{containerExtension}
```

A Movie requires a present, valid `container_extension`. TYFINO does not guess `mp4`, `mkv`, or another Movie container when the provider omits it. Such an item remains visible but reports that playback information is unavailable.

### 5.3 Component limits

- Provider item ID: non-empty, at most 256 Unicode code points, encoded as one path segment.
- Container extension: 1–12 lowercase ASCII letters or digits after normalization; no dot, slash, whitespace, percent escape, query, or fragment.
- Base URL, username, password, and HTTP consent come only from the active encrypted account.
- The constructed URI must retain the active account's exact normalized scheme, host, port, and approved base path.

## 6. Transport and redirect policy

- HTTPS is preferred.
- HTTP playback is permitted only when the same active account has stored explicit cleartext consent from the authentication flow.
- There is no HTTPS-to-HTTP downgrade.
- Certificate validation is never bypassed.
- Request URLs, response headers, media manifests, redirects, and errors are treated as sensitive.
- Playback uses bounded connection and read timeouts selected during implementation and verified by tests.
- Media segment retries remain Media3's bounded player behavior; TYFINO does not add an unbounded application retry loop.
- The UI exposes one explicit retry/reconnect action after terminal playback failure.

Some Xtream live and movie endpoints redirect to a media origin. V1 permits at most **three** HTTP redirects for playback only when each target:

1. is an absolute `https` URI, or remains `http` under the active account's explicit cleartext consent;
2. does not downgrade from HTTPS to HTTP;
3. contains no URI user-info or fragment;
4. does not cause TYFINO to forward an authorization header or cookie;
5. is never persisted or logged.

Authentication and catalog requests keep their existing no-redirect policy. The playback redirect exception does not alter them.

## 7. Media types and compatibility

The first implementation supports formats handled by the device's platform decoders through Media3, including progressive media and HLS when identified safely. A file extension is only a format hint and never a guarantee that a device decoder exists.

Rules:

- No software codec extension is bundled in V1.
- A decoder initialization or unsupported-format failure is distinct from a network/provider failure.
- The UI does not claim universal codec support.
- Media3 modules remain version-aligned.
- A provider-specific user agent, cookie, referer, custom header, DRM configuration, or certificate exception requires a separate compatibility decision.

## 8. Player lifecycle

### 8.1 Entry

1. Receive only the safe playback identity, never a URI or credentials, through navigation.
2. Load and validate the active account.
3. Load an eligible Movie resume record, if applicable.
4. Build the secret playback URI in memory.
5. Create one media item and prepare the player.
6. Start playback only while the destination owner remains current.

### 8.2 Foreground and background

- V1 video pauses when the application loses the foreground.
- Playback resumes only through explicit user action after returning.
- No foreground service or media notification is added in this slice.
- Audio must not continue invisibly after the playback destination leaves composition.

### 8.3 Exit

1. Persist the final eligible Movie position.
2. Invalidate the destination epoch.
3. Detach the player surface.
4. Stop and clear the secret media item.
5. Release the player.

Logout first invalidates playback, then stops and clears the player, then removes account-owned resume/catalog state, and finally removes encrypted Xtream credentials. Cleanup failure must not permit stale playback to continue.

## 9. Audio and subtitles

V1 exposes tracks discovered by Media3 from the active media:

- select an available embedded audio track;
- select an available embedded subtitle track;
- disable subtitles;
- preserve Media3/device defaults when the user makes no explicit selection.

Track labels prefer language display name, then provider/media label, then a neutral localized fallback. Track metadata is bounded before display and is not persisted in V1.

External subtitle discovery and storage are deferred because catalog items do not yet provide an approved subtitle-source contract. Media3-supported side-loaded WebVTT, TTML, SubRip, and SSA/ASS may be added only after that contract is approved.

## 10. Movie resume and Continue Watching

Live TV never creates a resume record.

A Movie resume record stores only:

- Account ID;
- section fixed to `MOVIES`;
- provider item ID;
- last position in milliseconds;
- observed duration in milliseconds when known;
- update timestamp.

It must not store the title, artwork URL, username, password, host, playback URI, media manifest, activation data, or raw player error.

### 10.1 Save rules

- Do not save positions below **30 seconds**.
- While playing, checkpoint no more often than every **10 seconds**.
- Save once on pause, foreground loss, item replacement, and player exit when eligible.
- If duration is known and playback reaches **95%** or has less than **60 seconds** remaining, delete the resume record and treat the Movie as completed.
- Invalid, negative, future, or greater-than-duration positions are rejected.
- A database write failure does not stop playback; it produces a credential-free local diagnostic state.

### 10.2 Resume rules

- Offer automatic resume when the saved position is at least 30 seconds and not completed.
- Seek only after the media timeline is ready and ownership still matches.
- Clamp a valid saved position to the current duration when provider metadata changed.
- A stale record from another Account ID is never read or applied.

### 10.3 Continue Watching eligibility

A Movie appears in Continue Watching when:

- its saved position is at least **60 seconds**;
- it is not completed under the 95% / 60-seconds-remaining rule;
- the active catalog still contains the same Account ID + provider item ID.

Continue Watching ordering is most recently updated first. The row itself is implemented after the player and resume repository pass their isolated tests.

## 11. Previous live channel

The player keeps only the immediately previous successfully started Live provider item ID in memory for the current signed-in application session.

- It is not persisted to disk.
- It is cleared on logout or account replacement.
- Selecting Previous swaps the current and previous channel, allowing a second press to return.
- The target is revalidated against the active Account ID and current catalog before a new secret URI is built.
- A failed channel start does not overwrite the last successfully playing pair.

## 12. UI, TV input, and accessibility

- The video surface consumes the available window rather than checking device models.
- System bars and cutouts remain respected outside intentional full-screen mode.
- D-pad center toggles or activates the focused control; Back reveals controls first and then exits according to platform conventions.
- Focus is visible and deterministic on play/pause, seek, audio, subtitles, previous channel, retry, and Back.
- Loading indicators never steal focus.
- Live playback does not show a misleading resumable seek position when the stream is not seekable.
- Movie seek controls are exposed only when Media3 reports seeking is supported.
- TalkBack labels identify the content and each control without exposing provider IDs or URLs.
- RTL mirrors control layout where appropriate but does not reverse media time progression.
- Playback errors include a focusable retry or Back action.

## 13. Safe failure taxonomy

The UI maps detailed causes to bounded credential-free categories:

- active account changed or signed out;
- invalid playback metadata;
- offline;
- connection timeout;
- provider unavailable or access rejected;
- redirect rejected;
- unsupported media format;
- decoder unavailable or failed;
- playback interrupted;
- local resume storage unavailable;
- unknown safe fallback.

Exceptions shown to the user or diagnostics must not include a URI, host, path, query, username, password, redirect target, response body, or manifest excerpt.

## 14. Local persistence

Resume data uses a separate structured Android database abstraction owned only by playback history. All keys begin with Account ID. Replacement and deletion are transactional.

- Logout deletes the active account's resume records or makes them inaccessible before the next UI publication.
- Database corruption or migration failure is contained to the resume database and must not delete licensing state, encrypted Xtream credentials, or catalog metadata.
- Migrations are forward-only and tested. Downgrade is unsupported.
- Playback URI and credentials are forbidden columns and forbidden migration inputs.

## 15. Required verification before implementation merge

### 15.1 Unit tests

- Construct the approved Live and Movie path forms with segment encoding.
- Reject blank/oversized IDs and unsafe/missing extensions.
- Reject HTTPS downgrade, excessive redirects, user-info, fragments, and unapproved HTTP.
- Verify credential-bearing URIs are absent from persistence and safe error objects.
- Verify Account A cannot start, resume, or alter Account B playback.
- Verify logout, account replacement, destination exit, and newer selection reject stale completions.
- Verify resume thresholds, checkpoint cadence, completion deletion, clamping, and invalid positions.
- Verify Live never writes resume data.
- Verify previous-live-channel swap and clear behavior.

### 15.2 Integration and UI tests

- Live HLS and transport-stream samples.
- Progressive Movie sample with seek and resume.
- Embedded multiple-audio and subtitle samples.
- No-track, non-seekable, buffering, offline, timeout, HTTP rejection, redirect rejection, unsupported format, and decoder failure states.
- Player release during rapid Back, navigation, logout, and account replacement.
- Touch on compact windows; D-pad on TV dimensions; RTL; TalkBack labels; configuration change and fold/unfold.
- Physical-device checks for representative phone, Android TV/Google TV, and at least one lower-performance API 24-class device before release qualification.

### 15.3 Performance and resource checks

- One player instance only.
- Catalog scrolling remains responsive before and after playback.
- Player and surface are released with no leaked Activity or network work.
- No unbounded retry loop, playlist growth, queue, or in-memory history.
- Startup performs no player creation until the user selects playable content.

## 16. Implementation sequence after approval

1. Playback identity, URI builder, redirect policy, and ownership gate.
2. Media3 player destination for Live and Movies with safe errors and lifecycle release.
3. Embedded audio/subtitle selection and TV-focused controls.
4. Resume database and repository.
5. Movie resume integration and Continue Watching row.
6. Previous live channel action.
7. Device/emulator qualification and compatibility backlog.

Each step remains an atomic pull request. Series details/episodes, EPG, artwork, search, favorites, broader history, PiP, and final branding stay separate.

## 17. Decisions approved by this contract

Approval makes the following V1 decisions binding:

- Media3 1.11.0 with ExoPlayer, HLS, and PlayerView;
- one foreground player and no background video;
- Live and Movie playback only in the first slice;
- direct device-to-provider playback with in-memory secret URI construction;
- safe bounded playback redirects without HTTPS downgrade;
- Movie-only resume thresholds and Continue Watching eligibility;
- embedded audio/subtitle selection;
- in-memory previous live channel;
- the ownership, persistence, failure, accessibility, and verification rules above.

Any expansion to provider-specific headers, cookies, DRM, external subtitles, software codecs, background/PiP, Series episodes, or alternate Xtream URL forms requires a separate explicit decision.
