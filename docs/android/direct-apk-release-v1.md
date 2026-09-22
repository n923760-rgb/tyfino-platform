# TYFINO Direct APK Release Contract v1

Status: **DECIDED distribution path / PARTIAL signing qualification / FIRST RELEASE BLOCKED**  
Approved distribution path: Direct APK  
Approved: 2026-09-20

## Permanent update identity

Android accepts an update only when it has the same application ID and signing certificate as the installed application. TYFINO therefore uses:

- application ID `com.tyfino.player`;
- one permanent production signing key;
- PKCS12 keystore format;
- a recorded SHA-256 signing-certificate fingerprint;
- the same protected key for every direct APK update.

Losing the key, its passwords, or every usable backup permanently prevents updates to existing installations. Replacing it would require customers to uninstall TYFINO and lose application-local state.

## Custody rules

- Never commit a keystore, password, Base64 key, recovery file, or signing secret.
- Generate the production key only during the separately controlled provisioning step.
- Keep two encrypted backups in separate owner-controlled locations before the first release.
- Prove that both backups can be opened and expose the same certificate fingerprint.
- Store GitHub signing values only in the protected `production-apk` environment.
- Never generate or retain the signing key on the public TYFINO VPS.
- Do not send signing material through chat, screenshots, issues, pull requests, logs, or support messages.

Required protected values are:

- `TYFINO_ANDROID_KEYSTORE_BASE64`
- `TYFINO_ANDROID_STORE_PASSWORD`
- `TYFINO_ANDROID_KEY_ALIAS`
- `TYFINO_ANDROID_KEY_PASSWORD`
- `TYFINO_ANDROID_CERT_SHA256`

## Release workflow

The manual `Build signed APK` workflow:

1. materializes the protected keystore only on the ephemeral runner;
2. builds the optimized Release variant with the approved production licensing origin;
3. verifies the APK signature;
4. rejects a signing certificate that differs from the approved SHA-256 fingerprint;
5. emits the APK, APK checksum, and public certificate report as a 30-day owner-review artifact;
6. removes the materialized keystore even after failure.

The workflow does not publish the APK to customers. Distribution remains a separate explicit release action.

## Current qualification evidence

As of 2026-09-22, repository evidence shows:

- the manual protected `Build signed APK` workflow completed successfully on then-official `main@0ab59398d64e936430cced924d0cd8f4fdc56c3f`;
- workflow run: `35568182674` — **PASS**;
- the workflow verified the APK signature and configured SHA-256 certificate fingerprint before artifact upload;
- owner-review artifact `tyfino-signed-apk-0ab59398d64e936430cced924d0cd8f4fdc56c3f-1` was created with 30-day retention and is not customer distribution.

This evidence proves that protected signing material was usable in the GitHub environment for that run. It does **not** prove how the key was originally generated, that two independent owner-controlled encrypted backups exist, that either backup has passed a recovery drill, or that the checksum/fingerprint has been retained durably outside the expiring CI artifact.

## First-release gate

The first customer release remains blocked until all of the following are proven:

- **NOT PROVEN** — the key was generated with strong independent passwords under owner control;
- **NOT PROVEN** — two recoverable encrypted backups exist in separate owner-controlled locations;
- **PARTIAL** — the configured certificate fingerprint matched the successfully signed APK, but durable owner-controlled fingerprint/recovery evidence remains required;
- **PASS for workflow operation** — manual workflow run `35568182674` succeeded from the official `main` commit that existed at execution time;
- **PARTIAL** — the workflow generated an APK SHA-256 checksum inside the 30-day owner-review artifact, but durable owner-controlled retention is not proven;
- **BLOCKED** — physical phone and Android TV/Google TV qualification has not been recorded as passing;
- **BLOCKED** — production operations, off-site backup, monitoring, retention, and rollback gates are not yet approved;
- **NOT AUTHORIZED** — customer distribution/release has not been authorized by the owner.

A successful signing workflow is qualification evidence only. It is not a release decision.
