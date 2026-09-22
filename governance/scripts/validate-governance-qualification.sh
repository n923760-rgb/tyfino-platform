#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fail(){ printf '[governance-qualification] ERROR: %s\n' "$*" >&2; exit 1; }
for f in governance/GOVERNANCE_QUALIFICATION.md governance/GOVERNANCE_QUALIFICATION_STATUS.md governance/governance-qualification.json governance/governance-qualification/run-project-qualification.py governance/scripts/verify-repository-state.sh governance/scripts/verify-environment-capacity.py; do
  [[ -f "$f" ]] || fail "Missing $f"
done
jq -e '
 .phase == 6 and
 (.qualification_status == "PENDING" or .qualification_status == "QUALIFIED") and
 .runner == "governance/governance-qualification/run-project-qualification.py" and
 .validator == "governance/scripts/validate-governance-qualification.sh" and
 (.required_fixtures | sort == ["clean_repository","dirty_worktree","missing_evidence","resource_failure","unauthorized_action","wrong_official_head"]) and
 (if .qualification_status == "QUALIFIED" then
   .status == "QUALIFIED" and
   .qualification_evidence.implementation_pr == 124 and
   .qualification_evidence.implementation_pr_head == "60746c985d77e26a7a29bb1740dd93ee42f96019" and
   .qualification_evidence.implementation_pr_validate_run_id == 35783372970 and
   .qualification_evidence.implementation_pr_validate_run_number == 340 and
   .qualification_evidence.implementation_merge_sha == "6822ec730bb09efda9b345d98fa16f3e83255c8e" and
   .qualification_evidence.post_merge_validate_run_id == 35783938855 and
   .qualification_evidence.post_merge_validate_run_number == 341
  else true end)
' governance/governance-qualification.json >/dev/null || fail "Invalid Phase 6 manifest or qualification evidence"
grep -Fq 'GOVERNANCE_QUALIFICATION.md' governance/PROJECT_PROFILE.md || fail "Project profile missing Phase 6"
grep -Fq 'governance-qualification.json' governance/RESOURCE_MAP.md || fail "Resource map missing Phase 6"
grep -Fq 'Run governance qualification adversarial suite' .github/workflows/ci.yml || fail "CI does not run Phase 6 suite"
python governance/governance-qualification/run-project-qualification.py
printf '[governance-qualification] PASS: Phase 6 structure, evidence and adversarial suite are valid.\n'
