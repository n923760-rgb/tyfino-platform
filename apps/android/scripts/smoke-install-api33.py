#!/usr/bin/env python3
"""Clean-install and launch the exact configured artifact on an offline API33 emulator."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

downloads = Path(sys.argv[1]).resolve()
source = os.environ["TYFINO_SOURCE_SHA"]
candidates = []
for path in downloads.iterdir():
    match = re.fullmatch("tyfino-owner-test-" + re.escape(source) + r"-([0-9]+)", path.name)
    if path.is_dir() and match:
        candidates.append((int(match.group(1)), path))
assert candidates, "No exact-source owner APK artifact downloaded"
owner_attempt, bundle = max(candidates, key=lambda candidate: candidate[0])
apk = bundle / "TYFINO-device-test.apk"
metadata = json.loads((bundle / "installation-metadata.json").read_text())
assert metadata["source_sha"] == source and (bundle / "source-sha.txt").read_text().strip() == source
assert hashlib.sha256(apk.read_bytes()).hexdigest() == metadata["sha256"], "Downloaded APK checksum mismatch"
assert not metadata["test_only"] and metadata["standalone"]
assert metadata["application_id"] == "com.tyfino.player"

sdk = Path(os.environ["ANDROID_HOME"])
adb = sdk / "platform-tools/adb"
emulator = sdk / "emulator/emulator"
manager = sdk / "cmdline-tools/latest/bin/avdmanager"
sdkmanager = sdk / "cmdline-tools/latest/bin/sdkmanager"
image = "system-images;android-33;google_apis;x86_64"
serial = "emulator-5580"
name = "tyfino_clean_install_api33"
evidence_dir = Path("build/api33-install-evidence")
evidence_dir.mkdir(parents=True, exist_ok=True)

def command(args, *, timeout=30, check=True, input_text=None, environment=None):
    result = subprocess.run(
        [str(arg) for arg in args], input=input_text, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout, check=False, env=environment,
    )
    if check and result.returncode != 0:
        detail = " ".join((result.stderr or result.stdout).splitlines()[:3])[:400]
        detail = re.sub(r"(?:https?|rtsp|rtsps)://[^ ]+", "[URL redacted]", detail)
        raise RuntimeError("Command failed: " + Path(str(args[0])).name + ": " + detail)
    return result

def device(*args, **kwargs):
    return command([adb, "-s", serial, *args], **kwargs)

runtime = Path(os.environ["RUNNER_TEMP"])
android_user = runtime / "tyfino-api33-android-user"
avd_directory = android_user / "avd"
avd_directory.mkdir(parents=True, exist_ok=True)
android_environment = os.environ.copy()
android_environment["ANDROID_USER_HOME"] = str(android_user)
android_environment["ANDROID_AVD_HOME"] = str(avd_directory)
command([sdkmanager, image], timeout=900, environment=android_environment)
command([manager, "create", "avd", "--name", name, "--package", image,
         "--device", "pixel_2", "--path", avd_directory / (name + ".avd"), "--force"],
        timeout=60, input_text="no\n", environment=android_environment)
assert (avd_directory / (name + ".ini")).is_file(), "AVD registry missing from the explicit emulator directory"
with (runtime / "tyfino-api33-emulator.log").open("w") as emulator_log:
    process = subprocess.Popen(
        [str(emulator), "-avd", name, "-port", "5580", "-no-window", "-no-audio",
         "-no-boot-anim", "-no-snapshot", "-wipe-data", "-gpu", "swiftshader_indirect",
         "-memory", "1536", "-cores", "2"],
        stdout=emulator_log, stderr=subprocess.STDOUT, env=android_environment,
    )
    def boot_failure(reason):
        diagnostic = (runtime / "tyfino-api33-emulator.log").read_text(errors="replace")
        selected = [line for line in diagnostic.splitlines()
                    if any(marker in line.upper() for marker in ("ERROR", "FATAL", "PANIC", "WARNING"))]
        detail = " ".join(selected[-8:])[:1200]
        detail = re.sub(r"(?:https?|rtsp|rtsps)://[^ ]+", "[URL redacted]", detail)
        raise RuntimeError(reason + "; exit=" + str(process.poll()) + "; " + detail)

    try:
        deadline = time.monotonic() + 300
        while time.monotonic() < deadline:
            if process.poll() is not None:
                boot_failure("API33 emulator exited before boot")
            boot = device("shell", "getprop", "sys.boot_completed", check=False, timeout=10)
            if boot.returncode == 0 and boot.stdout.strip() == "1":
                break
            time.sleep(3)
        else:
            boot_failure("API33 emulator boot timed out")
        assert device("shell", "getprop", "ro.build.version.sdk").stdout.strip() == "33"
        abi = device("shell", "getprop", "ro.product.cpu.abi").stdout.strip()
        assert abi == "x86_64"
        # Keep the first-run launch offline; no licensing/provider action is invoked.
        device("shell", "cmd", "connectivity", "airplane-mode", "enable")
        device("shell", "svc", "wifi", "disable")
        device("shell", "svc", "data", "disable")
        assert device("shell", "settings", "get", "global", "airplane_mode_on").stdout.strip() == "1"
        installed = device("shell", "pm", "list", "packages", "com.tyfino.player").stdout
        assert "package:com.tyfino.player" not in installed, "Emulator was not a clean install"
        result = device("install", str(apk), timeout=120)  # No -t, -r, bypass or grant flags.
        assert re.search(r"^Success$", result.stdout, re.MULTILINE), "APK installer did not report Success"
        assert "base.apk" in device("shell", "pm", "path", "com.tyfino.player").stdout
        resolved = device("shell", "cmd", "package", "resolve-activity", "--brief",
                          "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
                          "-p", "com.tyfino.player").stdout
        launcher_components = [line.strip() for line in resolved.splitlines()
                               if re.fullmatch(r"com\.tyfino\.player/[A-Za-z0-9_.$]+", line.strip())]
        assert len(launcher_components) == 1, "Installed package has no unique phone launcher"
        launcher_component = launcher_components[0]
        launched = device("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN",
                          "-c", "android.intent.category.LAUNCHER", "-n", launcher_component, timeout=60)
        if not re.search(r"^Status: ok$", launched.stdout, re.MULTILINE):
            prefixes = ("Status:", "Error:", "Warning:", "Activity:", "LaunchState:", "TotalTime:", "WaitTime:")
            selected = [line for line in (launched.stdout + "\n" + launched.stderr).splitlines()
                        if line.strip().startswith(prefixes)][:8]
            detail = json.dumps(selected)[:1000]
            detail = re.sub(r"(?:https?|rtsp|rtsps)://[^ ]+", "[URL redacted]", detail)
            alive = bool(device("shell", "pidof", "com.tyfino.player", check=False, timeout=10).stdout.strip())
            state = device("shell", "dumpsys", "activity", "activities", check=False, timeout=10).stdout
            resumed = any("com.tyfino.player/" in line and
                          ("mResumedActivity" in line or "topResumedActivity" in line)
                          for line in state.splitlines())
            raise RuntimeError("Launcher did not report success; selected=" + detail +
                               "; package_alive=" + str(alive) + "; target_resumed=" + str(resumed))
        time.sleep(5)
        assert device("shell", "pidof", "com.tyfino.player").stdout.strip(), "Application exited after launch"
        activities = device("shell", "dumpsys", "activity", "activities").stdout
        assert any("com.tyfino.player/" in line and
                   ("mResumedActivity" in line or "topResumedActivity" in line)
                   for line in activities.splitlines()), "TYFINO is not the resumed activity"
        evidence = {
            "repository": os.environ["GITHUB_REPOSITORY"], "source_sha": source,
            "owner_artifact_attempt": owner_attempt,
            "apk_sha256": metadata["sha256"], "apk_size_bytes": apk.stat().st_size,
            "signing_certificate_sha256": metadata["signing_certificate_sha256"],
            "android_api": 33, "emulator_abi": abi, "profile": "Pixel 2",
            "clean_install": "PASS", "install_test_only_flag": False,
            "launcher_start": "PASS", "launcher_component": launcher_component,
            "process_alive_after_seconds": 5,
            "resumed_activity": "PASS", "network": "airplane-mode",
            "physical_device": "NOT RUN", "provider_media": "NOT RUN",
        }
        (evidence_dir / "result.json").write_text(json.dumps(evidence, indent=2) + "\n")
        print("PASS: exact configured APK clean-installs and launches offline on API33 without test-only flags")
        print(json.dumps(evidence, indent=2))
    finally:
        try:
            device("emu", "kill", check=False, timeout=10)
        except (subprocess.TimeoutExpired, OSError):
            pass
        if process.poll() is None:
            process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
