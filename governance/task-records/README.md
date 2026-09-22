# TYFINO Durable Task Records

This directory is reserved for engineering Task/Result Packets that require a durable repository record.

Most routine tasks should use the PR description and attributable CI/runtime evidence instead of creating permanent record files.

When a durable record is justified, use:

- `YYYY/MM/YYYY-MM-DD-<task-slug>-task.md`
- `YYYY/MM/YYYY-MM-DD-<task-slug>-result.md`

Use UTC dates and lowercase kebab-case slugs.

Do not store secrets, signing material, credentials, recovery codes, secret-bearing URLs, or private production data here.

Historical records are evidence only. They must never be reused as current expected repository state without live verification.
