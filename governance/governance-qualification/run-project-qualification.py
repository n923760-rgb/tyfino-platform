#!/usr/bin/env python3
from __future__ import annotations
import json, subprocess, sys, tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
LIVE=ROOT/"governance/scripts/verify-repository-state.sh"
ENV=ROOT/"governance/scripts/verify-environment-capacity.py"
POLICY=ROOT/"governance/scripts/evaluate-executor-policy.py"

class Failure(RuntimeError): pass
def run(cmd,cwd=None): return subprocess.run(cmd,cwd=cwd,text=True,capture_output=True)
def require(ok,msg):
    if not ok: raise Failure(msg)
def expect(name,cmd,code,cwd=None,contains=None):
    r=run(cmd,cwd)
    combined=r.stdout+"\n"+r.stderr
    require(r.returncode==code,f"{name}: expected {code}, got {r.returncode}\n{combined}")
    if contains: require(contains in combined,f"{name}: missing {contains!r}\n{combined}")
    print(f"PASS: {name}")
def git(cwd,*args):
    r=run(["git",*args],cwd)
    require(r.returncode==0,f"git {' '.join(args)} failed\n{r.stdout}\n{r.stderr}")
    return r.stdout.strip()
def write(path,data): path.write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8")

def base_state():
    return {
      "identity":{
        "expected_repository":"n923760-rgb/tyfino-platform","live_repository":"n923760-rgb/tyfino-platform",
        "expected_official_branch":"main","live_official_branch":"main",
        "expected_official_head":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","live_official_head":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      },
      "requested_protected_actions":[],"authorized_protected_actions":[],
      "requested_actions":["read"],"authorized_actions":["read"],
      "requested_scope":["governance"],"authorized_scope":["governance"],
      "required_evidence":[],"available_evidence":[],
      "actual_commands":[],"claimed_executed_commands":[],
      "report_text":"","synthetic_secret_canaries":[]
    }

def main():
  with tempfile.TemporaryDirectory(prefix="tyfino-govq-") as td:
    tmp=Path(td)
    remote=tmp/"remote.git"; seed=tmp/"seed"; work=tmp/"work"
    require(run(["git","init","--bare",str(remote)]).returncode==0,"bare init failed")
    require(run(["git","init","-b","main",str(seed)]).returncode==0,"seed init failed")
    git(seed,"config","user.email","qualification@example.invalid"); git(seed,"config","user.name","TYFINO Qualification")
    (seed/"README.md").write_text("synthetic qualification fixture\n",encoding="utf-8")
    git(seed,"add","README.md"); git(seed,"commit","-m","fixture: initial"); git(seed,"remote","add","origin",str(remote)); git(seed,"push","-u","origin","main")
    head=git(seed,"rev-parse","HEAD")
    require(run(["git","clone","-b","main",str(remote),str(work)]).returncode==0,"clone failed")

    expect("clean repository accepted",["bash",str(LIVE),"main",head,"remote.git"],0,work,"LIVE_GATE=PASS")
    (work/"README.md").write_text("dirty fixture\n",encoding="utf-8")
    expect("dirty worktree stops safely",["bash",str(LIVE),"main",head,"remote.git"],22,work,"STOP: working tree is not clean")
    git(work,"reset","--hard","HEAD")
    expect("wrong SHA stops safely",["bash",str(LIVE),"main","0"*40,"remote.git"],23,work,"STOP: unexpected official HEAD")

    unauthorized=base_state(); unauthorized["requested_actions"]=["write"]; unauthorized["expected_decision"]="STOP_UNAUTHORIZED_ACTION"
    up=tmp/"unauthorized.json"; write(up,unauthorized)
    expect("unauthorized action stops safely",[sys.executable,str(POLICY),str(up)],0,None,"STOP_UNAUTHORIZED_ACTION")

    missing=base_state(); missing["required_evidence"]=["physical_runtime"]; missing["available_evidence"]=[]; missing["expected_decision"]="STOP_MISSING_EVIDENCE"
    mp=tmp/"missing-evidence.json"; write(mp,missing)
    expect("missing evidence stops safely",[sys.executable,str(POLICY),str(mp)],0,None,"STOP_MISSING_EVIDENCE")

    expect("resource failure stops safely",[sys.executable,str(ENV),"--path",str(tmp),"--min-free-bytes",str(2**63-1)],31,None,"insufficient free disk")

  print("PASS: TYFINO governance qualification adversarial suite completed")

if __name__=="__main__":
  try: main()
  except Failure as exc:
    print(f"FAIL: {exc}",file=sys.stderr); raise SystemExit(1)
