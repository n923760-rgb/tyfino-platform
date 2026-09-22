# TYFINO Project Governance Profile

Status: QUALIFIED
Governance baseline: Engineering Governance v1.0.0
Qualification record: governance/ADOPTION_STATUS.md
Repository foundation record: governance/REPOSITORY_FOUNDATION_STATUS.md

## Project Identity

PROJECT NAME: TYFINO
REPOSITORY: n923760-rgb/tyfino-platform
OFFICIAL BRANCH: main

## Governing Instructions

REPOSITORY INSTRUCTION FILE: AGENTS.md
PROJECT BASELINE: README.md
ENGINEERING GUIDES:
- docs/architecture.md
- docs/data-ownership.md
- docs/security.md
- apps/android/README.md

SUBSYSTEM CONTRACTS:
- docs/licensing-trial-contract-v1.md
- docs/android/catalog-synchronization-contract-v1.md
- docs/android/device-qualification-v1.md
- docs/android/direct-apk-release-v1.md
- docs/android/live-epg-contract-v1.md
- docs/android/multiple-xtream-accounts-contract-v1.md
- docs/android/playback-contract-v1.md
- docs/android/series-episodes-contract-v1.md
- docs/android/xtream-authentication-account-ownership-contract-v1.md

## Engineering Systems

CI SYSTEM:
- .github/workflows/ci.yml — Validate
- .github/workflows/release-apk.yml — manual protected signed APK build

REPOSITORY FOUNDATION:
- governance/REPOSITORY_FOUNDATION_STATUS.md — Phase 1 repository-foundation qualification and external protection gate
- .github/CODEOWNERS — repository ownership
- .github/pull_request_template.md — atomic task/evidence PR discipline

TASK / RESULT PROTOCOL:
- governance/TASK_RESULT_PROTOCOL.md — Phase 3 packet rules, storage, evidence, and qualification boundary
- governance/TASK_RESULT_PROTOCOL_STATUS.md — exact Phase 3 qualification evidence
- governance/templates/TASK_PACKET.md — standard atomic work order
- governance/templates/RESULT_PACKET.md — standard attributable execution result
- governance/task-result-protocol.json — machine-readable protocol manifest
- governance/scripts/validate-task-result-protocol.sh — CI drift/structure validation

AI EXECUTOR QUALIFICATION:
- governance/AI_EXECUTOR_QUALIFICATION.md — Phase 4 behavioral requirements and qualification boundary
- governance/ai-executor-qualification.json — machine-readable Phase 4 manifest
- governance/scripts/evaluate-executor-policy.py — deterministic fail-closed policy evaluator
- governance/scripts/validate-ai-executor-qualification.sh — safe fixture validation
- governance/executor-qualification/fixtures/ — synthetic non-production qualification fixtures

CONTROLLER: Engineering Controller
EXECUTOR: Engineering Executor / authorized GitHub Actions or local execution environment

## Protected Actions

PROTECTED ACTIONS:
- merge
- release
- tag
- signing
- production_deploy
- destructive_database_migration
- production_credential_rotation
- server_destruction_or_reinstall
- force_push
- history_rewrite
- repository_deletion
- permanent_release_artifact_deletion

Technical access does not grant protected-action authority.

## Test Strategy

Use the smallest deterministic test that proves the changed contract, then the relevant regression surface.

Current repository CI includes:
- deployment configuration validation;
- API type-check, tests, and build;
- restricted PostgreSQL role/migration checks;
- admin type-check and build;
- database backup/restore verification;
- Android debug and optimized unsigned release builds;
- Android unit tests;
- Android lint;
- managed-device instrumentation on phone API 27 and tablet API 35.

Physical phone, Android TV/Google TV, API-24-class, media, RTL, accessibility, and performance qualification remain separate evidence under the Android device qualification contract.

## Evidence

EVIDENCE LOCATION: task-specific CI runs, reports, logs, runtime records, checksums, and approved artifacts.
REPORT LOCATION: task Result Packet or PR evidence.

Never classify BLOCKED, SKIPPED, or NOT RUN as PASS.
Never reuse the historical adoption SHA as a future Task Packet expected HEAD; fetch current live state.

## Deployment Boundary

Repository source and CI evidence do not authorize production deployment.

Production deployment remains governed by `docs/deployment.md`, including its explicit entry gate, backup/restore, secret, monitoring, rollback, database-role, signing, endpoint, and environment requirements.

## Project-Specific Stop Conditions

Stop mutation when any of the following occurs:
- unexpected official `main` HEAD;
- wrong repository or branch;
- dirty canonical source when a clean base is required;
- conflicting active PR for the task branch;
- applicable instruction or subsystem-contract conflict;
- task authority ambiguity;
- secret or signing-material exposure;
- Android package/application identity change without explicit approval;
- production licensing endpoint change without explicit approval;
- signing/release/deployment action without explicit owner authorization;
- missing required evidence;
- environment or runner incapable of proving the required behavior;
- task expands into unrelated product/backend/Android work.
