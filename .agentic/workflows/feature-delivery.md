# Feature Delivery Workflow

## Flow

1. Prepare readiness by identifying the base branch and creating or validating the dedicated task branch and worktree.
2. Require the active writer to append or update its own persisted writer event before final Definition of Ready approval or any stage handoff.
3. Validate the persisted task ledger evidence before confirming the task meets final Definition of Ready approval.
4. Implement the approved scope in the task worktree.
5. Run required automated validation and capture evidence.
6. Update documentation and the AI engineering log when required, with each active writer persisting its own writer event before relinquishing the writer role.
7. Confirm the full writer-event history, derive the writer set from canonical `agentId` values, verify every changed repository file is covered by at least one writer event, and run independent review with a different agent.
8. Wait for human approval before merge.

## Sequencing Rules

* only one writer may be active at a time
* the active writer updates its own writer event when the current stage, files changed, or session evidence changes
* the orchestrator validates persisted writer events and reviewer evidence but never writes the ledger
* every changed repository file must be covered by at least one writer event
* shared files may appear in multiple writer events
* documentation starts after implementation and validation evidence exist
* review starts only after implementation and documentation are complete and `selectedReviewerAgentId NOT IN writerSet`
* the orchestrator must stop the workflow if writer evidence is incomplete, any changed file lacks coverage, or reviewer independence cannot be guaranteed
* blocked validation or review stops the workflow until addressed

<!-- FOP_EFFECTIVE_VERSION:START -->
## Effective version

This normal feature-delivery workflow becomes enforceable beginning with
`FOP-AGENTIC-002`.

Before routing a normal task:

1. verify the external runtime-contract manifest;
2. verify that no protected control-plane path is in scope;
3. prepare the isolated branch and worktree;
4. complete Definition of Ready;
5. select only required specialists;
6. enforce one writer at a time;
7. execute quality gates;
8. run an independent read-only Reviewer;
9. require human approval before merge.

Bootstrap and governance changes follow
`.agentic/policies/governance-change.md`.
<!-- FOP_EFFECTIVE_VERSION:END -->
