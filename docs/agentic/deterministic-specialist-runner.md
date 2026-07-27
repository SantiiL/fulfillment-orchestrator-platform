# Deterministic Specialist Runner

## Purpose

The deterministic specialist runner is the repository-governed execution model
for agentic tasks after approval and external activation.

Its job is to turn a human-approved task spec into a repeatable stage sequence
with one active writer at a time, closed-world state tracking, explicit review
independence, and auditable recovery points.

`FOP-AGENTIC-003` adds the repository source, policies, schema, and tests for
this runner. It does not make the runner active for normal tasks yet.

## Trust Boundary

Repository source is descriptive and auditable. External runtime state is
authoritative and executable.

Repository source defines:

* task-spec schema and example under `.agentic/`
* workflow and policy documents under `.agentic/`
* the runner source and tests under `scripts/agentic/`
* task ledgers under `.agentic/runs/`

External runtime authority defines:

* installed entrypoint `~/.openclaw/scripts/fop-deterministic-runner`
* runtime config `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
* verifier `~/.openclaw/scripts/verify-fop-runtime-contracts.sh`
* runtime-contract hashes under `~/.openclaw/trust/...`
* executable specialist workspaces outside repository task worktrees

The runner always verifies the installed external runtime before `prepare` and
before every stage attempt. Repository files do not override executable paths.

## Threat Model

The runner is designed for hostile or drifting inputs, not for a trusted happy
path only.

Primary threats:

* malformed or oversized JSON specs
* duplicate keys, NaN, and Infinity tricks during parsing
* task-spec attempts to smuggle shell commands or absolute paths
* symlink escapes and path traversal outside the worktree
* directory-symlink write-through into external targets
* unauthorized writer edits outside approved paths
* read-only stage mutations
* runtime drift between prepare and later stage execution
* stale or tampered runner state
* concurrent writers colliding on the same repository
* reviewer reuse from the writer set
* accidental Git ref or index mutation during execution
* stale locks being cleared casually instead of through a manual fail-closed
  operator step

## Runtime Config

The Python CLI reads runtime authority from
`~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json` by default. That file must provide:

* `schemaVersion: fop-deterministic-runner-runtime/v1`
* `openClawExecutable`
* `runtimeContractVerifier`
* `runRoot`
* `lockRoot`

All runtime paths must be absolute. The runner hashes the runtime config, the
OpenClaw executable, and the verifier, stores those hashes in state, and fails
closed if they change before later stages or status verification.

## Task Specification

The accepted spec schema is `fop-task-spec/v1`.

Required top-level sections:

* `task`
* `repo`
* `governance`
* `scope`
* `paths`
* `stages`

Important repository fields:

* `repo.baseBranch`
* `repo.taskBranch`
* `repo.hostWorktreePath`
* optional `repo.sandboxMirrorPath`

Important governance fields:

* `humanPreApproval`
* `finalHumanApprovalRequired`, which must remain `true`
* optional `designApprovalTimestamp`

Writable-path rules:

* paths must be repo-relative
* absolute paths are rejected
* traversal such as `../` is rejected
* only exact paths or terminal `/**` directory patterns are allowed
* protected control-plane paths require `GOVERNANCE_CHANGE` plus human
  preapproval

For `GOVERNANCE_CHANGE` tasks, `stages.documentationWriter` is mandatory.

## Stages

The runner executes the frozen `stageOrder` in this sequence:

1. one or more `writer` stages
2. zero or more `read-only-gate` stages
3. optional `documentation-writer`
4. mandatory `final-reviewer`

Writer stages and the documentation writer may mutate only their stage
`allowedPaths`. Read-only gates and the final reviewer must declare an empty
`allowedPaths` list.

Each stage definition includes:

* `stageId`
* `agentId`
* `promptFile`
* `allowedPaths`
* `timeoutSeconds`
* `successMarkers`
* `maxAttempts`

Every `stageId` must satisfy the strict safe identifier rule enforced by the
schema and runner before any filesystem interpolation. Unsafe separators,
traversal, and other non-safe identifier forms are rejected.

The runner freezes prompt files during `prepare`, stores their SHA-256 hashes,
and later executes the external specialist with argv only. It does not evaluate
task-spec shell text.

## Allowed-Path Policy

Global write permission comes from `paths.globallyWritablePaths`. Stage-level
permission must be a subset of that global allowlist.

Enforcement behavior:

* changed paths are detected by pre/post snapshots
* every changed path is normalized and canonicalized back into the worktree
* specialist children run under real fail-closed Landlock write confinement
  before `exec`
* writable roots are limited to the validated task worktree plus the
  per-attempt runtime artifact directory
* confinement unavailability blocks the stage before specialist launch
* read-only stage mutations are fully restored and the stage is blocked
* writer overreach restores only unauthorized paths, preserves authorized edits,
  and records `PATH_VIOLATION`
* pre-existing and newly created directory-symlink write-through attempts are
  blocked and external targets remain unchanged

This is why the runner can preserve good work while still failing closed on
scope leaks.

## Commands

Validate a spec:

```bash
bash ~/.openclaw/scripts/fop-deterministic-runner validate-spec --spec /absolute/path/task-spec.json
```

Prepare a run:

```bash
bash ~/.openclaw/scripts/fop-deterministic-runner prepare --spec /absolute/path/task-spec.json
```

Run the next incomplete stage:

```bash
bash ~/.openclaw/scripts/fop-deterministic-runner run --task-id FOP-AGENTIC-003
```

Resume after a failed or blocked stage:

```bash
bash ~/.openclaw/scripts/fop-deterministic-runner resume --task-id FOP-AGENTIC-003
```

Inspect status:

```bash
bash ~/.openclaw/scripts/fop-deterministic-runner status --task-id FOP-AGENTIC-003 --verify
```

## Prepare, Run, Resume, And Status

`prepare` does the closed-world setup:

* validates the hostile spec
* verifies the external runtime
* freezes canonical spec JSON and its SHA-256 digest
* freezes prompt files and their hashes
* snapshots the host worktree
* initializes run state under the external `runRoot`

`run`, `resume`, and `status` all select the task through a validated
`--task-id`. The production CLI does not accept public `--runtime-config` or
`--run-dir` overrides.

`run` executes exactly the first incomplete stage from the frozen spec.

`resume` re-checks runtime identity, state integrity, live-child absence, and
last stable snapshot consistency before retrying the first non-success stage.

`status` is read-only. With `--verify`, it also rechecks runtime identity and
the last stable worktree snapshot.

## State And Logs

Each task run is stored under `runRoot/<task-id>/` with private permissions.

Key artifacts:

* `task-spec.json`
* `task-spec.sha256`
* `state.json`
* `state.sha256`
* `runtime-verification/*.log`
* `snapshots/*.json`
* `snapshots/*.status.txt`
* `snapshots/*.diff.txt`
* `backups/*.json`
* `blobs/<sha256>`
* `restores/*.json`
* per-attempt `logs/*.log`
* per-attempt `results/*.child.json`
* per-attempt `results/*.result.json`

`state.json` records status, runtime identity hashes, frozen prompt digests,
stage order, stage records, the last stable snapshot, and any active child PID.

The runner derives `run_dir`, `stage_run_id`, child result paths, and stage
result paths internally from authoritative runtime state plus the validated
task identifier.

## Locks And Concurrency

The runner uses two lock layers:

* per-run lock: `run_dir/runner.lock`
* global writer lock: `lockRoot/fop-writer.lock`

The per-run lock prevents concurrent mutation of one task state. The global
writer lock ensures only one writer-type stage runs at a time across the
runtime. Read-only gates do not take the global writer lock.

If a lock already exists, the runner fails with `LOCK_CONFLICT`.

Accepted residual risk: stale-lock cleanup remains a manual, fail-closed
operator action. Operators must investigate ownership first and should not
delete locks casually.

Deterministic tests `36` and `37` cover pre-existing and newly created
directory-symlink write-through attempts and confirm those writes are blocked.

## Snapshots, Backups, And Restoration

The runner snapshots the worktree:

* at prepare time
* before each stage
* after each stage
* after any remediation or restoration
* during status or resume verification

Each snapshot captures filesystem entry metadata plus Git `status --short
--branch` and `diff --stat` output for diagnosis.

Before each stage, the runner materializes a content-addressed backup so it can
restore mutated paths without relying on Git checkout or reset behavior.

Snapshot, backup, validation, and restoration use symlink-safe filesystem
handling and do not follow symlinks into external locations.

## Mutation Detection And Restoration

After stage execution, the runner compares pre/post snapshots.

Outcomes:

* no mutation outside policy: stage may succeed
* read-only mutation: all changed paths are restored, stage becomes `BLOCKED`
* writer unauthorized mutation: only unauthorized paths are restored, stage
  becomes `FAILED`
* attempted directory-symlink write-through outside the validated writable roots
  is blocked by confinement and leaves the external target unchanged

Restoration records a restore manifest so operators can see exactly what was
rolled back.

## Ledger Evidence Enforcement

Writer-type stages do not succeed on exit status alone.

Successful `writer` and `documentation-writer` stages must produce canonical
ledger evidence whose digest changes and whose `ledgerEvidence` payload matches
the exact:

* `agentId`
* `stageRunId`
* stage type
* completion status
* authorized changed-file set

If any part is missing or mismatched, the runner records `LEDGER_MISMATCH` and
leaves the task in `PAUSED_FAILED`.

## Reviewer Independence

The runner derives the writer set from successful `writer` and
`documentation-writer` stage records.

Before the final reviewer runs, the runner checks that the selected reviewer
agent is not in that derived writer set. If the reviewer is reused, the runner
records `INDEPENDENCE_FAIL` and stops.

This keeps review independent even when documentation is the final writer stage.

## Human Approval Gates

`governance.finalHumanApprovalRequired` is mandatory and must stay `true`.

Even after all stages pass, the runner stops at
`WAITING_FOR_HUMAN_APPROVAL`. That state is intentional. Stage success is not
merge authority, installation authority, or runtime activation authority.

## Failure States

Primary failure classifications are:

* `AGENT_EXIT`
* `ATTEMPT_LIMIT`
* `INDEPENDENCE_FAIL`
* `LEDGER_MISMATCH`
* `LOCK_CONFLICT`
* `MIRROR_DRIFT`
* `PATH_VIOLATION`
* `RUNTIME_VERIFY_FAILED`
* `SPEC_INVALID`
* `STATE_TAMPERED`
* `TIMEOUT`
* `WORKTREE_INVALID`

Primary task statuses are:

* `PREPARED`
* `RUNNING`
* `PAUSED_FAILED`
* `PAUSED_BLOCKED`
* `WAITING_FOR_HUMAN_APPROVAL`

## Recovery Procedures

Use this sequence:

1. Run `status --verify` to confirm runtime identity and snapshot integrity.
2. Read the latest stage result JSON and log file under the run directory.
3. If the stage was blocked by read-only mutation or path overreach, inspect
   the restore manifest to confirm what was reverted.
4. Correct the underlying issue without mutating protected runtime artifacts by
   hand.
5. Run `resume --task-id <task-id>` if the run is resumable.

Do not bypass failure by editing `state.json`, deleting locks casually, or
forcing Git cleanup. State tampering is detected by `state.sha256`.

## Forbidden Git Behavior

The runner deliberately avoids Git operations that mutate refs or the index.

Forbidden subcommands include:

* `git branch`
* `git checkout`
* `git cherry-pick`
* `git clean`
* `git commit`
* `git merge`
* `git push`
* `git rebase`
* `git reset`
* `git stash`
* `git switch`
* `git worktree add`
* `git worktree remove`

Git is used only for read-oriented evidence such as `status` and `diff`.

## External Installation

This repository change does not install the runner into `~/.openclaw`.

After governance approval, the reviewed entrypoint, Python runtime, verifier,
and related specialist contracts must be installed externally in the operator's
OpenClaw home. Until that happens, repository copies remain source artifacts,
not runtime authority.

## Manifest Regeneration

The runtime-contract manifest is also external. It must be regenerated only
after the approved runtime has been installed, never during this governance
task's implementation pass.

After regeneration, the operator must run:

```bash
bash ~/.openclaw/scripts/verify-fop-runtime-contracts.sh
```

Normal tasks must not claim the deterministic runner is active until that
verification passes from the installed verifier.

## Post-Merge Activation

Post-merge activation remains pending. The required sequence is:

1. final human approval of `FOP-AGENTIC-003`
2. merge of the reviewed repository change
3. external installation into `~/.openclaw`
4. runtime-contract manifest regeneration
5. successful runtime verification from the installed verifier
6. only then normal-task adoption

## End-To-End Example

Example lifecycle for a governance task:

1. Author a spec that points to the host worktree, ledger path, allowed paths,
   prompts, writer stages, read-only gates, documentation writer, and final
   reviewer.
2. Run `validate-spec` to reject malformed JSON, path traversal, or invalid
   stage definitions before any mutation.
3. Run `prepare` to freeze the spec and prompts, verify the installed runtime,
   and create the first stable snapshot.
4. Run `run --task-id ...` repeatedly. Each call executes one stage in order:
   writer, testing gate, documentation writer, then final reviewer.
5. If a read-only stage mutates files or a writer escapes its allowlist, review
   the restore manifest, fix the stage cause, and `resume`.
6. After the final reviewer returns `APPROVE`, the task stops at
   `WAITING_FOR_HUMAN_APPROVAL`.
7. Only after later human approval, external installation, manifest
   regeneration, and verifier success can operators treat the runner as active
   for normal tasks.
