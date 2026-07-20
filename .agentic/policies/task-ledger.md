# Task Ledger Policy

Every task must persist a ledger entry in `.agentic/runs/<task-id>.md`.

Minimum required fields:

* task id
* base branch
* task branch
* worktree path
* current stage
* writer events
* derived writer set
* selected reviewer agentId
* selected reviewer role-contract path
* reviewer independence expression
* reviewer independence result
* reviewer verdict
* human approval

Ledger rules:

* readiness preparation must be recorded before final Definition of Ready approval
* the active writer must append or update its own writer event before relinquishing the writer role or requesting stage advancement
* every writer event must record the writer `agentId`, role-contract path, stage, session key or run ID, files changed, and timestamp or equivalent ordering evidence
* the orchestrator validates persisted writer events but never authors the ledger or writer events
* the writer set must be derived from the persisted writer events and must include every repository writer that changed files for the task
* every changed repository file must be covered by at least one writer event
* shared files may appear in multiple writer events
* only one writer may be active at a time, even though the writer set may grow across handoffs
* missing writer-event evidence or uncovered changed files block progression
* reviewer selection must record the reviewer `agentId`, reviewer role-contract path, reviewer independence expression, and reviewer independence result
* independent review may run only when `selectedReviewerAgentId NOT IN writerSet`
* the orchestrator must refuse independent review when reviewer independence cannot be proven from the ledger evidence
* reviewer verdicts are limited to `APPROVE`, `REQUEST_CHANGES`, or `BLOCKED`
* reviewer entries are read-only review evidence and must not be used to justify reviewer-authored fixes
* human approval remains mandatory before merge

<!-- FOP_BOOTSTRAP_AND_GOVERNANCE:START -->
## Bootstrap and governance tasks

The normal writer-set and reviewer-independence model applies from
`FOP-AGENTIC-002`.

A task classified as `BOOTSTRAP_GOVERNANCE` or `GOVERNANCE_CHANGE` must
record:

- the classification;
- the human pre-approval when applicable;
- deterministic validation evidence;
- advisory specialist findings;
- human acceptance;
- runtime-manifest status.

It must not represent an automated Reviewer verdict as an independent approval
of the same control-plane rules being created or changed.
<!-- FOP_BOOTSTRAP_AND_GOVERNANCE:END -->
