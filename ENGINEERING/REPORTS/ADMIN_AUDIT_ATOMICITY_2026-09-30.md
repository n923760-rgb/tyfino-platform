# Admin code creation and settings audit atomicity

Task ID: `2026-09-30-admin-audit-atomicity`
Date: 2026-09-30 UTC
Repository: `n923760-rgb/tyfino-platform`
Official branch / inspected source: `main` / `ef420e7e9c0d968f271ada14bce1ab3076a655c7`
Task branch: `fix/admin-audit-atomicity-2026-09-30`

## Authority and scope

The owner requested review/improvement of the entire project and then instructed continued work. This round corrects one consistency issue: Admin code creation and settings changes must commit together with their audit records. Merge, release, signing, deployment, production database operations and identity changes are not authorized.

Execution mode: authenticated GitHub API/MCP and JavaScript orchestration; the existing GitHub Actions workflow runs repository build and PostgreSQL integration validation. No local shell, Node/PostgreSQL process, Android/browser or physical-device execution is available.

Live repository/default branch/official HEAD, instructions, contracts and open PRs were inspected. Official baseline Validate run 36665625417 succeeded. PR #166 remains open for a separate licensing correction; it is not merged into the official baseline. This round changes different production handlers. Its integration hook is before the support-role setup, whereas #166's integration additions concern the later licensing journey. This roadmap update is appended after the evidence links; it does not replace #166's earlier reconciliation sections.

## Root cause and correction

FACT: code creation calls `db.query` to insert the code, then separately calls `writeAudit(db, ...)`. If audit insertion fails, the code remains committed although its one-time plaintext value is not returned. A uniqueness-shaped audit error invokes the existing five-attempt retry and can leave five partial rows.

FACT: settings updates also run their business write and audit insertion as separate auto-committed statements. Audit failure can return an error while retaining the settings change.

Both handlers now acquire one client and execute BEGIN, the business write, audit insertion on that same client, and COMMIT. Errors cause ROLLBACK; finally releases the connection. Code-generation retries remain bounded to five attempts, with each failed attempt rolled back and released before the next. Success responses occur after COMMIT.

No runtime schema, dependencies, public API shape, roles, audit-chain protections, Android behavior or deployment configuration changes are included.

## Regression coverage

The new `apps/api/src/test-helpers/admin-audit-atomicity.ts` helper runs serially inside the existing PostgreSQL/Fastify lifecycle integration test. A separate parallel database test would interfere with that test's final schema-unavailability probe.

Only the disposable test owner's database connection installs a temporary audit rejection trigger; the existing chain and append-only triggers remain enabled. Cleanup drops only the temporary trigger/function in finally.

The integration assertions cover:
- generic audit failure returns a sanitized 500 without persisting a code or audit row;
- failed settings audit preserves the original setting and audit count;
- uniqueness-shaped audit failure exhausts the existing five retries with 503 and no partial codes;
- after injection is removed, successful creation commits exactly one code and its matching redacted audit event;
- successful settings changes/restoration commit matching audit-count increments;
- the audit chain still verifies after rollbacks, retry exhaustion and recovery.

The trial setting is restored before the original licensing lifecycle continues.

## Validation / result record

PASS: nine-case JavaScript execution of extracted actual handlers against fake SQL transaction dependencies. Cases cover creation/settings success, audit failure, uniqueness-shaped audit failure, one collision followed by success, bounded collision exhaustion, and early invalid/unauthorized returns. The old handlers leave partial business state; the correction removes it. Every acquired connection is released.

One verifier attempt failed because the orchestration runtime lacks `structuredClone`; the verifier was corrected to use JSON cloning for its synthetic state. This was a verifier-environment issue, not a production-source failure. No repository mutation preceded the successful checks.

NOT RUN locally: Node typecheck, API tests/build or PostgreSQL. The existing exact-head Validate CI is the executor for these checks; its actual run ID/results will be recorded in the PR description.

NOT RUN: browser, physical-device/provider, signing, release or production qualification.

Full five-file scope is reviewed before PR creation: the Admin handlers file, the existing integration test with its serial hook, one test helper, this report, and the canonical roadmap.

The next product correction is clipboard success/error handling, separately from this transaction fix.
