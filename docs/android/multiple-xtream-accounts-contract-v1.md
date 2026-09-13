# TYFINO Multiple Xtream Accounts Contract v1

Status: **DECIDED — approved by the owner on 2026-09-13; implementation pending**

This contract defines the complete-release account portfolio that follows the
single-account Android foundation. It overrides the one-account replacement and
removal rules in section 15 of
`xtream-authentication-account-ownership-contract-v1.md` only where this
document explicitly differs. All security, transport, bounded-resource, stale
result, and direct Android-to-provider rules remain mandatory.

## 1. Product boundary

- The complete Android release supports up to eight saved Xtream Codes accounts.
- Exactly one saved account may be authoritative and active at a time.
- Users may add, inspect, switch, and remove saved accounts.
- M3U and Stalker/MAC Portal remain outside this contract.
- TYFINO licensing remains independent from IPTV accounts and never receives
  provider credentials, catalog data, viewing activity, or stream references.

## 2. Stable identity and duplicate matching

- Every successfully authenticated account owns a random stable Account ID.
- A provider username, display label, list position, or database row position
  must never be used as the account identity.
- Duplicate matching uses the canonical provider base URL plus the exact,
  case-sensitive username.
- Authenticating a duplicate updates that account's encrypted credentials and
  HTTP consent, increments its generation, preserves its Account ID and
  account-scoped data, and makes it active.
- Authenticating a new pair creates a new Account ID and makes it active only
  after authentication and encrypted persistence both succeed.
- A failed authentication never adds, updates, removes, or activates an account.

## 3. Bounded portfolio

- The portfolio contains at most eight accounts.
- Adding a ninth distinct account is rejected before persistence and does not
  remove an existing account.
- Accounts are ordered by most recent successful activation or credential
  update. Ordering is presentation metadata and not identity.
- No background provider authentication, catalog refresh, EPG refresh, artwork
  loading, or playback preparation runs for an inactive account.

## 4. Account presentation

- The switcher identifies an account using its safe provider origin and
  username already stored in the encrypted account payload.
- Passwords, secret-bearing request URLs, stream URLs, and provider response
  tokens are never displayed.
- Custom account labels are deferred until a demonstrated usability need
  justifies another persisted field.
- The active account is exposed with a selected semantic for touch, keyboard,
  TalkBack, and TV D-pad users.

## 5. Authoritative switching

- Switching is an explicit user action and requires no provider request.
- The outgoing account becomes unable to commit new state before the incoming
  account is exposed as active.
- Pending catalog, search, favorites, history, resume, EPG, series-details, and
  playback work for the outgoing account is cancelled when practical and must
  still fail commit-time ownership checks.
- The switch transaction persists the new active Account ID atomically with the
  encrypted portfolio before UI navigation commits.
- A persistence failure leaves the previous account active and reports a
  bounded local-storage failure.
- Switching clears or recreates in-memory destination state. Account A content
  must never appear under Account B identity, including during recomposition or
  process restoration.

## 6. Removal

- Removal requires explicit confirmation and targets one exact Account ID.
- Removing an inactive account leaves the current active account unchanged.
- Removing the active account synchronously invalidates its ownership before
  credential or account-scoped data deletion begins.
- No remaining account is selected automatically after active-account removal;
  the account chooser is shown and the user selects the next account.
- Removal deletes only the target account's encrypted credentials, catalog,
  favorites, history, resume, Continue Watching, EPG, and other implemented
  account-scoped state.
- Removal is not undoable. Re-adding the same provider and username requires
  authentication and receives a new Account ID.

## 7. Encrypted persistence and migration

- All account credentials and the active Account ID are stored in one
  versioned AES-GCM payload protected by the existing non-exportable Android
  Keystore key and backup exclusion.
- The payload contains no catalog, history, resume, EPG, or playback data.
- On first successful portfolio read, a valid legacy single-account payload is
  migrated without changing its Account ID, generation, endpoint, username,
  password, or HTTP consent.
- Migration writes and verifies the new payload before removing the legacy
  value. An interrupted migration must leave at least one valid representation.
- Invalid authenticated decryption, invalid schema, duplicate Account IDs,
  duplicate canonical endpoint/username pairs, an unknown active Account ID, or
  an over-limit portfolio fails closed and requires sign-in again.
- No plaintext recovery copy or credential log is permitted.

## 8. UI flow

- A signed-in user can open the account switcher from the top-level application
  shell and Settings using touch, keyboard, or TV D-pad.
- The switcher lists the active account first, then inactive accounts by recent
  use, and exposes Add account and Manage accounts actions.
- Add account reuses the existing Host, Username, Password, and explicit HTTP
  consent flow.
- Back from Add account preserves the current active account and returns to the
  switcher.
- A successful add or duplicate credential update activates the authenticated
  account and returns to the primary catalog destination.
- Destructive removal remains in account management, not the quick switch list.

## 9. Performance and resource rules

- Portfolio decryption is bounded to the eight-account payload and must not
  initiate network or database work.
- Switching must not scan or delete other accounts' SQLite partitions.
- Only the active account's visible destination loads data.
- Stored account summaries used by the switcher are derived from the decrypted
  bounded portfolio; credentials are not copied into a second plaintext store.

## 10. Required automated coverage

Before the portfolio implementation qualifies:

- a legacy single account migrates without identity or credential changes;
- save, reload, and active selection preserve all accepted accounts;
- a ninth distinct account is rejected without eviction;
- a duplicate updates in place and preserves Account ID and scoped data;
- a failed add preserves the active account and portfolio;
- switching invalidates an outgoing asynchronous owner before UI commit;
- process restoration returns only the persisted active account;
- inactive-account removal preserves the active account;
- active-account removal leaves no automatic selection;
- exact-account cleanup never deletes another partition;
- malformed, duplicated, over-limit, or undecryptable payloads fail closed;
- switcher selection, focus, RTL, and TalkBack semantics pass instrumentation.

## 11. Physical qualification

Phone, tablet, foldable, Android TV/Google TV, RTL, TalkBack, lifecycle, and
low-memory qualification must cover adding, switching, restoring, and removing
accounts. Automated emulator success does not replace physical-device evidence.

## 12. Deferred

- More than eight saved accounts.
- Custom labels, folders, profiles, or household permissions.
- Account import/export, cloud sync, backup transfer, or sharing.
- Automatic fallback to another account after removal or provider failure.
- Background refresh of inactive accounts.
- M3U and Stalker/MAC Portal accounts.

