#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

manifest="governance/task-result-protocol.json"

fail() {
  printf '[task-result-protocol] ERROR: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "Required file is missing: $1"
}

require_heading() {
  local file="$1"
  local heading="$2"
  grep -Fxq "## $heading" "$file" || fail "$file is missing required heading: $heading"
}

require_file "$manifest"
jq -e . "$manifest" >/dev/null

protocol="$(jq -r '.protocol' "$manifest")"
task_template="$(jq -r '.task_template' "$manifest")"
result_template="$(jq -r '.result_template' "$manifest")"
records_root="$(jq -r '.durable_records_root' "$manifest")"

require_file "$protocol"
require_file "$task_template"
require_file "$result_template"
require_file "$records_root/README.md"

while IFS= read -r heading; do
  require_heading "$task_template" "$heading"
done < <(jq -r '.task_required_sections[]' "$manifest")

while IFS= read -r heading; do
  require_heading "$result_template" "$heading"
done < <(jq -r '.result_required_sections[]' "$manifest")

expected_statuses='["BLOCKED","FAIL","NOT RUN","PASS","SKIPPED"]'
actual_statuses="$(jq -c '.allowed_validation_statuses | sort' "$manifest")"
[[ "$actual_statuses" == "$expected_statuses" ]]   || fail "Validation status vocabulary must remain PASS/FAIL/BLOCKED/SKIPPED/NOT RUN"

jq -e '
  (.allowed_task_types | length) == 5 and
  (.allowed_task_types | index("READ-ONLY DIAGNOSIS")) != null and
  (.allowed_task_types | index("IMPLEMENTATION")) != null and
  (.allowed_task_types | index("RUNTIME QUALIFICATION")) != null and
  (.allowed_task_types | index("CI INVESTIGATION")) != null and
  (.allowed_task_types | index("INFRASTRUCTURE MAINTENANCE")) != null
' "$manifest" >/dev/null || fail "Task type vocabulary is incomplete"

grep -Fq "$task_template" governance/RESOURCE_MAP.md   || fail "Resource map does not link the Task Packet template"
grep -Fq "$result_template" governance/RESOURCE_MAP.md   || fail "Resource map does not link the Result Packet template"
grep -Fq "$protocol" governance/PROJECT_PROFILE.md   || fail "Project profile does not link the Task/Result protocol"
grep -Fq '"task_result_protocol"' governance/project-profile.json   || fail "Machine-readable project profile does not expose task_result_protocol"

jq -e '
  .task_result_protocol.status == "CI_GATED" and
  .task_result_protocol.protocol == "governance/TASK_RESULT_PROTOCOL.md" and
  .task_result_protocol.task_template == "governance/templates/TASK_PACKET.md" and
  .task_result_protocol.result_template == "governance/templates/RESULT_PACKET.md"
' governance/project-profile.json >/dev/null   || fail "Machine-readable project profile Task/Result linkage is invalid"

printf '[task-result-protocol] PASS: Task/Result protocol is internally consistent.\n'
