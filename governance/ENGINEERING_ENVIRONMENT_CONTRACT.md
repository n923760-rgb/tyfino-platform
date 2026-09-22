# TYFINO Engineering Environment Contract

Status: PARTIALLY QUALIFIED — repository-declared build/test environment is source-controlled and drift-gated; external execution environments must still be verified per task.

## Qualification Boundary

TYFINO separates engineering-environment qualification into two layers.

### Repository-declared environment

The reproducible source declaration is owned by:

- `governance/engineering-environment.json`;
- `.nvmrc`;
- `.java-version`;
- Node package lockfiles;
- the Android Gradle Wrapper and its distribution SHA-256;
- `docker-compose.yml`;
- `.github/workflows/ci.yml`;
- database backup/restore scripts.

The drift gate is:

- `governance/scripts/validate-engineering-environment.sh`;
- executed by the existing `deployment-config` CI job on pull requests and `main`.

The gate must fail when source-controlled environment declarations disagree. A future toolchain change must update the manifest and every authoritative source together.

Status: **SOURCE-CONTROLLED / CI-GATED**

### External execution environment

A GitHub-hosted runner, development VPS, staging host, or production host is not qualified merely because repository declarations are correct.

Its identity, actual installed/runtime versions, resources, secret mechanism, backup state, and source SHA remain execution evidence.

Status: **PER-TASK QUALIFICATION REQUIRED**

## Local Toolchain Selectors

For supported local work:

- Node.js major: `22`, selected by `.nvmrc`;
- Java major: `17`, selected by `.java-version`;
- Gradle: `9.6.0`, through the committed Gradle Wrapper;
- Gradle distribution SHA-256: `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`;
- PostgreSQL integration baseline: `postgres:16-alpine`, normally supplied through Docker/CI rather than a required host installation.

The Node package manifests require Node `>=22`. API/Admin dependency resolution is locked with committed `package-lock.json` files.

## GitHub Actions Validation Environment

Current repository workflows use GitHub-hosted `ubuntu-latest` runners.

Validated repository requirements:

- Node.js 22 for API and Admin;
- PostgreSQL 16 Alpine service for database/API validation;
- Temurin JDK 17 for Android;
- Gradle 9.6.0 through the committed wrapper;
- Android SDK/emulator with KVM acceleration for managed-device instrumentation;
- Docker Compose and `jq` for deployment/environment validation.

The runner image selector `ubuntu-latest` is intentionally **not** treated as a pinned operating-system image. Runner image/version details are execution evidence and must be taken from the actual CI run.

## Android Build / Device Baseline

Source-declared Android platform baseline:

- `minSdk = 24`;
- `compileSdk = 37`;
- `targetSdk = 37`.

Managed-device CI currently qualifies:

- `phone-api-27`;
- `tablet-api-35`.

These managed devices do not replace the separate physical-device qualification contract.

## Health Checks

The reproducible service definition includes:

- PostgreSQL readiness through `pg_isready`;
- API readiness through `/readyz`;
- dependency ordering that waits for database/API health where applicable.

A static declaration is not runtime proof. Each runtime/deployment task must capture live health evidence.

## Backup / Restore

Repository-owned backup evidence includes:

- `database/scripts/create-backup.sh`;
- SHA-256 checksum creation;
- structural `pg_restore --list` validation;
- `database/scripts/verify-backup-restore.sh`;
- disposable-database restore;
- required-table checks;
- audit-chain verification;
- a tamper-resistance probe;
- CI job `database-backup-restore`.

This qualifies the repository backup/restore mechanism. It does **not** prove that a particular external VPS currently has a recent off-host backup.

A backup system is only operationally qualified for a target environment after an attributable restore succeeds for that environment.

## Signed APK Environment

The manual protected workflow:

- runs on a GitHub-hosted Ubuntu runner;
- uses GitHub environment `production-apk`;
- materializes signing material only into `$RUNNER_TEMP`;
- verifies the approved certificate SHA-256 fingerprint;
- creates an owner-review artifact;
- removes the materialized signing key in an always-run cleanup step.

The logical secret names may be documented. Secret values must never enter source, reports, screenshots, PR comments, or ordinary logs.

Signing and release remain protected actions and are not authorized by this environment contract.

## Development / Deployment Machine

No canonical development VPS or production host identity is established by repository source alone.

Before any task depends on such a machine, verify live:

- host/environment identity;
- source path;
- Git state and exact SHA;
- toolchain/runtime versions;
- writable artifact/evidence paths;
- disk/resource capacity;
- secret mechanism;
- backup/restore state;
- concurrent heavy workloads.

## Resource Policy

Heavy Android builds, emulator instrumentation, database restore tests, packaging, and comparable workloads must not be treated as valid performance evidence when the host is resource-starved.

Use one heavy workload at a time on constrained machines where concurrency could invalidate results.

Resource exhaustion, unavailable KVM, or insufficient disk is a stop condition when it prevents the task from proving its required behavior.

## Source / Evidence Separation

Keep separate:

- repository source;
- temporary build output;
- caches;
- CI artifacts;
- accepted evidence;
- reports;
- signing material;
- production secrets.

Caches and disposable build output are not authoritative evidence.

## Stop Conditions

Stop if:

- the environment manifest and authoritative source declarations disagree;
- required runtime/toolchain is unavailable;
- disk/resource state cannot support the test;
- emulator acceleration required by the task is unavailable;
- environment identity is ambiguous;
- exact source SHA cannot be established;
- signing secrets are unavailable for an explicitly authorized signing task;
- the task would require production access not explicitly authorized;
- an external host is being treated as qualified without live evidence.

## Qualification Rule

The repository-declared engineering environment may be reported PASS only when the exact tested source passes the environment drift gate and the relevant build/test jobs.

External machines and GitHub runner instances must be classified independently from repository source. Never convert a source-level PASS into a runtime/production PASS.
