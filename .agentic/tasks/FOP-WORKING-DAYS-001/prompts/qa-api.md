TASK_ID: FOP-WORKING-DAYS-001
ROLE: qa-api
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Operate strictly read-only.

Validate the HTTP contract:

1. GET `/api/v1/fulfillment-nodes/{id}/working-days`
   - 200 with node id and deterministic workingDays.
2. PUT `/api/v1/fulfillment-nodes/{id}/working-days`
   - JSON body with complete `workingDays` set;
   - 200 response;
   - replacement semantics;
   - repeated identical request remains idempotent.
3. Structured errors:
   - invalid node UUID -> 400 `INVALID_FULFILLMENT_NODE_ID`;
   - missing node -> 404 `FULFILLMENT_NODE_NOT_FOUND`;
   - missing/null/empty/unknown day -> 400 `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`.
4. Existing create/get/list node contracts remain backward compatible.
5. Provide and evaluate a Postman-compatible cURL plan covering:
   - create node and observe all-seven-day default;
   - GET default schedule;
   - PUT Monday-Friday;
   - GET updated schedule;
   - idempotent repeat;
   - invalid UUID;
   - missing node;
   - missing, empty and invalid day payloads;
   - PostgreSQL verification.

Do not modify files.
Do not commit, push, create a PR or merge.

Finish exactly with:
QA_API_VERDICT_PASS
