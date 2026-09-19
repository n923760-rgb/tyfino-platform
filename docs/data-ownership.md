# TYFINO Data Ownership Baseline

Status: **APPROVED**

Approved: 2026-09-19

This document defines the authoritative owner and prohibited destinations for TYFINO data classes. It supplements `docs/architecture.md` and does not replace scoped lifecycle contracts.

| Data class | Authority / storage | Permitted flow | Prohibited flow |
| --- | --- | --- | --- |
| IPTV Host, Username, Password | Android protected local storage | Android ↔ user's provider | TYFINO API, admin UI, TYFINO database, logs, analytics, backups |
| Provider catalog, EPG, stream URLs | Android bounded local state/cache | User's provider → Android | TYFINO API, admin UI, licensing database |
| Favorites, history, resume | Android account-scoped local storage | Local Android features | TYFINO services and cross-account reads |
| Saved IPTV accounts | Android account portfolio | Explicit local add/switch/remove | TYFINO services and cloud backup |
| Installation identity | Android protected storage; hashed/minimized licensing record | Android ↔ licensing API | IPTV provider identity, routine admin display, ordinary logs |
| Trial and entitlement | Licensing API and PostgreSQL | Licensing API → owning Android installation | Provider-dependent decisions and client-clock authority |
| Activation Code | Shown once by Admin; HMAC digest plus suffix in PostgreSQL | Admin creation → customer → activation endpoint | Plaintext persistence, ordinary logs, URL/query string |
| Licensing session token | Android protected storage; digest in PostgreSQL | Android ↔ licensing API | Admin display, logs, provider requests |
| Admin customer metadata | PostgreSQL and authorized Admin UI | Authorized administrative workflows | Android responses and IPTV provider flows |
| Audit events | PostgreSQL append-oriented audit storage | Authorized Admin read | Secrets, full codes/tokens, mutable business authority |

## Ownership rules

- Every stored row or local record has one authoritative owner.
- Account-scoped Android data keys include stable Account ID plus content type and provider content ID where applicable.
- A stale asynchronous result cannot commit merely because its coroutine or request was not cancelled; it must prove current ownership at commit time.
- Removing one IPTV account clears only that account's owned local partitions and never affects another account.
- Resetting a paid-device binding is a licensing operation. It does not delete or modify Android IPTV accounts.
- Revoking or expiring an application entitlement does not alter the user's external IPTV subscription.
- Database backups and support exports follow the same data boundary as the primary store; they do not justify collecting prohibited data.

## Retention boundary

Implemented bounded retention rules are documented by their scoped contracts and tests. Exact production retention periods for administrative metadata, audit history, and deleted licensing identities remain **OPEN** and must be approved before production launch.
