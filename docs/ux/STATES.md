# TYFINO UI States and Recovery Guidance

Status: CURRENT-STATE MAP WITH PROPOSED IMPROVEMENTS — no UI behavior changed
Planning baseline: `main@cd83a1c23819ae972185c748e7487156890a7dbf` (2026-09-27)

The current Android strings in `apps/android/app/src/main/res/values[-ar]/strings.xml` and screen implementations are the source for shipped wording. This table is a review aid, not a substitute for resource files or the Android contracts. User-facing error text must be short, actionable, localized, and must not expose provider IDs, credentials, Activation Codes, or internal exception details.

## State map

| Journey | Current state and recovery | Qualification or follow-up |
| --- | --- | --- |
| Licensing check/activation | Progress text and indicator; explicit Retry or Activate; distinct invalid code, used trial, device limit, expired and revoked wording | Test offline/reboot/clock and TalkBack live region on physical device |
| Xtream sign-in | Progress, validation, HTTP risk confirmation, error and retry paths; authentication does not wait for catalogs | Check invalid credentials versus unreachable provider, safe focus and no secret echo |
| Live/Movie/Series catalog | Loading indicator, stale/failure and Retry, localized empty content and search/favorites/recent-history empty states | Search remains local to downloaded categories; do not imply global provider search |
| Movie/Series details | Loading, metadata fallback, malformed/unsupported/error states and action to refresh or return | Preserve account ownership and focus after failure |
| Live EPG | Loading, empty, stale/error and refresh/dismiss; playback remains independent of EPG | Verify current/next on real providers and D-pad schedule navigation |
| Playback | Bounded prepare/failure states, account-change and unsafe metadata errors, explicit control actions | Verify offline/unsupported decoder, resource release and physical controls |
| Home | Existing shelves appear only with available active-account items; empty Home offers a discovery hint | No full-catalog preload to fill the page |

The owner-supplied example strings (for example, "تعذر الاتصال. تحقق من الشبكة.") are copy proposals; use the current localized resource and the error's actual meaning until a separate UI change reviews context, action, Arabic/English parity, accessibility, and tests. Do not collapse distinct licensing revocation, trial expiry, provider authentication, and playback failures into one generic network message.

## Loading, offline, and focus

Current catalog, licensing, account, EPG and detail screens use progress indicators plus text; artwork cards retain a local placeholder. A blanket requirement to replace every spinner with a skeleton is **not implemented or approved**. Evaluate any skeleton per journey for layout stability, accessibility announcements, D-pad continuity, startup and low-memory cost. Local cached search should not display a network-loading state for a local query.

The repository handles bounded network failures and offline licensing grace according to the contract. A persistent app-wide offline banner and a `NetworkMonitor` component were proposed in the supplied review; they are **not asserted as implemented** here. Any new banner needs a reliable signal and must not imply an external provider is offline merely because the licensing API is unreachable. Playback cannot be promised to continue offline when its stream requires a network connection.

Visible focus, predictable traversal, Back, and restoration are required for TV/Google TV. Managed-device tests exercise some first-focus targets, but physical D-pad qualification remains BLOCKED in [`docs/android/device-qualification-v1.md`](../android/device-qualification-v1.md). The tablet emulator's `SettingsScreenTest.accountSwitcherReceivesInitialDpadFocus` failed on first attempts in Validate #439 and #441 and passed on rerun; diagnose stability in a separate Android task. Avoid changing focus behavior as a documentation side effect.

Record UI findings with exact SHA, APK variant, device/input, language/font scale, steps, sanitized screenshot/video and result in the [manual test record](../android/manual-test-record-template.md). PR #159, if merged, changes Movies/Series sort and episode-history presentation; retest those states against its exact source before updating this current-state map.
