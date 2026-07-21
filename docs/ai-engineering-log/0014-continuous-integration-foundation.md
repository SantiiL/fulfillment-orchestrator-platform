# AI Engineering Log 0014: Continuous Integration Foundation

## Context

This change documents the repository CI workflow that already exists in `.github/workflows/ci.yml`.

The goal is to make the implemented GitHub Actions behavior explicit in the top-level documentation and to preserve task evidence for the documentation stage without changing the workflow itself.

## Scope

This documentation-only stage updates:

* `README.md`
* `docs/ai-engineering-log/0014-continuous-integration-foundation.md`
* `.agentic/runs/FOP-AGENTIC-002.md`

The stage documents:

* CI triggers for `push`, `pull_request`, and `workflow_dispatch`
* Java 21 execution on GitHub-hosted runners with immutable action pins
* Gradle wrapper validation through `gradle/actions/setup-gradle`, wrapper executability checks, and shell syntax validation
* Testcontainers-backed PostgreSQL integration testing on the GitHub-hosted Docker daemon
* failed-test artifact capture for reports and raw results
* the least-privilege security choices already present in the workflow
* deterministic documentation reconciliation performed by the dedicated documentation specialist after cross-agent `sessions_send` was blocked, while preserving specialist identities and read/write boundaries

The stage does not modify application code, Gradle build logic, tests, or the workflow implementation.

## Implemented Workflow

The repository CI workflow runs on:

* pushes to `main` and `develop`
* pull requests targeting `main` and `develop`
* manual `workflow_dispatch`

The single `build-and-test` job runs on `ubuntu-latest` with Temurin Java 21 and executes:

```text
actions/checkout -> actions/setup-java -> gradle/actions/setup-gradle (validate-wrappers: true) -> shell validation -> ./gradlew clean test --no-daemon
```

Before Gradle test execution, the workflow also verifies that `./gradlew` is executable and runs `bash -n` across `scripts/agentic/*.sh`.

## Selected Actions And Security Choices

The documented workflow uses these actions:

* `actions/checkout@d23441a48e516b6c34aea4fa41551a30e30af803` (`v6`)
* `actions/setup-java@03ad4de0992f5dab5e18fcb136590ce7c4a0ac95` (`v5`)
* `gradle/actions/setup-gradle@3f131e8634966bd73d06cc69884922b02e6faf92` (`v6`)
* `actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a` (`v7`)

The workflow keeps the security posture intentionally narrow:

* top-level permissions remain `contents: read` only
* `actions/checkout` disables credential persistence with `persist-credentials: false`
* every third-party action is pinned to an immutable commit SHA with an adjacent major-version comment
* wrapper validation is integrated into `gradle/actions/setup-gradle` with `validate-wrappers: true`, so no separate wrapper-validation action is used
* no secrets, write permissions, or service containers are introduced
* test artifacts upload only on failure, reducing unnecessary artifact exposure

These choices match the passed security gate evidence already recorded for least privilege, immutable pinning, fork PR safety, artifact safety, and control-plane integrity.

## Why No PostgreSQL Service Container Is Required

The Gradle test suite already uses Testcontainers for integration tests. On GitHub Actions, those containers run against the GitHub-hosted Docker daemon, so PostgreSQL is provisioned by the test process itself rather than by a static workflow service definition.

Keeping database startup inside Testcontainers has two benefits here:

* CI uses the same integration-test mechanism as local automated test runs
* the workflow avoids duplicate PostgreSQL configuration and extra runner-side credentials or lifecycle management

This is why the implemented CI foundation does not require a separate `services.postgres` block in the workflow.

## Failed-Test Artifacts

When the Gradle test step fails, the workflow uploads:

* `build/reports/tests/test`
* `build/test-results/test`

These files are published as the `gradle-test-artifacts` artifact with a 7-day retention period so failures can be diagnosed from rendered HTML reports and raw XML results after the job exits.

## Specialist Execution Notes

The final documentation reconciliation was executed by the dedicated documentation specialist runner directly in the task worktree because cross-agent `sessions_send` routing was blocked in the control plane at this stage.

That fallback did not broaden authority:

* specialist identities stayed explicit in the ledger
* the documentation specialist remained the sole active writer for this stage
* read/write boundaries stayed limited to the task-approved files

## AI-Assisted Work

Codex was used to:

* inspect the implemented workflow and existing ledger evidence
* draft a concise README CI summary that matches the checked-in workflow
* record workflow triggers, validation steps, security choices, and Testcontainers rationale
* append documentation-stage provenance to the task ledger

## Human-Owned Decisions

The following decisions remained human-owned:

* keeping the CI foundation implementation unchanged during the documentation stage
* retaining Java 21 and the existing Gradle test entrypoint
* relying on Testcontainers instead of adding a workflow-level PostgreSQL service container
* preserving least-privilege workflow permissions and failure-only artifact upload
* using the direct deterministic specialist runner once cross-agent `sessions_send` was blocked, while preserving specialist identity and file-scope boundaries
* keeping reviewer verdict and human approval pending until later stages
