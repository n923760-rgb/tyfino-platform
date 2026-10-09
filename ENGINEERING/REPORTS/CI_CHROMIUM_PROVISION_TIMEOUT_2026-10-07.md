# CI Chromium provisioning deadline — 2026-10-07

Task ID: ci-chromium-provision-timeout-20261007
Type: IMPLEMENTATION
Controller/executor: current authorized engineering session.
Repository: n923760-rgb/tyfino-platform
Official base: main@a125ced5337c5304258f7ee81419817de5e8f97a
Base tree: 2c787ef3e92b1d5554f187f17e5ca9cc494e6921
Isolated branch/worktree: fix/ci-chromium-provision-timeout-20261007, tyfino-ci-timeout.

## Authority and live gate

The owner requests continued work, error correction and interface development without repeated pauses. Continuing reviewed-merge authority applies. This is a separate bounded qualification-prerequisite correction, not a Home source change.

Current AGENTS.md, project profile, canonical roadmap, central MASTER_GOVERNANCE.md, CI qualification contract/manifest and actual workflow were read. Official main/default identity and exact base were reverified. Baseline [Validate37664582379](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37664582379) passed all seven jobs,103 cases per device with zero failures/skips and configured APK offline API33 clean-install/launch. #189 was the only open PR and is temporarily closed to preserve one-reviewable-PR sequencing. Its reviewed source/native evidence and branch are retained; expected documentation composition is controlled by this same task controller, not an unknown external conflict.

Execution: local Git/source/shell/JDK17 and authenticated repository connector/Actions. Local Android SDK/emulator/usable Gradle distribution remain unavailable. A transient workspace disconnect during the Home artifact transfer recovered, and its native-qualified development APK was downloaded and hash-verified. No environment failure is converted to PASS.
Stop: unexpected source/conflict outside the two controlled tasks, secrets, missing required evidence, causal failures, qualification bypass or protected scope expansion.

## Evidence and finding

Home source70e9fb867baa0ab873590737f59b7594f4ce59b0, [Validate37665154418](https://github.com/n923760-rgb/tyfino-platform/actions/runs/37665154418): six jobs PASS. Phone112944931608 and tablet112944931466 each ran103 cases with zero failures/skips; exact owner APK offline API33 installation/launch PASS. Admin typecheck/build PASS. Its Install pinned Chromium test tool step started2026-10-07T18:13:40Z and was still in progress after18:49Z. The running-job log download returned BlobNotFound; internal apt/npm/download cause remains UNKNOWN. No old-job runtime failure or diagnostic root cause is invented, and that pending job is not accepted as PASS.

FACT: this setup step has no timeout-minutes. The same job's subsequent browser test already has a5-minute deadline. Official [GitHub workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#jobsjob_idtimeout-minutes) documents a default360-minute job budget and a positive-integer step deadline. The observed setup can therefore hold qualification far longer than the test itself.

## Change and validation

Add only timeout-minutes:15 to the pinned Chromium provisioning step. Keep its exact npm/Playwright1.58.2 command, browser test, all seven job definitions, checkout/source verification, artifact naming, permissions, triggers, concurrency and every product/test file unchanged. No retry, continue-on-error, skipped test or source-policy bypass is introduced. This bounds setup and makes an eventual provisioning failure/log archive observable; it does not claim to repair the unknown network/package cause or change the already-running old workflow.

Required smallest proof: complete three-file scoped diff; exact single-property workflow insertion and unchanged source/tests; whitespace and all six governance validators. Use the same seven-job exact-source regression,103 cases each device and configured APK API33 installation. No new mirror test is added for the declarative property. Actual local outcomes and frozen source/CI identities belong to the task PR.

A slower legitimate install can exceed15 minutes and fail explicitly; that outcome requires complete-log diagnosis and evidence before any unchanged-source retry. Local native execution remains BLOCKED. Physical devices, providers/media, TalkBack, performance and production signing/release/deployment remain NOT RUN/outside scope.

PASS locally: git diff --check; product/source/test directory comparison; parsed old/new YAML structural equality after removing only the15-minute property; all103 existing native cases retained; all six governance validators including the six adversarial checks. Intended staged scope is exactly workflow, roadmap and this report. These are source/governance checks, not changed-source native/runtime results.

After this source qualifies, review/integrate within owner authorization, verify official-main source/tree, then reconcile the retained Home-only change against this baseline, reopen #189 and qualify its new exact source before final integration/delivery. Original pending/cancelled evidence remains historical, not a replacement for new-source qualification. productionReady=false.
