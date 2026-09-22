# TYFINO CI Qualification

Status: **CI-GATED — QUALIFICATION PENDING**

Governance baseline: Engineering Governance v1.0.0  
Governance rollout phase: Phase 5 — CI Qualification

## Purpose

Phase 5 proves that TYFINO CI is an attributable final-verification surface for the exact source under review.

The qualified CI contract must prove:

- builds execute;
- automated tests execute;
- evidence remains attributable to an exact Git SHA and workflow run;
- uploaded artifacts identify the exact source SHA and run attempt;
- pull-request jobs execute the exact PR head, not an implicit merge ref;
- push jobs execute the exact pushed SHA;
- missing or failed evidence is never converted into PASS.

## Qualified Workflow

Primary validation workflow:

- `.github/workflows/ci.yml` — `Validate`

The protected manual signed-APK workflow remains outside Phase 5 qualification and continues to require separate protected-action authority.

## Exact-Source Rule

Every Validate job must checkout:

```text
github.event.pull_request.head.sha || github.sha
```

Immediately after checkout, every job definition must verify that:

```text
git rev-parse HEAD == expected source SHA
```

The matrix instrumentation definition counts as one job definition and produces the phone/tablet executions from the same exact source contract.

## Evidence / Artifact Attribution

The deployment-config job must emit a small source-attestation artifact containing repository, event, expected SHA, actual checked-out SHA, run ID, run attempt, and ref.

Every uploaded CI artifact must include:

- the exact source SHA; and
- the workflow run attempt

in its artifact name.

This applies to the debug APK, source attestation, and instrumentation failure reports.

## Required Regression Surface

The authoritative Validate run consists of seven jobs:

- `deployment-config`;
- `api`;
- `admin`;
- `database-backup-restore`;
- `android`;
- `android-instrumentation (phone-api-27)`;
- `android-instrumentation (tablet-api-35)`.

Required evidence includes the build/test steps already declared in those jobs, including API tests/build, Admin build, database backup/restore, Android debug/release builds, Android unit tests/lint, and managed-device instrumentation.

## Qualification Boundary

Green CI proves only the automated contracts actually executed by the workflow on the recorded source.

It does not replace:

- physical device qualification;
- production deployment qualification;
- signed release qualification;
- runtime media/provider behavior that is not exercised in CI.

## Qualification Sequence

1. Implement the exact-source and artifact-attribution controls.
2. Run Validate on the exact implementation PR head.
3. Require all seven jobs to PASS.
4. Merge only with current authority and unchanged PR head.
5. Run Validate again on the exact merge commit.
6. Require all seven jobs to PASS.
7. Record both run IDs and exact SHAs in `governance/CI_QUALIFICATION_STATUS.md`.
8. Only then change Phase 5 qualification status to QUALIFIED.
