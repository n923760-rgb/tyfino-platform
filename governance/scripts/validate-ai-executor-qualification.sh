#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

manifest="governance/ai-executor-qualification.json"
evaluator="governance/scripts/evaluate-executor-policy.py"

fail() {
  printf '[ai-executor-qualification] ERROR: %s\n' "$*" >&2
  exit 1
}

[[ -f "$manifest" ]] || fail "Missing manifest"
[[ -f "$evaluator" ]] || fail "Missing evaluator"
jq -e . "$manifest" >/dev/null
python3 -m py_compile "$evaluator"

contract="$(jq -r '.contract' "$manifest")"
fixtures_root="$(jq -r '.fixtures_root' "$manifest")"
[[ -f "$contract" ]] || fail "Missing contract: $contract"
[[ -d "$fixtures_root" ]] || fail "Missing fixture directory: $fixtures_root"

expected_decisions='["ALLOW","STOP_MISSING_EVIDENCE","STOP_REPORT_INTEGRITY","STOP_SCOPE_EXPANSION","STOP_SECRET_EXPOSURE","STOP_SOURCE_MISMATCH","STOP_UNAUTHORIZED_ACTION","STOP_UNAUTHORIZED_PROTECTED_ACTION"]'
actual_decisions="$(jq -c '.decisions | sort' "$manifest")"
[[ "$actual_decisions" == "$expected_decisions" ]] || fail "Decision vocabulary drifted"

while IFS= read -r fixture_id; do
  fixture="$fixtures_root/$fixture_id.json"
  [[ -f "$fixture" ]] || fail "Missing required fixture: $fixture"
  jq -e --arg id "$fixture_id" '.fixture_id == $id and (.expected_decision | type == "string")' "$fixture" >/dev/null \
    || fail "Invalid fixture identity: $fixture"
  python3 "$evaluator" "$fixture" >/dev/null
done < <(jq -r '.required_fixture_ids[]' "$manifest")

grep -Fq "$contract" governance/PROJECT_PROFILE.md \
  || fail "Project profile does not link the AI executor qualification contract"
grep -Fq "$manifest" governance/RESOURCE_MAP.md \
  || fail "Resource map does not link the AI executor qualification manifest"
grep -Fq '"ai_executor_qualification"' governance/project-profile.json \
  || fail "Machine-readable project profile does not expose ai_executor_qualification"

jq -e '
  .ai_executor_qualification.framework_status == "CI_GATED" and
  .ai_executor_qualification.policy_gate_status == "QUALIFIED" and
  (.ai_executor_qualification.ai_behavioral_status == "NOT_QUALIFIED" or
   .ai_executor_qualification.ai_behavioral_status == "OBSERVED_PENDING_CI" or
   .ai_executor_qualification.ai_behavioral_status == "QUALIFIED") and
  .ai_executor_qualification.qualification_record == "governance/AI_EXECUTOR_QUALIFICATION_STATUS.md" and
  (.ai_executor_qualification.policy_gate_evidence.implementation_pr_validate_run_id | type == "number") and
  (.ai_executor_qualification.policy_gate_evidence.post_merge_validate_run_id | type == "number") and
  (if .ai_executor_qualification.ai_behavioral_status == "QUALIFIED"
   then .ai_executor_qualification.phase_status == "QUALIFIED"
        and (.ai_executor_qualification.behavioral_qualification_scope | type == "string" and length > 40)
        and .ai_executor_qualification.behavioral_qualification_record == "governance/AI_EXECUTOR_QUALIFICATION_STATUS.md"
        and .ai_executor_qualification.behavioral_qualification_evidence.implementation_pr == 119
        and .ai_executor_qualification.behavioral_qualification_evidence.implementation_pr_head == "63202f789cf4659ff2e7025b25cf3d11be7f71b7"
        and .ai_executor_qualification.behavioral_qualification_evidence.implementation_pr_validate_run_id == 35769249519
        and .ai_executor_qualification.behavioral_qualification_evidence.implementation_merge_sha == "1cdfdee16c239b96db52b1d2b57d6eada9f4daed"
        and .ai_executor_qualification.behavioral_qualification_evidence.post_merge_validate_run_id == 35770034055
   else true
   end)
' governance/project-profile.json >/dev/null \
  || fail "Project profile qualification boundary or evidence is invalid"

jq -e '
  .policy_gate_status == "QUALIFIED" and
  (.ai_behavioral_status == "NOT_QUALIFIED" or
   .ai_behavioral_status == "OBSERVED_PENDING_CI" or
   .ai_behavioral_status == "QUALIFIED") and
  .qualification_record == "governance/AI_EXECUTOR_QUALIFICATION_STATUS.md" and
  (if .ai_behavioral_status == "QUALIFIED"
   then .status == "QUALIFIED_WITH_SCOPED_BEHAVIORAL_EVIDENCE"
        and .phase_status == "QUALIFIED"
        and (.behavioral_qualification_scope | type == "string" and length > 40)
        and .behavioral_qualification_record == "governance/AI_EXECUTOR_QUALIFICATION_STATUS.md"
        and .behavioral_qualification_evidence.implementation_pr == 119
        and .behavioral_qualification_evidence.implementation_pr_head == "63202f789cf4659ff2e7025b25cf3d11be7f71b7"
        and .behavioral_qualification_evidence.implementation_pr_validate_run_id == 35769249519
        and .behavioral_qualification_evidence.implementation_merge_sha == "1cdfdee16c239b96db52b1d2b57d6eada9f4daed"
        and .behavioral_qualification_evidence.post_merge_validate_run_id == 35770034055
   else true
   end)
' "$manifest" >/dev/null || fail "Manifest qualification state is inconsistent"

[[ -f governance/AI_EXECUTOR_QUALIFICATION_STATUS.md ]] \
  || fail "Missing Phase 4 qualification status record"

printf '[ai-executor-qualification] PASS: policy gate and evidence-bounded AI behavioral state are valid.\n'
