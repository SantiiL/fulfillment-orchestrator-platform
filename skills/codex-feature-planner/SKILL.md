# Codex Feature Planner Skill

## Purpose

Use this skill to transform a feature request into a safe, clear and reviewable implementation task for Codex.

The goal is to avoid vague prompts and uncontrolled code generation.

Codex should receive enough context to implement a small, well-scoped change while respecting the project architecture, tests and documentation standards.

## When to Use

Use this skill when:

* A feature is ready to be implemented.
* An issue needs to be converted into a Codex-ready task.
* A refactor or bug fix requires clear constraints.
* Tests and acceptance criteria need to be included in the implementation prompt.
* A change should remain small and reviewable.

## Inputs

Provide:

* Issue description.
* Business context.
* Acceptance criteria.
* Affected modules.
* Architecture constraints.
* Testing expectations.
* Files or packages to inspect.
* Out-of-scope items.
* Any relevant ADRs.

## Planning Checklist

Before generating a Codex task, verify:

### 1. Scope

* Is the feature small enough for one pull request?
* Are the boundaries clear?
* Is anything explicitly out of scope?
* Can the change be reviewed safely?

### 2. Architecture Constraints

* Which module owns the change?
* Which modules may be called?
* Which dependencies should be avoided?
* Does the change respect the modular monolith approach?

### 3. Implementation Boundaries

* What should Codex modify?
* What should Codex not modify?
* Are public APIs allowed to change?
* Are database migrations needed?
* Are documentation updates needed?

### 4. Testing Requirements

* What unit tests are required?
* What integration tests are required?
* Are Testcontainers needed?
* What edge cases must be covered?
* What failure scenarios should be tested?

### 5. Review Risks

* What could Codex overcomplicate?
* What architecture boundaries could be violated?
* What hidden coupling could be introduced?
* What security or consistency issues should be checked?

## Expected Output

Generate a Codex-ready task using this structure:

### Task Title

Short implementation title.

### Context

Explain the feature and why it is needed.

### Goal

Describe the expected outcome.

### Files and Areas to Inspect

List relevant files, packages or modules.

### Implementation Requirements

Provide specific implementation instructions.

### Architecture Constraints

Define architectural rules Codex must follow.

### Out of Scope

List what must not be changed.

### Testing Requirements

List required tests.

### Documentation Requirements

List documentation updates if needed.

### Review Checklist

List what the human reviewer must verify after Codex completes the task.

## Codex Prompt Template

Use this template when preparing the final prompt:

```text
Implement the feature described below.

Context:
[Explain business and technical context.]

Goal:
[Explain expected outcome.]

Architecture constraints:
- Follow the modular monolith approach.
- Keep business rules inside the owning module.
- Do not introduce cross-module coupling.
- Do not add infrastructure complexity unless explicitly requested.
- Keep the change small and reviewable.

Implementation requirements:
- [Requirement 1]
- [Requirement 2]
- [Requirement 3]

Testing requirements:
- Add or update unit tests.
- Add or update integration tests when persistence or external boundaries are involved.
- Cover relevant edge cases.
- Ensure all tests pass.

Out of scope:
- [Out-of-scope item 1]
- [Out-of-scope item 2]

Documentation:
- Update documentation if the behavior or architecture changes.
- Add an ADR only if the decision affects architecture.

After implementation:
- Summarize the changes.
- List tests added or updated.
- Mention any assumptions made.
- Mention any files that should be manually reviewed.
```

## Rules

* Codex should not decide architecture alone.
* Codex should not introduce large unrelated changes.
* Codex should not silently change public contracts.
* Codex should not bypass tests.
* Codex should not add new frameworks without explicit approval.
* Codex should not hide assumptions.
* Every generated change must be manually reviewed.
