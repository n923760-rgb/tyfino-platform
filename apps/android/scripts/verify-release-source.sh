#!/usr/bin/env bash
# No signing material is read or created by this preflight.
set -Eeuo pipefail

fail() { echo "[release-source] $1" >&2; exit 1; }

test "${GITHUB_REPOSITORY:-}" = n923760-rgb/tyfino-platform || fail 'Wrong repository.'
test "${GITHUB_EVENT_NAME:-}" = workflow_dispatch || fail 'Manual owner request required.'
test "${GITHUB_REF:-}" = refs/heads/main || fail 'Select the official main branch.'
[[ "${TYFINO_EXPECTED_SOURCE_SHA:-}" =~ ^[0-9a-f]{40}$ ]] || fail 'Supply the full reviewed source SHA.'
test "${GITHUB_SHA:-}" = "$TYFINO_EXPECTED_SOURCE_SHA" || fail 'Workflow source differs from the reviewed SHA.'
test "$(git rev-parse HEAD)" = "$TYFINO_EXPECTED_SOURCE_SHA" || fail 'Checkout differs from the reviewed SHA.'
tracked_status=$(git status --porcelain --untracked-files=no)
test -z "$tracked_status" || fail 'Tracked source is modified.'

# FETCH_HEAD comes from a fresh explicit main fetch, never a stale local tracking ref.
git fetch --no-tags origin refs/heads/main
test "$(git rev-parse FETCH_HEAD)" = "$TYFINO_EXPECTED_SOURCE_SHA" || fail 'Official main has moved; review the new source first.'
echo "[release-source] PASS: exact official source $TYFINO_EXPECTED_SOURCE_SHA"
