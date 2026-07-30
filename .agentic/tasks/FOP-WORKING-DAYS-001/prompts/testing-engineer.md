TASK_ID: FOP-WORKING-DAYS-001
ROLE: testing-engineer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Operate strictly read-only.

Validate:
- every acceptance criterion;
- FulfillmentNode domain defaults and invariants;
- collection immutability;
- normalized persistence and seven-day backfill;
- GET and PUT behavior;
- deterministic Monday-to-Sunday response ordering;
- missing, null, empty and invalid payload handling;
- invalid UUID and missing node handling;
- idempotent replacement;
- create/get/list regressions;
- Orders and capacity-aware allocation regressions;
- Flyway migration behavior against PostgreSQL;
- `./gradlew clean test --no-daemon`;
- Writer Event and File Coverage completeness;
- absence of unrelated changes.

Do not modify files.
Do not commit, push, create a PR or merge.

Finish exactly with:
TESTING_VERDICT_PASS
