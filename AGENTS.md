# Fulfillment Orchestrator Platform Agent Guide

## Project Context

The Fulfillment Orchestrator Platform is a Java 21 and Spring Boot application built as a modular monolith.

Current architecture expectations:

* business capabilities remain modular and explicit
* the domain layer must remain framework-free
* application code coordinates use cases without collapsing module boundaries
* agents must preserve existing behavior and architecture

## Branching And Worktrees

* `develop` is the integration branch
* every task must use a dedicated branch and a dedicated Git worktree
* `main` and `develop` must never be edited directly
* the primary checkout must remain untouched while a task worktree is active
* agents must work only inside the approved task worktree for the active task
* readiness is not approved until branch and worktree preparation are recorded in the task ledger
* only one repository writer may be active at a time

## Quality And Validation

* automated tests are expected for every change
* manual cURL validation is expected for API-facing behavior
* changes must remain small, reviewable, and aligned with existing module boundaries
* no agent may merge a pull request
* human approval is mandatory before merge

## Definition Of Done Expectations

Definition of Done includes:

* automated validation evidence
* manual validation evidence when behavior is user-visible or API-facing
* documentation updates when repository guidance or behavior changes
* an AI engineering log entry that records AI-assisted implementation or review work
* a persisted task ledger entry under `.agentic/runs/` with writer, reviewer, and human-approval evidence

See `.agentic/` for task roles, policies, and workflow details.

<!-- FOP_BOOTSTRAP_CLASSIFICATION:START -->
## Agentic governance version

The repository-local workflow was bootstrapped by `FOP-AGENTIC-001`.

That task is human-approved bootstrap governance and does not claim that the
controls it created independently approved themselves.

Normal agentic enforcement begins with `FOP-AGENTIC-002`.
<!-- FOP_BOOTSTRAP_CLASSIFICATION:END -->
