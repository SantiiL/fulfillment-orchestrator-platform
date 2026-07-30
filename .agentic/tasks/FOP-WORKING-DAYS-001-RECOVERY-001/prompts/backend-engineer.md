TASK_ID: FOP-WORKING-DAYS-001-RECOVERY-001
PARENT_TASK_ID: FOP-WORKING-DAYS-001
ROLE: backend-engineer
WORKTREE: /home/santi/agentic-worktrees/fulfillment-orchestrator-platform/fop-working-days-001
CANONICAL_LEDGER: .agentic/runs/FOP-WORKING-DAYS-001.md

You are performing the explicitly human-approved deterministic recovery of
FOP-WORKING-DAYS-001. The original backend stage exhausted both attempts. Attempt 2
implemented the feature successfully but the runner rejected it for path
violations and restored every unauthorized path. Reuse the retained authorized
diff; do not restart the feature from scratch.

Current retained state:
- Fulfillment domain, API, persistence, migration and fulfillment tests remain.
- `.gradle/**` was restored and must not be recreated.
- Changes under `orders/**` were restored and must not be recreated.
- New application services and their tests under the top-level `workingdays`
  package were restored.
- The retained controller and tests may therefore reference missing types.

Recovery goal:
Make the retained Fulfillment Node Weekly Working Days implementation coherent,
compilable and fully tested while staying entirely inside the allowed paths.

Required implementation:
1. Inspect the retained diff before editing.
2. Recreate or relocate the explicit get/replace working-days application
   contracts and services under the existing Fulfillment module:
   `src/main/java/com/santilugani/fulfillmentorchestrator/fulfillment/application/**`.
3. Recreate their tests under:
   `src/test/java/com/santilugani/fulfillmentorchestrator/fulfillment/application/**`.
4. Update retained imports and wiring so the controller uses the Fulfillment
   application layer.
5. Keep `FulfillmentNode` as the owner of a non-null, non-empty immutable
   `Set<DayOfWeek>`.
6. Preserve the seven-day compatibility default for new and existing nodes.
7. Preserve the normalized V5 table and JPA `@ElementCollection` mapping.
8. Preserve:
   - GET `/api/v1/fulfillment-nodes/{id}/working-days`
   - PUT `/api/v1/fulfillment-nodes/{id}/working-days`
   - deterministic Monday-to-Sunday response order
   - idempotent full-set replacement
   - the approved structured 400/404 errors.
9. Keep existing create/get/list Fulfillment Node behavior unchanged.
10. Do not enforce working days during allocation.

Strict path rules:
- You may modify only the paths granted by the task spec.
- Do not modify any file under `orders/**`.
- Do not create or modify files under the top-level `workingdays/**` package.
- Do not create `.gradle/**`, `build/**`, logs or generated files in the worktree.
- Do not modify build files or add dependencies.

Testing:
- Use a Gradle user home outside the worktree:
  `GRADLE_USER_HOME="$TMPDIR/gradle-user-home"`.
- Because the shell wrapper may be blocked by confinement, use:
  `GRADLE_USER_HOME="$TMPDIR/gradle-user-home" java -Dorg.gradle.appname=gradlew -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain clean test --no-daemon`
- Run focused tests first when useful, then the complete clean test suite.
- Run `git diff --check`.
- Confirm `git status --porcelain=v1 -uall` contains no `.gradle`,
  `orders/**`, or top-level `workingdays/**` changes.

Canonical ledger recovery:
Rewrite `.agentic/runs/FOP-WORKING-DAYS-001.md` so it reflects the real state.
- Keep task metadata, approved goal, scope and final human approval PENDING.
- Record the original attempt 1 AGENT_EXIT and attempt 2 PATH_VIOLATION as
  recovery history, not successful gates.
- Record one canonical backend recovery Writer Event covering only files that
  are actually changed after this recovery.
- File Coverage must contain only currently changed files.
- Mark backend recovery PASS only after tests pass.
- Leave testing-gate, api-qa-gate, security-gate, documentation, final review,
  CI and human approval PENDING unless this stage produced direct evidence.
- Remove references to restored files under `orders/**` and top-level
  `workingdays/**`.
- Do not invent approval or reviewer evidence.

Do not commit, push, create a PR or merge.

Finish exactly with:
BACKEND_RECOVERY_DONE
