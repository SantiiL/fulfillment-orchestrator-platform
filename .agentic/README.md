# Agentic Workflow Foundation

This directory defines the repository-local, auditable source for the
Fulfillment Orchestrator Platform multi-agent workflow.

Recognized repository agents:

* `orchestrator`
* `architect`
* `security`
* `backend-engineer`
* `frontend-engineer`
* `infra`
* `testing-engineer`
* `qa-api`
* `documentation`
* `reviewer`

Current repository flow:

```text
readiness preparation -> frozen task spec -> deterministic stage execution -> validation evidence -> documentation -> reviewer independence check -> independent review -> human approval
```

Key conventions:

* `.agentic/templates/task-ledger.md` defines the persisted task-ledger shape
* `.agentic/runs/<task-id>.md` stores the repository audit ledger for each task
* `.agentic/policies/task-ledger.md` defines the minimum evidence recorded in
  the ledger
* every `.agentic/roles/<agentId>.md` filename defines the canonical
  case-sensitive OpenClaw `agentId`
* repository source is auditable and descriptive, but not executable runtime
  authority

## Runtime Boundary

The authoritative deterministic runner, runtime config, and manifest verifier
live outside repository task worktrees.

Repository source under `.agentic/**` defines:

* schemas
* policies
* workflow descriptions
* descriptive role contracts
* fixtures and examples

The authoritative external runtime defines:

* the installed `~/.openclaw/scripts/fop-deterministic-runner` entrypoint
* the external runtime config under `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
* the manifest verifier and runtime-contract hashes
* the executable specialist workspaces
* real fail-closed Landlock child write confinement for specialist stages when
  the host supports the required kernel primitive

The operator-facing runbook for this boundary lives in
`docs/agentic/deterministic-specialist-runner.md`.

## Activation Status

`FOP-AGENTIC-003` is a `GOVERNANCE_CHANGE`.

It adds the reusable deterministic specialist runner source to the repository,
but it does not make the runner active for normal tasks yet.

Normal tasks must not use the deterministic runner until:

1. the governance change receives final human approval
2. the external installed copy is updated
3. the external runtime-contract manifest is regenerated
4. runtime verification succeeds from the installed verifier
