# TYFINO Repository Engineering Instructions

TYFINO uses the central **Master Engineering System** from `n923760-rgb/engineering-governance`.

The target repository is always the source of live project truth. Historical chats, AI memory, old Pull Requests, screenshots, archived SHAs, copied configs, and another project's governance files are context only.

## Authority order

1. Current explicit owner instruction.
2. The nearest applicable `AGENTS.md`.
3. The central Master Engineering System (`engineering-governance/MASTER_GOVERNANCE.md`).
4. `/ENGINEERING/MASTER_ROADMAP.md`.
5. Approved TYFINO architecture/security/data-ownership documents and scoped subsystem contracts.
6. TYFINO engineering-environment and task/result governance under `governance/`.

A lower layer may narrow authority but must not widen a higher layer.

## Canonical engineering state

- Master roadmap: `/ENGINEERING/MASTER_ROADMAP.md`
- Detailed reports: `/ENGINEERING/REPORTS/`
- Evidence indexes/metadata: `/ENGINEERING/EVIDENCE/`

Do not create a competing roadmap elsewhere in the repository.

## Before any repository mutation

Verify live:

- repository identity is `n923760-rgb/tyfino-platform`;
- official/default branch is `main`;
- current official `main` HEAD;
- current branch or API target branch;
- open/conflicting Pull Requests;
- actual execution capabilities available in the current session;
- nearest `AGENTS.md`, `governance/PROJECT_PROFILE.md`, `/ENGINEERING/MASTER_ROADMAP.md`, and relevant subsystem contracts;
- exact task scope and current owner authorization.

If source identity, authority, scope, or required evidence is ambiguous, stop mutation and classify the gap honestly.

## Work discipline

- One confirmed problem or one coherent governance change per branch and Pull Request.
- Do not push ordinary changes directly to `main`.
- Audit, review, and diagnosis are read-only unless implementation is explicitly requested.
- Do not make speculative fixes or unrelated refactors.
- Use the smallest deterministic validation first, then the affected regression surface.
- Review the full diff for accidental files, unrelated changes, and secrets before asking for merge.
- Report validation as `PASS`, `FAIL`, `BLOCKED`, `UNKNOWN`, `NOT RUN`, or `SKIPPED`.
- Important conclusions require attributable evidence tied to the exact source tested.

## Protected actions

Merge, release, tag, signing, store publication, production deployment, DNS changes, destructive database migration, production credential rotation, destructive production operations, server/VPS/cloud destruction or reinstall, repository deletion, force-push/history rewrite, and permanent release-artifact deletion require explicit current owner authorization for that exact action.

Technical access does not equal authorization.

## Secrets and identity

Never commit or expose credentials, tokens, authorization headers, IPTV passwords, activation codes, signing material, private keys, recovery codes, production secrets, or secret-bearing URLs.

Do not change TYFINO package/application identity, app name, version policy, signing model, approved production licensing endpoint, branding identity, or ABI policy without explicit approval.

## Android-specific rules

Read `apps/android/README.md` and the applicable contracts under `docs/android/` before Android work.

Android work must remain:

- performance-first and lightweight;
- adaptive to available window size and input mode;
- RTL-safe and accessible;
- usable with TV / Google TV D-pad focus;
- resilient to stale asynchronous results;
- non-blocking for artwork/background work.

Every asynchronous completion that can mutate visible or persisted state must revalidate its authoritative account/destination/generation owner at commit time; cancellation alone is not sufficient.

Physical-device, real-provider/media, accessibility, RTL, low-memory, TV/foldable, and performance claims require their own runtime evidence and must not be inferred from source or emulator CI alone.
