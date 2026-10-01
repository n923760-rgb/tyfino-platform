#!/usr/bin/env python3
"""Inspect owner-test APK bytes without using release signing material."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

apk = Path(sys.argv[1]).resolve()
output = Path(sys.argv[2])
output.mkdir(parents=True, exist_ok=True)
sdk = Path(os.environ["ANDROID_HOME"])
tools = sorted(
    (p for p in (sdk / "build-tools").iterdir() if re.fullmatch(r"\d+\.\d+\.\d+", p.name)),
    key=lambda p: tuple(map(int, p.name.split("."))),
)
assert tools, "No stable Android build tools installed"
build_tools = tools[-1]
manifest = subprocess.check_output(
    [str(sdk / "cmdline-tools/latest/bin/apkanalyzer"), "manifest", "print", str(apk)],
    text=True,
)
root = ET.fromstring(manifest)
android = "{http://schemas.android.com/apk/res/android}"
assert root.get("package") == "com.tyfino.player", "Unexpected application identity"
uses_sdk = root.find("uses-sdk")
assert uses_sdk is not None
minimum = int(uses_sdk.get(android + "minSdkVersion", "0"))
target = int(uses_sdk.get(android + "targetSdkVersion", "0"))
assert minimum == 24 and target == 37, "Unexpected SDK policy"
application = root.find("application")
assert application is not None
assert application.get(android + "testOnly", "false") == "false", "APK requires test-only installation"
assert root.get("split") is None and not root.findall("uses-split"), "APK is not standalone"
assert root.get(android + "versionCode") == "1"
assert root.get(android + "versionName") == "1.0.0"
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, "APK ZIP integrity failure"
    libraries = sorted(name for name in archive.namelist() if name.startswith("lib/") and name.endswith(".so"))
    abis = sorted({name.split("/")[1] for name in libraries})
    assert not libraries or "arm64-v8a" in abis, "No ARM64 native library support"
    dex_files = [name for name in archive.namelist() if re.fullmatch(r"classes(?:\d+)?\.dex", name)]
    approved_origin = b"https://api.tyfino.online"
    assert any(approved_origin in archive.read(name) for name in dex_files), "Owner-test licensing origin missing"
signature = subprocess.check_output(
    [str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs",
     "--min-sdk-version", "24", "--max-sdk-version", "33", str(apk)],
    text=True,
)
assert "Verifies" in signature
certificate = re.search(r"certificate SHA-256 digest: ([0-9a-fA-F]+)", signature)
assert certificate, "Signing certificate digest missing"
# Check native ZIP alignment needed for direct loading on Android 13.
subprocess.run([str(build_tools / "zipalign"), "-c", "-p", "4", str(apk)], check=True)
sha256 = hashlib.sha256(apk.read_bytes()).hexdigest()
evidence = {
    "repository": os.environ["GITHUB_REPOSITORY"],
    "source_sha": os.environ["TYFINO_SOURCE_SHA"],
    "variant": "debug-owner-test",
    "application_id": root.get("package"),
    "version_code": 1,
    "version_name": "1.0.0",
    "min_sdk": minimum,
    "target_sdk": target,
    "test_only": False,
    "standalone": True,
    "native_abis": abis,
    "signature_verified_api_range": [24, 33],
    "signing_certificate_sha256": certificate.group(1).lower(),
    "licensing_origin": "https://api.tyfino.online",
    "sha256": sha256,
    "size_bytes": apk.stat().st_size,
    "physical_install": "NOT RUN",
}
(output / "installation-metadata.json").write_text(json.dumps(evidence, indent=2) + "\n")
(output / "certificate.txt").write_text(signature)
(output / "TYFINO-device-test.apk.sha256").write_text(sha256 + "  TYFINO-device-test.apk\n")
(output / "source-sha.txt").write_text(evidence["source_sha"] + "\n")
(output / "INSTALL.txt").write_text(
    "Owner development test build. Extract the GitHub ZIP first, then open TYFINO-device-test.apk.\n"
    "Allow installs from the file manager when Android asks. Do not rename the ZIP to APK.\n"
    "Use installation-metadata.json and the SHA-256 file to identify these exact APK bytes.\n"
    "Temporary debug signing; physical installation/provider behavior has not been qualified.\n"
)
print(json.dumps(evidence, indent=2))
