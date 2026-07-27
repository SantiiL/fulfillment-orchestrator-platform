#!/usr/bin/env python3
"""Deterministic specialist runner for repository-governed task execution."""

from __future__ import annotations

import argparse
import ctypes
import errno
import hashlib
import json
import os
import re
import shutil
import signal
import stat
import subprocess
import sys
import tempfile
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Dict, Iterable, List, Optional, Sequence, Tuple


SPEC_SCHEMA_VERSION = "fop-task-spec/v1"
RUNTIME_CONFIG_SCHEMA_VERSION = "fop-deterministic-runner-runtime/v1"
STATE_SCHEMA_VERSION = "fop-deterministic-runner-state/v1"
LEDGER_EVIDENCE_SCHEMA_VERSION = "fop-task-ledger-evidence/v1"
DEFAULT_RUNTIME_CONFIG_PATH = "~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json"
MAX_SPEC_BYTES = 262144
MAX_PROMPT_BYTES = 131072
MAX_STAGE_ATTEMPTS = 5
DEFAULT_TIMEOUT_SECONDS = 900

STATUS_PREPARING = "PREPARING"
STATUS_PREPARED = "PREPARED"
STATUS_RUNNING = "RUNNING"
STATUS_PAUSED_FAILED = "PAUSED_FAILED"
STATUS_PAUSED_BLOCKED = "PAUSED_BLOCKED"
STATUS_WAITING_FOR_HUMAN_APPROVAL = "WAITING_FOR_HUMAN_APPROVAL"

WRITER_STAGE_TYPES = {"writer", "documentation-writer"}
READ_ONLY_STAGE_TYPES = {"read-only-gate", "final-reviewer"}
SUCCESS_VERDICTS = {"PASS", "APPROVE"}
FAILURE_AGENT_EXIT = "AGENT_EXIT"
FAILURE_ATTEMPT_LIMIT = "ATTEMPT_LIMIT"
FAILURE_INDEPENDENCE = "INDEPENDENCE_FAIL"
FAILURE_LEDGER = "LEDGER_MISMATCH"
FAILURE_LOCK = "LOCK_CONFLICT"
FAILURE_MIRROR_DRIFT = "MIRROR_DRIFT"
FAILURE_PATH = "PATH_VIOLATION"
FAILURE_PREPARE = "RUN_ALREADY_EXISTS"
FAILURE_RUNTIME = "RUNTIME_VERIFY_FAILED"
FAILURE_SPEC = "SPEC_INVALID"
FAILURE_STATE = "STATE_TAMPERED"
FAILURE_TIMEOUT = "TIMEOUT"
FAILURE_WORKTREE = "WORKTREE_INVALID"

FORBIDDEN_ENV_PREFIXES = ("BASH_ENV", "ENV", "GIT_", "PYTHON", "LD_")
ALLOWED_ENV_KEYS = ("HOME", "LANG", "LC_ALL", "LOGNAME", "PATH", "TERM", "TMPDIR", "USER")

PROTECTED_CONTROL_PATTERNS = (
    ".agentic/README.md",
    ".agentic/policies/**",
    ".agentic/runtime-contracts.md",
    ".agentic/runtime-requirements.json",
    ".agentic/roles/**",
    ".agentic/schemas/**",
    ".agentic/templates/**",
    ".agentic/workflows/**",
    "scripts/agentic/**",
)

FORBIDDEN_GIT_SUBCOMMANDS = {
    "branch",
    "checkout",
    "cherry-pick",
    "clean",
    "commit",
    "merge",
    "push",
    "rebase",
    "reset",
    "stash",
    "switch",
    "worktree add",
    "worktree remove",
}

SAFE_IDENTIFIER_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")
AGENT_ID_RE = re.compile(r"^[a-z][a-z0-9-]*$")
PR_SET_NO_NEW_PRIVS = 38
LANDLOCK_CREATE_RULESET_VERSION = 1
LANDLOCK_RULE_PATH_BENEATH = 1
LANDLOCK_ACCESS_FS_WRITE_FILE = 1 << 1
LANDLOCK_ACCESS_FS_REMOVE_DIR = 1 << 4
LANDLOCK_ACCESS_FS_REMOVE_FILE = 1 << 5
LANDLOCK_ACCESS_FS_MAKE_CHAR = 1 << 6
LANDLOCK_ACCESS_FS_MAKE_DIR = 1 << 7
LANDLOCK_ACCESS_FS_MAKE_REG = 1 << 8
LANDLOCK_ACCESS_FS_MAKE_SOCK = 1 << 9
LANDLOCK_ACCESS_FS_MAKE_FIFO = 1 << 10
LANDLOCK_ACCESS_FS_MAKE_BLOCK = 1 << 11
LANDLOCK_ACCESS_FS_MAKE_SYM = 1 << 12
LANDLOCK_ACCESS_FS_REFER = 1 << 13
LANDLOCK_ACCESS_FS_TRUNCATE = 1 << 14
LIBC = ctypes.CDLL(None, use_errno=True)


class RunnerError(RuntimeError):
    pass


class SpecValidationError(RunnerError):
    pass


class StateValidationError(RunnerError):
    pass


class LandlockRulesetAttr(ctypes.Structure):
    _fields_ = [("handled_access_fs", ctypes.c_uint64)]


class LandlockPathBeneathAttr(ctypes.Structure):
    _fields_ = [
        ("allowed_access", ctypes.c_uint64),
        ("parent_fd", ctypes.c_int32),
    ]


@dataclass(frozen=True)
class RuntimeConfig:
    config_path: Path
    openclaw_executable: Path
    runtime_contract_verifier: Path
    run_root: Path
    lock_root: Path
    config_hash: str
    openclaw_hash: str
    verifier_hash: str


def now_utc() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def canonical_json_bytes(value: Any) -> bytes:
    return json.dumps(
        value,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=True,
        allow_nan=False,
    ).encode("utf-8")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def atomic_write_bytes(path: Path, payload: bytes, mode: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=str(path.parent), delete=False) as handle:
        handle.write(payload)
        temp_path = Path(handle.name)
    os.chmod(temp_path, mode)
    os.replace(temp_path, path)


def atomic_write_json(path: Path, payload: Any, mode: int = 0o600) -> None:
    atomic_write_bytes(path, canonical_json_bytes(payload), mode)


def load_hostile_json(path: Path, max_bytes: int) -> Any:
    raw = path.read_bytes()
    if len(raw) > max_bytes:
        raise SpecValidationError(f"Spec exceeds maximum size of {max_bytes} bytes: {path}")

    def reject_duplicates(pairs: List[Tuple[str, Any]]) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                raise SpecValidationError(f"Duplicate JSON key rejected: {key}")
            result[key] = value
        return result

    def reject_constant(token: str) -> None:
        raise SpecValidationError(f"Unsupported numeric token rejected: {token}")

    try:
        return json.loads(
            raw.decode("utf-8"),
            object_pairs_hook=reject_duplicates,
            parse_constant=reject_constant,
        )
    except UnicodeDecodeError as exc:
        raise SpecValidationError(f"Spec is not valid UTF-8: {path}") from exc
    except json.JSONDecodeError as exc:
        raise SpecValidationError(f"Invalid JSON in {path}: {exc}") from exc


def check_allowed_mapping_keys(value: Dict[str, Any], allowed: Sequence[str], label: str) -> None:
    allowed_names = {name[:-1] if name.endswith("!") else name for name in allowed}
    unknown = sorted(set(value.keys()) - allowed_names)
    if unknown:
        raise SpecValidationError(f"Unknown fields in {label}: {', '.join(unknown)}")
    missing = [name for name in allowed if name.endswith("!") and name[:-1] not in value]
    if missing:
        raise SpecValidationError(f"Missing required fields in {label}: {', '.join(item[:-1] for item in missing)}")


def require_boolean(value: Any, label: str) -> bool:
    if not isinstance(value, bool):
        raise SpecValidationError(f"{label} must be a boolean")
    return value


def require_string(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value:
        raise SpecValidationError(f"{label} must be a non-empty string")
    return value


def require_string_list(value: Any, label: str, allow_empty: bool = False) -> List[str]:
    if not isinstance(value, list) or any(not isinstance(item, str) or not item for item in value):
        raise SpecValidationError(f"{label} must be a list of non-empty strings")
    if not allow_empty and not value:
        raise SpecValidationError(f"{label} must not be empty")
    return list(value)


def require_object(value: Any, label: str) -> Dict[str, Any]:
    if not isinstance(value, dict):
        raise SpecValidationError(f"{label} must be an object")
    return value


def require_runtime_object(value: Any, label: str) -> Dict[str, Any]:
    if not isinstance(value, dict):
        raise RunnerError(f"{FAILURE_LEDGER}: {label} must be an object")
    return value


def require_runtime_string(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value:
        raise RunnerError(f"{FAILURE_LEDGER}: {label} must be a non-empty string")
    return value


def require_runtime_string_list(value: Any, label: str) -> List[str]:
    if not isinstance(value, list) or any(not isinstance(item, str) or not item for item in value):
        raise RunnerError(f"{FAILURE_LEDGER}: {label} must be a list of non-empty strings")
    return list(value)


def run_command(
    argv: Sequence[str],
    *,
    cwd: Optional[Path] = None,
    env: Optional[Dict[str, str]] = None,
    timeout: Optional[int] = None,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        list(argv),
        cwd=str(cwd) if cwd else None,
        env=env,
        capture_output=True,
        text=True,
        timeout=timeout,
        check=False,
    )
    if check and result.returncode != 0:
        raise RunnerError(
            f"Command failed ({result.returncode}): {' '.join(argv)}\n{result.stdout}{result.stderr}"
        )
    return result


def landlock_syscall_number(name: str) -> Optional[int]:
    candidates = {
        "landlock_create_ruleset": 444,
        "landlock_add_rule": 445,
        "landlock_restrict_self": 446,
    }
    direct = getattr(os, name.upper(), None)
    if isinstance(direct, int):
        return direct
    prefixed = getattr(os, f"SYS_{name}", None)
    if isinstance(prefixed, int):
        return prefixed
    if sys.platform != "linux":
        return None
    if os.uname().machine not in {"x86_64", "aarch64", "arm64"}:
        return None
    return candidates.get(name)


def supported_landlock_write_access(abi_version: int) -> int:
    access = (
        LANDLOCK_ACCESS_FS_WRITE_FILE
        | LANDLOCK_ACCESS_FS_REMOVE_DIR
        | LANDLOCK_ACCESS_FS_REMOVE_FILE
        | LANDLOCK_ACCESS_FS_MAKE_CHAR
        | LANDLOCK_ACCESS_FS_MAKE_DIR
        | LANDLOCK_ACCESS_FS_MAKE_REG
        | LANDLOCK_ACCESS_FS_MAKE_SOCK
        | LANDLOCK_ACCESS_FS_MAKE_FIFO
        | LANDLOCK_ACCESS_FS_MAKE_BLOCK
        | LANDLOCK_ACCESS_FS_MAKE_SYM
    )
    if abi_version >= 2:
        access |= LANDLOCK_ACCESS_FS_REFER
    if abi_version >= 3:
        access |= LANDLOCK_ACCESS_FS_TRUNCATE
    return access


def landlock_abi_version() -> int:
    syscall_number = landlock_syscall_number("landlock_create_ruleset")
    if syscall_number is None:
        return 0
    result = LIBC.syscall(
        syscall_number,
        ctypes.c_void_p(),
        ctypes.c_size_t(0),
        ctypes.c_uint(LANDLOCK_CREATE_RULESET_VERSION),
    )
    if result >= 0:
        return int(result)
    err = ctypes.get_errno()
    if err in {errno.ENOSYS, errno.EOPNOTSUPP, errno.EINVAL}:
        return 0
    raise OSError(err, "landlock_create_ruleset(version)")


def make_write_confinement_preexec(repo_root: Path, writable_roots: Sequence[Path]) -> Callable[[], None]:
    abi_version = landlock_abi_version()
    if abi_version <= 0:
        raise RunnerError(f"{FAILURE_PATH}: write confinement unavailable on this host")
    create_ruleset_syscall = landlock_syscall_number("landlock_create_ruleset")
    add_rule_syscall = landlock_syscall_number("landlock_add_rule")
    restrict_self_syscall = landlock_syscall_number("landlock_restrict_self")
    if create_ruleset_syscall is None or add_rule_syscall is None or restrict_self_syscall is None:
        raise RunnerError(f"{FAILURE_PATH}: write confinement syscalls unavailable on this host")
    handled_access = supported_landlock_write_access(abi_version)
    unique_roots = [path.resolve() for path in dict.fromkeys(writable_roots)]
    for root in unique_roots:
        if not root.is_absolute():
            raise RunnerError(f"{FAILURE_PATH}: confinement root must be absolute: {root}")
        ensure_real_directory(root)

    def install_write_confinement() -> None:
        ruleset_attr = LandlockRulesetAttr(handled_access_fs=handled_access)
        ruleset_fd = LIBC.syscall(
            create_ruleset_syscall,
            ctypes.byref(ruleset_attr),
            ctypes.sizeof(ruleset_attr),
            ctypes.c_uint(0),
        )
        if ruleset_fd < 0:
            err = ctypes.get_errno()
            raise OSError(err, "landlock_create_ruleset")
        try:
            for root in unique_roots:
                parent_fd = os.open(str(root), os.O_RDONLY | os.O_DIRECTORY | getattr(os, "O_CLOEXEC", 0))
                try:
                    path_beneath = LandlockPathBeneathAttr(
                        allowed_access=handled_access,
                        parent_fd=parent_fd,
                    )
                    result = LIBC.syscall(
                        add_rule_syscall,
                        ctypes.c_int(ruleset_fd),
                        ctypes.c_int(LANDLOCK_RULE_PATH_BENEATH),
                        ctypes.byref(path_beneath),
                        ctypes.c_uint(0),
                    )
                    if result < 0:
                        err = ctypes.get_errno()
                        raise OSError(err, f"landlock_add_rule({root})")
                finally:
                    os.close(parent_fd)
            if LIBC.prctl(ctypes.c_int(PR_SET_NO_NEW_PRIVS), ctypes.c_ulong(1), 0, 0, 0) != 0:
                err = ctypes.get_errno()
                raise OSError(err, "prctl(PR_SET_NO_NEW_PRIVS)")
            result = LIBC.syscall(restrict_self_syscall, ctypes.c_int(ruleset_fd), ctypes.c_uint(0))
            if result < 0:
                err = ctypes.get_errno()
                raise OSError(err, "landlock_restrict_self")
        finally:
            os.close(ruleset_fd)

    return install_write_confinement


def is_absolute_path_text(value: str) -> bool:
    return value.startswith("/") or re.match(r"^[A-Za-z]:[\\/]", value) is not None


def normalize_repo_path(pattern: str, *, allow_glob: bool) -> str:
    if "\x00" in pattern:
        raise SpecValidationError("Path contains NUL byte")
    if not pattern:
        raise SpecValidationError("Path must not be empty")
    if is_absolute_path_text(pattern):
        raise SpecValidationError(f"Absolute path is not allowed: {pattern}")
    if pattern in {".", ".."}:
        raise SpecValidationError(f"Ambiguous relative path is not allowed: {pattern}")
    if pattern.startswith("./") or pattern.startswith("../") or "/../" in pattern or "/./" in pattern:
        raise SpecValidationError(f"Path traversal is not allowed: {pattern}")
    if pattern.startswith("!") or "!" in pattern:
        raise SpecValidationError(f"Negation is not allowed in path patterns: {pattern}")
    if "\\" in pattern:
        raise SpecValidationError(f"Backslashes are not allowed in repository paths: {pattern}")
    if "//" in pattern:
        raise SpecValidationError(f"Repeated path separators are not allowed: {pattern}")
    generic_wildcards = {"?", "[", "]", "{", "}"}
    if any(token in pattern for token in generic_wildcards):
        raise SpecValidationError(f"Unsupported wildcard syntax in path pattern: {pattern}")

    star_count = pattern.count("*")
    if star_count:
        if not allow_glob or not pattern.endswith("/**") or star_count != 2:
            raise SpecValidationError(f"Only terminal '/**' directory patterns are allowed: {pattern}")
        if "*" in pattern[:-3]:
            raise SpecValidationError(f"Only terminal '/**' directory patterns are allowed: {pattern}")
        base = pattern[:-3]
        if not base:
            raise SpecValidationError("Directory glob requires a non-empty base path")
        for part in base.split("/"):
            if part in {"", ".", ".."}:
                raise SpecValidationError(f"Invalid path segment in pattern: {pattern}")
        return pattern

    for part in pattern.split("/"):
        if part in {"", ".", ".."}:
            raise SpecValidationError(f"Invalid path segment in pattern: {pattern}")
        if "*" in part:
            raise SpecValidationError(f"Unsupported wildcard syntax in path pattern: {pattern}")
    return pattern


def ensure_repo_target_is_safe(repo_root: Path, pattern: str) -> None:
    base = pattern[:-3] if pattern.endswith("/**") else pattern
    current = repo_root
    for index, segment in enumerate(base.split("/"), start=1):
        current = current / segment
        try:
            info = os.lstat(current)
        except FileNotFoundError:
            if index == len(base.split("/")):
                break
            continue
        if stat.S_ISLNK(info.st_mode):
            raise SpecValidationError(f"Symlink ancestor or target is not allowed for path: {pattern}")
        resolved = current.resolve()
        if not str(resolved).startswith(str(repo_root.resolve()) + os.sep) and resolved != repo_root.resolve():
            raise SpecValidationError(f"Canonical path escapes worktree: {pattern}")


def path_matches_pattern(pattern: str, rel_path: str) -> bool:
    if pattern.endswith("/**"):
        base = pattern[:-3]
        return rel_path == base or rel_path.startswith(base + "/")
    return rel_path == pattern


def pattern_covers_pattern(parent: str, child: str) -> bool:
    if child == parent:
        return True
    if parent.endswith("/**"):
        base = parent[:-3]
        if child.endswith("/**"):
            return child[:-3] == base or child[:-3].startswith(base + "/")
        return child == base or child.startswith(base + "/")
    return False


def verify_protected_path_rules(
    normalized_paths: Sequence[str],
    classification: str,
    human_preapproval: bool,
) -> None:
    for pattern in normalized_paths:
        matching_protected = [protected for protected in PROTECTED_CONTROL_PATTERNS if pattern_covers_pattern(pattern, protected) or pattern_covers_pattern(protected, pattern)]
        if not matching_protected:
            continue
        if classification != "GOVERNANCE_CHANGE" or not human_preapproval:
            raise SpecValidationError(f"Protected control-plane path requires governance preapproval: {pattern}")
        if pattern in {".agentic/**", "scripts/**"}:
            raise SpecValidationError(f"Protected control-plane path must be explicitly listed: {pattern}")


def validate_task_id(task_id: str) -> str:
    if not SAFE_IDENTIFIER_RE.match(task_id):
        raise SpecValidationError(f"Invalid task id: {task_id}")
    return task_id


def validate_stage_id(stage_id: str) -> str:
    if not SAFE_IDENTIFIER_RE.match(stage_id):
        raise SpecValidationError(f"Invalid stageId: {stage_id}")
    return stage_id


def validate_ref_name(branch_name: str, *, allow_protected: bool) -> str:
    require_string(branch_name, "branch")
    result = run_command(["git", "check-ref-format", "--branch", branch_name], check=False)
    if result.returncode != 0:
        raise SpecValidationError(f"Invalid branch name: {branch_name}")
    if not allow_protected and branch_name in {"main", "develop"}:
        raise SpecValidationError(f"Protected branch is not allowed for task execution: {branch_name}")
    return branch_name


def validate_agent_id(agent_id: str, repo_root: Path) -> str:
    if not AGENT_ID_RE.match(agent_id):
        raise SpecValidationError(f"Invalid agent id: {agent_id}")
    role_path = repo_root / ".agentic" / "roles" / f"{agent_id}.md"
    if not role_path.is_file():
        raise SpecValidationError(f"Unknown agent id; role contract is missing: {agent_id}")
    return agent_id


def validate_worktree(task_id: str, host_worktree_path: Path, task_branch: str) -> Path:
    if not host_worktree_path.is_absolute():
        raise SpecValidationError("repo.hostWorktreePath must be absolute")
    if not host_worktree_path.is_dir():
        raise SpecValidationError(f"Host worktree path not found: {host_worktree_path}")
    worktree_root = Path(
        run_command(["git", "-C", str(host_worktree_path), "rev-parse", "--show-toplevel"]).stdout.strip()
    )
    if worktree_root != host_worktree_path.resolve():
        raise SpecValidationError(f"Host worktree path must be the worktree root: {host_worktree_path}")
    branch = run_command(["git", "-C", str(host_worktree_path), "symbolic-ref", "--quiet", "--short", "HEAD"]).stdout.strip()
    if branch != task_branch:
        raise SpecValidationError(
            f"Host worktree branch does not match spec for {task_id}: expected {task_branch}, found {branch}"
        )
    git_common_dir = Path(
        run_command(
            ["git", "-C", str(host_worktree_path), "rev-parse", "--path-format=absolute", "--git-common-dir"]
        ).stdout.strip()
    )
    primary_repo = git_common_dir.parent.resolve()
    if primary_repo == host_worktree_path.resolve():
        raise SpecValidationError("Primary checkout is not allowed as the task worktree")
    listed = run_command(["git", "-C", str(primary_repo), "worktree", "list", "--porcelain"]).stdout
    if f"worktree {host_worktree_path.resolve()}" not in listed:
        raise SpecValidationError(f"Worktree is not registered in git worktree list: {host_worktree_path}")
    return host_worktree_path.resolve()


def stage_prompt_digest_path(run_dir: Path, stage_id: str, digest: str, original_name: str) -> Path:
    safe_name = re.sub(r"[^A-Za-z0-9._-]", "-", original_name)
    return run_dir / "frozen-prompts" / f"{stage_id}-{digest[:16]}-{safe_name}"


def build_minimal_child_env() -> Dict[str, str]:
    env: Dict[str, str] = {}
    for key in ALLOWED_ENV_KEYS:
        value = os.environ.get(key)
        if value:
            env[key] = value
    if "LANG" not in env:
        env["LANG"] = "C.UTF-8"
    if "LC_ALL" not in env:
        env["LC_ALL"] = "C.UTF-8"
    for key in list(env):
        if key.startswith(FORBIDDEN_ENV_PREFIXES):
            env.pop(key, None)
    return env


def write_runtime_verification_log(
    runtime: RuntimeConfig,
    target_path: Path,
    *,
    label: str,
) -> None:
    result = run_command(
        [str(runtime.runtime_contract_verifier)],
        env=build_minimal_child_env(),
        check=False,
    )
    payload = (
        f"label={label}\n"
        f"timestamp={now_utc()}\n"
        f"command={runtime.runtime_contract_verifier}\n"
        f"exit_code={result.returncode}\n\n"
        f"{result.stdout}{result.stderr}"
    ).encode("utf-8")
    atomic_write_bytes(target_path, payload, 0o600)
    if result.returncode != 0:
        raise RunnerError(f"{FAILURE_RUNTIME}: {runtime.runtime_contract_verifier}")


def load_runtime_config(path_text: Optional[str]) -> RuntimeConfig:
    path = Path(os.path.expanduser(path_text or DEFAULT_RUNTIME_CONFIG_PATH)).resolve()
    payload = load_hostile_json(path, MAX_SPEC_BYTES)
    payload = require_object(payload, "runtime config")
    allowed = [
        "schemaVersion!",
        "openClawExecutable!",
        "runtimeContractVerifier!",
        "runRoot!",
        "lockRoot!",
    ]
    check_allowed_mapping_keys(payload, allowed, "runtime config")
    if payload["schemaVersion"] != RUNTIME_CONFIG_SCHEMA_VERSION:
        raise SpecValidationError(
            f"Unsupported runtime config schema version: {payload['schemaVersion']}"
        )
    openclaw = Path(require_string(payload["openClawExecutable"], "openClawExecutable")).resolve()
    verifier = Path(require_string(payload["runtimeContractVerifier"], "runtimeContractVerifier")).resolve()
    run_root = Path(require_string(payload["runRoot"], "runRoot")).resolve()
    lock_root = Path(require_string(payload["lockRoot"], "lockRoot")).resolve()
    for label, candidate in (
        ("openClawExecutable", openclaw),
        ("runtimeContractVerifier", verifier),
        ("runRoot", run_root),
        ("lockRoot", lock_root),
    ):
        if not candidate.is_absolute():
            raise SpecValidationError(f"{label} must be an absolute path")
    if not openclaw.is_file():
        raise SpecValidationError(f"OpenClaw executable not found: {openclaw}")
    if not verifier.is_file():
        raise SpecValidationError(f"Runtime verifier not found: {verifier}")
    run_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    lock_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(run_root, 0o700)
    os.chmod(lock_root, 0o700)
    return RuntimeConfig(
        config_path=path,
        openclaw_executable=openclaw,
        runtime_contract_verifier=verifier,
        run_root=run_root,
        lock_root=lock_root,
        config_hash=sha256_file(path),
        openclaw_hash=sha256_file(openclaw),
        verifier_hash=sha256_file(verifier),
    )


def validate_stage_definition(
    stage_payload: Dict[str, Any],
    stage_type: str,
    repo_root: Path,
    global_paths: Sequence[str],
    classification: str,
    human_preapproval: bool,
) -> Dict[str, Any]:
    check_allowed_mapping_keys(
        stage_payload,
        [
            "stageId!",
            "agentId!",
            "promptFile!",
            "allowedPaths!",
            "timeoutSeconds!",
            "successMarkers!",
            "maxAttempts!",
        ],
        f"{stage_type} stage",
    )
    stage_id = validate_stage_id(require_string(stage_payload["stageId"], "stageId"))
    agent_id = validate_agent_id(require_string(stage_payload["agentId"], "agentId"), repo_root)
    prompt_file = normalize_repo_path(require_string(stage_payload["promptFile"], "promptFile"), allow_glob=False)
    ensure_repo_target_is_safe(repo_root, prompt_file)
    prompt_path = (repo_root / prompt_file).resolve()
    if not prompt_path.is_file():
        raise SpecValidationError(f"Prompt file not found: {prompt_file}")
    if prompt_path.stat().st_size > MAX_PROMPT_BYTES:
        raise SpecValidationError(f"Prompt file exceeds maximum size of {MAX_PROMPT_BYTES} bytes: {prompt_file}")
    allowed_paths = [
        normalize_repo_path(item, allow_glob=True)
        for item in require_string_list(stage_payload["allowedPaths"], "allowedPaths", allow_empty=True)
    ]
    if stage_type in READ_ONLY_STAGE_TYPES and allowed_paths:
        raise SpecValidationError(f"Read-only stage must have an empty allowedPaths list: {stage_id}")
    verify_protected_path_rules(allowed_paths, classification, human_preapproval)
    for item in allowed_paths:
        ensure_repo_target_is_safe(repo_root, item)
        if not any(pattern_covers_pattern(parent, item) for parent in global_paths):
            raise SpecValidationError(
                f"Stage path is outside globally writable paths for {stage_id}: {item}"
            )
    timeout_seconds = stage_payload["timeoutSeconds"]
    if not isinstance(timeout_seconds, int) or timeout_seconds <= 0:
        raise SpecValidationError(f"timeoutSeconds must be a positive integer for {stage_id}")
    max_attempts = stage_payload["maxAttempts"]
    if not isinstance(max_attempts, int) or max_attempts <= 0 or max_attempts > MAX_STAGE_ATTEMPTS:
        raise SpecValidationError(
            f"maxAttempts must be between 1 and {MAX_STAGE_ATTEMPTS} for {stage_id}"
        )
    success_markers = require_string_list(stage_payload["successMarkers"], "successMarkers")
    return {
        "stageId": stage_id,
        "stageType": stage_type,
        "agentId": agent_id,
        "promptFile": prompt_file,
        "allowedPaths": allowed_paths,
        "timeoutSeconds": timeout_seconds,
        "successMarkers": success_markers,
        "maxAttempts": max_attempts,
    }


def validate_and_freeze_spec(spec_path: Path) -> Dict[str, Any]:
    spec = load_hostile_json(spec_path, MAX_SPEC_BYTES)
    spec = require_object(spec, "spec")
    check_allowed_mapping_keys(
        spec,
        [
            "schemaVersion!",
            "task!",
            "repo!",
            "governance!",
            "scope!",
            "paths!",
            "stages!",
        ],
        "task spec",
    )
    if spec["schemaVersion"] != SPEC_SCHEMA_VERSION:
        raise SpecValidationError(f"Unsupported schemaVersion: {spec['schemaVersion']}")

    task = require_object(spec["task"], "task")
    check_allowed_mapping_keys(task, ["id!", "title!", "classification!", "issueUrl"], "task")
    task_id = validate_task_id(require_string(task["id"], "task.id"))
    title = require_string(task["title"], "task.title")
    classification = require_string(task["classification"], "task.classification")
    issue_url = task.get("issueUrl")
    if issue_url is not None:
        require_string(issue_url, "task.issueUrl")

    repo = require_object(spec["repo"], "repo")
    check_allowed_mapping_keys(
        repo,
        ["baseBranch!", "taskBranch!", "hostWorktreePath!", "sandboxMirrorPath"],
        "repo",
    )
    base_branch = validate_ref_name(require_string(repo["baseBranch"], "repo.baseBranch"), allow_protected=True)
    task_branch = validate_ref_name(require_string(repo["taskBranch"], "repo.taskBranch"), allow_protected=False)
    host_worktree_path = validate_worktree(
        task_id,
        Path(require_string(repo["hostWorktreePath"], "repo.hostWorktreePath")).expanduser(),
        task_branch,
    )
    sandbox_mirror_text = repo.get("sandboxMirrorPath")
    sandbox_mirror_path: Optional[Path] = None
    if sandbox_mirror_text is not None:
        sandbox_mirror_path = Path(require_string(sandbox_mirror_text, "repo.sandboxMirrorPath")).expanduser().resolve()
        if not sandbox_mirror_path.is_absolute():
            raise SpecValidationError("repo.sandboxMirrorPath must be absolute when provided")
        if sandbox_mirror_path == host_worktree_path:
            raise SpecValidationError("repo.sandboxMirrorPath must differ from repo.hostWorktreePath")

    governance = require_object(spec["governance"], "governance")
    check_allowed_mapping_keys(
        governance,
        ["humanPreApproval!", "finalHumanApprovalRequired!", "designApprovalTimestamp"],
        "governance",
    )
    human_preapproval = require_boolean(governance["humanPreApproval"], "governance.humanPreApproval")
    final_human_approval = require_boolean(
        governance["finalHumanApprovalRequired"], "governance.finalHumanApprovalRequired"
    )
    if not final_human_approval:
        raise SpecValidationError("governance.finalHumanApprovalRequired must be true")
    if governance.get("designApprovalTimestamp") is not None:
        require_string(governance["designApprovalTimestamp"], "governance.designApprovalTimestamp")

    scope = require_object(spec["scope"], "scope")
    check_allowed_mapping_keys(
        scope,
        ["goal!", "acceptanceCriteria!", "outOfScope!", "constraints!", "validations!"],
        "scope",
    )
    goal = require_string(scope["goal"], "scope.goal")
    acceptance_criteria = require_string_list(scope["acceptanceCriteria"], "scope.acceptanceCriteria")
    out_of_scope = require_string_list(scope["outOfScope"], "scope.outOfScope")
    constraints = require_string_list(scope["constraints"], "scope.constraints")
    validations = require_string_list(scope["validations"], "scope.validations")

    paths = require_object(spec["paths"], "paths")
    check_allowed_mapping_keys(paths, ["ledgerPath!", "globallyWritablePaths!"], "paths")
    repo_root = host_worktree_path
    ledger_path = normalize_repo_path(require_string(paths["ledgerPath"], "paths.ledgerPath"), allow_glob=False)
    ensure_repo_target_is_safe(repo_root, ledger_path)
    if not (repo_root / ledger_path).is_file():
        raise SpecValidationError(f"Ledger path not found: {ledger_path}")
    global_paths = [
        normalize_repo_path(item, allow_glob=True)
        for item in require_string_list(paths["globallyWritablePaths"], "paths.globallyWritablePaths")
    ]
    if ledger_path not in global_paths:
        global_paths.append(ledger_path)
    verify_protected_path_rules(global_paths, classification, human_preapproval)
    for item in global_paths:
        ensure_repo_target_is_safe(repo_root, item)

    stages = require_object(spec["stages"], "stages")
    check_allowed_mapping_keys(
        stages,
        ["writerStages!", "readOnlyGates!", "documentationWriter", "finalReviewer!"],
        "stages",
    )
    writer_payloads = stages["writerStages"]
    if not isinstance(writer_payloads, list) or not writer_payloads:
        raise SpecValidationError("stages.writerStages must be a non-empty list")
    read_only_payloads = stages["readOnlyGates"]
    if not isinstance(read_only_payloads, list):
        raise SpecValidationError("stages.readOnlyGates must be a list")
    documentation_payload = stages.get("documentationWriter")
    if classification == "GOVERNANCE_CHANGE" and documentation_payload is None:
        raise SpecValidationError("stages.documentationWriter is required for GOVERNANCE_CHANGE tasks")

    flattened_stages: List[Dict[str, Any]] = []
    seen_stage_ids: set[str] = set()
    for payload in writer_payloads:
        stage = validate_stage_definition(
            require_object(payload, "writer stage"),
            "writer",
            repo_root,
            global_paths,
            classification,
            human_preapproval,
        )
        if stage["stageId"] in seen_stage_ids:
            raise SpecValidationError(f"Duplicate stageId: {stage['stageId']}")
        seen_stage_ids.add(stage["stageId"])
        flattened_stages.append(stage)
    for payload in read_only_payloads:
        stage = validate_stage_definition(
            require_object(payload, "read-only gate"),
            "read-only-gate",
            repo_root,
            global_paths,
            classification,
            human_preapproval,
        )
        if stage["stageId"] in seen_stage_ids:
            raise SpecValidationError(f"Duplicate stageId: {stage['stageId']}")
        seen_stage_ids.add(stage["stageId"])
        flattened_stages.append(stage)
    if documentation_payload is not None:
        stage = validate_stage_definition(
            require_object(documentation_payload, "documentation writer"),
            "documentation-writer",
            repo_root,
            global_paths,
            classification,
            human_preapproval,
        )
        if stage["stageId"] in seen_stage_ids:
            raise SpecValidationError(f"Duplicate stageId: {stage['stageId']}")
        seen_stage_ids.add(stage["stageId"])
        flattened_stages.append(stage)
    final_reviewer = validate_stage_definition(
        require_object(stages["finalReviewer"], "final reviewer"),
        "final-reviewer",
        repo_root,
        global_paths,
        classification,
        human_preapproval,
    )
    if final_reviewer["stageId"] in seen_stage_ids:
        raise SpecValidationError(f"Duplicate stageId: {final_reviewer['stageId']}")
    flattened_stages.append(final_reviewer)

    frozen = {
        "schemaVersion": SPEC_SCHEMA_VERSION,
        "task": {
            "id": task_id,
            "title": title,
            "classification": classification,
            **({"issueUrl": issue_url} if issue_url is not None else {}),
        },
        "repo": {
            "baseBranch": base_branch,
            "taskBranch": task_branch,
            "hostWorktreePath": str(host_worktree_path),
            **({"sandboxMirrorPath": str(sandbox_mirror_path)} if sandbox_mirror_path else {}),
        },
        "governance": {
            "humanPreApproval": human_preapproval,
            "finalHumanApprovalRequired": final_human_approval,
            **(
                {"designApprovalTimestamp": governance["designApprovalTimestamp"]}
                if governance.get("designApprovalTimestamp") is not None
                else {}
            ),
        },
        "scope": {
            "goal": goal,
            "acceptanceCriteria": acceptance_criteria,
            "outOfScope": out_of_scope,
            "constraints": constraints,
            "validations": validations,
        },
        "paths": {
            "ledgerPath": ledger_path,
            "globallyWritablePaths": global_paths,
        },
        "stages": {
            "writerStages": [stage for stage in flattened_stages if stage["stageType"] == "writer"],
            "readOnlyGates": [stage for stage in flattened_stages if stage["stageType"] == "read-only-gate"],
            **(
                {
                    "documentationWriter": next(
                        stage for stage in flattened_stages if stage["stageType"] == "documentation-writer"
                    )
                }
                if any(stage["stageType"] == "documentation-writer" for stage in flattened_stages)
                else {}
            ),
            "finalReviewer": final_reviewer,
        },
    }
    frozen["stageOrder"] = flattened_stages
    return frozen


def walk_repo_entries(repo_root: Path) -> Dict[str, Dict[str, Any]]:
    entries: Dict[str, Dict[str, Any]] = {}

    def visit(path: Path, rel_path: str) -> None:
        info = os.lstat(path)
        mode = stat.S_IMODE(info.st_mode)
        if stat.S_ISLNK(info.st_mode):
            entries[rel_path] = {
                "kind": "symlink",
                "mode": mode,
                "target": os.readlink(path),
            }
            return
        if stat.S_ISDIR(info.st_mode):
            if rel_path:
                entries[rel_path] = {"kind": "dir", "mode": mode}
            for child in sorted(path.iterdir(), key=lambda item: item.name):
                if child.name == ".git":
                    continue
                child_rel = f"{rel_path}/{child.name}" if rel_path else child.name
                visit(child, child_rel)
            return
        if stat.S_ISREG(info.st_mode):
            entries[rel_path] = {
                "kind": "file",
                "mode": mode,
                "sha256": sha256_file(path),
            }
            return
        raise RunnerError(f"Unsupported filesystem entry in worktree: {path}")

    visit(repo_root, "")
    entries.pop("", None)
    return entries


def create_snapshot(repo_root: Path, snapshot_root: Path, snapshot_label: str) -> Dict[str, Any]:
    snapshot_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    manifest = {
        "schemaVersion": "fop-worktree-snapshot/v1",
        "label": snapshot_label,
        "createdAt": now_utc(),
        "root": str(repo_root),
        "entries": walk_repo_entries(repo_root),
    }
    digest = sha256_bytes(canonical_json_bytes(manifest))
    snapshot_id = f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{digest[:16]}"
    manifest["snapshotId"] = snapshot_id
    manifest["snapshotDigest"] = digest
    atomic_write_json(snapshot_root / f"{snapshot_id}.json", manifest)
    status_result = run_command(
        ["git", "-C", str(repo_root), "status", "--short", "--branch"],
        check=False,
    )
    diff_result = run_command(
        ["git", "-C", str(repo_root), "diff", "--no-ext-diff", "--stat"],
        check=False,
    )
    atomic_write_bytes(
        snapshot_root / f"{snapshot_id}.status.txt",
        status_result.stdout.encode("utf-8"),
        0o600,
    )
    atomic_write_bytes(
        snapshot_root / f"{snapshot_id}.diff.txt",
        diff_result.stdout.encode("utf-8"),
        0o600,
    )
    return manifest


def materialize_backup(snapshot: Dict[str, Any], repo_root: Path, blobs_root: Path, backups_root: Path, name: str) -> Path:
    blobs_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    backups_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    entries = snapshot["entries"]
    for rel_path, entry in entries.items():
        if entry["kind"] != "file":
            continue
        digest = entry["sha256"]
        blob_path = blobs_root / digest
        if blob_path.exists():
            continue
        blob_source = repo_root / rel_path
        atomic_write_bytes(blob_path, blob_source.read_bytes(), 0o600)
    manifest_path = backups_root / f"{name}.json"
    backup_manifest = {
        "schemaVersion": "fop-stage-backup/v1",
        "createdAt": now_utc(),
        "snapshotId": snapshot["snapshotId"],
        "entries": entries,
    }
    atomic_write_json(manifest_path, backup_manifest)
    return manifest_path


def load_json_file(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RunnerError(f"Missing JSON file: {path}") from exc
    except json.JSONDecodeError as exc:
        raise RunnerError(f"Invalid JSON file: {path}") from exc


def diff_snapshot_entries(before: Dict[str, Any], after: Dict[str, Any]) -> List[str]:
    changed: List[str] = []
    keys = sorted(set(before) | set(after))
    for key in keys:
        if before.get(key) != after.get(key):
            changed.append(key)
    return changed


def lexical_ancestors(rel_path: str) -> List[str]:
    parts = rel_path.split("/")
    return ["/".join(parts[:index]) for index in range(1, len(parts))]


def remove_path(path: Path) -> None:
    try:
        info = os.lstat(path)
    except FileNotFoundError:
        return
    if stat.S_ISDIR(info.st_mode) and not stat.S_ISLNK(info.st_mode):
        shutil.rmtree(path)
    else:
        path.unlink()


def changed_path_safety_findings(repo_root: Path, changed_paths: Sequence[str]) -> List[str]:
    findings: List[str] = []
    for rel_path in sorted(dict.fromkeys(changed_paths)):
        normalize_repo_path(rel_path, allow_glob=False)
        current = repo_root
        for segment in rel_path.split("/"):
            current = current / segment
            try:
                info = os.lstat(current)
            except FileNotFoundError:
                break
            if stat.S_ISLNK(info.st_mode):
                findings.append(rel_path)
                break
    return findings


def restore_removal_roots(
    changed_paths: Sequence[str],
    current_entries: Dict[str, Dict[str, Any]],
    backup_entries: Dict[str, Dict[str, Any]],
) -> List[str]:
    changed_set = set(changed_paths)
    roots: List[str] = []
    for rel_path in sorted(changed_set, key=lambda item: (item.count("/"), item)):
        blocked = False
        for ancestor in lexical_ancestors(rel_path):
            if ancestor not in changed_set:
                continue
            ancestor_entry = current_entries.get(ancestor)
            if ancestor_entry is None or ancestor_entry.get("kind") != "dir":
                blocked = True
                break
        if not blocked:
            current_entry = current_entries.get(rel_path)
            backup_entry = backup_entries.get(rel_path)
            if (
                current_entry is not None
                and current_entry.get("kind") == "dir"
                and backup_entry is not None
                and backup_entry.get("kind") == "dir"
            ):
                continue
            roots.append(rel_path)
    return roots


def ensure_real_directory(path: Path) -> None:
    try:
        info = os.lstat(path)
    except FileNotFoundError as exc:
        raise RunnerError(f"{FAILURE_PATH}: missing restore parent directory: {path}") from exc
    if stat.S_ISLNK(info.st_mode) or not stat.S_ISDIR(info.st_mode):
        raise RunnerError(f"{FAILURE_PATH}: restore parent is not a real directory: {path}")


def ensure_real_parent_directory(repo_root: Path, target: Path) -> None:
    if target == repo_root:
        return
    relative_parent = target.relative_to(repo_root)
    current = repo_root
    ensure_real_directory(current)
    for segment in relative_parent.parts:
        current = current / segment
        ensure_real_directory(current)


def restore_paths(
    repo_root: Path,
    backup_manifest_path: Path,
    changed_paths: Sequence[str],
    current_entries: Dict[str, Dict[str, Any]],
    blobs_root: Path,
    restore_root: Path,
    restore_name: str,
) -> Path:
    restore_root.mkdir(parents=True, exist_ok=True, mode=0o700)
    backup = load_json_file(backup_manifest_path)
    entries = backup["entries"]
    restored: List[str] = []
    normalized_paths = sorted(set(changed_paths), key=lambda item: (item.count("/"), item))
    for rel_path in restore_removal_roots(normalized_paths, current_entries, entries):
        remove_path(repo_root / rel_path)
    for rel_path in normalized_paths:
        target = repo_root / rel_path
        entry = entries.get(rel_path)
        if entry is None:
            restored.append(rel_path)
            continue
        try:
            current_info = os.lstat(target)
        except FileNotFoundError:
            current_info = None
        ensure_real_parent_directory(repo_root, target.parent)
        if entry["kind"] == "dir":
            if current_info is not None and (stat.S_ISLNK(current_info.st_mode) or not stat.S_ISDIR(current_info.st_mode)):
                remove_path(target)
            if current_info is None or stat.S_ISLNK(current_info.st_mode) or not stat.S_ISDIR(current_info.st_mode):
                target.mkdir(mode=entry["mode"])
            os.chmod(target, entry["mode"])
        elif entry["kind"] == "file":
            if current_info is not None:
                remove_path(target)
            blob_path = blobs_root / entry["sha256"]
            atomic_write_bytes(target, blob_path.read_bytes(), entry["mode"])
        elif entry["kind"] == "symlink":
            if current_info is not None:
                remove_path(target)
            os.symlink(entry["target"], target)
        else:
            raise RunnerError(f"Unsupported backup entry kind: {entry['kind']}")
        restored.append(rel_path)
    manifest = {
        "schemaVersion": "fop-restore-manifest/v1",
        "createdAt": now_utc(),
        "restoredPaths": restored,
        "backupManifest": str(backup_manifest_path),
    }
    restore_path = restore_root / f"{restore_name}.json"
    atomic_write_json(restore_path, manifest)
    return restore_path


def safe_realpath(repo_root: Path, rel_path: str) -> Path:
    candidate = (repo_root / rel_path).resolve()
    repo_real = repo_root.resolve()
    if candidate != repo_real and not str(candidate).startswith(str(repo_real) + os.sep):
        raise RunnerError(f"Canonical path escapes worktree: {rel_path}")
    return candidate


def enforce_changed_paths_safety(repo_root: Path, changed_paths: Sequence[str]) -> None:
    for rel_path in changed_paths:
        normalize_repo_path(rel_path, allow_glob=False)
        ensure_repo_target_is_safe(repo_root, rel_path)
        safe_realpath(repo_root, rel_path)


def acquire_lock(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    payload = {"pid": os.getpid(), "createdAt": now_utc()}
    try:
        fd = os.open(str(path), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except OSError as exc:
        if exc.errno == errno.EEXIST:
            raise RunnerError(f"{FAILURE_LOCK}: {path}")
        raise
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(payload, handle, sort_keys=True)
    except Exception:
        os.unlink(path)
        raise


def release_lock(path: Path) -> None:
    try:
        path.unlink()
    except FileNotFoundError:
        return


def save_state(run_dir: Path, state: Dict[str, Any]) -> None:
    state["updatedAt"] = now_utc()
    state_path = run_dir / "state.json"
    digest_path = run_dir / "state.sha256"
    payload = canonical_json_bytes(state)
    atomic_write_bytes(state_path, payload, 0o600)
    atomic_write_bytes(digest_path, sha256_bytes(payload).encode("ascii"), 0o600)


def load_state(run_dir: Path) -> Dict[str, Any]:
    state_path = run_dir / "state.json"
    digest_path = run_dir / "state.sha256"
    payload = state_path.read_bytes()
    digest = digest_path.read_text(encoding="utf-8").strip()
    if sha256_bytes(payload) != digest:
        raise StateValidationError(f"{FAILURE_STATE}: state digest mismatch")
    state = json.loads(payload.decode("utf-8"))
    if state.get("schemaVersion") != STATE_SCHEMA_VERSION:
        raise StateValidationError(f"Unsupported state schema version: {state.get('schemaVersion')}")
    return state


def load_run_dir(runtime: RuntimeConfig, task_id: str) -> Path:
    return (runtime.run_root / validate_task_id(task_id)).resolve()


def stage_records_for(state: Dict[str, Any], stage_id: str) -> List[Dict[str, Any]]:
    return [record for record in state["stageRecords"] if record["stageId"] == stage_id]


def stage_successful(state: Dict[str, Any], stage_id: str) -> bool:
    return any(record["status"] == "SUCCESS" for record in stage_records_for(state, stage_id))


def first_incomplete_stage(state: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    for stage in state["stageOrder"]:
        if not stage_successful(state, stage["stageId"]):
            return stage
    return None


def compute_writer_set(state: Dict[str, Any]) -> List[str]:
    writers = []
    for record in state["stageRecords"]:
        if record["status"] == "SUCCESS" and record["stageType"] in WRITER_STAGE_TYPES:
            if record["agentId"] not in writers:
                writers.append(record["agentId"])
    return writers


def verify_runtime_identity(runtime: RuntimeConfig, state: Dict[str, Any]) -> None:
    expected = state["runtimeIdentity"]
    if runtime.config_hash != expected["runtimeConfigHash"]:
        raise RunnerError("runtime manifest mismatch: runtime config hash changed")
    if runtime.openclaw_hash != expected["openClawHash"]:
        raise RunnerError("runtime manifest mismatch: OpenClaw executable hash changed")
    if runtime.verifier_hash != expected["runtimeVerifierHash"]:
        raise RunnerError("runtime manifest mismatch: verifier hash changed")


def digest_optional_file(path: Path) -> Optional[str]:
    if not path.exists():
        return None
    if not path.is_file():
        raise RunnerError(f"{FAILURE_LEDGER}: expected ledger file path, found non-file entry: {path}")
    return sha256_file(path)


def changed_non_directory_paths(
    before_entries: Dict[str, Dict[str, Any]],
    after_entries: Dict[str, Dict[str, Any]],
    changed_paths: Sequence[str],
) -> List[str]:
    material_paths: List[str] = []
    for rel_path in changed_paths:
        before_entry = before_entries.get(rel_path)
        after_entry = after_entries.get(rel_path)
        if (
            (before_entry is not None and before_entry.get("kind") != "dir")
            or (after_entry is not None and after_entry.get("kind") != "dir")
        ):
            material_paths.append(rel_path)
    return material_paths


def verify_writer_ledger_evidence(
    *,
    child_result: Dict[str, Any],
    stage: Dict[str, Any],
    stage_run_id: str,
    ledger_digest_before: Optional[str],
    ledger_digest_after: Optional[str],
    actual_authorized_paths: Sequence[str],
) -> str:
    if ledger_digest_before == ledger_digest_after:
        raise RunnerError(f"{FAILURE_LEDGER}: ledger digest did not change for {stage_run_id}")

    evidence = require_runtime_object(child_result.get("ledgerEvidence"), "ledgerEvidence")
    allowed = {
        "schemaVersion",
        "agentId",
        "stageRunId",
        "stageType",
        "status",
        "filesChanged",
    }
    unknown = sorted(set(evidence.keys()) - allowed)
    if unknown:
        raise RunnerError(f"{FAILURE_LEDGER}: unknown ledgerEvidence fields: {', '.join(unknown)}")
    for required_key in ("schemaVersion", "agentId", "stageRunId", "stageType", "status", "filesChanged"):
        if required_key not in evidence:
            raise RunnerError(f"{FAILURE_LEDGER}: missing ledgerEvidence.{required_key}")

    schema_version = require_runtime_string(evidence["schemaVersion"], "ledgerEvidence.schemaVersion")
    if schema_version != LEDGER_EVIDENCE_SCHEMA_VERSION:
        raise RunnerError(f"{FAILURE_LEDGER}: unsupported ledger evidence schema version: {schema_version}")
    if require_runtime_string(evidence["agentId"], "ledgerEvidence.agentId") != stage["agentId"]:
        raise RunnerError(f"{FAILURE_LEDGER}: ledger agentId mismatch for {stage_run_id}")
    if require_runtime_string(evidence["stageRunId"], "ledgerEvidence.stageRunId") != stage_run_id:
        raise RunnerError(f"{FAILURE_LEDGER}: ledger stageRunId mismatch for {stage_run_id}")
    if require_runtime_string(evidence["stageType"], "ledgerEvidence.stageType") != stage["stageType"]:
        raise RunnerError(f"{FAILURE_LEDGER}: ledger stageType mismatch for {stage_run_id}")
    if require_runtime_string(evidence["status"], "ledgerEvidence.status") != "COMPLETED":
        raise RunnerError(f"{FAILURE_LEDGER}: ledger evidence is not completed for {stage_run_id}")

    expected_paths = sorted(dict.fromkeys(actual_authorized_paths))
    event_paths: List[str] = []
    for item in require_runtime_string_list(evidence["filesChanged"], "ledgerEvidence.filesChanged"):
        normalized = normalize_repo_path(item, allow_glob=False)
        event_paths.append(normalized)
    event_paths = sorted(dict.fromkeys(event_paths))
    if event_paths != expected_paths:
        raise RunnerError(
            f"{FAILURE_LEDGER}: ledger filesChanged mismatch for {stage_run_id}: "
            f"expected {expected_paths}, found {event_paths}"
        )
    return "UPDATED"


def ensure_snapshot_matches(run_dir: Path, snapshot_id: str, repo_root: Path, label: str) -> Dict[str, Any]:
    expected_manifest = load_json_file(run_dir / "snapshots" / f"{snapshot_id}.json")
    current_manifest = create_snapshot(repo_root, run_dir / "snapshots", label)
    if current_manifest["entries"] != expected_manifest["entries"]:
        raise RunnerError(f"worktree snapshot drift detected for {repo_root}")
    return current_manifest


def freeze_prompts(run_dir: Path, spec: Dict[str, Any]) -> Dict[str, Dict[str, str]]:
    frozen: Dict[str, Dict[str, str]] = {}
    repo_root = Path(spec["repo"]["hostWorktreePath"])
    for stage in spec["stageOrder"]:
        prompt_rel = stage["promptFile"]
        if prompt_rel in frozen:
            continue
        prompt_source = repo_root / prompt_rel
        payload = prompt_source.read_bytes()
        digest = sha256_bytes(payload)
        target = stage_prompt_digest_path(run_dir, stage["stageId"], digest, prompt_source.name)
        if not target.exists():
            atomic_write_bytes(target, payload, 0o600)
        frozen[prompt_rel] = {
            "frozenPath": str(target),
            "sha256": digest,
        }
    return frozen


def prepare_run(runtime: RuntimeConfig, spec_path: Path) -> Dict[str, Any]:
    spec = validate_and_freeze_spec(spec_path)
    task_id = spec["task"]["id"]
    run_dir = runtime.run_root / task_id
    try:
        run_dir.mkdir(parents=True, exist_ok=False, mode=0o700)
    except FileExistsError as exc:
        raise RunnerError(f"{FAILURE_PREPARE}: task run already exists: {run_dir}") from exc
    os.chmod(run_dir, 0o700)
    runtime_logs = run_dir / "runtime-verification"
    runtime_logs.mkdir(parents=True, exist_ok=True, mode=0o700)
    write_runtime_verification_log(
        runtime,
        runtime_logs / f"prepare-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}.log",
        label="prepare",
    )
    spec_payload = canonical_json_bytes(spec)
    spec_hash = sha256_bytes(spec_payload)
    atomic_write_bytes(run_dir / "task-spec.json", spec_payload, 0o600)
    atomic_write_bytes(run_dir / "task-spec.sha256", spec_hash.encode("ascii"), 0o600)
    frozen_prompts = freeze_prompts(run_dir, spec)
    initial_snapshot = create_snapshot(Path(spec["repo"]["hostWorktreePath"]), run_dir / "snapshots", "prepare")
    state = {
        "schemaVersion": STATE_SCHEMA_VERSION,
        "taskId": task_id,
        "status": STATUS_PREPARED,
        "preparedAt": now_utc(),
        "runDir": str(run_dir),
        "runtimeConfigPath": str(runtime.config_path),
        "runtimeIdentity": {
            "runtimeConfigHash": runtime.config_hash,
            "openClawHash": runtime.openclaw_hash,
            "runtimeVerifierHash": runtime.verifier_hash,
        },
        "specHash": spec_hash,
        "frozenSpecPath": str(run_dir / "task-spec.json"),
        "hostWorktreePath": spec["repo"]["hostWorktreePath"],
        "sandboxMirrorPath": spec["repo"].get("sandboxMirrorPath"),
        "stageOrder": spec["stageOrder"],
        "stageRecords": [],
        "frozenPrompts": frozen_prompts,
        "lastStableSnapshotId": initial_snapshot["snapshotId"],
        "lastStableSnapshotDigest": initial_snapshot["snapshotDigest"],
        "activeChildPid": None,
        "currentStageRunId": None,
    }
    save_state(run_dir, state)
    return {
        "taskId": task_id,
        "runDir": str(run_dir),
        "status": STATUS_PREPARED,
        "specHash": spec_hash,
    }


def status_payload(state: Dict[str, Any], *, verify: bool, runtime: Optional[RuntimeConfig]) -> Dict[str, Any]:
    payload = {
        "taskId": state["taskId"],
        "status": state["status"],
        "runDir": state["runDir"],
        "lastStableSnapshotId": state["lastStableSnapshotId"],
        "writerSet": compute_writer_set(state),
        "nextStage": first_incomplete_stage(state),
        "stageRecords": state["stageRecords"],
    }
    if verify:
        if runtime is None:
            runtime = load_runtime_config(None)
        verify_runtime_identity(runtime, state)
        ensure_snapshot_matches(Path(state["runDir"]), state["lastStableSnapshotId"], Path(state["hostWorktreePath"]), "status-verify")
        payload["verified"] = True
    return payload


def determine_failure_status(stage_type: str, verdict: str) -> str:
    if verdict == "BLOCKED":
        return STATUS_PAUSED_BLOCKED
    if stage_type == "final-reviewer" and verdict == "APPROVE":
        return STATUS_WAITING_FOR_HUMAN_APPROVAL
    return STATUS_PAUSED_FAILED


def detect_live_pid(pid: Optional[int]) -> bool:
    if not pid:
        return False
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def stage_attempt_number(state: Dict[str, Any], stage_id: str) -> int:
    return len(stage_records_for(state, stage_id)) + 1


def load_frozen_spec(run_dir: Path, state: Dict[str, Any]) -> Dict[str, Any]:
    payload = (run_dir / "task-spec.json").read_bytes()
    digest = (run_dir / "task-spec.sha256").read_text(encoding="utf-8").strip()
    if sha256_bytes(payload) != digest or digest != state["specHash"]:
        raise RunnerError("frozen spec hash mismatch")
    return json.loads(payload.decode("utf-8"))


def verify_mirror_if_present(run_dir: Path, state: Dict[str, Any], stage_type: str) -> None:
    mirror_text = state.get("sandboxMirrorPath")
    if not mirror_text or stage_type not in READ_ONLY_STAGE_TYPES:
        return
    host_manifest = create_snapshot(Path(state["hostWorktreePath"]), run_dir / "snapshots", "mirror-host-check")
    mirror_manifest = create_snapshot(Path(mirror_text), run_dir / "snapshots", "mirror-sandbox-check")
    if host_manifest["entries"] != mirror_manifest["entries"]:
        raise RunnerError(FAILURE_MIRROR_DRIFT)


def stage_env(tmpdir: Path) -> Dict[str, str]:
    env = build_minimal_child_env()
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    env["TMPDIR"] = str(tmpdir)
    return env


def execute_stage(
    runtime: RuntimeConfig,
    run_dir: Path,
    state: Dict[str, Any],
    spec: Dict[str, Any],
    stage: Dict[str, Any],
) -> Dict[str, Any]:
    verify_runtime_identity(runtime, state)
    if detect_live_pid(state.get("activeChildPid")):
        raise RunnerError("live child process detected")
    verify_mirror_if_present(run_dir, state, stage["stageType"])

    attempt = stage_attempt_number(state, stage["stageId"])
    if attempt > stage["maxAttempts"]:
        raise RunnerError(f"{FAILURE_ATTEMPT_LIMIT}: {stage['stageId']}")

    write_runtime_verification_log(
        runtime,
        run_dir
        / "runtime-verification"
        / f"{stage['stageId']}-attempt-{attempt}-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}.log",
        label=stage["stageId"],
    )

    repo_root = Path(state["hostWorktreePath"])
    ledger_path = spec["paths"]["ledgerPath"]
    ledger_realpath = safe_realpath(repo_root, ledger_path)
    pre_snapshot = create_snapshot(repo_root, run_dir / "snapshots", f"{stage['stageId']}-pre")
    ledger_digest_before = digest_optional_file(ledger_realpath)
    backup_manifest_path = materialize_backup(
        pre_snapshot,
        repo_root,
        run_dir / "blobs",
        run_dir / "backups",
        f"{stage['stageId']}-attempt-{attempt}",
    )

    if stage["stageType"] == "final-reviewer":
        writer_set = compute_writer_set(state)
        if stage["agentId"] in writer_set:
            stage_run_id = f"{stage['stageId']}-attempt-{attempt}-{uuid.uuid4().hex[:12]}"
            record = {
                "stageId": stage["stageId"],
                "stageType": stage["stageType"],
                "agentId": stage["agentId"],
                "stageRunId": stage_run_id,
                "attempt": attempt,
                "startedAt": now_utc(),
                "endedAt": now_utc(),
                "preSnapshotId": pre_snapshot["snapshotId"],
                "postSnapshotId": pre_snapshot["snapshotId"],
                "mutatedPaths": [],
                "verdict": "REQUEST_CHANGES",
                "status": "FAILED",
                "logPath": None,
                "resultPath": None,
                "failureClassification": FAILURE_INDEPENDENCE,
                "ledgerEvidenceStatus": "PRESENT",
            }
            state["stageRecords"].append(record)
            state["status"] = STATUS_PAUSED_FAILED
            save_state(run_dir, state)
            raise RunnerError(FAILURE_INDEPENDENCE)

    stage_run_id = f"{stage['stageId']}-attempt-{attempt}-{uuid.uuid4().hex[:12]}"
    attempt_root = run_dir / f"{'resume' if state['stageRecords'] else 'prepare'}-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}"
    logs_dir = attempt_root / "logs"
    results_dir = attempt_root / "results"
    tmp_dir = attempt_root / "tmp"
    logs_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    results_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    tmp_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    log_path = logs_dir / f"{stage_run_id}.log"
    child_result_path = results_dir / f"{stage_run_id}.child.json"
    stage_result_path = results_dir / f"{stage_run_id}.result.json"
    write_confinement = make_write_confinement_preexec(repo_root, [repo_root, attempt_root])

    prompt_info = state["frozenPrompts"][stage["promptFile"]]
    argv = [
        str(runtime.openclaw_executable),
        "--agent-id",
        stage["agentId"],
        "--stage-id",
        stage["stageId"],
        "--stage-type",
        stage["stageType"],
        "--stage-run-id",
        stage_run_id,
        "--task-id",
        state["taskId"],
        "--worktree",
        str(repo_root),
        "--prompt-file",
        prompt_info["frozenPath"],
        "--result-file",
        str(child_result_path),
    ]

    state["status"] = STATUS_RUNNING
    state["currentStageRunId"] = stage_run_id
    save_state(run_dir, state)

    started_at = now_utc()
    try:
        with log_path.open("w", encoding="utf-8") as handle:
            process = subprocess.Popen(
                argv,
                stdout=handle,
                stderr=subprocess.STDOUT,
                cwd=str(repo_root),
                env=stage_env(tmp_dir),
                text=True,
                preexec_fn=write_confinement,
            )
            state["activeChildPid"] = process.pid
            save_state(run_dir, state)
            timed_out = False
            try:
                exit_code = process.wait(timeout=stage["timeoutSeconds"])
            except subprocess.TimeoutExpired:
                timed_out = True
                process.kill()
                exit_code = process.wait()
            finally:
                state["activeChildPid"] = None
                state["currentStageRunId"] = None
                save_state(run_dir, state)
    except Exception as exc:
        state["activeChildPid"] = None
        state["currentStageRunId"] = None
        state["status"] = STATUS_PAUSED_BLOCKED
        save_state(run_dir, state)
        raise RunnerError(f"{FAILURE_PATH}: failed to launch confined specialist for {stage_run_id}: {exc}") from exc
    ended_at = now_utc()

    log_text = log_path.read_text(encoding="utf-8")
    post_snapshot = create_snapshot(repo_root, run_dir / "snapshots", f"{stage['stageId']}-post")
    changed_paths = diff_snapshot_entries(pre_snapshot["entries"], post_snapshot["entries"])
    changed_file_paths = changed_non_directory_paths(pre_snapshot["entries"], post_snapshot["entries"], changed_paths)
    path_safety_findings = changed_path_safety_findings(repo_root, changed_paths)

    ledger_digest_after = digest_optional_file(ledger_realpath)
    ledger_status = "NOT_REQUIRED"
    failure_classification: Optional[str] = None
    verdict = "PASS"
    status = "SUCCESS"
    restore_manifest_path: Optional[Path] = None
    child_result: Optional[Dict[str, Any]] = None

    if timed_out:
        failure_classification = FAILURE_TIMEOUT
        verdict = "REQUEST_CHANGES"
        status = "FAILED"
    elif exit_code != 0:
        failure_classification = FAILURE_AGENT_EXIT
        verdict = "REQUEST_CHANGES"
        status = "FAILED"
    elif not all(marker in log_text for marker in stage["successMarkers"]):
        failure_classification = FAILURE_AGENT_EXIT
        verdict = "REQUEST_CHANGES"
        status = "FAILED"
    elif not child_result_path.is_file():
        failure_classification = FAILURE_AGENT_EXIT
        verdict = "REQUEST_CHANGES"
        status = "FAILED"
    else:
        child_result = load_json_file(child_result_path)
        child_verdict = child_result.get("verdict", "PASS")
        if stage["stageType"] == "final-reviewer":
            if child_verdict not in {"APPROVE", "REQUEST_CHANGES", "BLOCKED"}:
                raise RunnerError(f"Invalid reviewer verdict: {child_verdict}")
            verdict = child_verdict
            if child_verdict != "APPROVE":
                status = "FAILED" if child_verdict == "REQUEST_CHANGES" else "BLOCKED"
        else:
            if child_verdict not in {"PASS", "REQUEST_CHANGES", "BLOCKED"}:
                raise RunnerError(f"Invalid stage verdict: {child_verdict}")
            verdict = child_verdict
            if child_verdict == "REQUEST_CHANGES":
                status = "FAILED"
            elif child_verdict == "BLOCKED":
                status = "BLOCKED"

    unauthorized_paths = [
        rel_path
        for rel_path in changed_paths
        if not any(path_matches_pattern(pattern, rel_path) for pattern in stage["allowedPaths"])
    ]
    authorized_changed_file_paths = [
        rel_path
        for rel_path in changed_file_paths
        if any(path_matches_pattern(pattern, rel_path) for pattern in stage["allowedPaths"])
    ]
    if stage["stageType"] in READ_ONLY_STAGE_TYPES and changed_paths:
        try:
            restore_manifest_path = restore_paths(
                repo_root,
                backup_manifest_path,
                changed_paths,
                post_snapshot["entries"],
                run_dir / "blobs",
                run_dir / "restores",
                stage_run_id,
            )
            remediation_snapshot = create_snapshot(repo_root, run_dir / "snapshots", f"{stage['stageId']}-restored")
        except Exception as exc:
            state["status"] = STATUS_PAUSED_BLOCKED
            save_state(run_dir, state)
            raise RunnerError(f"{FAILURE_PATH}: restoration failed for {stage_run_id}: {exc}") from exc
        if remediation_snapshot["entries"] != pre_snapshot["entries"]:
            state["status"] = STATUS_PAUSED_BLOCKED
            save_state(run_dir, state)
            raise RunnerError(f"{FAILURE_PATH}: restored snapshot mismatch for {stage_run_id}")
        post_snapshot = remediation_snapshot
        failure_classification = FAILURE_PATH
        verdict = "BLOCKED"
        status = "BLOCKED"
    elif unauthorized_paths:
        try:
            restore_manifest_path = restore_paths(
                repo_root,
                backup_manifest_path,
                unauthorized_paths,
                post_snapshot["entries"],
                run_dir / "blobs",
                run_dir / "restores",
                stage_run_id,
            )
            remediation_snapshot = create_snapshot(repo_root, run_dir / "snapshots", f"{stage['stageId']}-remediated")
        except Exception as exc:
            state["status"] = STATUS_PAUSED_FAILED
            save_state(run_dir, state)
            raise RunnerError(f"{FAILURE_PATH}: restoration failed for {stage_run_id}: {exc}") from exc
        post_snapshot = remediation_snapshot
        failure_classification = FAILURE_PATH
        verdict = "REQUEST_CHANGES"
        status = "FAILED"
    elif stage["stageType"] in WRITER_STAGE_TYPES and failure_classification is None and status == "SUCCESS":
        try:
            if child_result is None:
                raise RunnerError(f"{FAILURE_LEDGER}: missing child result for {stage_run_id}")
            ledger_status = verify_writer_ledger_evidence(
                child_result=child_result,
                stage=stage,
                stage_run_id=stage_run_id,
                ledger_digest_before=ledger_digest_before,
                ledger_digest_after=ledger_digest_after,
                actual_authorized_paths=authorized_changed_file_paths,
            )
        except (RunnerError, SpecValidationError):
            failure_classification = FAILURE_LEDGER
            verdict = "REQUEST_CHANGES"
            status = "FAILED"
            ledger_status = "MISMATCH"

    record = {
        "stageId": stage["stageId"],
        "stageType": stage["stageType"],
        "agentId": stage["agentId"],
        "stageRunId": stage_run_id,
        "attempt": attempt,
        "startedAt": started_at,
        "endedAt": ended_at,
        "preSnapshotId": pre_snapshot["snapshotId"],
        "postSnapshotId": post_snapshot["snapshotId"],
        "mutatedPaths": changed_paths,
        "verdict": verdict,
        "status": status,
        "logPath": str(log_path),
        "resultPath": str(stage_result_path),
        "failureClassification": failure_classification,
        "ledgerEvidenceStatus": ledger_status,
        "ledgerDigestBefore": ledger_digest_before,
        "ledgerDigestAfter": ledger_digest_after,
        "restoreManifestPath": str(restore_manifest_path) if restore_manifest_path else None,
        "unauthorizedPaths": unauthorized_paths,
        "pathSafetyFindings": path_safety_findings,
    }
    atomic_write_json(stage_result_path, record)
    state["stageRecords"].append(record)
    state["lastStableSnapshotId"] = post_snapshot["snapshotId"]
    state["lastStableSnapshotDigest"] = post_snapshot["snapshotDigest"]
    state["status"] = determine_failure_status(stage["stageType"], verdict)
    if status == "SUCCESS" and stage["stageType"] != "final-reviewer":
        next_stage = first_incomplete_stage(state)
        state["status"] = STATUS_PREPARED if next_stage else STATUS_WAITING_FOR_HUMAN_APPROVAL
    save_state(run_dir, state)
    return record


def prepare_command(args: argparse.Namespace) -> int:
    runtime = load_runtime_config(None)
    result = prepare_run(runtime, Path(args.spec).expanduser().resolve())
    print(json.dumps(result, sort_keys=True))
    return 0


def validate_spec_command(args: argparse.Namespace) -> int:
    result = validate_and_freeze_spec(Path(args.spec).expanduser().resolve())
    payload = {
        "taskId": result["task"]["id"],
        "schemaVersion": result["schemaVersion"],
        "specHash": sha256_bytes(canonical_json_bytes(result)),
        "stageOrder": [stage["stageId"] for stage in result["stageOrder"]],
    }
    print(json.dumps(payload, sort_keys=True))
    return 0


def run_or_resume(args: argparse.Namespace, *, resume: bool) -> int:
    runtime = load_runtime_config(None)
    run_dir = load_run_dir(runtime, args.task_id)
    task_lock = run_dir / "runner.lock"
    acquire_lock(task_lock)
    writer_lock = runtime.lock_root / "fop-writer.lock"
    try:
        state = load_state(run_dir)
        verify_runtime_identity(runtime, state)
        spec = load_frozen_spec(run_dir, state)
        if state["taskId"] != spec["task"]["id"]:
            raise RunnerError("state task id does not match frozen spec")
        if detect_live_pid(state.get("activeChildPid")):
            raise RunnerError("live child process detected")
        ensure_snapshot_matches(run_dir, state["lastStableSnapshotId"], Path(state["hostWorktreePath"]), "resume-check")
        if resume and state["status"] not in {STATUS_PAUSED_FAILED, STATUS_PAUSED_BLOCKED, STATUS_RUNNING, STATUS_PREPARED}:
            raise RunnerError(f"Task is not resumable from state {state['status']}")
        stage = first_incomplete_stage(state)
        if stage is None:
            print(json.dumps(status_payload(state, verify=False, runtime=None), sort_keys=True))
            return 0
        if stage["stageType"] in WRITER_STAGE_TYPES:
            acquire_lock(writer_lock)
        try:
            record = execute_stage(runtime, run_dir, state, spec, stage)
        finally:
            if stage["stageType"] in WRITER_STAGE_TYPES:
                release_lock(writer_lock)
    finally:
        release_lock(task_lock)
    print(json.dumps(record, sort_keys=True))
    return 0


def status_command(args: argparse.Namespace) -> int:
    runtime = load_runtime_config(None)
    run_dir = load_run_dir(runtime, args.task_id)
    state = load_state(run_dir)
    print(json.dumps(status_payload(state, verify=args.verify, runtime=runtime), sort_keys=True))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    validate_spec_parser = subparsers.add_parser("validate-spec")
    validate_spec_parser.add_argument("--spec", required=True)
    validate_spec_parser.set_defaults(func=validate_spec_command)

    prepare_parser = subparsers.add_parser("prepare")
    prepare_parser.add_argument("--spec", required=True)
    prepare_parser.set_defaults(func=prepare_command)

    run_parser = subparsers.add_parser("run")
    run_parser.add_argument("--task-id", required=True)
    run_parser.set_defaults(func=lambda args: run_or_resume(args, resume=False))

    resume_parser = subparsers.add_parser("resume")
    resume_parser.add_argument("--task-id", required=True)
    resume_parser.set_defaults(func=lambda args: run_or_resume(args, resume=True))

    status_parser = subparsers.add_parser("status")
    status_parser.add_argument("--task-id", required=True)
    status_parser.add_argument("--verify", action="store_true")
    status_parser.set_defaults(func=status_command)
    return parser


def main(argv: Sequence[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return args.func(args)
    except (RunnerError, SpecValidationError, StateValidationError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
