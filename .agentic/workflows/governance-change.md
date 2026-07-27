# Governance Change Workflow

Use this workflow when a task changes the agentic control plane, runtime
boundary, specialist contracts, workflow gates, schemas, or deterministic
runner source.

## Sequence

1. classify the task as `GOVERNANCE_CHANGE`
2. obtain explicit human preapproval before implementation
3. implement the repository-local governance change in an isolated task
   worktree
4. run deterministic validation for the runner, path policy, and repository
   safety controls
5. collect advisory Architecture, Security, Testing, Documentation, and
   Reviewer evidence as applicable
6. obtain final human approval for the governance diff
7. update the externally installed runner and specialist contracts outside the
   repository
8. regenerate the external runtime-contract manifest
9. allow normal tasks to use the runner only after manifest verification passes

## FOP-AGENTIC-003 Status

`FOP-AGENTIC-003` is itself a governance change.

Its repository source is auditable but not yet the executable authority.

Normal tasks must continue to treat the externally installed runner and its
runtime config as authoritative, and they may not route through the
deterministic runner until the post-approval installation and manifest
verification steps complete.
