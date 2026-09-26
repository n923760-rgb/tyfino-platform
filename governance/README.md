# TYFINO Governance

TYFINO adopts the reusable **Master Engineering System** from:

- Central reference: https://github.com/n923760-rgb/engineering-governance
- Primary authority: `MASTER_GOVERNANCE.md`
- Project repository: `n923760-rgb/tyfino-platform`
- Official branch: `main`

The central governance repository owns reusable engineering rules. TYFINO owns only its project-specific profile, roadmap, reports, evidence, contracts, and qualification records.

## Canonical project engineering storage

- `/ENGINEERING/MASTER_ROADMAP.md` — the single permanent engineering roadmap.
- `/ENGINEERING/REPORTS/` — detailed re-baselines, audits, qualification reports, and result summaries.
- `/ENGINEERING/EVIDENCE/` — evidence indexes and metadata for attributable proof.

Do not create another TYFINO roadmap elsewhere.

## Authority

1. Current explicit owner instruction.
2. Root or nearest `AGENTS.md`.
3. Central Master Engineering System.
4. Approved TYFINO architecture/security/data-ownership documents and scoped subsystem contracts.
5. `/ENGINEERING/MASTER_ROADMAP.md` as the canonical engineering state and sequencing record.
6. Project-specific governance/environment/task-result controls in this directory.

The roadmap does not override an applicable approved subsystem contract.

## Existing governance records

The files already under `governance/` remain useful TYFINO-specific controls and historical qualification evidence. They are not deleted merely because the central system evolved.

Historical SHAs, CI run IDs, and prior qualification states are evidence about those past rounds only. They must never be reused as current source truth.

## Operating rule

**Live repository truth → governance → one Master Roadmap → bounded task → isolated execution → validation → evidence → review → protected decision**

Before every source mutation, verify the current repository state and actual execution capabilities again.

Protected actions require explicit current owner authorization. Technical access alone is never authority.
