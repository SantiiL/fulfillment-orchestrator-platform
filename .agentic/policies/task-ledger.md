# Task Ledger Policy

Every task must persist a repository audit ledger in
`.agentic/runs/<task-id>.md`.

The ledger is auditable evidence.

It is not the authoritative runtime state.

The authoritative stage records, stage run IDs, writer set, and reviewer
independence outcome live in the externally persisted deterministic-runner
state once that runtime is active.

## Minimum Ledger Fields

* task id
* classification
* base branch
* task branch
* worktree path
* current stage
* writer events
* derived writer set
* selected reviewer agentId
* reviewer independence expression
* reviewer independence result
* reviewer verdict
* human approval

## Writer Evidence Rules

* only one writer may be active at a time
* the active writer appends or updates its own writer event before handoff
* every writer event records the canonical `agentId`, role-contract path,
  stage, session key or run ID, files changed, and ordering evidence
* writer and documentation-writer stages must also emit canonical
  machine-readable ledger evidence with schema version
  `fop-task-ledger-evidence/v1`, exact `stageRunId`, canonical `agentId`,
  stage type, completed status, and exact changed-file coverage
* the authoritative runner captures the ledger digest before and after every
  writer or documentation-writer stage; unchanged, missing, or inconsistent
  evidence is a `LEDGER_MISMATCH`
* every changed repository file must be covered by at least one writer event
* shared files may appear in multiple writer events
* missing writer evidence blocks progression
* remediation writer events must preserve prior writer events and append the
  specific findings resolved, deterministic test identifiers/counts, and
  remaining approval or re-review dependencies, including the confinement
  strategy when security remediation changes specialist isolation semantics

## Reviewer Rules

* independent review may run only when `selectedReviewerAgentId NOT IN writerSet`
* reviewer verdicts are limited to `APPROVE`, `REQUEST_CHANGES`, or `BLOCKED`
* reviewer evidence is read-only audit evidence and must not be used to justify
  reviewer-authored fixes

## Governance Tasks

`BOOTSTRAP_GOVERNANCE` and `GOVERNANCE_CHANGE` tasks must also record:

* deterministic validation evidence
* advisory Architecture and Security evidence when present
* runtime-manifest status
* external installation status
* final human approval status

`FOP-AGENTIC-003` remains a governance task until the external installation
and manifest-regeneration steps finish after human approval.
