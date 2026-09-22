#!/usr/bin/env python3
import argparse, os, shutil, tempfile
from pathlib import Path
EXIT_PATH_UNAVAILABLE=30
EXIT_INSUFFICIENT_DISK=31
EXIT_NOT_WRITABLE=32
def stop(message,code):
    print(f"STOP: {message}")
    raise SystemExit(code)
def non_negative_int(value):
    parsed=int(value)
    if parsed<0: raise argparse.ArgumentTypeError("must be non-negative")
    return parsed
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--path",default=".")
    p.add_argument("--min-free-bytes",type=non_negative_int,default=0)
    p.add_argument("--skip-write-test",action="store_true")
    a=p.parse_args()
    target=Path(a.path)
    if not target.exists() or not target.is_dir(): stop(f"environment path is unavailable or not a directory: {target}",EXIT_PATH_UNAVAILABLE)
    try: usage=shutil.disk_usage(target)
    except OSError as exc: stop(f"cannot inspect disk capacity for {target}: {exc}",EXIT_PATH_UNAVAILABLE)
    if usage.free<a.min_free_bytes: stop(f"insufficient free disk: required={a.min_free_bytes} actual={usage.free}",EXIT_INSUFFICIENT_DISK)
    if not a.skip_write_test:
        try:
            with tempfile.NamedTemporaryFile(mode="wb",dir=target,prefix=".governance-write-test-",delete=True) as h:
                h.write(b"governance-write-test\n"); h.flush(); os.fsync(h.fileno())
        except OSError as exc: stop(f"environment path is not safely writable: {target}: {exc}",EXIT_NOT_WRITABLE)
    print("ENVIRONMENT_GATE=PASS")
if __name__=="__main__": main()
