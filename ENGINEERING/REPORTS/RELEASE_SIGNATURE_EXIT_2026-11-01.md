# Fail closed when APK signature verification fails

Task: TYFINO-20261101-signature-verification-exit — IMPLEMENTATION.
Controller/executor: Codex, sequential implementation and self-review.
Authority: owner requests continued TYFINO readiness work; standing reviewed-merge authorization applies after successful requirements. Signing, release, publication and deployment are excluded.

## Source and scope

Verified repository/default branch: n923760-rgb/tyfino-platform, main.
Official base: 2907d2360b13afd3d4af86d2cb46ec783903f5a9; local HEAD and freshly fetched origin/main match. No open/conflicting PRs at the initial gate.
Task branch: fix/release-signature-failure-20261101.
Scope: the existing release signature-verification run block, no-key regression coverage in the existing release preflight suite, this report and canonical roadmap. No app identity, signing key/model, endpoint, version, dependency or runtime behavior changes.
Stop for source drift, conflict, secrets, scope expansion or failed required checks pending diagnosis.

## Diagnosis and correction

The protected workflow pipes apksigner output through tee. Its unspecified Linux shell uses bash -e without automatic pipefail; a failed verifier can be hidden by successful tee. If output contains the configured certificate digest, the later certificate comparison can succeed and emit an owner-review APK despite verification failure. This is a verified control-flow defect, not evidence of a previously invalid customer APK.

The regression executes the actual workflow verification block in a disposable real Git repository, with a synthetic APK and a fake verifier that reports a matching synthetic certificate but exits 7. Before correction: 14 tests, 1 failure; the failed assertion establishes that the production block returned success. The other 13 source/verification controls passed.

Add set -euo pipefail to the existing verification block. A failed apksigner or tee now stops before APK/checksum/source files are emitted. Existing certificate comparison and success outputs remain unchanged. Tests use no signing keys, Android SDK, network, installation or real signing operation.

## Validation and limits

PASS local: all 14 release guard tests, including verifier-failure rejection and exact success APK/checksum/source, wrong-certificate and missing-certificate controls; git diff --check.
PASS local: engineering-environment, Task/Result protocol, AI executor policy, behavioral-trial, CI qualification and governance adversarial validators. Full four-file self-review found no signing material or unrelated changes.
Exact-source seven-job hosted Validate is required on the PR and recorded there. Local Android build/emulator execution is BLOCKED by unavailable toolchains. Actual production apksigner/keystore execution, physical device/provider acceptance, signing, release and deployment are NOT RUN.

Source reference: GitHub workflow shell semantics, https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax (jobs.<job_id>.steps.shell). Fixture execution establishes the failure propagation only; it does not qualify cryptographic verification of a real APK or signing-key custody.
