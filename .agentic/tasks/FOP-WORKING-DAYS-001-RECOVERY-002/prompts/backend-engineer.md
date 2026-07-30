TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-002
PARENT_TASK_ID: FOP-WORKING-DAYS-001
PRIOR_RECOVERY_ID: FOP-WORKING-DAYS-001-RECOVERY-001
ROLE: backend-engineer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001
CANONICAL_LEDGER: .agentic/runs/FOP-WORKING-DAYS-001.md

This is a narrowly scoped test-coverage correction after the read-only testing
gate found exactly two gaps. The implementation and full test suite already
passed; do not redesign or restart the feature.

Authoritative findings from testing-gate attempt 1:
1. Application-layer idempotency coverage is missing for
   ReplaceFulfillmentNodeWorkingDaysService.
2. PUT `/api/v1/fulfillment-nodes/{id}/working-days` lacks integration coverage
   for invalid UUID and missing-node responses.

Required changes:
- Add one explicit application-layer idempotency test to
  `src/test/java/com/santilugani/fulfillmentorchestrator/fulfillment/application/ReplaceFulfillmentNodeWorkingDaysServiceTest.java`.
  Execute the same full-set replacement twice and prove the resulting working
  days and persisted aggregate remain unchanged/deterministic.
- Add one PUT invalid-UUID integration test to
  `src/test/java/com/santilugani/fulfillmentorchestrator/fulfillment/api/FulfillmentNodeControllerIntegrationTest.java`.
  Assert HTTP 400 and code `INVALID_FULFILLMENT_NODE_ID`.
- Add one PUT missing-node integration test to the same integration test file.
  Assert HTTP 404 and code `FULFILLMENT_NODE_NOT_FOUND`.
- Preserve the existing test style, helpers and assertion conventions.
- Do not modify production code unless a genuine compilation defect is
  discovered. The expected correction is tests-only plus the canonical ledger.

Strict path rules:
- You may modify only:
  - the two test files named above;
  - `.agentic/runs/FOP-WORKING-DAYS-001.md`.
- Do not modify Orders, production code, migrations, build files, top-level
  `workingdays/**`, `.gradle/**`, `build/**`, task specs or prompts.
- Do not create generated files in the worktree.

Verification:
- Inspect the prior testing-gate log:
  `/home/santi/.openclaw/task-runs/FOP-WORKING-DAYS-001-RECOVERY-001/resume-20260727T193832Z/logs/testing-gate-attempt-1-37295d45aec2.log`
- Run the two focused test classes.
- Run the complete clean test suite.
- Use external Gradle cache, project cache and build output exactly as used by
  the successful prior recovery/testing execution. Keep every generated file
  under `$TMPDIR`.
- Run `git diff --check`.
- Confirm no changed path exists under Orders, top-level workingdays, `.gradle`,
  `build`, build scripts or dependency manifests.

Canonical ledger:
- Preserve the successful backend recovery event.
- Record testing-gate attempt 1 as `REQUEST_CHANGES`, with the two concrete
  coverage gaps.
- Add one correction Writer Event containing only the two test files and ledger.
- Update exact File Coverage.
- Mark `test-coverage-fix` PASS only after focused and full suites pass.
- Keep the new testing gate, API QA, security, documentation, final review,
  CI and human approval PENDING.

Do not commit, push, create a PR or merge.

Finish exactly with:
TEST_COVERAGE_FIX_DONE
