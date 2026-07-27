# Feature Delivery Workflow

## Flow

1. prepare readiness with the approved task branch, worktree, and ledger
2. validate or freeze the task specification
3. execute ordered writer stages one at a time
4. run ordered read-only gates
5. execute the optional documentation writer stage when required
6. derive the writer set and verify reviewer independence
7. run the final read-only Reviewer
8. stop for final human approval

## Sequencing Rules

* only one writer may be active at a time
* each writer records its own ledger evidence before handoff
* documentation follows implementation and validation evidence
* read-only gates and the final Reviewer must not mutate repository files
* review starts only when `selectedReviewerAgentId NOT IN writerSet`
* blocked validation, path-policy failure, reviewer-independence failure, or
  runtime verification failure stops the workflow

## Activation Rule

The deterministic runner becomes the normal execution mechanism only after:

1. `FOP-AGENTIC-003` is human approved
2. the external runner installation is complete
3. the external runtime-contract manifest is regenerated
4. installed-manifest verification passes

Until those gates are complete, this repository defines the intended workflow
but does not claim that the deterministic runner is active for normal tasks.
