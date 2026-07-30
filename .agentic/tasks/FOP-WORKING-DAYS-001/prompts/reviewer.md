TASK_ID: FOP-WORKING-DAYS-001
ROLE: reviewer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Operate strictly read-only.

First confirm reviewer independence:
`reviewer NOT IN {backend-engineer, documentation}`.

Review:
- task goal and acceptance criteria;
- modular monolith boundaries;
- framework-free domain;
- FulfillmentNode ownership of weekly working days;
- default all-seven-day compatibility decision;
- normalized migration and backfill;
- GET/PUT API clarity and backward compatibility;
- unit, integration and regression tests;
- API QA and Security evidence;
- documentation and AI engineering log;
- Writer Events and File Coverage;
- absence of allocation enforcement, holiday logic or unrelated changes;
- readiness for explicit human approval.

Do not modify files.
Do not commit, push, create a PR or merge.

When no required findings remain, include:
REQUIRED_CHANGES: NONE

Finish exactly with:
FINAL_REVIEW_APPROVE
