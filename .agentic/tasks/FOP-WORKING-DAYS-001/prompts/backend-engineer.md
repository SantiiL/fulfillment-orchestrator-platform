TASK_ID: FOP-WORKING-DAYS-001
ROLE: backend-engineer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001
LEDGER: .agentic/runs/FOP-WORKING-DAYS-001.md

Implement only the approved Fulfillment Node Weekly Working Days feature.

Business goal:
Each FulfillmentNode must own a non-empty weekly operating schedule without changing allocation behavior yet.

Required domain behavior:
1. Add a non-null, non-empty `Set<DayOfWeek>` to FulfillmentNode.
2. New nodes default to all seven DayOfWeek values because existing behavior is 24/7.
3. Reconstituted nodes include persisted working days.
4. Expose clear domain behavior such as `isWorkingDay(DayOfWeek)` and `replaceWorkingDays(Set<DayOfWeek>)`.
5. Reject null or empty sets.
6. Defensively copy collections and never expose mutable internal state.
7. Keep the domain free from Spring, JPA and HTTP.

Persistence:
1. Add exactly `src/main/resources/db/migration/V5__add_fulfillment_node_working_days.sql`.
2. Create `fulfillment_node_working_days` with:
   - fulfillment_node_id UUID NOT NULL;
   - day_of_week VARCHAR NOT NULL;
   - primary key (fulfillment_node_id, day_of_week);
   - foreign key to fulfillment_nodes(id);
   - no cascade delete unless already required by existing persistence behavior.
3. Backfill all seven DayOfWeek values for every existing fulfillment node.
4. Use JPA `@ElementCollection` in infrastructure if it is the simplest mapping.
5. Do not use CSV, JSON or custom collection serialization.
6. Keep existing fulfillment node timestamps and catalog behavior intact.

API:
1. `GET /api/v1/fulfillment-nodes/{id}/working-days`
2. `PUT /api/v1/fulfillment-nodes/{id}/working-days`
3. PUT body:
   `{"workingDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]}`
4. Both successful responses should contain the node id and working days.
5. Return days in deterministic Monday-to-Sunday order.
6. PUT replaces the full set and is idempotent.
7. Preserve the existing structured error shape.
8. Use existing Fulfillment API error infrastructure where suitable.
9. Required cases:
   - invalid node UUID -> 400 `INVALID_FULFILLMENT_NODE_ID`;
   - missing node -> 404 `FULFILLMENT_NODE_NOT_FOUND`;
   - missing/null/empty/invalid workingDays -> 400 `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`.

Application:
1. Add explicit use cases/services for get and replace working days.
2. Keep orchestration readable.
3. Reuse the existing FulfillmentNodeRepository.
4. Do not create a generic scheduling engine, mapper framework or utility class.

Tests:
1. Domain unit tests for defaults, replacement, null/empty rejection, `isWorkingDay`, immutability and reconstitution.
2. Application tests for get, replace, missing node and idempotent replacement.
3. Integration tests for GET/PUT, deterministic order, invalid UUID, missing node, missing/empty/invalid payload, persistence and backfill compatibility.
4. Regression tests for create/get/list fulfillment nodes.
5. Confirm Orders and capacity-aware allocation tests still pass.
6. Run `./gradlew clean test --no-daemon`.

Scope boundaries:
- Do not modify Orders production code.
- Do not enforce working days during allocation.
- Do not add holidays, dates, hours, cutoffs or time zones per node.
- Do not add dependencies.
- Do not commit, push, create a PR or merge.

Ledger obligations:
1. Record one canonical backend-engineer Writer Event.
2. Update File Coverage for every changed file.
3. Record tests and important trade-offs.
4. Do not mark human approval.

Finish exactly with:
BACKEND_IMPLEMENTATION_DONE
