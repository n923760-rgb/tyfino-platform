# Offline Android TV owner bundle preflight — 2026-11-18

## Task and authority

- Task: TYFINO-20261118-physical-tv-owner-bundle-preflight; IMPLEMENTATION of the next bounded TV-qualification preparation step after owner continuation request “تمام نكمل”.
- Repository/official branch: `n923760-rgb/tyfino-platform`, `main`.
- Independently re-read official starting source: `d240642b4943ba9bb0f63205cffecbca94d0b29f` (tree `97fc46774b23aec9cad2d249ac51fc43d9a4beb8`).
- Initial open PR: #232 only, draft/release evidence document on a separate path. Physical-TV test issue: #234.
- Isolated task branch: `feat/tv-bundle-preflight-20261118`.
- Work discipline: one coherent ZIP preflight utility/test/documentation/CI change. No Android UI/player, licensing API, database, signing, release workflow, key or deployment change; no protected merge authorized for this new PR.

## Finding and bounded design

The official source has 7/7 exact-main Validate PASS, 172 managed phone tests and 172 managed tablet tests, and offline API33 owner APK launch PASS. Real TV acceptance remains BLOCKED. The owner needs a safe way to select the correct CI development ZIP and generate a new local manual-TV evidence worksheet without silently promoting old/wrong APK bytes.

The tool `apps/android/scripts/tv-bundle-preflight.py` reads the **downloaded artifact ZIP without extracting it**, rejects missing/duplicate/unexpected entries, unsafe member paths or links, oversized members, bad metadata, source mismatch, invalid development package policy, altered checksums and mismatches against **two independently sourced values**: full official SHA and APK SHA-256 from matching CI. It never accepts provider account details, logs IPTV data, calls a network, invokes adb, installs an APK, or reads signing secrets. It can create a 0600 local worksheet with all physical scenarios initially BLOCKED. It refuses to overwrite the worksheet.

This is a consistency preflight, NOT a cryptographic signing verification. Independently authenticate the actual artifact/source reference from the trusted CI, and use the release's protected verifier for actual APK signature verification. The physical TV user must execute/record each scenario.

## Files and acceptance

1. `apps/android/scripts/tv-bundle-preflight.py`: minimal stdlib ZIP/digest/metadata and optional local worksheet.
2. `apps/android/scripts/test-tv-bundle-preflight.py`: synthetic-only adversarial tests including matching archive, wrong source, wrong trusted digest, altered APK and package, TV launcher mismatch, traversal/duplicates, symlink, existing record and private output permissions.
3. `docs/android/physical-tv-preflight.md`: exact CI source/artifact example and owner remote-only manual testing instructions.
4. `.github/workflows/ci.yml`: one stdlib test step under existing deployment-config job.
5. This evidence report and the canonical roadmap entry.

Acceptance requires the isolated PR CI to complete unchanged baseline seven jobs and pass the new test step, with exact task head recorded. No synthetic test qualifies a physical TV.

## Validation ledger

- PASS: initial official-main source and PR-conflict inspection.
- PASS: prior official-main Validate `44262185211`, phone+tablet 172/0/0, configured owner APK API33 launch (baseline only).
- PASS: local prototype of equivalent offline bundle checks executed 10 synthetic accept/reject cases, 0 failures, before repository mutation. Exact pushed source remains subject to CI.
- PENDING: exact PR-head `Validate` seven-job verification and synthetic test step.
- NOT RUN: physical TV hardware, OEM/codec/real provider, TalkBack, production signing or key recovery.
- BLOCKED: commercial release until separate custody, runtime and operations gates.

No secret-bearing data, credentials or production APK is embedded in the repository. The fixed example hash is public *development* CI evidence, not a production signing value.

## Next step

Inspect the PR run. If and only if its source-bound CI passes, review the full diff and retain it for owner-approved merge. Then run the worksheet on an actual owner-controlled Android TV/Google TV and record sanitized evidence under issue #234. Preserve release draft #232 until source identity and customer-release gates are resolved.