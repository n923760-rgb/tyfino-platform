# TYFINO Direct APK Release Contract v1

Status: **DECIDED distribution path / BLOCKED signing material**  
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

## First-release gate

The first signed APK remains blocked until all of the following are proven:

- the key is generated with strong independent passwords;
- two recoverable encrypted backups exist;
- the certificate fingerprint is recorded and matches the workflow secret;
- the manual workflow succeeds from the official `main` commit;
- the resulting APK checksum is recorded;
- physical phone and Android TV/Google TV qualification passes;
- production operations, off-site backup, monitoring, retention, and rollback gates are approved.
