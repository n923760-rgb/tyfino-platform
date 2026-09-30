# TYFINO project source review and first-use expiry correction

Date: 2026-09-30 (UTC)
Task ID: `2026-09-30-first-use-expiry`
Repository: `n923760-rgb/tyfino-platform`
Official branch / inspected source: `main` / `ef420e7e9c0d968f271ada14bce1ab3076a655c7`
Task branch: `fix/licensing-first-use-expiry-2026-09-30`

## Scope and evidence boundary

The owner requested review and improvement of the entire project. This round inspected the repository tree, instructions, architecture/security/data ownership and licensing contracts, Android composition/manifest/account storage/catalog/background notifications/playback configuration and lifecycle, licensing/Admin request handlers, Admin UI/API/components, build manifests, CI, and deployment configuration. This is a targeted cross-project source review, not an exhaustive audit of every file or runtime qualification.

Execution mode: authenticated GitHub repository API/MCP, JavaScript orchestration, and the existing GitHub Actions validation environment. No local shell, Node dependency installation, PostgreSQL, Android SDK, browser automation, physical devices or production access are available in this session. Source preparation and small deterministic checks execute in session memory; CI is the executor for repository build/integration validation.

Live facts at inspection:
- The default and official branch is `main`; no open PRs were returned.
- PRs #159, #164 and #165 have been merged. The old roadmap checkpoint naming #159 as active is historical.
- Validate [run 36665625417](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36665625417) succeeded on the inspected source.
- Device-test APK [run 36666243511](https://github.com/n923760-rgb/tyfino-platform/actions/runs/36666243511) succeeded on the same source. This does not establish signed-release or physical-device qualification.

## Confirmed problem and correction

FACT: `apps/api/src/routes/licensing.ts` rejects an activation whenever `pre_activation_expires_at <= now`, even when `activated_at` is already populated. Thus a valid one-year or lifetime grant can be rejected on same-installation reactivation, and a replacement device can fail after an authorized reset. Refresh has a separate grant-expiry check; it does not use this first-use deadline.

Requirement evidence: `docs/api.md` §3.3 scopes pre-activation expiry to an unused code; §5.3 requires reset to permit replacement without changing paid duration. The Admin creation field explicitly states that the optional deadline has no effect after activation.

The correction requires `!activation.activated_at` before rejecting on the first-use deadline. The persisted activation timestamp is preserved by existing activation/reset logic. Revocation, device binding and paid grant expiry remain independently enforced. No schema change is necessary.

Regression coverage extends the existing PostgreSQL/Fastify integration journey:
- expired unused code is rejected;
- a code activated before its deadline can reactivate on the same installation after that deadline;
- one-year replacement after reset preserves original grant start and expiry;
- lifetime reactivation and replacement preserve original start and null expiry;
- revoked lifetime codes and expired paid grants remain rejected;
- the existing second-device limit remains checked after the expired first-use deadline.

Only this confirmed behavior is remediated in this PR.

## Remaining findings and improvement order

1. **FACT / high priority — Admin business writes and audit insertion are not atomic.** In `apps/api/src/routes/admin.ts`, code creation commits through `db.query` before `writeAudit`; settings updates do the same. If audit insertion fails, the mutation persists despite a failed response. For creation, the full code is never returned and cannot subsequently be retrieved. Follow-up: one transaction per business mutation and its audit event, with injected audit-failure rollback tests. Database trigger protection alone does not join these separate statements into one transaction.
2. **FACT / medium priority — Clipboard success is reported before success is known.** `apps/admin/src/pages.tsx` invokes `void navigator.clipboard.writeText(createdCode)` and immediately sends the success toast. A rejected clipboard promise can show a false success and becomes unhandled. Follow-up: await the write, handle unavailable/denied Clipboard API, and keep the one-time code visible with manual-copy guidance.
3. **FACT / medium priority — Dashboard counts describe a bounded slice.** The API returns at most 500 newest codes, and Dashboard computes totals from that list. It also counts persisted `active` status without considering `grantExpiresAt`, so expired one-year grants may be counted as active. Follow-up: define server-side aggregate license-state semantics and qualify them with more than 500 records and expired grants.
4. **FACT / accessibility source gap — Admin modal has no keyboard containment/restoration.** `apps/admin/src/components.tsx` focuses Close and handles Escape but implements no Tab containment, background inertness or trigger-focus restoration. Its effect also depends on frequently recreated `onClose` callbacks. Browser keyboard/screen-reader evidence is NOT RUN. Follow-up: bounded modal accessibility change and browser validation.
5. **INFERENCE / Android diagnosis needed — Concurrent foreground/background catalog ownership.** `TyfinoApp.kt` and `NewContentCheckJob.kt` construct separate repository/store instances. Catalog operation counters and mutexes are per repository instance, so their checks do not themselves coordinate simultaneous refreshes across instances. Determine whether store commit rules prevent older same-account results from replacing newer data, and test account removal/switch during a background check. A race has not been reproduced; no speculative Android patch is included.
6. **FACT / qualification gap — Admin CI checks types/build, not browser behavior.** The Admin package has no test script, and its CI job runs typecheck/build only. API integration coverage uses PostgreSQL; Android has unit and managed-device checks. Physical TV/Google TV, API-24/low-memory, foldable, real-provider/media, RTL/TalkBack and performance evidence remain separate gates under `docs/android/device-qualification-v1.md`.

Production host state, backup freshness, signing custody/recovery and production runtime were not inspected. They remain UNKNOWN rather than inferred from repository or CI results.

## Validation and result packet

- PASS — seven-case JavaScript truth table using the condition extracted from inspected source and the proposed condition. The old condition rejects an already activated grant after its first-use deadline; the correction passes all cases.
- PASS — eight-case JavaScript execution of the actual activation-handler body extracted from source, removing only the TypeScript query generic, with fake database/licensing dependencies. Cases cover unused past/future deadlines, one-year reactivation, one-year/lifetime replacement, device limit, revocation and paid expiry. Existing handler fails the activated cases; corrected handler returns expected statuses and commits/rolls back appropriately. This is isolated handler proof, not Fastify/PostgreSQL integration evidence.
- NOT RUN locally — `npm run typecheck`, `npm test`, `npm run build`; this session has no shell/Node/PostgreSQL runtime. The existing Validate workflow will execute these on the exact PR head. The PR's current validation status and run links are the result record for CI.
- NOT RUN — physical-device/provider/media, browser/keyboard/accessibility or production tests.

Changed files: activation predicate, existing API integration tests, this report, canonical roadmap. Review the complete API compare/diff before PR creation. No dependency, workflow, schema, Android identity, signing, production origin, deployment or protected action changes are included.

Protected merge/sign/release/deploy actions have no authorization in this round. The recommended next engineering correction is transactional Admin creation/settings auditing after this isolated licensing correction is reviewed.
