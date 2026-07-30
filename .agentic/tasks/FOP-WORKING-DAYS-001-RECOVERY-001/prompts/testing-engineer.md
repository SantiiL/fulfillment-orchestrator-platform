TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-001
PARENT_TASK_ID: FOP-WORKING-DAYS-001
ROLE: testing-engineer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001

Perform a read-only testing gate for the recovered Fulfillment Node Weekly
Working Days feature.

Verify:
1. The retained and recovered diff stays inside Fulfillment, its tests, V5 and
   the canonical ledger.
2. No changed path exists under `orders/**`, top-level `workingdays/**`,
   `.gradle/**`, `build/**`, build files or dependency manifests.
3. Domain invariants cover seven-day defaults, reconstitution, replacement,
   null/empty rejection, `isWorkingDay`, defensive copying and immutability.
4. Application tests cover get, replace, missing node and idempotency.
5. Integration tests cover GET/PUT, deterministic ordering, invalid UUID,
   missing node, missing/null/empty/invalid payload, persistence, backfill and
   create/get/list regression.
6. Orders and capacity-aware allocation tests still pass without modifying
   Orders.

Use only external Gradle cache:
`GRADLE_USER_HOME="$TMPDIR/gradle-user-home"`.
Run the complete suite with GradleWrapperMain and run `git diff --check`.
Do not modify any repository file. Do not update the ledger.

Finish exactly with:
TESTING_VERDICT_PASS
