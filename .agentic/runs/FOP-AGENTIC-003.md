# FOP-AGENTIC-003

## Task Metadata

| Field | Value |
| --- | --- |
| Task id | `FOP-AGENTIC-003` |
| Issue | `#25` |
| Issue URL | `https://github.com/SantiiL/fulfillment-orchestrator-platform/issues/25` |
| Classification | `GOVERNANCE_CHANGE` |
| Base branch | `develop` |
| Task branch | `chore/deterministic-specialist-runner` |
| Worktree path | `/home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-agentic-003` |
| Session key | `fop-agentic-003-documentation-human-approval-20260727-145118` |
| Human preapproval | `APPROVED` |
| Design approval timestamp | `2026-07-21T12:25:42+00:00` |
| Current stage | `final-human-approval-recording` |
| Derived writer set | `infra, documentation` |
| Selected reviewer | `reviewer` |
| Reviewer independence expression | `reviewer NOT IN {infra, documentation}` |
| Reviewer independence result | `PASS` |
| Reviewer verdict | `APPROVE` |
| Human approval | `APPROVED` |

## Goal

Implement the reusable deterministic specialist runner and aligned governance
documents without activating the runner for normal tasks before external
installation and manifest verification.

## Acceptance Criteria

* provide the Python 3 standard-library deterministic runner, safe shell
  entrypoint, and deterministic test harness
* enforce hostile-spec validation, frozen spec and prompt hashes, runtime
  verification, locks, snapshots, bounded remediation, reviewer independence,
  and final human-approval stop behavior
* update governance documents so the deterministic runner becomes the intended
  normal execution mechanism only after final human approval, external
  installation, manifest regeneration, and manifest verification

## Required Controls

* `RC1` hostile JSON parsing, size bounds, duplicate-key rejection, NaN and
  Infinity rejection, strict schema fields, canonical spec hash
* `RC2` fixed external specialist invocation through argv arrays only, no
  shell evaluation, frozen prompt-file hashing
* `RC3` external state as runtime authority, ledger and stdout as audit
  evidence only
* `RC4` restricted repo-relative path syntax plus symlink and canonical-path
  safety
* `RC5` exact task id, branch, and agent-id validation
* `RC6` minimal child environment and restrictive artifact permissions
* `RC7` snapshot, backup, restore, and atomic state/result handling under
  locks
* `RC8` fail-closed resume on drift, tamper, live child, or attempt-limit
  violations
* `RC9` runtime verification from authoritative external paths only
* `RC10` absence of ref/index-mutating git operations
* `RC11` deterministic fake-runtime tests for hostile input, path safety,
  mutation remediation, resume integrity, and runtime-boundary rules

## Advisory Evidence

* Architect advisory evidence: `READY FOR IMPLEMENTATION`
* Security advisory evidence: `PASS`
* Independent Testing: `PASS`
* Implementation Security: `PASS`
* Documentation reconciliation: `PASS`
* Final Testing: `PASS`
* Final Reviewer: `APPROVE`
* Final human approval: `APPROVED`
* External installation: `PENDING`
* Runtime manifest regeneration: `PENDING`

## Out Of Scope

* installing the runner into `~/.openclaw` during this task
* regenerating the external runtime manifest during this task
* modifying `src/**`
* committing, pushing, merging, creating a pull request, or staging files

## Protected Paths Intentionally Changed

* `.agentic/README.md`
* `.agentic/policies/deterministic-runner.md`
* `.agentic/policies/governance-change.md`
* `.agentic/policies/task-ledger.md`
* `.agentic/policies/trust-boundary.md`
* `.agentic/runtime-contracts.md`
* `.agentic/runtime-requirements.json`
* `.agentic/roles/architect.md`
* `.agentic/roles/security.md`
* `.agentic/roles/testing-engineer.md`
* `.agentic/roles/frontend-engineer.md`
* `.agentic/roles/qa-api.md`
* `.agentic/runs/FOP-AGENTIC-003.md`
* `.agentic/schemas/deterministic-task-spec.schema.json`
* `.agentic/templates/deterministic-task-spec.example.json`
* `.agentic/templates/task-ledger.md`
* `.agentic/workflows/feature-delivery.md`
* `.agentic/workflows/governance-change.md`
* `README.md`
* `docs/agentic/deterministic-specialist-runner.md`
* `docs/ai-engineering-log/0015-deterministic-specialist-runner.md`
* `scripts/agentic/fop-deterministic-runner`
* `scripts/agentic/fop_deterministic_runner.py`
* `scripts/agentic/tests/__init__.py`
* `scripts/agentic/tests/test_fop_deterministic_runner.py`

## Writer Events

### Writer Event 1

* agentId: `infra`
* role contract: `.agentic/roles/infra.md`
* stage: `implementation-and-governance-documentation`
* session key or run ID: `fop-agentic-003-infra-20260721-122529`
* files changed: `.agentic/README.md`, `.agentic/policies/deterministic-runner.md`, `.agentic/policies/governance-change.md`, `.agentic/policies/task-ledger.md`, `.agentic/policies/trust-boundary.md`, `.agentic/runtime-contracts.md`, `.agentic/runtime-requirements.json`, `.agentic/roles/architect.md`, `.agentic/roles/security.md`, `.agentic/roles/testing-engineer.md`, `.agentic/roles/frontend-engineer.md`, `.agentic/roles/qa-api.md`, `.agentic/runs/FOP-AGENTIC-003.md`, `.agentic/schemas/deterministic-task-spec.schema.json`, `.agentic/templates/deterministic-task-spec.example.json`, `.agentic/templates/task-ledger.md`, `.agentic/workflows/feature-delivery.md`, `.agentic/workflows/governance-change.md`, `.agentic/test-fixtures/deterministic-runner/prompts/infra.md`, `.agentic/test-fixtures/deterministic-runner/prompts/testing-engineer.md`, `.agentic/test-fixtures/deterministic-runner/prompts/documentation.md`, `.agentic/test-fixtures/deterministic-runner/prompts/reviewer.md`, `scripts/agentic/fop-deterministic-runner`, `scripts/agentic/fop_deterministic_runner.py`, `scripts/agentic/tests/__init__.py`, `scripts/agentic/tests/fixtures/fake_openclaw.py`, `scripts/agentic/tests/fixtures/fake_runtime_verifier.py`, `scripts/agentic/tests/test_fop_deterministic_runner.py`
* timestamp or ordering evidence: `2026-07-21T12:25:29+00:00`

### Writer Event 2

* agentId: `documentation`
* role contract: `.agentic/roles/documentation.md`
* stage: `documentation`
* session key or run ID: `fop-agentic-003-documentation-20260721-150545`
* files changed: `.agentic/README.md`, `.agentic/runs/FOP-AGENTIC-003.md`, `README.md`, `docs/agentic/deterministic-specialist-runner.md`, `docs/ai-engineering-log/0015-deterministic-specialist-runner.md`
* timestamp or ordering evidence: `Writer Event 1 recorded first; documentation reconciliation recorded second under session key fop-agentic-003-documentation-20260721-150545`

### Writer Event 3

* agentId: `infra`
* role contract: `.agentic/roles/infra.md`
* stage: `security-remediation`
* session key or run ID: `fop-agentic-003-infra-security-remediation-20260721-164848`
* files changed: `.agentic/policies/deterministic-runner.md`, `.agentic/policies/task-ledger.md`, `.agentic/runtime-contracts.md`, `.agentic/runtime-requirements.json`, `.agentic/runs/FOP-AGENTIC-003.md`, `.agentic/schemas/deterministic-task-spec.schema.json`, `scripts/agentic/fop_deterministic_runner.py`, `scripts/agentic/tests/fixtures/fake_openclaw.py`, `scripts/agentic/tests/test_fop_deterministic_runner.py`
* findings remediated:
  * `stageId` now uses an exact safe identifier rule in code and schema, rejecting traversal and unsafe separators before any filesystem interpolation
  * production CLI runtime authority replacement flags were removed; the runner now resolves `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json` internally and derives run directories from the authoritative run root plus validated `taskId`
  * writer and documentation-writer stages now capture ledger digests, require canonical machine-readable ledger evidence keyed by the exact external `stageRunId`, and fail closed with `LEDGER_MISMATCH` on missing or inconsistent evidence
* test evidence:
  * `python3 -m py_compile scripts/agentic/fop_deterministic_runner.py scripts/agentic/tests/test_fop_deterministic_runner.py scripts/agentic/tests/fixtures/fake_openclaw.py scripts/agentic/tests/fixtures/fake_runtime_verifier.py` -> `PASS`
  * `python3 -c 'import json; ...'` for `.agentic/schemas/deterministic-task-spec.schema.json`, `.agentic/templates/deterministic-task-spec.example.json`, and `.agentic/runtime-requirements.json` -> `JSON_OK`
  * `python3 -c 'import json, jsonschema; ...'` validating `.agentic/templates/deterministic-task-spec.example.json` against `.agentic/schemas/deterministic-task-spec.schema.json` -> `SCHEMA_OK`
  * `python3 scripts/agentic/fop_deterministic_runner.py validate-spec --spec .agentic/templates/deterministic-task-spec.example.json` -> `PASS`
  * `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (32 tests)`
  * `./gradlew test --no-daemon` -> `BUILD SUCCESSFUL`
* ordering evidence:
  * previous Security review produced three required remediation findings
  * those findings are implemented in repository source and deterministic tests
  * independent Security re-review is pending before Security can return to `PASS`
  * documentation reconciliation is pending
  * final Testing is pending
  * reviewer advisory is pending
  * external installation is pending
  * manifest regeneration is pending
  * final human approval is pending

### Writer Event 4

* agentId: `documentation`
* role contract: `.agentic/roles/documentation.md`
* stage: `documentation-reconciliation`
* session key or run ID: `fop-agentic-003-documentation-reconcile-20260721-172724`
* files changed: `.agentic/README.md`, `.agentic/runs/FOP-AGENTIC-003.md`, `README.md`, `docs/agentic/deterministic-specialist-runner.md`, `docs/ai-engineering-log/0015-deterministic-specialist-runner.md`
* Security re-review: `PASS`
* REQUIRED_CHANGES: `NONE`
* deterministic test evidence: `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (32 tests)`
* reconciliation evidence:
  * documented the strict safe identifier rule for every `stageId`
  * removed operator guidance implying public `--runtime-config` or `--run-dir` usage
  * documented the authoritative runtime-config path as `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
  * documented validated `--task-id` selection for `run`, `resume`, and `status`
  * documented ledger-evidence enforcement for writer stages, including digest change, exact `agentId`, `stageRunId`, stage type, and authorized mutation-set matching, with `LEDGER_MISMATCH` on mismatch
  * updated deterministic test references from `27` to `32`
  * recorded that all three required Security findings were remediated and independently re-reviewed as `PASS`
  * recorded the accepted residual risk that stale-lock cleanup remains manual and fail closed
  * preserved the activation boundary: external installation pending, manifest regeneration pending, and the runner not yet approved for normal tasks
* ordering evidence:
  * Writer Events 1 through 3 remain preserved unchanged
  * derived writer set remains `infra, documentation`
  * reviewer independence remains `reviewer NOT IN {infra, documentation}`
  * final Testing remains `PENDING`
  * final Reviewer remains `PENDING`
  * final human approval remains `PENDING`
  * external installation remains `PENDING`
* manifest regeneration remains `PENDING`

### Writer Event 5

* agentId: `infra`
* role contract: `.agentic/roles/infra.md`
* stage: `human-review-remediation`
* session key or run ID: `fop-agentic-003-infra-human-review-20260725-150228`
* files changed: `.agentic/policies/deterministic-runner.md`, `.agentic/policies/task-ledger.md`, `.agentic/runtime-contracts.md`, `.agentic/runs/FOP-AGENTIC-003.md`, `scripts/agentic/fop_deterministic_runner.py`, `scripts/agentic/tests/test_fop_deterministic_runner.py`
* findings remediated:
  * `Finding 1` safe restoration after symlink mutation now restores read-only mutations before returning, removes unsafe targets with `lstat` semantics, skips descendant traversal beneath changed symlink/non-directory ancestors, restores parent-before-child only through real directories, and requires the restored snapshot to equal the pre-stage snapshot
  * `Finding 2` the per-task `runner.lock` is now acquired immediately after deriving the validated run directory and remains held across state load, digest/runtime/spec verification, live-child and drift checks, resume validation, stage selection, stage execution, and final state persistence
  * `Finding 3` `prepare_run` now atomically creates the task run directory and rejects any existing run with `RUN_ALREADY_EXISTS` without overwriting frozen spec bytes, spec digest, prompts, snapshots, or state
* deterministic tests added:
  * `test_33_read_only_symlink_mutation_is_restored`
  * `test_34_task_lock_covers_state_load_and_stage_selection`
  * `test_35_prepare_rejects_existing_task_run`
* total deterministic test count: `35`
* test evidence:
  * `bash -n scripts/agentic/fop-deterministic-runner` -> `PASS`
  * `python3 -m py_compile scripts/agentic/fop_deterministic_runner.py scripts/agentic/tests/test_fop_deterministic_runner.py scripts/agentic/tests/fixtures/fake_openclaw.py scripts/agentic/tests/fixtures/fake_runtime_verifier.py` -> `PASS`
  * `python3 -c 'import json, pathlib; ...'` for `.agentic/schemas/deterministic-task-spec.schema.json`, `.agentic/templates/deterministic-task-spec.example.json`, and `.agentic/runtime-requirements.json` -> `JSON_OK`
  * `python3 scripts/agentic/fop_deterministic_runner.py validate-spec --spec .agentic/templates/deterministic-task-spec.example.json` -> `PASS`
  * `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (35 tests)`
  * `./gradlew test --no-daemon` -> `BUILD SUCCESSFUL`
  * `git diff --cached --name-only` -> `NO STAGED FILES`
* ordering evidence:
  * Writer Events 1 through 4 remain preserved unchanged
  * derived writer set remains `infra, documentation`
  * reviewer independence remains `reviewer NOT IN {infra, documentation}`
  * final Security re-review pending
  * Documentation reconciliation pending
  * final Testing pending
  * final Reviewer pending
  * final human approval pending
  * external installation pending
* runtime manifest regeneration pending

### Writer Event 6

* agentId: `infra`
* role contract: `.agentic/roles/infra.md`
* stage: `symlink-write-confinement-remediation`
* session key or run ID: `fop-agentic-003-infra-symlink-containment-20260725-172727`
* files changed: `.agentic/policies/deterministic-runner.md`, `.agentic/policies/task-ledger.md`, `.agentic/policies/trust-boundary.md`, `.agentic/runtime-contracts.md`, `.agentic/runtime-requirements.json`, `.agentic/runs/FOP-AGENTIC-003.md`, `scripts/agentic/fop_deterministic_runner.py`, `scripts/agentic/tests/test_fop_deterministic_runner.py`
* confinement strategy:
  * the runner now installs kernel-enforced child write confinement before every specialist `exec`
  * writable roots are limited to the validated task worktree plus the per-attempt runtime artifact directory
  * if that host confinement primitive is unavailable, the runner fails closed before specialist launch
* deterministic tests added:
  * `test_36_preexisting_symlink_directory_write_through_is_blocked`
  * `test_37_new_symlink_directory_write_through_is_blocked`
* total deterministic test count: `37`
* ordering evidence:
  * Writer Events 1 through 5 remain preserved unchanged
  * this remediation follows Writer Event 5 and addresses the remaining Security finding about directory-symlink write-through outside the worktree
  * derived writer set remains `infra, documentation`
  * Security re-review pending
  * Documentation reconciliation pending
  * final Testing pending
  * final Reviewer pending
  * final human approval pending
  * external installation pending
  * runtime manifest regeneration pending

### Writer Event 7

* agentId: `documentation`
* role contract: `.agentic/roles/documentation.md`
* stage: `final-documentation-reconciliation`
* session key or run ID: `fop-agentic-003-documentation-final-20260725-181556`
* files changed: `.agentic/README.md`, `.agentic/runs/FOP-AGENTIC-003.md`, `README.md`, `docs/agentic/deterministic-specialist-runner.md`, `docs/ai-engineering-log/0015-deterministic-specialist-runner.md`
* Security re-review: `PASS`
* REQUIRED_CHANGES: `NONE`
* deterministic test evidence: `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (37 tests)`
* reconciliation evidence:
  * documented that specialist children use real fail-closed Landlock write confinement before `exec`
  * documented that writable roots are limited to the validated worktree plus per-attempt runtime artifacts
  * documented that confinement unavailability blocks before specialist launch
  * documented that snapshot, backup, validation, and restoration do not follow symlinks
  * documented that pre-existing and newly created directory-symlink write-through attempts are blocked and external targets remain unchanged
  * recorded deterministic coverage for `test_36_preexisting_symlink_directory_write_through_is_blocked` and `test_37_new_symlink_directory_write_through_is_blocked`
  * updated deterministic test references from `35` and `32` to `37`
  * preserved the activation boundary: external installation pending, runtime-manifest regeneration pending, final Testing pending, final Reviewer pending, and final human approval pending
  * preserved the accepted residual risk that stale-lock recovery remains manual and fail closed
* ordering evidence:
  * Writer Events 1 through 6 remain preserved unchanged
  * this documentation reconciliation follows Writer Event 6 and records the final implemented confinement behavior after Security returned `PASS` with `REQUIRED_CHANGES: NONE`
  * derived writer set remains `infra, documentation`
  * reviewer independence remains `reviewer NOT IN {infra, documentation}`
  * final Testing remains `PENDING`
  * final Reviewer remains `PENDING`
  * final human approval remains `PENDING`
  * external installation remains `PENDING`
  * runtime manifest regeneration remains `PENDING`

### Writer Event 8

* agentId: `documentation`
* role contract: `.agentic/roles/documentation.md`
* stage: `ledger-file-coverage-reconciliation`
* session key or run ID: `fop-agentic-003-documentation-ledger-coverage-20260725-190932`
* files changed: `.agentic/runs/FOP-AGENTIC-003.md`
* prior Reviewer verdict: `REQUEST_CHANGES`
* finding:
  * Writer Event 5 records `.agentic/runs/FOP-AGENTIC-003.md` as changed, but the File Coverage appendix omitted Writer Event 5 for that file while claiming complete coverage
* resolution:
  * reconciled the File Coverage appendix from all recorded writer events, added Writer Event 5 to the `.agentic/runs/FOP-AGENTIC-003.md` row, and added Writer Event 8 to that same row because this reconciliation also changes the ledger file
* deterministic test count: `37`
* ordering evidence:
  * Writer Events 1 through 7 remain preserved unchanged
  * this documentation reconciliation follows the Reviewer `REQUEST_CHANGES` finding and updates only `.agentic/runs/FOP-AGENTIC-003.md`
  * the `.agentic/runs/FOP-AGENTIC-003.md` File Coverage row now reflects every recorded writer event that changed the ledger file: Writer Events 1, 2, 3, 4, 5, 6, 7, and 8
  * final Testing remains `PENDING`
  * final Reviewer remains `PENDING`
  * final human approval remains `PENDING`
  * external installation remains `PENDING`
  * runtime manifest regeneration remains `PENDING`

### Writer Event 9

* agentId: `documentation`
* role contract: `.agentic/roles/documentation.md`
* stage: `final-human-approval-recording`
* session key or run ID: `fop-agentic-003-documentation-human-approval-20260727-145118`
* files changed: `.agentic/runs/FOP-AGENTIC-003.md`
* Final Testing: `PASS`
* Final Reviewer: `APPROVE`
* Reviewer REQUIRED_CHANGES: `NONE`
* Security: `PASS`
* deterministic test count: `37`
* ordering evidence:
  * Writer Events 1 through 8 remain preserved unchanged
  * this documentation writer event follows completed final gates and records the authoritative final human approval for `FOP-AGENTIC-003`
  * deterministic ordering evidence remains `python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner` -> `PASS (37 tests)`
  * final Testing is now `PASS`
  * final Reviewer is now `APPROVE`
  * final human approval is now `APPROVED`
  * external installation remains `PENDING`
  * runtime manifest regeneration remains `PENDING`
  * the governance change is ready for commit, push, PR, CI, and merge

## Human Approval Event

* actor: `human operator`
* scope: `FOP-AGENTIC-003`
* literal approval: `APRUEBO FOP-AGENTIC-003`
* merge authorization: `granted`
* external activation permitted: `only after merge`

## File Coverage

* `.agentic/README.md` -> `Writer Event 1, Writer Event 2, Writer Event 4, Writer Event 7`
* `.agentic/policies/deterministic-runner.md` -> `Writer Event 1, Writer Event 3, Writer Event 5, Writer Event 6`
* `.agentic/policies/governance-change.md` -> `Writer Event 1`
* `.agentic/policies/task-ledger.md` -> `Writer Event 1, Writer Event 3, Writer Event 5, Writer Event 6`
* `.agentic/policies/trust-boundary.md` -> `Writer Event 1, Writer Event 6`
* `.agentic/runtime-contracts.md` -> `Writer Event 1, Writer Event 3, Writer Event 5, Writer Event 6`
* `.agentic/runtime-requirements.json` -> `Writer Event 1, Writer Event 3, Writer Event 6`
* `.agentic/roles/architect.md` -> `Writer Event 1`
* `.agentic/roles/security.md` -> `Writer Event 1`
* `.agentic/roles/testing-engineer.md` -> `Writer Event 1`
* `.agentic/roles/frontend-engineer.md` -> `Writer Event 1`
* `.agentic/roles/qa-api.md` -> `Writer Event 1`
* `.agentic/runs/FOP-AGENTIC-003.md` -> `Writer Event 1, Writer Event 2, Writer Event 3, Writer Event 4, Writer Event 5, Writer Event 6, Writer Event 7, Writer Event 8, Writer Event 9`
* `.agentic/schemas/deterministic-task-spec.schema.json` -> `Writer Event 1, Writer Event 3`
* `.agentic/templates/deterministic-task-spec.example.json` -> `Writer Event 1`
* `.agentic/templates/task-ledger.md` -> `Writer Event 1`
* `.agentic/workflows/feature-delivery.md` -> `Writer Event 1`
* `.agentic/workflows/governance-change.md` -> `Writer Event 1`
* `.agentic/test-fixtures/deterministic-runner/prompts/infra.md` -> `Writer Event 1`
* `.agentic/test-fixtures/deterministic-runner/prompts/testing-engineer.md` -> `Writer Event 1`
* `.agentic/test-fixtures/deterministic-runner/prompts/documentation.md` -> `Writer Event 1`
* `.agentic/test-fixtures/deterministic-runner/prompts/reviewer.md` -> `Writer Event 1`
* `README.md` -> `Writer Event 2, Writer Event 4, Writer Event 7`
* `docs/agentic/deterministic-specialist-runner.md` -> `Writer Event 2, Writer Event 4, Writer Event 7`
* `docs/ai-engineering-log/0015-deterministic-specialist-runner.md` -> `Writer Event 2, Writer Event 4, Writer Event 7`
* `scripts/agentic/fop-deterministic-runner` -> `Writer Event 1`
* `scripts/agentic/fop_deterministic_runner.py` -> `Writer Event 1, Writer Event 3, Writer Event 5, Writer Event 6`
* `scripts/agentic/tests/__init__.py` -> `Writer Event 1`
* `scripts/agentic/tests/fixtures/fake_openclaw.py` -> `Writer Event 1, Writer Event 3`
* `scripts/agentic/tests/fixtures/fake_runtime_verifier.py` -> `Writer Event 1`
* `scripts/agentic/tests/test_fop_deterministic_runner.py` -> `Writer Event 1, Writer Event 3, Writer Event 5, Writer Event 6`
* complete changed-file coverage: `PASS`

## Quality Gates

| Check | Result | Evidence |
| --- | --- | --- |
| Deterministic runner implementation | `PASS` | `Python runner, shell entrypoint, schema/example, fixtures, and deterministic tests added in scope` |
| Architect advisory evidence | `PASS` | `recommended external runner architecture captured before implementation` |
| Security advisory evidence | `PASS` | `Security returned advisory verdict PASS with REQUIRED_CHANGES NONE after the symlink-write confinement remediation` |
| Independent Testing | `PASS` | `deterministic runner fake-agent test harness covers hostile input, path safety, mutation remediation, resume integrity, runtime verification, reviewer independence, forbidden Git assertions, and now passes 37 deterministic tests` |
| Implementation Security | `PASS` | `final implementation reflects the remediated fail-closed confinement behavior and Security returned PASS with REQUIRED_CHANGES NONE` |
| Documentation reconciliation | `PASS` | `final documentation reconciliation records the implemented confinement behavior, symlink-safe restoration rules, and deterministic test coverage` |
| Final Testing | `PASS` | `deterministic ordering evidence remains python3 -m unittest scripts.agentic.tests.test_fop_deterministic_runner -> PASS (37 tests)` |
| Final Reviewer | `APPROVE` | `final reviewer outcome recorded as APPROVE with REQUIRED_CHANGES NONE` |
| External installation | `PENDING` | `authoritative ~/.openclaw installation remains out of scope for this task` |
| Runtime manifest regeneration | `PENDING` | `external runtime-contract manifest must be regenerated only after approval and installation` |
| Human approval | `APPROVED` | `human operator recorded literal approval APRUEBO FOP-AGENTIC-003 with merge authorization granted and external activation permitted only after merge` |

## Notes

* repository source is auditable but not executable authority
* the externally installed runner and runtime config remain authoritative
* normal tasks cannot use the deterministic runner until installation and
  manifest regeneration and installed-runtime verification complete
* `FOP-AGENTIC-003` remains a governance task through final approval and later
  external activation steps
* Security has now returned `PASS` with `REQUIRED_CHANGES: NONE`
* final Testing is `PASS`, final Reviewer is `APPROVE`, and human approval is
  `APPROVED`
* specialist children now use real fail-closed Landlock write confinement,
  limited to the validated worktree plus per-attempt artifacts
* snapshot, backup, validation, and restoration do not follow symlinks, and
  directory-symlink write-through attempts leave external targets unchanged
* accepted residual risks remain manual stale-lock cleanup and explicit human
  cleanup for abandoned or partially created task-run directories as
  fail-closed operator procedures
* this governance change is ready for commit, push, PR, CI, and merge
* normal tasks must not use the runner before post-merge installation,
  manifest regeneration, and installed verification

READY FOR COMMIT AND MERGE: YES
HUMAN APPROVAL RECORDED: YES
