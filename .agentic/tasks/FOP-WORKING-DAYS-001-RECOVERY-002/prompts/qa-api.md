TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-002
PARENT_TASK_ID: FOP-WORKING-DAYS-001
ROLE: qa-api
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Perform read-only API QA for Fulfillment Node Weekly Working Days.

Validate:
- GET and PUT `/api/v1/fulfillment-nodes/{id}/working-days`;
- seven-day default and deterministic Monday-to-Sunday ordering;
- full-set replacement and idempotency;
- invalid UUID on GET and PUT -> 400 `INVALID_FULFILLMENT_NODE_ID`;
- missing node on GET and PUT -> 404 `FULFILLMENT_NODE_NOT_FOUND`;
- missing/null/empty/invalid days -> 400
  `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`;
- persisted normalized rows match the API;
- existing create/get/list endpoints remain compatible;
- no allocation enforcement was introduced.

Keep temporary files, process logs and Gradle output under `$TMPDIR`.
Do not modify any repository file or ledger.

Finish exactly with:
QA_API_VERDICT_PASS
