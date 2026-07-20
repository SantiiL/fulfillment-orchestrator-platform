# Runtime Contracts

The executable contracts used by OpenClaw are maintained outside repository
task worktrees under `~/.openclaw/workspace-*`.

The repository role files are auditable descriptions. They do not replace or
override the runtime contracts.

Before a normal task starts, the deterministic runner must execute:

```bash
bash ~/.openclaw/scripts/verify-fop-runtime-contracts.sh
```

A failed verification blocks task execution.

The initial runtime manifest is created as part of `FOP-AGENTIC-001`.

Governance changes that intentionally modify runtime contracts must regenerate
the manifest after human approval.
