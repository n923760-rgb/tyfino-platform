# TYFINO Task / Result Protocol

Status: **PHASE 3 IMPLEMENTATION CANDIDATE**

Governance baseline: Engineering Governance v1.0.0  
Governance rollout phase: Phase 3 — Task / Result Protocol

## Purpose

Every meaningful engineering task must have an attributable work order before mutation and an attributable result after execution.

The protocol exists to answer:

1. What exact source was the task based on?
2. What exact authority was granted?
3. What was in scope and intentionally out of scope?
4. What commands and validation actually ran?
5. What evidence supports the result?
6. What remains blocked, unverified, or intentionally not performed?

A plan is not execution evidence. A planned command must never be reported as an executed command.

## Standard Templates

Use:

- `governance/templates/TASK_PACKET.md`
- `governance/templates/RESULT_PACKET.md`

The templates are authoritative for packet structure. They intentionally reference stable repository governance instead of copying the entire governance baseline into every task.

## When a Task Packet Is Required

A Task Packet is required before:

- repository mutation;
- CI investigation that may trigger or rerun workflows;
- runtime qualification;
- infrastructure maintenance;
- signing/release preparation;
- production-affecting investigation;
- any task whose scope or authority could be ambiguous.

A simple read-only factual lookup that does not mutate state may be represented directly in the working conversation/report when repository instructions allow it.

## Packet Location

The controller may keep a live Task Packet in the PR description, issue, or authorized execution context when that location preserves all required fields.

A repository file is required only when a durable engineering record is useful.

Durable packet/result records use:

`governance/task-records/YYYY/MM/YYYY-MM-DD-<task-slug>-task.md`

`governance/task-records/YYYY/MM/YYYY-MM-DD-<task-slug>-result.md`

Rules:

- use UTC date for repository records;
- use lowercase kebab-case task slugs;
- do not overwrite a historical record for a different execution;
- add a short differentiator when two records would otherwise collide;
- never place credentials, tokens, signing material, recovery codes, secret-bearing URLs, or plaintext production secrets in packet files.

## Source Identity Rules

Before mutation, the Task Packet must record the exact live source identity that matters to the task.

For repository tasks this normally includes:

- repository;
- official branch;
- verified official HEAD;
- work branch/worktree;
- current PR and PR head when applicable;
- relevant baseline CI state.

Historical SHAs are evidence, not future expected source.

If live source differs from the expected source, mutation stops until the mismatch is understood.

## Authority Rules

The packet may narrow authority but may never widen authority beyond:

1. current owner instruction;
2. repository instructions;
3. applicable subsystem contracts;
4. engineering environment contract.

Protected actions remain protected even when technically possible.

## Validation Vocabulary

Every validation item must use one of:

- `PASS`
- `FAIL`
- `BLOCKED`
- `SKIPPED`
- `NOT RUN`

`BLOCKED`, `SKIPPED`, and `NOT RUN` are never equivalent to `PASS`.

Every important PASS or FAIL should identify the exact tested source and attributable evidence.

## Result Rules

A Result Packet records only actions that actually happened.

It must distinguish:

### Facts

Directly observed evidence.

### Inferences

Engineering conclusions derived from facts.

### Assumptions

Conditions not directly verified.

Assumptions must not be presented as facts.

The packet must also record:

- validation not run and why;
- residual risks;
- first causal failure when a task fails;
- protected actions intentionally not performed;
- the smallest recommended next action.

## Artifacts

For any retained artifact record:

- path or artifact identifier;
- SHA-256 when practical;
- size when relevant;
- whether it contains sensitive data.

Sensitive artifacts must not be attached to public task records merely to satisfy evidence requirements.

## Machine Validation

`governance/task-result-protocol.json` defines the source-controlled protocol manifest.

`governance/scripts/validate-task-result-protocol.sh` checks:

- manifest JSON validity;
- template existence;
- required template sections;
- allowed validation status vocabulary;
- project-profile/resource-map linkage.

The validation script is executed by the normal `Validate` workflow.

## Qualification Boundary

This protocol can qualify its repository-declared structure and CI validation.

It cannot by itself prove that every future human or AI executor will obey the protocol. That behavioral property belongs to Phase 4 — AI Executor Qualification.
