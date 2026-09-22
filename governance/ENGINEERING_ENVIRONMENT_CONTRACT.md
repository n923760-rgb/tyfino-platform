# TYFINO Engineering Environment Contract

Status: PARTIALLY QUALIFIED — environment identity must be re-verified per task.

## Environment Classes

### GitHub Actions validation environment

Current repository workflows use GitHub-hosted Ubuntu runners.

Validated toolchain requirements visible in repository CI:
- Node.js 22 for API and Admin;
- PostgreSQL 16 service for database/API validation;
- JDK 17 for Android;
- Gradle 9.6.0 for Android;
- Android SDK/emulator with KVM acceleration for managed-device instrumentation;
- Docker Compose and jq for deployment-configuration validation.

Runner image details are execution evidence and must be taken from the actual CI run, not hard-coded here.

### Signed APK environment

The manual protected workflow:
- runs on a GitHub-hosted Ubuntu runner;
- uses GitHub environment `production-apk`;
- materializes signing material only into `$RUNNER_TEMP`;
- verifies the approved certificate SHA-256 fingerprint;
- creates an owner-review artifact;
- removes the materialized signing key in an always-run cleanup step.

The logical secret names may be documented. Secret values must never enter source, reports, screenshots, PR comments, or ordinary logs.

### Development / deployment machine

No canonical development VPS or production host identity is established by this governance adoption.

Before any task depends on such a machine, verify live:
- host/environment identity;
- source path;
- Git state;
- toolchain;
- writable artifact/evidence paths;
- disk/resource capacity;
- secret mechanism;
- backup/restore state;
- concurrent heavy workloads.

## Resource Policy

Heavy Android builds, emulator instrumentation, database restore tests, packaging, and comparable workloads must not be treated as valid performance evidence when the host is resource-starved.

Use one heavy workload at a time on constrained machines where concurrency could invalidate results.

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
- required runtime/toolchain is unavailable;
- disk/resource state cannot support the test;
- emulator acceleration required by the task is unavailable;
- environment identity is ambiguous;
- signing secrets are unavailable for an explicitly authorized signing task;
- the task would require production access not explicitly authorized.
