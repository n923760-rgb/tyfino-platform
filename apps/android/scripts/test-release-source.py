#!/usr/bin/env python3
"""Exercise the signing preflight against disposable real Git repositories; no SDK/key/network."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("verify-release-source.sh").resolve()


def git(directory, *args):
    return subprocess.check_output(
        ["git", "-C", str(directory), *args], text=True, stderr=subprocess.PIPE,
    ).strip()


class ReleaseSourceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="tyfino-release-source-")
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.remote = root / "origin"
        self.remote.mkdir()
        git(self.remote, "init", "--initial-branch=main")
        git(self.remote, "config", "user.name", "Release fixture")
        git(self.remote, "config", "user.email", "fixture@example.invalid")
        self.old = self.commit("first")
        self.source = self.commit("reviewed")
        self.checkout = root / "checkout"
        git(root, "clone", str(self.remote), str(self.checkout))
        self.env = {
            **os.environ,
            "GITHUB_REPOSITORY": "n923760-rgb/tyfino-platform",
            "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_SHA": self.source,
            "TYFINO_EXPECTED_SOURCE_SHA": self.source,
        }

    def commit(self, value):
        (self.remote / "source.txt").write_text(value)
        git(self.remote, "add", "source.txt")
        git(self.remote, "commit", "-m", value)
        return git(self.remote, "rev-parse", "HEAD")

    def check(self, accepted, message, **env):
        result = subprocess.run(
            ["bash", str(SCRIPT)], cwd=self.checkout, env={**self.env, **env},
            text=True, capture_output=True, timeout=10,
        )
        self.assertEqual(result.returncode == 0, accepted, result.stdout + result.stderr)
        self.assertIn(message, result.stdout + result.stderr)

    def test_exact_current_main_passes(self):
        self.check(True, "PASS: exact official source " + self.source)

    def test_wrong_repository_fails(self):
        self.check(False, "Wrong repository", GITHUB_REPOSITORY="other/project")

    def test_non_manual_event_fails(self):
        self.check(False, "Manual owner request required", GITHUB_EVENT_NAME="push")

    def test_branch_and_tag_requests_fail(self):
        for ref in ("refs/heads/topic", "refs/tags/v1"):
            with self.subTest(ref=ref):
                self.check(False, "Select the official main", GITHUB_REF=ref)

    def test_missing_short_or_shell_input_fails(self):
        for source in ("", self.source[:7], "$(touch unexpected)", "a" * 41):
            with self.subTest(source=source):
                self.check(False, "Supply the full reviewed source SHA", TYFINO_EXPECTED_SOURCE_SHA=source)
        self.assertFalse((self.checkout / "unexpected").exists())

    def test_workflow_source_mismatch_fails(self):
        self.check(False, "Workflow source differs", GITHUB_SHA=self.old)

    def test_wrong_checkout_fails(self):
        git(self.checkout, "checkout", "--detach", self.old)
        self.check(False, "Checkout differs")

    def test_modified_source_fails(self):
        (self.checkout / "source.txt").write_text("unreviewed")
        self.check(False, "Tracked source is modified")

    def test_main_drift_detected_even_with_stale_tracking_ref(self):
        self.commit("new official source")
        self.assertEqual(git(self.checkout, "rev-parse", "origin/main"), self.source)
        self.check(False, "Official main has moved")

    def test_fetch_failure_cannot_accept_cached_main(self):
        git(self.checkout, "remote", "set-url", "origin", str(self.remote / "missing"))
        self.check(False, "does not appear to be a git repository")


if __name__ == "__main__":
    unittest.main()
