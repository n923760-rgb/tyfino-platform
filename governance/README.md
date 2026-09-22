# TYFINO Governance

TYFINO adopts the reusable Engineering Governance baseline:

- Baseline repository: https://github.com/n923760-rgb/engineering-governance
- Baseline release: v1.0.0
- Project repository: n923760-rgb/tyfino-platform
- Official branch: main

This directory contains TYFINO-specific customization only. It does not duplicate the entire master governance system.

Authority order for TYFINO remains:

1. Current owner instruction.
2. Root `AGENTS.md` and any nearer scoped repository instruction.
3. Approved subsystem contracts under `docs/`.
4. `governance/ENGINEERING_ENVIRONMENT_CONTRACT.md`.
5. Advisory engineering guides.
6. Atomic Task Packet.

Current source truth must always be verified live before mutation.

Do not store live task SHAs, credentials, signing material, production secrets, or secret-bearing URLs in long-lived governance profiles.
