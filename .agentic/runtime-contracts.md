# Runtime Contracts

Repository files under `.agentic/**` are auditable source only.

They do not replace the authoritative external runtime.

The authoritative deterministic runner stack lives outside repository task
worktrees and includes:

* `~/.openclaw/scripts/fop-deterministic-runner`
* `~/.openclaw/config/fulfillment-orchestrator-platform/deterministic-runner.json`
* `~/.openclaw/scripts/verify-fop-runtime-contracts.sh`
* `~/.openclaw/trust/fulfillment-orchestrator-platform/runtime-contracts.sha256`
* the external specialist workspaces under `~/.openclaw/workspace-*`

Before `prepare` and before every stage attempt, the active external runner
must verify the installed runtime contracts with:

```bash
bash ~/.openclaw/scripts/verify-fop-runtime-contracts.sh
```

The active external runner must also:

* derive the run directory from the authoritative run root plus validated
  `taskId`, then acquire the per-task lock before loading state or selecting a
  stage
* keep that lock through state validation, drift checks, stage execution, and
  final state persistence
* create new task run directories atomically and reject pre-existing runs
  fail closed without overwriting frozen state
* install host-enforced specialist write confinement before every stage launch
  so writes resolve only inside the validated task worktree plus per-attempt
  runtime artifact directories
* refuse to launch a specialist when that write-confinement primitive is not
  available on the host

## Governance Activation

`FOP-AGENTIC-003` contributes repository-local source for the deterministic
runner and remains a governance task.

The runner is not active for normal tasks until:

1. final human approval of `FOP-AGENTIC-003`
2. external installation of the reviewed runner and any specialist-contract
   updates
3. regeneration of the external runtime-contract manifest
4. successful manifest verification from the installed verifier

Governance changes that intentionally modify the external executable runtime
must regenerate the manifest after human approval, never during the task
implementation itself.
