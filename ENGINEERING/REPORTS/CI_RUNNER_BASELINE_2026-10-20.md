# Validate runner/toolchain baseline correction

Official source09c1590e3b54330f6c97923b58870691336585f1. Owner-authorized continued fixes/refinement; standing reviewed merge requires all seven exact-source jobs PASS. About PR203/head881c6e73f83dbd6116ea6355d7ef0c5127ed6037 is held closed/unmerged with branch/history preserved pending this independent prerequisite. No Android product code in this correction.

## Attributable failure

Validate39487112700: complete Admin118850274904 log shows successful typecheck/build, then Playwright1.58.2 cannot install Chromium on ubuntu26.04-x64. Complete database118850274434 log shows backup creation, then PostgreSQL16 restore rejects SET transaction_timeout. Expected audit tamper/owner-denial messages are successful safety probes, not the cause. Both ran ubuntu-latest; previous successful official-main38571196794 Admin115876534839/database115876534441 logs identify Ubuntu24.04.5/imageubuntu-24.04. This is runner/toolchain drift, not the Android focus change. Production was not accessed.

## Bounded correction

Select ubuntu-24.04 for all six Validate definitions/seven executions, preserving commands/tests/source attribution/artifacts. Explicitly select existing /usr/lib/postgresql/16/bin clients for the PostgreSQL16 job, require all used executables and print dump/restore versions. Missing tools fail without fallback; do not filter SQL, skip Chromium, change production backup scripts/database service or weaken fail-closed checks. Dependencies, manual APK/signing workflows, permissions and protected environments are unchanged.

The environment manifest/contract mirrors this OS label. It is NOT an immutable runner-image pin, so pinned:false and per-run qualification remain. Strengthen the existing validator to reject mismatched runner selectors across the six Validate definitions. Manual/protected workflows are separately qualified. Current [primary runner inventory](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md), read2026-10-20, documents PostgreSQL16.15; actual corrected-run version and successful restore evidence remain required.

Local full diff/workflow/environment/six-governance checks and negative selector-drift fixture evidence are recorded on the PR. Existing exact-head Actions must pass every job, all136 baseline native cases/device and exact owner APK API33 installation before merge. Chromium scenarios and fail-closed database restore must actually execute. Self-review is not independent review; physical/provider/performance/signing/deployment/publication NOT RUN. Then reconcile/requalify held About203 as a descendant without rewriting published history. No blind retry or gate weakening.
