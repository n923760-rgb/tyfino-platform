#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

manifest="governance/ci-qualification.json"
workflow=".github/workflows/ci.yml"

fail() {
  printf '[ci-qualification] ERROR: %s\n' "$*" >&2
  exit 1
}

[[ -f "$manifest" ]] || fail "Missing CI qualification manifest"
[[ -f "$workflow" ]] || fail "Missing Validate workflow"
[[ -f governance/CI_QUALIFICATION.md ]] || fail "Missing CI qualification contract"
[[ -f governance/CI_QUALIFICATION_STATUS.md ]] || fail "Missing CI qualification status record"
jq -e . "$manifest" >/dev/null

grep -Fq 'contents: read' "$workflow" || fail "Validate workflow permissions are not read-only"
grep -Fq 'branches: [main]' "$workflow" || fail "Validate workflow is not scoped to main"
grep -Fq 'github.event.pull_request.head.sha || github.sha' "$workflow" || fail "Exact source expression missing"

checkout_count="$(grep -Fc 'uses: actions/checkout@v4' "$workflow")"
[[ "$checkout_count" -eq 6 ]] || fail "Expected six checkout job definitions, found $checkout_count"

verify_count="$(grep -Fc 'name: Verify exact source SHA' "$workflow")"
[[ "$verify_count" -eq 6 ]] || fail "Expected six exact-source verification steps, found $verify_count"

ref_count="$(grep -Fc 'ref: ${{ github.event.pull_request.head.sha || github.sha }}' "$workflow")"
[[ "$ref_count" -eq 6 ]] || fail "Expected six explicit exact-source checkout refs, found $ref_count"

for required in   'name: Record CI source identity'   'name: Upload CI source identity'   'name: tyfino-ci-source-${{ github.event.pull_request.head.sha || github.sha }}-${{ github.run_attempt }}'   'name: tyfino-debug-${{ github.event.pull_request.head.sha || github.sha }}-${{ github.run_attempt }}'   'name: android-test-${{ matrix.device }}-${{ github.event.pull_request.head.sha || github.sha }}-${{ github.run_attempt }}'   'name: Validate CI qualification manifest'   'name: Type-check'   'name: Test'   'name: Build'   'name: Build debug application'   'name: Build optimized unsigned release'   'name: Run unit tests'   'name: Run Android lint'   'name: Run managed-device instrumentation'   'name: Restore and verify backup'
do
  grep -Fq "$required" "$workflow" || fail "Missing required CI contract element: $required"
done

grep -Fq 'governance/CI_QUALIFICATION.md' governance/PROJECT_PROFILE.md   || fail "Project profile does not link CI qualification contract"
grep -Fq 'governance/ci-qualification.json' governance/RESOURCE_MAP.md   || fail "Resource map does not link CI qualification manifest"

jq -e '
  .ci_qualification.status == "CI_GATED" and
  (.ci_qualification.qualification_status == "PENDING" or
   .ci_qualification.qualification_status == "QUALIFIED") and
  .ci_qualification.contract == "governance/CI_QUALIFICATION.md" and
  .ci_qualification.manifest == "governance/ci-qualification.json" and
  .ci_qualification.validator == "governance/scripts/validate-ci-qualification.sh" and
  (if .ci_qualification.qualification_status == "QUALIFIED"
   then .ci_qualification.qualification_evidence.implementation_pr == 121
        and .ci_qualification.qualification_evidence.implementation_pr_head == "4bafe282c4ee71d7772ec55b94e83f23d0a2fd49"
        and .ci_qualification.qualification_evidence.implementation_pr_validate_run_id == 35775351792
        and .ci_qualification.qualification_evidence.implementation_merge_sha == "08027979d4170bfb8c264b9a44f2d6bf67ca9aaf"
        and .ci_qualification.qualification_evidence.post_merge_validate_run_id == 35775896315
        and .ci_qualification.artifact_evidence.pr_source_attestation.id == 10716011558
        and .ci_qualification.artifact_evidence.pr_source_attestation.name == "tyfino-ci-source-4bafe282c4ee71d7772ec55b94e83f23d0a2fd49-1"
        and .ci_qualification.artifact_evidence.pr_debug_apk.id == 10716420231
        and .ci_qualification.artifact_evidence.pr_debug_apk.name == "tyfino-debug-4bafe282c4ee71d7772ec55b94e83f23d0a2fd49-1"
        and .ci_qualification.artifact_evidence.merge_source_attestation.id == 10716415962
        and .ci_qualification.artifact_evidence.merge_source_attestation.name == "tyfino-ci-source-08027979d4170bfb8c264b9a44f2d6bf67ca9aaf-1"
        and .ci_qualification.artifact_evidence.merge_debug_apk.id == 10716531010
        and .ci_qualification.artifact_evidence.merge_debug_apk.name == "tyfino-debug-08027979d4170bfb8c264b9a44f2d6bf67ca9aaf-1"
   else true
   end)
' governance/project-profile.json >/dev/null || fail "Project profile CI qualification state is invalid"

jq -e '
  (.qualification_status == "PENDING" or .qualification_status == "QUALIFIED") and
  .workflow == ".github/workflows/ci.yml" and
  .exact_source_expression == "github.event.pull_request.head.sha || github.sha" and
  (if .qualification_status == "QUALIFIED"
   then .status == "QUALIFIED"
        and .qualification_evidence.implementation_pr == 121
        and .qualification_evidence.implementation_pr_head == "4bafe282c4ee71d7772ec55b94e83f23d0a2fd49"
        and .qualification_evidence.implementation_pr_validate_run_id == 35775351792
        and .qualification_evidence.implementation_merge_sha == "08027979d4170bfb8c264b9a44f2d6bf67ca9aaf"
        and .qualification_evidence.post_merge_validate_run_id == 35775896315
        and .artifact_evidence.pr_source_attestation.id == 10716011558
        and .artifact_evidence.pr_debug_apk.id == 10716420231
        and .artifact_evidence.merge_source_attestation.id == 10716415962
        and .artifact_evidence.merge_debug_apk.id == 10716531010
   else true
   end)
' "$manifest" >/dev/null || fail "CI qualification manifest state is invalid"

printf '[ci-qualification] PASS: exact-source CI qualification state and evidence are valid.\n'
