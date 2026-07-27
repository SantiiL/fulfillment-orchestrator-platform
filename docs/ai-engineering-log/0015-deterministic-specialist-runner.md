# AI Engineering Log 0015: Deterministic Specialist Runner

## Context

`FOP-AGENTIC-003` introduces the repository source and governance
documentation for a deterministic specialist runner while keeping executable
authority outside repository task worktrees.

The goal is to make specialist execution reproducible, auditable, and
recoverable without granting repository source implicit runtime authority.

## Trigger And Prior Evidence

During `FOP-AGENTIC-002`, cross-agent `sessions_send` routing failed in the
control plane, which exposed a gap between repository-described specialist flow
and a deterministic, independently runnable execution mechanism.

That failure did not change reviewer independence or file-scope rules, but it
did justify a stronger operating model: specialist sequencing should be
recoverable from frozen external state rather than dependent on best-effort
session routing.

## Decision

The repository now documents and ships source for an external deterministic
runner instead of treating repository-local workflow text as executable
authority.

The key boundary is intentional:

* repository source remains auditable and reviewable
* installed runtime under `~/.openclaw` remains authoritative
* normal-task activation stays pending until human approval, external
  installation, manifest regeneration, and verifier success

## Python Versus Bash Tradeoff

The runner uses Python 3 standard library code with a thin Bash entrypoint.

Why Python:

* hostile JSON validation needs duplicate-key rejection, NaN/Infinity refusal,
  canonical serialization, and structured state handling
* snapshot, backup, restore, digest, and lock handling are easier to implement
  safely with filesystem and JSON primitives
* resumable state and test fixtures are easier to keep deterministic in Python

Why still keep Bash:

* operators get a stable executable entrypoint
* the shell wrapper stays trivial and auditable
* runtime installation can continue to expose a familiar script path

## Security Controls

The change records and implements `RC1` through `RC11`:

* `RC1`: hostile JSON parsing with size limits, duplicate-key rejection,
  NaN/Infinity rejection, strict field checking, and canonical spec hashing
* `RC2`: fixed external specialist invocation through argv arrays only, with
  frozen prompt hashing and no shell evaluation
* `RC3`: external runtime state is authoritative; repository ledger text and
  stdout are audit evidence only
* `RC4`: repo-relative path syntax only, plus canonical-path and symlink safety
* `RC5`: exact task-id, branch, worktree, and agent-id validation, with `run`,
  `resume`, and `status` selecting runs only through a validated `--task-id`
* `RC6`: minimal child environment plus restrictive artifact permissions
* `RC6a`: real fail-closed Landlock write confinement before specialist launch,
  limited to the validated task worktree plus per-attempt runtime artifacts
* `RC7`: locked snapshot, backup, restore, and atomic state/result persistence
* `RC8`: fail-closed resume on drift, tamper, live-child, and attempt-limit
  violations
* `RC9`: runtime verification from authoritative external paths only
* `RC10`: no Git ref or index mutation behavior in the runner
* `RC11`: deterministic fake-agent tests for hostile input, path safety,
  mutation remediation, resume integrity, runtime-boundary rules, and reviewer
  independence

The final remediated implementation also closes the three required Security
findings:

* every `stageId` now follows a strict safe identifier rule before any
  filesystem use
* the production CLI no longer accepts public `--runtime-config` or `--run-dir`
  overrides, and instead resolves the authoritative runtime config from
  `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
* writer-type stages require changed ledger digests plus matching ledger
  evidence for `agentId`, `stageRunId`, stage type, and the exact authorized
  mutation set, otherwise the runner fails closed with `LEDGER_MISMATCH`
* specialist children now run under real fail-closed Landlock write
  confinement; if confinement is unavailable, the runner blocks before launch
* snapshot, backup, validation, and restoration do not follow symlinks, so
  pre-existing and newly created directory-symlink write-through attempts are
  blocked and external targets remain unchanged

## Fake-Agent Test Strategy

The validation harness uses fake executables instead of real specialist
sessions.

Components:

* `fake_openclaw.py` simulates stage execution, file mutations, exit codes, and
  verdict files
* `fake_runtime_verifier.py` simulates verifier pass/fail behavior
* `test_fop_deterministic_runner.py` builds throwaway Git repositories and
  worktrees to exercise runner behavior end to end

Covered scenarios include:

* duplicate JSON keys
* NaN and Infinity rejection
* invalid task ids, branches, and agent ids
* path traversal, unsupported wildcards, and symlink escapes
* governance protection for control-plane paths
* read-only mutation detection and restoration
* writer path overreach with bounded restoration
* reviewer independence failure
* runtime-verifier failure
* spoofed success markers with failing child exit
* worktree drift before resume
* state digest tampering
* lock conflict
* attempt limits
* crash recovery between stages
* final human-approval stop
* absence of forbidden Git operations
* proof that task-spec content is not executed as shell
* strict `stageId` rejection for traversal and unsafe separators
* CLI rejection of public runtime-authority override flags
* missing or mismatched ledger evidence for writer stages
* pre-existing directory-symlink write-through attempts
* newly created directory-symlink write-through attempts

## Current Validation Evidence

Current evidence for this governance change is:

* task header: `INDEPENDENT_TESTING=PASS`
* task header: `SECURITY_ADVISORY=PASS`
* Security re-review: `PASS`
* required Security changes: `NONE`
* repository test harness: `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (37 tests)`
* repository source includes the Python runner, Bash entrypoint, schema,
  example spec, runtime policy documents, and deterministic fixtures

This documentation pass did not activate the runtime and does not replace the
later final validation sweep, reviewer advisory, or human approval steps.

## Residual Risks

Residual risks remain visible and intentional:

* repository source and installed runtime can drift until post-approval
  installation is performed
* runtime manifest regeneration is a separate operational step and can be
  skipped unless explicitly governed
* stale-lock cleanup remains a manual, fail-closed operator procedure
* reviewer quality still depends on the independently selected reviewer agent
* human approval remains the final stop before activation

## Activation Status

Activation remains pending.

The runner must not be described as active for normal tasks until all of the
following happen in order:

1. final human approval for `FOP-AGENTIC-003`
2. external installation into `~/.openclaw`
3. runtime-contract manifest regeneration
4. successful verification from the installed verifier

## AI-Assisted Work

Codex was used to:

* inspect the new runner source, schema, policy, and tests
* reconcile the repository trust-boundary language with operator-facing docs
* write the operator guide and activation warnings
* append documentation-stage evidence to the task ledger without overwriting
  prior infra evidence

## Human-Owned Decisions

The following decisions remained human-owned:

* keeping runtime authority external to repository task worktrees
* requiring post-merge installation and manifest regeneration before activation
* choosing Python standard library for the runner and Bash only for the
  entrypoint shim
* preserving reviewer independence and final human approval as separate gates
