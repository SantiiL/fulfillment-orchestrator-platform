# AI-Assisted Engineering Workflow

This document describes how AI tools are used in this project.

The goal is to use AI as an engineering assistant, not as an uncontrolled code generator.

AI may support the development process, but architecture decisions, domain modeling, trade-offs, security boundaries, final code review and merge decisions remain human-owned.

## Purpose

This project uses AI-assisted workflows to improve productivity while preserving engineering ownership.

AI tools may be used for:

* Exploring architecture alternatives.
* Breaking down features.
* Drafting implementation plans.
* Generating initial code scaffolding.
* Suggesting tests.
* Reviewing diffs.
* Identifying refactor opportunities.
* Drafting documentation.
* Preparing pull request summaries.

## Human-Owned Responsibilities

The developer remains responsible for:

* Understanding the business domain.
* Defining module boundaries.
* Choosing architecture patterns.
* Validating AI-generated suggestions.
* Reviewing all generated code.
* Running and maintaining tests.
* Ensuring security and correctness.
* Deciding what is merged.
* Documenting relevant trade-offs.

## AI Usage Principles

### 1. Architecture First

AI should not generate implementation before the architecture and acceptance criteria are clear.

Before coding, each relevant feature should define:

* Business context.
* Acceptance criteria.
* Expected behavior.
* Edge cases.
* Testing strategy.
* Possible trade-offs.

### 2. Small Reviewable Changes

AI-assisted changes should be kept small and reviewable.

Large generated changes are harder to validate and increase the risk of hidden design or correctness issues.

### 3. Tests Before Risky Refactors

Before performing significant refactors, the existing behavior should be protected with tests.

AI may help identify characterization tests, but the developer must validate that the tests represent the intended behavior.

### 4. Deterministic Logic Before AI Logic

Business-critical rules should be implemented with deterministic logic whenever possible.

AI should not be used as the source of truth for core business decisions such as order lifecycle transitions, fulfillment assignment or consistency rules.

### 5. Document Important Decisions

When AI contributes to an architecture discussion, refactor or feature implementation, the final decision should be documented if it affects the design of the system.

Relevant documentation may include:

* Architecture Decision Records.
* Trade-off documents.
* Pull request notes.
* AI engineering logs.

## Expected Workflow

For each relevant feature:

1. Create an issue with business context and acceptance criteria.
2. Review the issue from an architecture perspective.
3. Define the testing strategy.
4. Use AI/Codex to assist with implementation if useful.
5. Review the generated diff critically.
6. Refactor manually if needed.
7. Run tests.
8. Update documentation.
9. Open a pull request.
10. Merge only after the change is understandable and validated.

## Example AI-Assisted Task

Feature: Implement idempotent order creation.

AI may help with:

* Listing edge cases.
* Suggesting database constraints.
* Drafting the implementation plan.
* Generating initial tests.
* Reviewing the final diff.

Human decisions include:

* Choosing where idempotency belongs in the architecture.
* Deciding whether idempotency keys are stored in PostgreSQL, Redis or both.
* Defining conflict behavior.
* Validating race condition handling.
* Approving the final implementation.

## Interview Note

This workflow demonstrates that AI can be used as a productivity tool while preserving senior engineering judgment.

The goal is not to outsource thinking. The goal is to increase delivery speed while keeping architecture, correctness and maintainability under human control.
