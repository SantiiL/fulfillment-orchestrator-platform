#!/usr/bin/env python3
from __future__ import annotations

import json
import pathlib
import sys


def main() -> int:
    control_path = pathlib.Path(__file__).with_name("fake_runtime_verifier.state.json")
    control = {"exitCode": 0, "stdout": ["VERIFIER_OK"]}
    if control_path.exists():
        control = json.loads(control_path.read_text(encoding="utf-8"))
    log_path = pathlib.Path(__file__).with_name("fake_runtime_verifier.invocations.log")
    with log_path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(control, sort_keys=True) + "\n")
    for line in control.get("stdout", []):
        print(line)
    for line in control.get("stderr", []):
        print(line, file=sys.stderr)
    return int(control.get("exitCode", 0))


if __name__ == "__main__":
    sys.exit(main())
