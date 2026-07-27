from __future__ import annotations

import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
import unittest
import hashlib
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
RUNNER = REPO_ROOT / "scripts" / "agentic" / "fop_deterministic_runner.py"
FIXTURE_ROOT = REPO_ROOT / "scripts" / "agentic" / "tests" / "fixtures"


class RunnerFixture:
    def __init__(self) -> None:
        self.tempdir = tempfile.TemporaryDirectory()
        self.root = Path(self.tempdir.name)
        self.primary_repo = self.root / "primary"
        self.worktree_root = self.root / "worktrees"
        self.host_worktree = self.worktree_root / "task"
        self.sandbox_worktree = self.root / "sandbox"
        self.home_root = self.root / "home"
        self.runtime_root = self.root / "runtime"
        self.run_root = self.runtime_root / "task-runs"
        self.lock_root = self.runtime_root / "locks"
        self.runtime_config = (
            self.home_root
            / ".openclaw"
            / "config"
            / "fulfillment-orchestrator-platform"
            / "deterministic-runner.json"
        )
        self.openclaw = self.runtime_root / "fake_openclaw.py"
        self.verifier = self.runtime_root / "fake_runtime_verifier.py"
        self.verifier_state = self.runtime_root / "fake_runtime_verifier.state.json"
        self.task_id = "FOP-AGENTIC-003"
        self.branch = "chore/deterministic-specialist-runner"
        self.base_branch = "feature-base"
        self.issue_url = "https://example.invalid/issues/25"
        self.spec_path = self.root / "task-spec.json"
        self._setup_repo()
        self._setup_runtime()

    def cleanup(self) -> None:
        self.tempdir.cleanup()

    def _run(self, argv: list[str], *, cwd: Path | None = None, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
        process_env = os.environ.copy()
        process_env["GIT_AUTHOR_NAME"] = "Test"
        process_env["GIT_AUTHOR_EMAIL"] = "test@example.com"
        process_env["GIT_COMMITTER_NAME"] = "Test"
        process_env["GIT_COMMITTER_EMAIL"] = "test@example.com"
        if env:
            process_env.update(env)
        return subprocess.run(argv, cwd=str(cwd or self.root), env=process_env, text=True, capture_output=True, check=True)

    def _setup_repo(self) -> None:
        self.primary_repo.mkdir(parents=True)
        self._run(["git", "init", "--initial-branch", "main"], cwd=self.primary_repo)
        self._write_repo_file(".agentic/runs/FOP-AGENTIC-003.md", "ledger\n")
        self._write_repo_file(".agentic/roles/infra.md", "# infra\n")
        self._write_repo_file(".agentic/roles/documentation.md", "# documentation\n")
        self._write_repo_file(".agentic/roles/reviewer.md", "# reviewer\n")
        self._write_repo_file(".agentic/roles/testing-engineer.md", "# testing\n")
        self._write_repo_file(".agentic/roles/security.md", "# security\n")
        self._write_repo_file(".agentic/roles/architect.md", "# architect\n")
        self._write_repo_file(".agentic/roles/frontend-engineer.md", "# frontend\n")
        self._write_repo_file(".agentic/roles/qa-api.md", "# qa\n")
        self._write_repo_file(".agentic/policies/trust-boundary.md", "trust\n")
        self._write_repo_file("docs/notes.md", "notes\n")
        self._write_repo_file("allowed/output.txt", "base\n")
        self._write_repo_file("readonly/source.txt", "clean\n")
        self._write_repo_file("review/notes.md", "pending\n")
        self._write_repo_file("prompts/infra.md", "infra prompt\n")
        self._write_repo_file("prompts/testing-engineer.md", "testing prompt\n")
        self._write_repo_file("prompts/documentation.md", "documentation prompt\n")
        self._write_repo_file("prompts/reviewer.md", "reviewer prompt\n")
        self._run(["git", "add", "."], cwd=self.primary_repo)
        self._run(["git", "commit", "-m", "initial"], cwd=self.primary_repo)
        self._run(["git", "checkout", "-b", self.base_branch], cwd=self.primary_repo)
        self.worktree_root.mkdir(parents=True)
        self._run(
            ["git", "worktree", "add", "-b", self.branch, str(self.host_worktree), self.base_branch],
            cwd=self.primary_repo,
        )
        shutil.copytree(self.host_worktree, self.sandbox_worktree)

    def _setup_runtime(self) -> None:
        self.runtime_root.mkdir(parents=True)
        self.runtime_config.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(FIXTURE_ROOT / "fake_openclaw.py", self.openclaw)
        shutil.copy2(FIXTURE_ROOT / "fake_runtime_verifier.py", self.verifier)
        os.chmod(self.openclaw, os.stat(self.openclaw).st_mode | stat.S_IXUSR)
        os.chmod(self.verifier, os.stat(self.verifier).st_mode | stat.S_IXUSR)
        self.verifier_state.write_text(json.dumps({"exitCode": 0, "stdout": ["VERIFIER_OK"]}), encoding="utf-8")
        self.run_root.mkdir(parents=True)
        self.lock_root.mkdir(parents=True)
        self.runtime_config.write_text(
            json.dumps(
                {
                    "schemaVersion": "fop-deterministic-runner-runtime/v1",
                    "openClawExecutable": str(self.openclaw),
                    "runtimeContractVerifier": str(self.verifier),
                    "runRoot": str(self.run_root),
                    "lockRoot": str(self.lock_root),
                },
                sort_keys=True,
            ),
            encoding="utf-8",
        )

    def _write_repo_file(self, relative_path: str, contents: str) -> None:
        target = self.primary_repo / relative_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(contents, encoding="utf-8")

    def write_spec(self, *, classification: str = "GOVERNANCE_CHANGE", human_preapproval: bool = True, include_documentation: bool = True, reviewer_agent: str = "reviewer", global_paths: list[str] | None = None, writer_allowed_paths: list[str] | None = None, include_sandbox_mirror: bool = False) -> Path:
        paths = global_paths or [
            ".agentic/runs/FOP-AGENTIC-003.md",
            "allowed/**",
            "docs/**",
            "readonly/**",
            "review/**",
            "prompts/**",
        ]
        writer_paths = writer_allowed_paths or ["allowed/**", ".agentic/runs/FOP-AGENTIC-003.md"]
        spec = {
            "schemaVersion": "fop-task-spec/v1",
            "task": {
                "id": self.task_id,
                "title": "Deterministic Specialist Runner",
                "classification": classification,
                "issueUrl": self.issue_url,
            },
            "repo": {
                "baseBranch": self.base_branch,
                "taskBranch": self.branch,
                "hostWorktreePath": str(self.host_worktree),
            },
            "governance": {
                "humanPreApproval": human_preapproval,
                "finalHumanApprovalRequired": True,
                "designApprovalTimestamp": "2026-07-21T12:25:42Z",
            },
            "scope": {
                "goal": "exercise the deterministic runner",
                "acceptanceCriteria": ["finish the fake stages"],
                "outOfScope": ["real runtime changes"],
                "constraints": ["standard library only"],
                "validations": ["unit tests"],
            },
            "paths": {
                "ledgerPath": ".agentic/runs/FOP-AGENTIC-003.md",
                "globallyWritablePaths": paths,
            },
            "stages": {
                "writerStages": [
                    {
                        "stageId": "writer-stage",
                        "agentId": "infra",
                        "promptFile": "prompts/infra.md",
                        "allowedPaths": writer_paths,
                        "timeoutSeconds": 30,
                        "successMarkers": ["WRITER_DONE"],
                        "maxAttempts": 2,
                    }
                ],
                "readOnlyGates": [
                    {
                        "stageId": "testing-gate",
                        "agentId": "testing-engineer",
                        "promptFile": "prompts/testing-engineer.md",
                        "allowedPaths": [],
                        "timeoutSeconds": 30,
                        "successMarkers": ["TESTING_DONE"],
                        "maxAttempts": 2,
                    }
                ],
                "finalReviewer": {
                    "stageId": "final-review",
                    "agentId": reviewer_agent,
                    "promptFile": "prompts/reviewer.md",
                    "allowedPaths": [],
                    "timeoutSeconds": 30,
                    "successMarkers": ["REVIEW_DONE"],
                    "maxAttempts": 2,
                },
            },
        }
        if include_sandbox_mirror:
            spec["repo"]["sandboxMirrorPath"] = str(self.sandbox_worktree)
        if include_documentation:
            spec["stages"]["documentationWriter"] = {
                "stageId": "documentation-stage",
                "agentId": "documentation",
                "promptFile": "prompts/documentation.md",
                "allowedPaths": ["docs/**", ".agentic/runs/FOP-AGENTIC-003.md"],
                "timeoutSeconds": 30,
                "successMarkers": ["DOCS_DONE"],
                "maxAttempts": 2,
            }
        self.spec_path.write_text(json.dumps(spec, indent=2, sort_keys=True), encoding="utf-8")
        return self.spec_path

    def set_behavior(self, behavior: dict[str, dict[str, object]]) -> None:
        (self.host_worktree / ".fake-openclaw-behavior.json").write_text(
            json.dumps(behavior, sort_keys=True),
            encoding="utf-8",
        )

    def run_runner(self, *args: str, expect_ok: bool = True) -> subprocess.CompletedProcess[str]:
        command = [sys.executable, str(RUNNER), *args]
        env = os.environ.copy()
        env["HOME"] = str(self.home_root)
        result = subprocess.run(command, text=True, capture_output=True, env=env)
        if expect_ok and result.returncode != 0:
            raise AssertionError(f"command failed: {command}\nSTDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}")
        if not expect_ok and result.returncode == 0:
            raise AssertionError(f"command unexpectedly succeeded: {command}\n{result.stdout}")
        return result

    @property
    def run_dir(self) -> Path:
        return self.run_root / self.task_id

    def state(self) -> dict[str, object]:
        return json.loads((self.run_dir / "state.json").read_text(encoding="utf-8"))

    def last_record(self) -> dict[str, object]:
        return self.state()["stageRecords"][-1]

    def snapshot(self, snapshot_id: str) -> dict[str, object]:
        return json.loads((self.run_dir / "snapshots" / f"{snapshot_id}.json").read_text(encoding="utf-8"))


class DeterministicRunnerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.fixture = RunnerFixture()

    def tearDown(self) -> None:
        self.fixture.cleanup()

    def prepare_happy_path(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/output.txt", "content": "writer update\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"},
                    ],
                },
                "testing-gate": {"stdout": ["TESTING_DONE"]},
                "documentation-stage": {
                    "stdout": ["DOCS_DONE"],
                    "mutations": [
                        {"path": "docs/notes.md", "content": "docs update\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "docs ledger\n"},
                    ],
                },
                "final-review": {"stdout": ["REVIEW_DONE"], "verdict": "APPROVE"},
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))

    def test_01_happy_path_to_human_approval(self) -> None:
        self.prepare_happy_path()
        for _ in range(4):
            self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        state = self.fixture.state()
        self.assertEqual("WAITING_FOR_HUMAN_APPROVAL", state["status"])
        self.assertEqual(
            ["infra", "documentation"],
            [
                record["agentId"]
                for record in state["stageRecords"]
                if record["stageType"] in {"writer", "documentation-writer"}
            ],
        )

    def test_02_duplicate_json_keys(self) -> None:
        self.fixture.spec_path.write_text('{"schemaVersion":"fop-task-spec/v1","schemaVersion":"fop-task-spec/v1"}', encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Duplicate JSON key rejected", result.stderr)

    def test_03_nan_and_infinity(self) -> None:
        self.fixture.spec_path.write_text('{"schemaVersion":"fop-task-spec/v1","task":{"id":"x","title":"x","classification":"GOVERNANCE_CHANGE","issueUrl":"x"},"repo":{"baseBranch":"feature-base","taskBranch":"chore/deterministic-specialist-runner","hostWorktreePath":"' + str(self.fixture.host_worktree) + '"},"governance":{"humanPreApproval":true,"finalHumanApprovalRequired":true},"scope":{"goal":"x","acceptanceCriteria":["x"],"outOfScope":["x"],"constraints":["x"],"validations":[NaN]},"paths":{"ledgerPath":".agentic/runs/FOP-AGENTIC-003.md","globallyWritablePaths":[".agentic/runs/FOP-AGENTIC-003.md"]},"stages":{"writerStages":[{"stageId":"writer","agentId":"infra","promptFile":"prompts/infra.md","allowedPaths":[".agentic/runs/FOP-AGENTIC-003.md"],"timeoutSeconds":30,"successMarkers":["done"],"maxAttempts":1}],"readOnlyGates":[],"documentationWriter":{"stageId":"docs","agentId":"documentation","promptFile":"prompts/documentation.md","allowedPaths":[".agentic/runs/FOP-AGENTIC-003.md"],"timeoutSeconds":30,"successMarkers":["done"],"maxAttempts":1},"finalReviewer":{"stageId":"review","agentId":"reviewer","promptFile":"prompts/reviewer.md","allowedPaths":[],"timeoutSeconds":30,"successMarkers":["done"],"maxAttempts":1}}}', encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Unsupported numeric token rejected", result.stderr)

    def test_04_unknown_fields(self) -> None:
        self.fixture.write_spec()
        spec = json.loads(self.fixture.spec_path.read_text(encoding="utf-8"))
        spec["task"]["unexpected"] = True
        self.fixture.spec_path.write_text(json.dumps(spec), encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Unknown fields in task", result.stderr)

    def test_05_oversized_spec(self) -> None:
        self.fixture.spec_path.write_text("x" * 300000, encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Spec exceeds maximum size", result.stderr)

    def test_06_invalid_task_id(self) -> None:
        self.fixture.write_spec()
        spec = json.loads(self.fixture.spec_path.read_text(encoding="utf-8"))
        spec["task"]["id"] = "../bad"
        self.fixture.spec_path.write_text(json.dumps(spec), encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Invalid task id", result.stderr)

    def test_07_invalid_branch(self) -> None:
        self.fixture.write_spec()
        spec = json.loads(self.fixture.spec_path.read_text(encoding="utf-8"))
        spec["repo"]["taskBranch"] = "bad branch"
        self.fixture.spec_path.write_text(json.dumps(spec), encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Invalid branch name", result.stderr)

    def test_08_invalid_agent_id(self) -> None:
        self.fixture.write_spec()
        spec = json.loads(self.fixture.spec_path.read_text(encoding="utf-8"))
        spec["stages"]["writerStages"][0]["agentId"] = "BAD"
        self.fixture.spec_path.write_text(json.dumps(spec), encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Invalid agent id", result.stderr)

    def test_09_absolute_path(self) -> None:
        self.fixture.write_spec(global_paths=["/tmp/bad"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Absolute path is not allowed", result.stderr)

    def test_10_traversal(self) -> None:
        self.fixture.write_spec(global_paths=["../bad"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Path traversal is not allowed", result.stderr)

    def test_11_unsupported_wildcard(self) -> None:
        self.fixture.write_spec(global_paths=["allowed/*"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Only terminal '/**' directory patterns are allowed", result.stderr)

    def test_12_symlink_escape(self) -> None:
        target = self.fixture.host_worktree / "escape"
        if target.exists():
            target.unlink()
        os.symlink("/tmp", target)
        self.fixture.write_spec(global_paths=["escape/file.txt"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Symlink ancestor or target is not allowed", result.stderr)

    def test_13_protected_path_without_governance_approval(self) -> None:
        self.fixture.write_spec(classification="FEATURE", human_preapproval=False, global_paths=[".agentic/policies/**", ".agentic/runs/FOP-AGENTIC-003.md"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Protected control-plane path requires governance preapproval", result.stderr)

    def test_14_protected_path_with_governance_approval(self) -> None:
        self.fixture.write_spec(global_paths=[".agentic/policies/trust-boundary.md", ".agentic/runs/FOP-AGENTIC-003.md", "allowed/**", "docs/**", "readonly/**", "review/**", "prompts/**"])
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path))
        self.assertEqual(0, result.returncode)

    def test_15_read_only_mutation_detection_and_restoration(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [{"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"}],
                },
                "testing-gate": {
                    "stdout": ["TESTING_DONE"],
                    "mutations": [{"path": "readonly/source.txt", "content": "dirty\n"}],
                },
                "documentation-stage": {
                    "stdout": ["DOCS_DONE"],
                    "mutations": [{"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "docs ledger\n"}],
                },
                "final-review": {"stdout": ["REVIEW_DONE"], "verdict": "APPROVE"},
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        state = self.fixture.state()
        self.assertEqual("PAUSED_BLOCKED", state["status"])
        self.assertEqual("clean\n", (self.fixture.host_worktree / "readonly/source.txt").read_text(encoding="utf-8"))
        self.assertIn("BLOCKED", result.stdout)

    def test_16_writer_path_leak_and_bounded_restoration(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/output.txt", "content": "good\n"},
                        {"path": "readonly/source.txt", "content": "bad\n"},
                    ],
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.assertEqual("good\n", (self.fixture.host_worktree / "allowed/output.txt").read_text(encoding="utf-8"))
        self.assertEqual("clean\n", (self.fixture.host_worktree / "readonly/source.txt").read_text(encoding="utf-8"))
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])

    def test_17_reviewer_independence_failure(self) -> None:
        self.fixture.write_spec(reviewer_agent="infra")
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/output.txt", "content": "ok\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"},
                    ],
                },
                "testing-gate": {"stdout": ["TESTING_DONE"]},
                "documentation-stage": {
                    "stdout": ["DOCS_DONE"],
                    "mutations": [
                        {"path": "docs/notes.md", "content": "docs\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "docs ledger\n"},
                    ],
                },
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("INDEPENDENCE_FAIL", result.stderr)

    def test_18_runtime_verifier_failure(self) -> None:
        self.prepare_happy_path()
        self.fixture.verifier_state.write_text(json.dumps({"exitCode": 2, "stdout": ["BROKEN"]}), encoding="utf-8")
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("RUNTIME_VERIFY_FAILED", result.stderr)

    def test_19_spoofed_marker_with_failing_process(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior({"writer-stage": {"stdout": ["WRITER_DONE"], "exitCode": 7}})
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])

    def test_20_worktree_drift_before_resume(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior({"writer-stage": {"stdout": ["WRITER_DONE"], "exitCode": 7}})
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        (self.fixture.host_worktree / "allowed/output.txt").write_text("drifted\n", encoding="utf-8")
        result = self.fixture.run_runner("resume", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("worktree snapshot drift detected", result.stderr)

    def test_21_state_digest_tampering(self) -> None:
        self.prepare_happy_path()
        (self.fixture.run_dir / "state.sha256").write_text("0" * 64, encoding="utf-8")
        result = self.fixture.run_runner("status", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("state digest mismatch", result.stderr)

    def test_22_lock_conflict(self) -> None:
        self.prepare_happy_path()
        lock_path = self.fixture.run_dir / "runner.lock"
        lock_path.write_text("{}", encoding="utf-8")
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("LOCK_CONFLICT", result.stderr)

    def test_23_attempt_limit(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior({"writer-stage": {"stdout": ["WRITER_DONE"], "exitCode": 7}})
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.fixture.run_runner("resume", "--task-id", self.fixture.task_id)
        result = self.fixture.run_runner("resume", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("ATTEMPT_LIMIT", result.stderr)

    def test_24_crash_between_stages_recovery(self) -> None:
        self.prepare_happy_path()
        state = self.fixture.state()
        state["status"] = "RUNNING"
        state["activeChildPid"] = None
        state["currentStageRunId"] = "crashed-run"
        payload = json.dumps(state, sort_keys=True, separators=(",", ":")).encode("utf-8")
        (self.fixture.run_dir / "state.json").write_bytes(payload)
        (self.fixture.run_dir / "state.sha256").write_text(hashlib.sha256(payload).hexdigest(), encoding="utf-8")
        result = self.fixture.run_runner("resume", "--task-id", self.fixture.task_id)
        self.assertEqual(0, result.returncode)

    def test_25_final_human_approval_stop(self) -> None:
        self.prepare_happy_path()
        for _ in range(4):
            self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        state = self.fixture.state()
        self.assertEqual("WAITING_FOR_HUMAN_APPROVAL", state["status"])

    def test_26_absence_of_forbidden_git_operations(self) -> None:
        flattened = [
            " ".join(line.split())
            for line in RUNNER.read_text(encoding="utf-8").splitlines()
            if '"git"' in line or "'git'" in line
        ]
        for forbidden in ("git commit", "git push", "git merge", "git checkout", "git switch", "git reset", "git clean", "git branch"):
            self.assertFalse(any(forbidden in line for line in flattened), forbidden)

    def test_27_no_shell_command_execution_from_task_spec(self) -> None:
        self.fixture.write_spec()
        (self.fixture.host_worktree / "prompts" / "infra.md").write_text("touch should-not-exist\n", encoding="utf-8")
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [{"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"}],
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        self.assertFalse((self.fixture.host_worktree / "should-not-exist").exists())

    def test_28_stage_id_path_traversal_rejected(self) -> None:
        self.fixture.write_spec()
        spec = json.loads(self.fixture.spec_path.read_text(encoding="utf-8"))
        spec["stages"]["writerStages"][0]["stageId"] = "../escape"
        self.fixture.spec_path.write_text(json.dumps(spec), encoding="utf-8")
        result = self.fixture.run_runner("validate-spec", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("Invalid stageId", result.stderr)

    def test_29_runtime_config_override_rejected(self) -> None:
        self.fixture.write_spec()
        result = self.fixture.run_runner(
            "prepare",
            "--spec",
            str(self.fixture.spec_path),
            "--runtime-config",
            str(self.fixture.runtime_root / "override.json"),
            expect_ok=False,
        )
        self.assertIn("unrecognized arguments: --runtime-config", result.stderr)

    def test_30_run_dir_override_rejected(self) -> None:
        result = self.fixture.run_runner(
            "status",
            "--task-id",
            self.fixture.task_id,
            "--run-dir",
            str(self.fixture.run_dir),
            expect_ok=False,
        )
        self.assertIn("unrecognized arguments: --run-dir", result.stderr)

    def test_31_writer_without_ledger_evidence_fails(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/output.txt", "content": "writer update\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"},
                    ],
                    "omitLedgerEvidence": True,
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        record = self.fixture.last_record()
        self.assertEqual("LEDGER_MISMATCH", record["failureClassification"])
        self.assertEqual("MISMATCH", record["ledgerEvidenceStatus"])
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])

    def test_32_writer_with_mismatched_ledger_evidence_fails(self) -> None:
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/output.txt", "content": "writer update\n"},
                        {"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"},
                    ],
                    "ledgerEvidence": {
                        "schemaVersion": "fop-task-ledger-evidence/v1",
                        "agentId": "infra",
                        "stageRunId": "wrong-stage-run-id",
                        "stageType": "writer",
                        "status": "COMPLETED",
                        "filesChanged": [
                            "allowed/output.txt",
                            ".agentic/runs/FOP-AGENTIC-003.md",
                        ],
                    },
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        record = self.fixture.last_record()
        self.assertEqual("LEDGER_MISMATCH", record["failureClassification"])
        self.assertEqual("MISMATCH", record["ledgerEvidenceStatus"])
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])

    def test_33_read_only_symlink_mutation_is_restored(self) -> None:
        outside_target = self.fixture.root / "outside-target.txt"
        outside_target.write_text("outside\n", encoding="utf-8")
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [{"path": ".agentic/runs/FOP-AGENTIC-003.md", "content": "writer ledger\n"}],
                },
                "testing-gate": {
                    "stdout": ["TESTING_DONE"],
                    "mutations": [
                        {
                            "path": "readonly/source.txt",
                            "kind": "symlink",
                            "target": str(outside_target),
                        }
                    ],
                },
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        record = self.fixture.last_record()
        restored_path = self.fixture.host_worktree / "readonly/source.txt"
        self.assertEqual("outside\n", outside_target.read_text(encoding="utf-8"))
        self.assertFalse(restored_path.is_symlink())
        self.assertEqual("clean\n", restored_path.read_text(encoding="utf-8"))
        self.assertEqual("PATH_VIOLATION", record["failureClassification"])
        self.assertIn("readonly/source.txt", record["mutatedPaths"])
        self.assertIn("readonly/source.txt", record["pathSafetyFindings"])
        self.assertEqual(
            self.fixture.snapshot(record["preSnapshotId"])["entries"],
            self.fixture.snapshot(record["postSnapshotId"])["entries"],
        )
        self.assertEqual("PAUSED_BLOCKED", self.fixture.state()["status"])
        self.assertIn("BLOCKED", result.stdout)

    def test_34_task_lock_covers_state_load_and_stage_selection(self) -> None:
        self.prepare_happy_path()
        (self.fixture.run_dir / "runner.lock").write_text("{}", encoding="utf-8")
        (self.fixture.run_dir / "state.json").write_text("{broken", encoding="utf-8")
        (self.fixture.run_dir / "state.sha256").write_text("not-a-digest", encoding="utf-8")
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id, expect_ok=False)
        self.assertIn("LOCK_CONFLICT", result.stderr)
        self.assertNotIn("state digest mismatch", result.stderr)
        self.assertNotIn("Invalid JSON file", result.stderr)
        self.assertNotIn("Task is not resumable", result.stderr)

    def test_35_prepare_rejects_existing_task_run(self) -> None:
        self.fixture.write_spec()
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        original_spec_bytes = (self.fixture.run_dir / "task-spec.json").read_bytes()
        original_state_bytes = (self.fixture.run_dir / "state.json").read_bytes()
        original_spec_digest = (self.fixture.run_dir / "task-spec.sha256").read_bytes()
        result = self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path), expect_ok=False)
        self.assertIn("RUN_ALREADY_EXISTS", result.stderr)
        self.assertEqual(original_spec_bytes, (self.fixture.run_dir / "task-spec.json").read_bytes())
        self.assertEqual(original_state_bytes, (self.fixture.run_dir / "state.json").read_bytes())
        self.assertEqual(original_spec_digest, (self.fixture.run_dir / "task-spec.sha256").read_bytes())

    def test_36_preexisting_symlink_directory_write_through_is_blocked(self) -> None:
        external_root = self.fixture.root / "external-preexisting"
        external_root.mkdir()
        external_target = external_root / "escape.txt"
        external_target.write_text("outside\n", encoding="utf-8")
        symlink_path = self.fixture.host_worktree / "allowed" / "linked-dir"
        os.symlink(external_root, symlink_path)
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {"path": "allowed/linked-dir/escape.txt", "content": "mutated\n"},
                    ],
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        record = json.loads(result.stdout)
        self.assertEqual("outside\n", external_target.read_text(encoding="utf-8"))
        self.assertEqual("FAILED", record["status"])
        self.assertEqual("AGENT_EXIT", record["failureClassification"])
        self.assertEqual([], record["mutatedPaths"])
        self.assertEqual([], record["unauthorizedPaths"])
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])

    def test_37_new_symlink_directory_write_through_is_blocked(self) -> None:
        external_root = self.fixture.root / "external-new"
        external_root.mkdir()
        external_target = external_root / "escape.txt"
        external_target.write_text("outside\n", encoding="utf-8")
        self.fixture.write_spec()
        self.fixture.set_behavior(
            {
                "writer-stage": {
                    "stdout": ["WRITER_DONE"],
                    "mutations": [
                        {
                            "path": "allowed/new-linked-dir",
                            "kind": "symlink",
                            "target": str(external_root),
                        },
                        {"path": "allowed/new-linked-dir/escape.txt", "content": "mutated\n"},
                    ],
                }
            }
        )
        self.fixture.run_runner("prepare", "--spec", str(self.fixture.spec_path))
        result = self.fixture.run_runner("run", "--task-id", self.fixture.task_id)
        record = json.loads(result.stdout)
        self.assertEqual("outside\n", external_target.read_text(encoding="utf-8"))
        self.assertEqual("FAILED", record["status"])
        self.assertEqual("AGENT_EXIT", record["failureClassification"])
        self.assertIn("allowed/new-linked-dir", record["mutatedPaths"])
        self.assertIn("allowed/new-linked-dir", record["pathSafetyFindings"])
        self.assertEqual("PAUSED_FAILED", self.fixture.state()["status"])


if __name__ == "__main__":
    unittest.main()
