# TYFINO Direct APK Release Contract v1

Status: **DECIDED distribution path / SIGNING WORKFLOW PROVEN / BLOCKED custody and release qualification**  
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

## Current signed-build evidence

Verified repository evidence as of 2026-09-22:

- manual `Build signed APK` run `35568182674` completed successfully from official `main@0ab59398d64e936430cced924d0cd8f4fdc56c3f`;
- key materialization, optimized signed build, certificate/package verification, owner-review artifact upload, and always-run key cleanup all completed successfully;
- artifact `tyfino-signed-apk-0ab59398d64e936430cced924d0cd8f4fdc56c3f-1` was created with GitHub artifact digest `sha256:d1c5eafafa5dd507909603626ff39a8e1acca5780fc892d6e1dafa1aa3385f07`;
- the workflow's certificate-verification step proves that the APK certificate matched the configured approved SHA-256 fingerprint for that run;
- the workflow generated the APK checksum file and public certificate report before the successful artifact upload.

A source comparison from that signed commit to `main@5726555bbc410a95e233bd1516361be67dfbe580` contains governance files only. No Android source or signing-workflow file changed in that range.

This historical evidence proves that usable protected signing material and certificate verification exist. It does **not** prove password strength, two independent recoverable owner backups, backup recovery, physical-device qualification, or production operations. A customer release must still run the protected signing workflow again on the exact release `main` SHA; an older signed artifact must not silently become the release artifact for newer source.

## First-release gate

The first signed APK remains blocked until all of the following are proven:

- the key is generated with strong independent passwords — usable signing material is proven by the successful workflow, but password strength is not repository-verifiable;
- two recoverable encrypted backups exist — still requires owner-controlled evidence;
- the certificate fingerprint is recorded and matches the workflow secret — proven for run `35568182674`;
- the manual workflow succeeds from the exact official `main` commit selected for customer release — historical success exists, but it must be rerun for the final release SHA;
- the resulting APK checksum is recorded — proven for the historical signed artifact; the final release requires its own checksum;
- physical phone and Android TV/Google TV qualification passes;
- production operations, off-site backup, monitoring, retention, and rollback gates are approved.
