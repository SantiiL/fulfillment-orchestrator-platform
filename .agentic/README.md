# Agentic Workflow Foundation

This directory defines the minimum repository-local rules required to run engineering tasks through the OpenClaw multi-agent workflow.

Recognized repository agents:

* `orchestrator`
* `backend-engineer`
* `documentation`
* `infra`
* `reviewer`

Current default flow:

```text
readiness preparation -> task ledger update -> definition of ready approval -> implementation -> validation -> documentation -> writer-set check -> independent review -> human approval
```

Human approval remains mandatory. These files describe how work should be prepared, executed, validated, and reviewed without changing the repository's existing architecture expectations.

Key conventions:

* `.agentic/templates/task-ledger.md` defines the persisted task ledger format
* `.agentic/runs/<task-id>.md` stores the live ledger for each task
* `.agentic/policies/task-ledger.md` defines the minimum evidence that must be recorded at each stage
* every `.agentic/roles/<agentId>.md` filename defines that role contract's canonical OpenClaw agent identity
* every repository agent identity is the exact case-sensitive OpenClaw `agentId`
* role labels and `agentId` values are distinct fields and cannot be interchanged informally
* the writer set contains canonical `agentId` values, never human-readable role names
* writer and reviewer entries must reference the matching role-contract path

<!-- FOP_BOOTSTRAP_TRUST_BOUNDARY:START -->
## Bootstrap and runtime trust boundary

`FOP-AGENTIC-001` is classified as `BOOTSTRAP_GOVERNANCE`.

It creates the repository-local workflow and is accepted by explicit human
approval rather than claiming that newly created controls independently
approved themselves.

The workflow becomes normative for regular delivery tasks beginning with
`FOP-AGENTIC-002`.

Repository role files are descriptive and auditable.

The executable OpenClaw contracts live outside task worktrees and are
hash-pinned as described in:

- `.agentic/policies/trust-boundary.md`
- `.agentic/policies/governance-change.md`
- `.agentic/runtime-contracts.md`
<!-- FOP_BOOTSTRAP_TRUST_BOUNDARY:END -->
