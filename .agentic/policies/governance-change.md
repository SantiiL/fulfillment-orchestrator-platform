# Governance Change Policy

A task is a governance change when it modifies the agentic control plane,
runtime-boundary rules, reviewer rules, task-ledger rules, deterministic
runner source, schemas, or workflow gates.

Governance changes require:

1. explicit classification as `GOVERNANCE_CHANGE`
2. explicit human preapproval before implementation
3. an isolated branch and worktree
4. deterministic testing of runner behavior and repository-safety controls
5. no claim that repository-authored rules independently approved themselves
6. final human review of the full diff
7. post-approval external installation when executable runtime files changed
8. post-approval regeneration of the external runtime-contract manifest
9. successful manifest verification before any normal task uses the new runtime

Automated Architect, Security, Testing, Documentation, and Reviewer inputs may
be advisory.

Human approval remains the acceptance authority for the governance change.

## FOP-AGENTIC-003

`FOP-AGENTIC-003` remains a governance task for the full repository
implementation phase.

It does not activate the deterministic runner for normal tasks by itself.

Normal tasks must continue to wait for the later external installation and
manifest-verification steps to complete.
