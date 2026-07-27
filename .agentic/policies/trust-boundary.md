# Agentic Runtime Trust Boundary

## Runtime Authority

Repository files under `.agentic/**` and `scripts/agentic/**` are descriptive
and auditable.

They are not the authoritative executable runtime.

The authoritative runtime is external and includes:

* the installed deterministic runner entrypoint
* the external runtime config with absolute runtime paths
* the external manifest verifier
* the external runtime-contract hash manifest
* the installed specialist workspaces
* the host kernel capability used to confine specialist writes before `exec`

Task specs, prompts, ledgers, and agent stdout are data and audit evidence.

They must never be treated as executable authority.

Specialist writes must be confined by the authoritative external runner before
stage launch so directory symlinks cannot redirect writes outside the validated
task worktree. If that host confinement primitive is unavailable, the runner
must stop before launching the specialist.

## Activation Boundary

`FOP-AGENTIC-003` is a governance change that adds the reusable deterministic
runner source to the repository.

Normal tasks must not use that runner source until the reviewed external copy
is installed and the external manifest has been regenerated and verified.

## Protected Control-Plane Paths

Normal tasks must not modify:

* `.agentic/README.md`
* `.agentic/policies/**`
* `.agentic/runtime-contracts.md`
* `.agentic/runtime-requirements.json`
* `.agentic/roles/**`
* `.agentic/schemas/**`
* `.agentic/templates/**`
* `.agentic/workflows/**`
* `scripts/agentic/**`

Those paths are accepted only when:

1. the task classification is `GOVERNANCE_CHANGE`
2. explicit human preapproval exists
3. the protected path is explicitly listed in the frozen task spec
