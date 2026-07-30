TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-001
PARENT_TASK_ID: FOP-WORKING-DAYS-001
ROLE: qa-api
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Perform a read-only API QA gate.

Validate the approved contracts:
- GET `/api/v1/fulfillment-nodes/{id}/working-days`
- PUT `/api/v1/fulfillment-nodes/{id}/working-days`
- default seven-day ordering Monday through Sunday
- deterministic replacement ordering
- idempotent replacement
- invalid UUID -> 400 `INVALID_FULFILLMENT_NODE_ID`
- missing node -> 404 `FULFILLMENT_NODE_NOT_FOUND`
- missing/null/empty/invalid days -> 400
  `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`
- existing create/get/list endpoints remain compatible
- persisted normalized rows match the API response.

Use `$TMPDIR` for Gradle cache, process logs and temporary files. Do not modify
the repository, task spec or ledger. Do not change Orders.

Finish exactly with:
QA_API_VERDICT_PASS
