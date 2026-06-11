# Refactor Safety Review Skill

## Purpose

Use this skill before performing a refactor.

The goal is to improve code structure without changing behavior unintentionally.

A refactor should make the system easier to understand, test or evolve. It should not introduce hidden behavior changes, broken contracts or unnecessary abstractions.

## When to Use

Use this skill when:

* Code needs to be reorganized.
* A service has too many responsibilities.
* Conditional logic should become a strategy or policy.
* Domain logic is mixed with infrastructure concerns.
* A method or class is becoming hard to test.
* Codex is going to perform a refactor.
* A refactor may affect public APIs or persistence.

## Inputs

Provide:

* Current code or file references.
* Reason for the refactor.
* Expected behavior that must remain unchanged.
* Existing tests.
* Known edge cases.
* Affected modules.
* Constraints.
* Target design if known.

## Refactor Safety Checklist

### 1. Refactor Goal

* What problem is the refactor solving?
* Is the refactor necessary now?
* Is the expected improvement clear?
* Could a smaller change solve the problem?

### 2. Behavior Preservation

* What behavior must remain unchanged?
* Are current edge cases known?
* Are API responses expected to remain the same?
* Are database changes involved?
* Are events or side effects involved?

### 3. Characterization Tests

Before refactoring risky code, identify tests that capture current behavior.

Recommended tests:

* Happy path.
* Invalid input.
* Boundary conditions.
* Failure scenarios.
* State transition rules.
* Persistence behavior.
* External dependency failures if applicable.

### 4. Architecture Boundaries

* Does the refactor improve module boundaries?
* Does it move logic to the correct owner?
* Does it remove coupling?
* Does it introduce unnecessary indirection?

### 5. Pattern Fit

If introducing a pattern, verify:

* What problem does the pattern solve?
* Is the pattern justified by current complexity?
* Is the pattern understandable for future maintainers?
* Could it be overengineering?

### 6. Rollback Strategy

* Can the change be reverted easily?
* Is the pull request small enough?
* Are risky changes isolated?
* Can behavior be compared before and after?

## Expected Output

Return the review using this structure:

### Refactor Summary

Explain what is being refactored and why.

### Current Behavior to Preserve

List the behavior that must not change.

### Recommended Refactor Approach

Explain the safest implementation path.

### Characterization Tests

List tests that should exist before the refactor.

### Risks

List possible risks and how to mitigate them.

### Architecture Impact

Explain whether the refactor improves boundaries, testability or maintainability.

### Pattern Recommendation

If a design pattern is proposed, explain whether it is justified.

### Codex Task

Provide a safe Codex prompt for the refactor.

### Human Review Checklist

List what the developer must verify manually.

## Safe Codex Refactor Prompt Template

```text
Refactor the selected code while preserving existing behavior.

Goal:
[Explain refactor goal.]

Behavior that must remain unchanged:
- [Behavior 1]
- [Behavior 2]
- [Behavior 3]

Constraints:
- Do not change public API contracts unless explicitly requested.
- Do not change database schema unless explicitly requested.
- Do not introduce new frameworks.
- Keep the change small and reviewable.
- Preserve existing tests.
- Add characterization tests before modifying risky behavior.
- Respect module boundaries.

After refactoring:
- Summarize what changed.
- Explain why behavior is preserved.
- List tests added or updated.
- Mention any assumptions.
- Mention any files that need manual review.
```

## Rules

* Do not refactor without a clear reason.
* Do not introduce patterns just to make code look advanced.
* Do not combine large refactors with new features.
* Prefer small, behavior-preserving changes.
* Add tests before changing risky code.
* Treat AI-generated refactors as suggestions that require manual review.
