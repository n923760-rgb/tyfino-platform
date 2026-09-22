#!/usr/bin/env bash
set -euo pipefail
usage(){ echo "Usage: $0 <official-branch> <expected-remote-head> [expected-repository-fragment]" >&2; exit 2; }
[[ $# -ge 2 ]] || usage
OFFICIAL_BRANCH="$1"; EXPECTED_HEAD="$2"; EXPECTED_REPO_FRAGMENT="${3:-}"
command -v git >/dev/null 2>&1 || { echo "STOP: git not found"; exit 10; }
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "STOP: not inside a Git worktree"; exit 11; }
REMOTE_URL="$(git remote get-url origin 2>/dev/null || true)"
CURRENT_BRANCH="$(git branch --show-current)"
LOCAL_HEAD="$(git rev-parse HEAD)"
WORKTREE_STATUS="$(git status --porcelain)"
if [[ -n "$EXPECTED_REPO_FRAGMENT" && "$REMOTE_URL" != *"$EXPECTED_REPO_FRAGMENT"* ]]; then echo "STOP: repository identity mismatch"; exit 20; fi
if [[ "$CURRENT_BRANCH" != "$OFFICIAL_BRANCH" ]]; then echo "STOP: wrong branch"; exit 21; fi
if [[ -n "$WORKTREE_STATUS" ]]; then echo "STOP: working tree is not clean"; git status --short; exit 22; fi
git fetch --quiet origin "$OFFICIAL_BRANCH"
REMOTE_HEAD="$(git rev-parse "origin/$OFFICIAL_BRANCH")"
if [[ "$REMOTE_HEAD" != "$EXPECTED_HEAD" ]]; then echo "STOP: unexpected official HEAD"; echo "expected_remote_head=$EXPECTED_HEAD"; echo "actual_remote_head=$REMOTE_HEAD"; exit 23; fi
printf 'LIVE_GATE=PASS\norigin=%s\nofficial_branch=%s\nremote_head=%s\nlocal_head=%s\nworking_tree=clean\n' "$REMOTE_URL" "$OFFICIAL_BRANCH" "$REMOTE_HEAD" "$LOCAL_HEAD"
