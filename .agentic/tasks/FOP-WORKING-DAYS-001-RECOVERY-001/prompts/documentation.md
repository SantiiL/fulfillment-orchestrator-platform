TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-001
PARENT_TASK_ID: FOP-WORKING-DAYS-001
ROLE: documentation
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001
CANONICAL_LEDGER: .agentic/runs/FOP-WORKING-DAYS-001.md

Update only approved documentation and the canonical ledger after all recovery
gates pass.

Document:
- weekly working days owned by FulfillmentNode;
- seven-day compatibility default;
- normalized V5 persistence;
- GET and PUT API contracts with Postman-compatible cURLs;
- deterministic ordering and structured errors;
- allocation enforcement remains deferred to FOP-WORKING-DAYS-002;
- recovery history: original AGENT_EXIT, PATH_VIOLATION, retained authorized
  diff and successful deterministic recovery;
- actual test, API QA and security evidence from completed gates.

Keep final reviewer and final human approval PENDING. Do not claim CI unless
actual CI evidence exists. Do not modify production or test code.

Finish exactly with:
DOCUMENTATION_DONE
