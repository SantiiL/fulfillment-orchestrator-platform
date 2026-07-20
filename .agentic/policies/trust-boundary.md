# Agentic Runtime Trust Boundary

## Runtime authority

Repository files under `.agentic/**` describe and audit the workflow, but they
are not the executable authority for agent behavior.

The authoritative runtime contracts are stored outside task worktrees:

- `~/.openclaw/workspace-orchestrator/AGENTS.md`
- `~/.openclaw/workspace-orchestrator/TOOLS.md`
- `~/.openclaw/workspace-reviewer/AGENTS.md`
- `~/.openclaw/workspace-reviewer/TOOLS.md`
- the remaining specialist workspaces under `~/.openclaw/workspace-*`
- OpenClaw runtime configuration
- deterministic runners under `~/.openclaw/scripts`

Their hashes are recorded outside the repository in:

`~/.openclaw/trust/fulfillment-orchestrator-platform/runtime-contracts.sha256`

A normal task must fail closed when runtime-contract verification fails.

## Bootstrap boundary

`FOP-AGENTIC-001` is the bootstrap task that creates the repository-local
workflow.

It cannot prove independence using the same controls that it is creating.

Therefore:

- its automated Architecture, Security and Reviewer results are advisory;
- it does not claim an independent automated approval;
- its acceptance authority is the human operator;
- the resulting governance becomes enforceable beginning with
  `FOP-AGENTIC-002`.

## Protected control-plane paths

Normal feature tasks must not modify:

- `.agentic/roles/orchestrator.md`
- `.agentic/roles/reviewer.md`
- `.agentic/policies/trust-boundary.md`
- `.agentic/policies/task-ledger.md`
- `.agentic/policies/governance-change.md`
- `.agentic/templates/task-ledger.md`
- `.agentic/workflows/feature-delivery.md`

Changes to those files require a governance-change task with explicit human
pre-approval and post-approval.
