#!/usr/bin/env python3
"""Offline owner APK ZIP preflight for a physical Android TV test.

Checks ZIP/metadata consistency against independently supplied CI source and
APK SHA-256 values. Does not cryptographically verify signing or run a TV test.
No network, ADB, provider credentials, licensing calls or APK installation.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import zipfile

APK = "TYFINO-device-test.apk"
FILES = {APK, "TYFINO-device-test.apk.sha256", "source-sha.txt",
         "installation-metadata.json", "certificate.txt", "INSTALL.txt"}
SCENARIOS = (
    "TV-only remote installation, launch, licensing entry and Back",
    "Arabic RTL and English LTR Home, Live, Movies, Series and Settings",
    "Remote-only Search, Favorites, History and account switching",
    "Nested removal and activation dialog cancel/explicit confirmation",
    "Audio/subtitle, picture-size and EPG dialog focus and Back",
    "Licensed test Live HLS/TS, previous channel and repeated switching",
    "EPG current/next and refreshed schedule",
    "Movie seek/resume, Series episodes and Continue Watching",
    "Pause, background, foreground, player exit and audio release",
    "Offline, timeout, invalid account, codec/stream error and recovery",
    "Overscan, readable focus, clipping, large fonts and accessibility",
    "Cold start, catalog scrolling, player start, CPU/memory/dropped frames",
)


def ensure(ok, reason):
    if not ok:
        raise ValueError(reason)


def inspect_bundle(path, expected_source, expected_digest):
    ensure(re.fullmatch(r"[0-9a-f]{40}", expected_source) is not None,
           "Expected official source SHA must be full lowercase hex")
    ensure(re.fullmatch(r"[0-9a-f]{64}", expected_digest) is not None,
           "Expected CI APK SHA-256 must be full lowercase hex")
    ensure(path.is_file() and not path.is_symlink(), "ZIP must be a regular file, not a symlink")
    try:
        with zipfile.ZipFile(path) as archive:
            infos = archive.infolist()
            ensure(len(infos) == len(FILES) and {x.filename for x in infos} == FILES,
                   "Unexpected, missing, or duplicate ZIP members")
            ensure(all(x.filename == Path(x.filename).name and
                       not x.filename.startswith("/") and "\\" not in x.filename and
                       not stat.S_ISLNK(x.external_attr >> 16) and
                       not x.is_dir() and not (x.flag_bits & 1)
                       for x in infos), "Unsafe ZIP member")
            ensure(all(0 <= x.file_size <= (120 * 1024 * 1024 if x.filename == APK else 256000)
                       for x in infos), "Oversized ZIP member")
            ensure(sum(x.file_size for x in infos) <= 121 * 1024 * 1024, "Oversized ZIP")
            other = {name: archive.read(name) for name in FILES if name != APK}
            count, digest = 0, hashlib.sha256()
            with archive.open(APK) as stream:
                for part in iter(lambda: stream.read(1024 * 1024), b""):
                    count += len(part)
                    ensure(count <= 120 * 1024 * 1024, "APK exceeds size limit")
                    digest.update(part)
            ensure(count == archive.getinfo(APK).file_size, "APK length mismatch")
        meta = json.loads(other["installation-metadata.json"].decode("utf-8"))
        sha_file = other["TYFINO-device-test.apk.sha256"].decode("ascii").strip()
        source_file = other["source-sha.txt"].decode("ascii").strip()
    except (OSError, zipfile.BadZipFile, UnicodeError, json.JSONDecodeError, RuntimeError) as exc:
        raise ValueError("Invalid or unreadable artifact ZIP") from exc
    ensure(type(meta) is dict, "Missing JSON metadata")
    ensure(meta.get("repository") == "n923760-rgb/tyfino-platform", "Wrong repository")
    ensure(meta.get("source_sha") == source_file == expected_source, "Source SHA mismatch")
    ensure(meta.get("sha256") == digest.hexdigest() == expected_digest, "APK SHA-256 mismatch")
    ensure(sha_file == digest.hexdigest() + "  " + APK, "Checksum file mismatch")
    ensure(type(meta.get("size_bytes")) is int and meta["size_bytes"] == count, "APK size mismatch")
    ensure(meta.get("variant") == "debug-owner-test", "Not owner development build")
    ensure(meta.get("application_id") == "com.tyfino.player", "Wrong application identity")
    ensure(type(meta.get("version_code")) is int and meta["version_code"] == 1 and
           meta.get("version_name") == "1.0.0", "Unexpected application version")
    ensure(meta.get("test_only") is False and meta.get("standalone") is True,
           "Non-standalone or test-only package")
    ensure(meta.get("phone_and_tv_launchers") is True and
           meta.get("leanback_required") is False and
           meta.get("touchscreen_required") is False, "TV launcher policy mismatch")
    ensure(type(meta.get("min_sdk")) is int and meta["min_sdk"] == 24 and
           type(meta.get("target_sdk")) is int and meta["target_sdk"] == 37 and
           meta.get("max_sdk") is None, "SDK policy mismatch")
    ensure(meta.get("signature_verified_api_range") == [24, 37], "Missing CI signature-check evidence")
    ensure(meta.get("licensing_origin") == "https://api.tyfino.online", "Wrong licensing endpoint")
    ensure(isinstance(meta.get("signing_certificate_sha256"), str) and
           re.fullmatch(r"[0-9a-f]{64}", meta["signing_certificate_sha256"]) is not None,
           "Invalid public certificate fingerprint")
    return {"repository": "n923760-rgb/tyfino-platform",
            "source_sha": expected_source, "apk_sha256": expected_digest,
            "apk_size_bytes": count, "bundle_consistency": "PASS",
            "signature_verified_by_this_script": "NOT RUN",
            "physical_tv_test": "NOT RUN", "provider_media_test": "NOT RUN",
            "production_signing": "NOT RUN", "customer_release": "BLOCKED"}


def worksheet(result):
    lines = ["# TYFINO — physical Android TV qualification",
             "", "Local owner worksheet. Do not publish account names, provider URLs,",
             "activation codes, device serials, stream URLs, tokens or private keys.",
             "", "## Exact source / artifact", "",
             "- Source: " + result["source_sha"],
             "- Expected and measured APK SHA-256: " + result["apk_sha256"],
             "- APK bytes: " + str(result["apk_size_bytes"]),
             "- ZIP integrity: PASS; this script has NOT independently verified APK signing",
             "- Device model, OS/API, tester, date: [record locally]",
             "- Input: physical TV remote ONLY; languages: Arabic and English",
             "", "## Physical execution checklist (not preapproved)", "",
             "| Scenario | Status | Safe observations |", "| --- | --- | --- |"]
    lines += ["| " + name + " | BLOCKED | Not run |" for name in SCENARIOS]
    lines += ["", "Change rows to PASS, FAIL or SKIPPED only after executing them.",
              "Emulator results are not physical-device qualifications.",
              "", "## Release boundary", "",
              "- Physical Android TV test: NOT RUN pending owner-provided evidence",
              "- Physical phone/real provider/low memory/TalkBack: BLOCKED",
              "- Recovered production key backups: UNKNOWN",
              "- Production signed upgrade test: NOT RUN",
              "- Failure reproduction and sanitized evidence ID: [fill locally]", ""]
    return "\n".join(lines)


def main(argv=None):
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle", type=Path, help="GitHub owner-test artifact ZIP; do not rename to APK")
    parser.add_argument("--expected-source", required=True)
    parser.add_argument("--expected-apk-sha256", required=True)
    parser.add_argument("--record", type=Path, help="Create a NEW private local Markdown worksheet")
    args = parser.parse_args(argv)
    result = inspect_bundle(args.bundle, args.expected_source, args.expected_apk_sha256)
    if args.record is not None:
        ensure(not args.record.is_symlink(), "Refusing symlink output")
        try:
            fd = os.open(args.record, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        except FileExistsError as exc:
            raise ValueError("Refusing to overwrite existing record") from exc
        with os.fdopen(fd, "w", encoding="utf-8") as file:
            file.write(worksheet(result))
    print(json.dumps(result, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError) as exc:
        print("BLOCKED: " + str(exc), file=sys.stderr)
        sys.exit(1)
