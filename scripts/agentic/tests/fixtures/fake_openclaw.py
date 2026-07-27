#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--agent-id", required=True)
    parser.add_argument("--stage-id", required=True)
    parser.add_argument("--stage-type", required=True)
    parser.add_argument("--stage-run-id", required=True)
    parser.add_argument("--task-id", required=True)
    parser.add_argument("--worktree", required=True)
    parser.add_argument("--prompt-file", required=True)
    parser.add_argument("--result-file", required=True)
    args = parser.parse_args()

    worktree = pathlib.Path(args.worktree)
    behavior_path = worktree / ".fake-openclaw-behavior.json"
    behavior = {}
    if behavior_path.exists():
        behavior = json.loads(behavior_path.read_text(encoding="utf-8"))
    stage = behavior.get(args.stage_id, {})

    invocations_path = pathlib.Path(args.result_file).with_suffix(".invocations.log")
    with invocations_path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(vars(args), sort_keys=True) + "\n")

    for mutation in stage.get("mutations", []):
        target = worktree / mutation["path"]
        target.parent.mkdir(parents=True, exist_ok=True)
        kind = mutation.get("kind", "file")
        if kind == "delete":
            if target.exists() or target.is_symlink():
                if target.is_dir() and not target.is_symlink():
                    for child in sorted(target.rglob("*"), reverse=True):
                        if child.is_file() or child.is_symlink():
                            child.unlink()
                        elif child.is_dir():
                            child.rmdir()
                    target.rmdir()
                else:
                    target.unlink()
        elif kind == "symlink":
            if target.exists() or target.is_symlink():
                target.unlink()
            os.symlink(mutation["target"], target)
        else:
            target.write_text(mutation.get("content", ""), encoding="utf-8")

    prompt_contents = pathlib.Path(args.prompt_file).read_text(encoding="utf-8")
    print(f"AGENT={args.agent_id}")
    print(f"STAGE={args.stage_id}")
    print(f"PROMPT_SHA={len(prompt_contents)}")
    for line in stage.get("stdout", []):
        print(line)

    ledger_evidence = stage.get("ledgerEvidence")
    if (
        ledger_evidence is None
        and not stage.get("omitLedgerEvidence", False)
        and args.stage_type in {"writer", "documentation-writer"}
    ):
        files_changed: list[str] = []
        for mutation in stage.get("mutations", []):
            path = mutation.get("path")
            if isinstance(path, str) and path not in files_changed:
                files_changed.append(path)
        ledger_evidence = {
            "schemaVersion": "fop-task-ledger-evidence/v1",
            "agentId": args.agent_id,
            "stageRunId": args.stage_run_id,
            "stageType": args.stage_type,
            "status": "COMPLETED",
            "filesChanged": files_changed,
        }

    result_path = pathlib.Path(args.result_file)
    result_path.parent.mkdir(parents=True, exist_ok=True)
    result_payload = {"verdict": stage.get("verdict", "PASS")}
    if ledger_evidence is not None:
        result_payload["ledgerEvidence"] = ledger_evidence
    result_path.write_text(json.dumps(result_payload, sort_keys=True), encoding="utf-8")
    return int(stage.get("exitCode", 0))


if __name__ == "__main__":
    sys.exit(main())
