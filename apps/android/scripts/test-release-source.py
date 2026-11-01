#!/usr/bin/env python3
"""Exercise release source/signature guards with disposable fixtures; no SDK/key/network."""
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest

SCRIPT = Path(__file__).with_name("verify-release-source.sh").resolve()
WORKFLOW = SCRIPT.parents[3] / ".github/workflows/release-apk.yml"


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


class ReleaseSignatureTest(unittest.TestCase):
    """Run the actual workflow verification block with synthetic APK/verifier output."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="tyfino-release-signature-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        git(self.root, "init", "--initial-branch=main")
        git(self.root, "config", "user.name", "Release fixture")
        git(self.root, "config", "user.email", "fixture@example.invalid")
        (self.root / "source.txt").write_text("synthetic release verification fixture")
        git(self.root, "add", "source.txt")
        git(self.root, "commit", "-m", "fixture")
        self.source = git(self.root, "rev-parse", "HEAD")
        self.apk = self.root / "app/build/outputs/apk/release/app-release.apk"
        self.apk.parent.mkdir(parents=True)
        self.apk.write_bytes(b"synthetic APK; never signed or installed")
        self.output = self.root / "build/release-output"
        self.fingerprint = "ab" * 32
        sdk = self.root / "sdk"
        signer = sdk / "build-tools/fixture/apksigner"
        signer.parent.mkdir(parents=True)
        signer.write_text('#!/bin/bash\nprintf "Signer #1 certificate SHA-256 digest: %s\\n" "$FIXTURE_CERT"\nexit "$FIXTURE_VERIFY_STATUS"\n')
        signer.chmod(0o700)
        self.env = {
            **os.environ,
            "ANDROID_HOME": str(sdk),
            "TYFINO_ANDROID_CERT_SHA256": self.fingerprint,
            "FIXTURE_CERT": self.fingerprint,
            "FIXTURE_VERIFY_STATUS": "0",
        }
        # Extract the production literal run block without introducing a YAML dependency.
        block = WORKFLOW.read_text().split("      - name: Verify signing certificate and package\n", 1)[1]
        block = block.split("      - name: Upload signed APK for owner review\n", 1)[0]
        script = textwrap.dedent(block.split("        run: |\n", 1)[1])
        self.script = self.root / "verify.sh"
        self.script.write_text(script)

    def verify(self, **env):
        # GitHub's unspecified Linux shell uses bash -e, without automatic pipefail.
        return subprocess.run(
            ["bash", "--noprofile", "--norc", "-e", str(self.script)],
            cwd=self.root, env={**self.env, **env}, text=True, capture_output=True, timeout=10,
        )

    def assert_no_review_package(self):
        for name in ("TYFINO-1.0.0.apk", "TYFINO-1.0.0.apk.sha256", "source-sha.txt"):
            self.assertFalse((self.output / name).exists(), name)

    def test_success_emits_exact_apk_checksum_and_source(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(self.apk.read_bytes(), (self.output / "TYFINO-1.0.0.apk").read_bytes())
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(),
                         (self.output / "TYFINO-1.0.0.apk.sha256").read_text().split()[0])
        self.assertEqual(self.source, (self.output / "source-sha.txt").read_text().strip())

    def test_verifier_failure_rejects_even_matching_reported_certificate(self):
        result = self.verify(FIXTURE_VERIFY_STATUS="7")
        self.assertNotEqual(0, result.returncode, "Verifier failure must block the owner-review package")
        self.assert_no_review_package()

    def test_wrong_certificate_rejects(self):
        result = self.verify(FIXTURE_CERT="cd" * 32)
        self.assertNotEqual(0, result.returncode)
        self.assert_no_review_package()

    def test_missing_certificate_rejects(self):
        result = self.verify(FIXTURE_CERT="")
        self.assertNotEqual(0, result.returncode)
        self.assert_no_review_package()


if __name__ == "__main__":
    unittest.main()
