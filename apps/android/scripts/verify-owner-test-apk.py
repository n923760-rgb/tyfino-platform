#!/usr/bin/env python3
"""Inspect owner-test APK bytes without using release signing material."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import struct
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
assert uses_sdk.get(android + "maxSdkVersion") is None, "APK restricts newer Android versions"
application = root.find("application")
assert application is not None
assert application.get(android + "testOnly", "false") == "false", "APK requires test-only installation"
assert root.get("split") is None and not root.findall("uses-split"), "APK is not standalone"
assert root.get(android + "versionCode") == "1"
assert root.get(android + "versionName") == "1.0.0"
features = {feature.get(android + "name"): feature for feature in root.findall("uses-feature")}
for name in ("android.hardware.touchscreen", "android.software.leanback"):
    assert name in features and features[name].get(android + "required") == "false", "Phone/TV feature is mandatory"
assert application.get(android + "banner"), "TV launcher banner missing"
launcher_categories = {
    category.get(android + "name")
    for activity in application.findall("activity")
    for intent in activity.findall("intent-filter")
    for category in intent.findall("category")
}
assert {"android.intent.category.LAUNCHER", "android.intent.category.LEANBACK_LAUNCHER"} <= launcher_categories

def inspect_elf(name, data):
    abi = name.split("/")[1]
    expected_class, expected_machine = {
        "arm64-v8a": (2, 183), "armeabi-v7a": (1, 40),
        "x86": (1, 3), "x86_64": (2, 62),
    }[abi]
    assert len(data) >= 64 and data[:4] == bytes([127]) + b"ELF", "Native library is not ELF"
    assert data[4] == expected_class and data[5] == 1, "ELF architecture mismatch"
    assert struct.unpack_from("<H", data, 18)[0] == expected_machine, "ELF machine mismatch"
    result = {"library": name, "abi": abi}
    if expected_class == 2:
        offset = struct.unpack_from("<Q", data, 32)[0]
        entry_size, count = struct.unpack_from("<HH", data, 54)
        assert entry_size >= 56 and count > 0 and offset + count * entry_size <= len(data)
        alignments = []
        for index in range(count):
            segment = struct.unpack_from("<IIQQQQQQ", data, offset + index * entry_size)
            if segment[0] == 1:  # PT_LOAD
                file_offset, virtual_address, alignment = segment[2], segment[3], segment[7]
                assert alignment >= 16384 and alignment & (alignment - 1) == 0, "64-bit library lacks 16KB load alignment"
                assert file_offset % alignment == virtual_address % alignment, "ELF load offset is misaligned"
                alignments.append(alignment)
        assert alignments, "Native library has no load segments"
        result["minimum_load_alignment_bytes"] = min(alignments)
    return result
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, "APK ZIP integrity failure"
    libraries = sorted(name for name in archive.namelist() if name.startswith("lib/") and name.endswith(".so"))
    abis = sorted({name.split("/")[1] for name in libraries})
    required_abis = {"arm64-v8a", "armeabi-v7a", "x86", "x86_64"}
    assert not libraries or set(abis) == required_abis, "Universal APK must contain all four native architectures"
    per_abi = {abi: {name.rsplit("/", 1)[1] for name in libraries if name.split("/")[1] == abi} for abi in abis}
    assert not libraries or all(names == per_abi["arm64-v8a"] for names in per_abi.values()), "Incomplete native-library coverage"
    elf_checks = [inspect_elf(name, archive.read(name)) for name in libraries]
    dex_files = [name for name in archive.namelist() if re.fullmatch(r"classes(?:\d+)?\.dex", name)]
    approved_origin = b"https://api.tyfino.online"
    assert any(approved_origin in archive.read(name) for name in dex_files), "Owner-test licensing origin missing"
signature = subprocess.check_output(
    [str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs",
     "--min-sdk-version", "24", "--max-sdk-version", "37", str(apk)],
    text=True,
)
assert "Verifies" in signature
certificate = re.search(r"certificate SHA-256 digest: ([0-9a-fA-F]+)", signature)
assert certificate, "Signing certificate digest missing"
# Verify 16KB native ZIP pages while retaining ordinary 4-byte entry alignment.
subprocess.run([str(build_tools / "zipalign"), "-c", "-P", "16", "4", str(apk)], check=True)
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
    "signature_verified_api_range": [24, 37],
    "native_elf_checks": elf_checks,
    "native_zip_page_alignment_bytes": 16384,
    "touchscreen_required": False,
    "leanback_required": False,
    "phone_and_tv_launchers": True,
    "max_sdk": None,
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
