# Bind signed-build preparation to reviewed official source

Task `2026-10-22-bind-release-source`, IMPLEMENTATION, based on official `main@43010dee7f0492bfd84a71d1dddba490285bb229`. Owner requested continued development and readiness. Preparation, isolated PR and existing reviewed-merge authority apply. Signing, dispatch of the signing workflow, release, tags, publication and production deployment are excluded. Package/version/key/endpoint/ABI policies stay unchanged.

## Live state and confirmed gap

PR206 merged at this main source. Its tree `6a4fc0c3d65421836029c0c196e322355e594e2d` equals qualified head2230a7a2518492e08dbbe2c7248003b083f78e79. Validate39517407781 passed seven jobs; full phone/tablet logs showed138 cases each,0 failures/skips, and exact owner APK API33 install/launch passed. Separate main-push query returned no runs at this checkpoint; no main CI PASS is claimed. No open PRs were returned before this task. Local source/Git and Python/Bash are available; Android SDK/emulator and physical/production runtime are unavailable.

FACT: the existing manual signed workflow used implicit checkout with no official-ref or expected-source check. It uploaded APK/hash/certificate but no checkout SHA inside the bundle. This permits selecting another workflow ref without an explicit source-rejection gate; no incorrect signing incident is asserted.

## Bounded change

Require full lowercase40-character owner-reviewed SHA input, explicit event-source checkout and a pre-key-materialization guard. It rejects wrong repository, nonmanual event, non-main ref, malformed expected SHA, workflow/checkout mismatch, tracked source edits, fresh-main mismatch and fetch failure. Official main is checked at preflight time; later main development does not change the checked-out build source. The output bundle adds `source-sha.txt`. Existing signing/certificate/cleanup/artifact behavior remains.

The gate executes code from the dispatched workflow source and is not an independent security boundary against a person allowed to rewrite that workflow. Repository/environment access controls remain separately required. A passing source gate proves no CI/device/provider/custody/upgrade/production qualification and grants no signing authorization.

## Validation

PASS: `python3 apps/android/scripts/test-release-source.py` —10 tests against real disposable local Git origins/checkouts, including remote main advancing while local origin/main stays stale and unavailable origin with cached state. No SDK, signing key or network required. The same suite runs in existing deployment-config Validate job.

PASS: Bash syntax and `git diff --check`. Exact-head workflow validation and remaining CI results belong to the task PR; local fixture success is not a successful signed workflow. No protected signing workflow was dispatched; end-to-end signing is NOT RUN because it requires separate exact-source owner authorization and release prerequisites. Physical phone/TV/provider/TalkBack/performance and current production operations remain NOT RUN/UNKNOWN. Key backup/recovery evidence remains owner-controlled.

## Next action

Review complete task diff and exact-head Validate. Preserve source attribution, then continue physical owner qualification and live operations evidence before a separately authorized exact-source signed build. Do not add unrelated features or infer a release percentage from passing fixtures.
