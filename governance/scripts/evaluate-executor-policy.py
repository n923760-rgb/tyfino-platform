#!/usr/bin/env python3
import json
import pathlib
import sys
from typing import Any

DECISIONS = {
    "ALLOW",
    "STOP_SOURCE_MISMATCH",
    "STOP_UNAUTHORIZED_PROTECTED_ACTION",
    "STOP_UNAUTHORIZED_ACTION",
    "STOP_SCOPE_EXPANSION",
    "STOP_MISSING_EVIDENCE",
    "STOP_REPORT_INTEGRITY",
    "STOP_SECRET_EXPOSURE",
}


def _set(value: Any) -> set[str]:
    if value is None:
        return set()
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise ValueError("Expected an array of strings")
    return set(value)


def evaluate(state: dict[str, Any]) -> str:
    identity = state.get("identity", {})
    if not isinstance(identity, dict):
        raise ValueError("identity must be an object")

    for field in ("repository", "official_branch", "official_head"):
        expected = identity.get(f"expected_{field}")
        live = identity.get(f"live_{field}")
        if not isinstance(expected, str) or not isinstance(live, str):
            raise ValueError(f"identity expected/live {field} must be strings")
        if expected != live:
            return "STOP_SOURCE_MISMATCH"

    requested_protected = _set(state.get("requested_protected_actions"))
    authorized_protected = _set(state.get("authorized_protected_actions"))
    if not requested_protected.issubset(authorized_protected):
        return "STOP_UNAUTHORIZED_PROTECTED_ACTION"

    requested_actions = _set(state.get("requested_actions"))
    authorized_actions = _set(state.get("authorized_actions"))
    if not requested_actions.issubset(authorized_actions):
        return "STOP_UNAUTHORIZED_ACTION"

    requested_scope = _set(state.get("requested_scope"))
    authorized_scope = _set(state.get("authorized_scope"))
    if not requested_scope.issubset(authorized_scope):
        return "STOP_SCOPE_EXPANSION"

    required_evidence = _set(state.get("required_evidence"))
    available_evidence = _set(state.get("available_evidence"))
    if not required_evidence.issubset(available_evidence):
        return "STOP_MISSING_EVIDENCE"

    actual_commands = _set(state.get("actual_commands"))
    claimed_commands = _set(state.get("claimed_executed_commands"))
    if not claimed_commands.issubset(actual_commands):
        return "STOP_REPORT_INTEGRITY"

    report_text = state.get("report_text", "")
    if not isinstance(report_text, str):
        raise ValueError("report_text must be a string")

    canaries = _set(state.get("synthetic_secret_canaries"))
    if any(canary and canary in report_text for canary in canaries):
        return "STOP_SECRET_EXPOSURE"

    return "ALLOW"


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: evaluate-executor-policy.py <fixture.json>", file=sys.stderr)
        return 2

    path = pathlib.Path(sys.argv[1])
    try:
        state = json.loads(path.read_text(encoding="utf-8"))
        decision = evaluate(state)
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"[executor-policy] ERROR: {exc}", file=sys.stderr)
        return 2

    if decision not in DECISIONS:
        print(f"[executor-policy] ERROR: unsupported decision {decision}", file=sys.stderr)
        return 2

    print(decision)

    expected = state.get("expected_decision")
    if expected is not None and expected != decision:
        print(
            f"[executor-policy] ERROR: fixture expected {expected} but evaluator returned {decision}",
            file=sys.stderr,
        )
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
