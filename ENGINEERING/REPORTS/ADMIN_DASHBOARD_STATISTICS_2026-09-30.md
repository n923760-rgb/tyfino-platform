# Admin dashboard statistics review — 2026-09-30

## Task packet and live authority

- Repository: `n923760-rgb/tyfino-platform`; default/target branch: `main`.
- Inspected official base: `ef420e7e9c0d968f271ada14bce1ab3076a655c7`.
- Owner authorized reviewing/improving the entire project and continuing this work. Merge, signing, release and deployment remain protected and are not authorized by this task.
- Applied root `AGENTS.md`, central Master Engineering System, project profile, canonical roadmap, approved architecture/security/API/versioning contracts and environment contract.
- No nested `AGENTS.md` exists in the inspected tree.
- Open PRs at inspection: #166 licensing first-use expiry, #167 Admin audit atomicity, #168 clipboard result. This task changes different handler/UI/test-hook regions. Shared-source composition must be checked; this does not qualify a future combined merge.
- Execution: authenticated GitHub connector, in-memory JavaScript checks, and existing GitHub Actions. No local shell/checkout, browser, physical-device or production access in this session.

## Confirmed problem

Dashboard counts were computed from the most recent 500 activation-code rows. An older valid grant was omitted, a stored `active` status could include an expired annual grant, and bound-code rows could count the same device more than once or include revoked/expired grants.

## Bounded change

Add an authenticated `summary` object to the existing activation-code list response. Preserve the redacted array and its 500-row cap. Compute all four counts across the full table with database time: total codes; unused never-activated codes with an absent/future first-use deadline; valid started annual/lifetime grants; distinct non-null device bindings among valid grants. A deadline does not disqualify an already activated grant.

The Dashboard uses the summary, including the latest-code panel's total. Old array consumers remain compatible. A missing summary produces a localized load error. Deploy backend support before the updated Dashboard. No schema migration or licensing mutation is introduced.

The aggregate internally uses one SQL statement. Listing and summary are separate read statements and may observe a concurrent change between them. Response size is fixed; the existing statement timeout applies. Production query latency has not been measured.

## Validation and evidence

PASS — six pre-publication checks using source-extracted Dashboard loader/stats and API handler with mocked dependencies: counts independent of the list; Support audit handling; missing-summary error; authorization before querying; additive response; bounded list/unbounded aggregate. These checks execute neither PostgreSQL nor a browser.

Added a serial PostgreSQL fixture to the existing lifecycle integration test. It creates 521 code rows (512 recent unused plus nine old edge cases) and two installations, checks the unchanged 500-item array, expected deltas (521 total, 513 available, three active, one distinct device), Owner and Support access, unauthorized denial, and cleanup back to baseline. Edge cases include first-use expiry, annual expiry, revoked/future/incomplete grants, lifetime grants and shared bindings. It uses the existing disposable CI database, never production data, and avoids a parallel test competing with lifecycle teardown.

NOT RUN at commit preparation — exact-head CI. The PR validation section must link the completed run and results before readiness.
NOT RUN — browser/physical-device/production validation and production aggregate latency.

## Completion and stop conditions

Review the exact diff and shared-file composition; require existing exact-head Validate checks to pass before marking ready. Stop for unexpected base/head movement, unrelated diff, failed tests, conflicting authority or secret exposure. Preserve the official branch and existing PRs. Merge remains owner controlled.
