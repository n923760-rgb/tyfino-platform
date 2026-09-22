#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

manifest="governance/engineering-environment.json"

fail() {
  printf '[environment-validation] ERROR: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "Required file is missing: $1"
}

for file in   "$manifest"   .nvmrc   .java-version   apps/api/package.json   apps/admin/package.json   apps/api/Dockerfile   apps/android/app/build.gradle.kts   apps/android/gradle/wrapper/gradle-wrapper.properties   docker-compose.yml   .github/workflows/ci.yml   database/scripts/create-backup.sh   database/scripts/verify-backup-restore.sh; do
  require_file "$file"
done

jq -e . "$manifest" >/dev/null

node_major="$(jq -r '.toolchains.node.major' "$manifest")"
java_major="$(jq -r '.toolchains.java.major' "$manifest")"
java_distribution="$(jq -r '.toolchains.java.distribution' "$manifest")"
gradle_version="$(jq -r '.toolchains.gradle.version' "$manifest")"
gradle_sha="$(jq -r '.toolchains.gradle.distribution_sha256' "$manifest")"
postgres_image="$(jq -r '.toolchains.postgres.image' "$manifest")"
compile_sdk="$(jq -r '.toolchains.android.compile_sdk' "$manifest")"
target_sdk="$(jq -r '.toolchains.android.target_sdk' "$manifest")"
min_sdk="$(jq -r '.toolchains.android.min_sdk' "$manifest")"

[[ "$(tr -d '[:space:]' < .nvmrc)" == "$node_major" ]]   || fail ".nvmrc does not match manifest Node major $node_major"
[[ "$(tr -d '[:space:]' < .java-version)" == "$java_major" ]]   || fail ".java-version does not match manifest Java major $java_major"

for package in apps/api/package.json apps/admin/package.json; do
  jq -e --arg expected ">=$node_major" '.engines.node == $expected' "$package" >/dev/null     || fail "$package engines.node does not match >=$node_major"
done

node_image="node:${node_major}-alpine"
node_stage_count="$(grep -Ec "^FROM ${node_image}([[:space:]]|$)" apps/api/Dockerfile || true)"
[[ "$node_stage_count" -eq 2 ]]   || fail "apps/api/Dockerfile must use $node_image for both build and runtime stages"

grep -Fq "image: $postgres_image" docker-compose.yml   || fail "docker-compose.yml PostgreSQL image does not match $postgres_image"
grep -Fq "image: $postgres_image" .github/workflows/ci.yml   || fail "CI PostgreSQL image does not match $postgres_image"

grep -Fq "gradle-${gradle_version}-bin.zip" apps/android/gradle/wrapper/gradle-wrapper.properties   || fail "Gradle wrapper version does not match $gradle_version"
grep -Fq "distributionSha256Sum=$gradle_sha" apps/android/gradle/wrapper/gradle-wrapper.properties   || fail "Gradle wrapper distribution SHA-256 does not match the manifest"

grep -Eq "compileSdk[[:space:]]*=[[:space:]]*${compile_sdk}([[:space:]]|$)" apps/android/app/build.gradle.kts   || fail "Android compileSdk does not match $compile_sdk"
grep -Eq "targetSdk[[:space:]]*=[[:space:]]*${target_sdk}([[:space:]]|$)" apps/android/app/build.gradle.kts   || fail "Android targetSdk does not match $target_sdk"
grep -Eq "minSdk[[:space:]]*=[[:space:]]*${min_sdk}([[:space:]]|$)" apps/android/app/build.gradle.kts   || fail "Android minSdk does not match $min_sdk"

grep -Fq "node-version: $node_major" .github/workflows/ci.yml   || fail "CI Node version does not match $node_major"
grep -Fq "distribution: $java_distribution" .github/workflows/ci.yml   || fail "CI Java distribution does not match $java_distribution"
grep -Fq "java-version: $java_major" .github/workflows/ci.yml   || fail "CI Java version does not match $java_major"

while IFS= read -r device; do
  grep -Fq "device: $device" .github/workflows/ci.yml     || fail "CI managed-device matrix is missing $device"
done < <(jq -r '.managed_device_qualification[]' "$manifest")

grep -Fq "test: [\"CMD-SHELL\", \"pg_isready" docker-compose.yml   || fail "PostgreSQL healthcheck is missing"
grep -Fq "fetch('http://127.0.0.1:3000/readyz')" docker-compose.yml   || fail "API readiness healthcheck is missing"

bash -n database/scripts/create-backup.sh
bash -n database/scripts/verify-backup-restore.sh
bash -n infrastructure/server/bootstrap-ubuntu.sh
bash -n infrastructure/server/deploy-preproduction.sh

jq -e '
  .source_environment_qualified == true and
  .external_environment.github_runner_image.pinned == false and
  .external_environment.development_vps.qualified == false and
  .external_environment.production_host.qualified == false
' "$manifest" >/dev/null   || fail "Environment qualification boundaries were widened without updating the contract"

printf '[environment-validation] PASS: repository-declared engineering environment is internally consistent.\n'
