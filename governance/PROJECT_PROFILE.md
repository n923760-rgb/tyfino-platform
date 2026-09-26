# TYFINO Project Governance Profile

Status: ACTIVE — LIVE VERIFICATION REQUIRED PER TASK
Governance baseline: Master Engineering System
Central reference: https://github.com/n923760-rgb/engineering-governance
Canonical roadmap: /ENGINEERING/MASTER_ROADMAP.md

## Project Identity

PROJECT NAME: TYFINO
REPOSITORY: n923760-rgb/tyfino-platform
REPOSITORY URL: https://github.com/n923760-rgb/tyfino-platform
DEFAULT BRANCH: main
OFFICIAL BRANCH: main

## Governing Instructions

REPOSITORY AUTHORITY FILE: AGENTS.md
PROJECT BASELINE: README.md
MASTER ROADMAP: /ENGINEERING/MASTER_ROADMAP.md
MASTER BASELINE REPORT: /ENGINEERING/REPORTS/MASTER_ENGINEERING_BASELINE_REPORT.md
REPORT LOCATION: /ENGINEERING/REPORTS/
EVIDENCE LOCATION: /ENGINEERING/EVIDENCE/

ARCHITECTURE / SECURITY / OWNERSHIP GUIDES:
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
- .github/workflows/device-test-apk.yml — manual device-test APK
- .github/workflows/release-apk.yml — protected signed APK flow

CURRENT PROTECTION MODEL:
- PR-based engineering policy is required by AGENTS.md.
- Live repository protection/ruleset state must be verified before protected decisions.
- Technical ability to write or merge is not authorization.

ENGINEERING ENVIRONMENT:
- governance/ENGINEERING_ENVIRONMENT_CONTRACT.md
- governance/ENGINEERING_ENVIRONMENT_STATUS.md
- governance/engineering-environment.json

TASK / RESULT PROTOCOL:
- governance/TASK_RESULT_PROTOCOL.md
- governance/templates/TASK_PACKET.md
- governance/templates/RESULT_PACKET.md
- governance/task-records/

CONTROLLER: Engineering Controller
EXECUTOR: authorized engineering executor, GitHub Actions, or other task-qualified environment

## Protected Actions

- merge
- release
- tag
- signing
- store publication
- production deployment
- DNS changes
- destructive database migration
- production credential rotation
- destructive production database operations
- server/VPS/cloud destruction or reinstall
- force push / history rewrite
- repository deletion
- permanent release-artifact deletion

Each protected action requires explicit current owner authorization for the exact target and scope.

## Test Strategy

Use the smallest deterministic proof for the changed contract, then the relevant regression surface.

Repository CI currently covers API, Admin, database/backup controls, Android builds, Android unit/lint checks, and managed-device instrumentation. Exact jobs must be re-read from the live workflow before relying on them.

Physical phone, Android TV/Google TV, low-RAM/API-24-class, foldable, real-provider/media, RTL, accessibility, and performance claims remain runtime-evidence gates.

## Security / Privacy Boundaries

- IPTV credentials remain direct Android-to-provider data and must not cross into TYFINO licensing.
- Activation codes and entitlement secrets must not be persisted or logged beyond approved secure storage behavior.
- Signing material and production secrets must never enter repository source, reports, screenshots, or ordinary logs.
- Production deployment remains separately authorized and qualified.

## Project-Specific Stop Conditions

Stop mutation when any of the following occurs:

- unexpected official `main` HEAD;
- wrong repository/branch or ambiguous source identity;
- conflicting task PR or overlapping scope;
- applicable instruction/contract conflict;
- task authority ambiguity;
- secret/signing-material exposure;
- missing required execution capability or evidence;
- Android package/application identity or production licensing-origin change without explicit approval;
- signing/release/deployment/merge without explicit current owner authorization;
- environment incapable of proving the required behavior;
- scope expansion into unrelated Android/backend/admin/infrastructure work.

## Notes

All current-state values are re-verified from live repository/environment evidence when they matter. Historical governance records remain evidence, not current truth.
