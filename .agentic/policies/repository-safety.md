# Repository Safety Policy

## Branch Protection Expectations

* never edit `main` directly
* never edit `develop` directly
* never run task implementation from the primary checkout
* always use a dedicated branch and worktree per task

## Change Scope Expectations

* preserve the modular monolith architecture
* keep the domain layer framework-free
* avoid unrelated edits
* do not change production behavior unless the task explicitly requires it
* do not add speculative agents or workflow stages that are not active yet

## Validation Expectations

* run required automated tests before handoff
* capture validation evidence without printing secrets
* stop and report blockers instead of forcing unsafe cleanup or merge operations

## Git Safety Expectations

* do not use destructive cleanup by default
* do not delete branches automatically during worktree cleanup
* do not remove the primary checkout or protected branches through helper scripts
* do not merge pull requests without human approval

<!-- FOP_PROTECTED_CONTROL_PLANE:START -->
## Protected agentic control plane

Normal delivery tasks may not modify protected control-plane files listed in
`.agentic/policies/trust-boundary.md`.

A detected modification blocks the normal workflow and requires
reclassification as `GOVERNANCE_CHANGE`.

The executable runtime contracts and their manifest remain outside the task
worktree.
<!-- FOP_PROTECTED_CONTROL_PLANE:END -->
