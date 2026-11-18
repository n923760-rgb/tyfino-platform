#!/usr/bin/env python3
"""Synthetic-only regression tests; no provider, APK signing, device or network."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

script = Path(__file__).with_name("tv-bundle-preflight.py")
spec = importlib.util.spec_from_file_location("tv_bundle_preflight", script)
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)
SOURCE = "d240642b4943ba9bb0f63205cffecbca94d0b29f"
APK = b"synthetic-fixture-no-real-signature" * 3
DIGEST = hashlib.sha256(APK).hexdigest()


def archive_members(apk=APK, source=SOURCE):
    digest = hashlib.sha256(apk).hexdigest()
    metadata = {
        "repository": "n923760-rgb/tyfino-platform", "source_sha": source,
        "sha256": digest, "size_bytes": len(apk), "variant": "debug-owner-test",
        "application_id": "com.tyfino.player", "version_code": 1,
        "version_name": "1.0.0", "test_only": False, "standalone": True,
        "phone_and_tv_launchers": True, "leanback_required": False,
        "touchscreen_required": False, "min_sdk": 24, "target_sdk": 37,
        "max_sdk": None, "signature_verified_api_range": [24, 37],
        "licensing_origin": "https://api.tyfino.online",
        "signing_certificate_sha256": "a" * 64,
    }
    return {
        "TYFINO-device-test.apk": apk,
        "TYFINO-device-test.apk.sha256": (digest + "  TYFINO-device-test.apk\n").encode(),
        "source-sha.txt": (source + "\n").encode(),
        "installation-metadata.json": json.dumps(metadata).encode(),
        "certificate.txt": b"public fixture only", "INSTALL.txt": b"synthetic",
    }


def make_zip(path, items):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, value in items.items():
            archive.writestr(name, value)


class OfflineBundleTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.path = Path(temp.name) / "owner.zip"

    def verify(self, expected_source=SOURCE, expected_digest=DIGEST):
        return tool.inspect_bundle(self.path, expected_source, expected_digest)

    def test_exact_bundle_validates_consistency_not_signature_or_tv(self):
        make_zip(self.path, archive_members())
        result = self.verify()
        self.assertEqual(result["bundle_consistency"], "PASS")
        self.assertEqual(result["signature_verified_by_this_script"], "NOT RUN")
        self.assertEqual(result["physical_tv_test"], "NOT RUN")
        self.assertEqual(result["customer_release"], "BLOCKED")
        self.assertEqual(tool.worksheet(result).count("| BLOCKED | Not run |"), len(tool.SCENARIOS))

    def test_wrong_expected_source_fails(self):
        make_zip(self.path, archive_members())
        with self.assertRaisesRegex(ValueError, "Source SHA mismatch"):
            self.verify(expected_source="0" * 40)

    def test_wrong_trusted_digest_fails_even_if_bundle_self_consistent(self):
        make_zip(self.path, archive_members(b"attacker bytes"))
        with self.assertRaisesRegex(ValueError, "APK SHA-256 mismatch"):
            self.verify()

    def test_modified_apk_fails(self):
        items = archive_members()
        items["TYFINO-device-test.apk"] = APK + b"tampered"
        make_zip(self.path, items)
        with self.assertRaisesRegex(ValueError, "APK SHA-256 mismatch"):
            self.verify()

    def test_wrong_package_fails(self):
        items = archive_members()
        record = json.loads(items["installation-metadata.json"])
        record["application_id"] = "com.malicious.player"
        items["installation-metadata.json"] = json.dumps(record).encode()
        make_zip(self.path, items)
        with self.assertRaisesRegex(ValueError, "Wrong application identity"):
            self.verify()

    def test_tv_launcher_policy_cannot_be_falsified(self):
        items = archive_members()
        record = json.loads(items["installation-metadata.json"])
        record["phone_and_tv_launchers"] = False
        items["installation-metadata.json"] = json.dumps(record).encode()
        make_zip(self.path, items)
        with self.assertRaisesRegex(ValueError, "TV launcher policy mismatch"):
            self.verify()

    def test_traversal_and_duplicate_entries_fail(self):
        items = archive_members()
        items["../other"] = b"not allowed"
        make_zip(self.path, items)
        with self.assertRaisesRegex(ValueError, "Unexpected, missing, or duplicate"):
            self.verify()
        make_zip(self.path, archive_members())
        with zipfile.ZipFile(self.path, "a") as z:
            with self.assertWarns(UserWarning):
                z.writestr("INSTALL.txt", "duplicate")
        with self.assertRaisesRegex(ValueError, "Unexpected, missing, or duplicate"):
            self.verify()

    def test_zip_symlink_not_accepted(self):
        original = self.path.parent / "real.zip"
        make_zip(original, archive_members())
        self.path.symlink_to(original)
        with self.assertRaisesRegex(ValueError, "not a symlink"):
            self.verify()

    def test_existing_record_is_not_overwritten(self):
        make_zip(self.path, archive_members())
        record = self.path.parent / "record.md"
        record.write_text("existing private notes")
        with self.assertRaisesRegex(ValueError, "Refusing to overwrite"):
            tool.main([str(self.path), "--expected-source", SOURCE,
                       "--expected-apk-sha256", DIGEST, "--record", str(record)])
        self.assertEqual(record.read_text(), "existing private notes")

    def test_new_record_has_private_permissions(self):
        make_zip(self.path, archive_members())
        record = self.path.parent / "record.md"
        self.assertEqual(tool.main([str(self.path), "--expected-source", SOURCE,
                                    "--expected-apk-sha256", DIGEST, "--record", str(record)]), 0)
        self.assertEqual(record.stat().st_mode & 0o777, 0o600)
        self.assertIn("Android TV qualification", record.read_text())


if __name__ == "__main__":
    unittest.main()
