# Deterministic Runner Policy

The deterministic specialist runner is the normal task-execution mechanism
only after all of the following are true:

1. `FOP-AGENTIC-003` is human approved.
2. the externally installed runner has been updated outside the repository.
3. the external runtime-contract manifest has been regenerated.
4. `bash ~/.openclaw/scripts/verify-fop-runtime-contracts.sh` passes.

Until then, repository source remains auditable guidance and normal tasks must
not claim the runner is active.

## Responsibilities

The runner must:

* validate hostile task specs and freeze their canonical JSON plus SHA-256 hash
* freeze prompt files and record their SHA-256 hashes
* load authoritative runtime paths only from `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
* execute stages strictly in order with one writer at a time
* verify runtime hashes before `prepare` and before every stage attempt
* acquire the per-task lock immediately after deriving the validated run
  directory and keep it through state load, runtime/spec verification, drift
  checks, stage selection, stage execution, and final state persistence
* snapshot the host worktree, persist content-addressed backups, and record
  machine-readable stage results
* install OS-enforced write confinement before every specialist `exec`, allow
  writes only inside the validated task worktree plus per-attempt runtime
  artifact directories, and fail closed before launch when that confinement is
  unavailable
* enforce stage path policy, bounded remediation, reviewer independence, and
  final human-approval stop conditions
* derive every run directory from the authoritative run root plus validated
  `taskId`
* create task run directories atomically and reject any pre-existing task run
  fail closed without overwriting frozen artifacts or state
* require writer and documentation-writer stages to update the ledger and emit
  canonical machine-readable ledger evidence keyed by the exact external
  `stageRunId`

The runner must not:

* resolve executable authority from repository content or task-spec command
  fields
* invoke `sessions_send`
* perform git ref or index mutation
* regenerate the runtime manifest during a task
* commit, push, merge, clean, or remove worktrees

## CLI

The Python CLI supports:

* `validate-spec`
* `prepare`
* `run`
* `resume`
* `status`

`prepare` freezes the spec and prompts under the external run root.

`run` starts the first incomplete stage from the frozen spec selected by the
validated `--task-id`.

`resume` reruns only the first non-success stage after closed-world
verification of state, runtime identity, and worktree drift.

`status` is read-only, selects a task by validated `--task-id`, and may
optionally verify runtime and snapshot consistency.

## Safety Rules

* allowed repository path syntax is limited to exact repo-relative paths and
  repo-relative directory patterns ending in `/**`
* snapshot, validation, backup, and restore must use `lstat`-based traversal
  and must never follow symlinks
* protected control-plane paths require `GOVERNANCE_CHANGE` classification,
  human preapproval, and explicit listing
* read-only mutations are always restored from the pre-stage snapshot before a
  blocked return; restoration must operate on lexical repo paths with
  `lstat`-based removal, never write through symlink ancestors, restore
  parent-before-child, and require exact post-restore snapshot parity
* specialist write confinement must block writes that resolve outside the
  worktree even when a directory symlink already exists or is created during
  the stage
* writer overreach restores only unauthorized paths, preserves authorized
  edits, and requires explicit resume
* writer and documentation-writer stages capture the ledger digest before and
  after execution; unchanged, missing, or inconsistent evidence yields
  `LEDGER_MISMATCH`
* reviewer independence is derived from successful writer and
  documentation-writer stage records stored in external state
