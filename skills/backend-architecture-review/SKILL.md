# Backend Architecture Review Skill

## Purpose

Use this skill to review backend features, design decisions and implementation plans before writing code.

The goal is to think like a senior backend engineer: clarify the business problem, identify architectural risks, compare alternatives and define a clean implementation direction.

## When to Use

Use this skill when:

* A new backend feature is being planned.
* A technical decision may affect architecture.
* Module boundaries are unclear.
* A feature may introduce coupling.
* A new pattern is being considered.
* A design needs to be explained in an interview.
* An ADR may be required.

## Inputs

Provide as much of the following context as possible:

* Feature description.
* Business goal.
* Current architecture.
* Affected modules.
* Acceptance criteria.
* Expected data model.
* API contract if available.
* Known constraints.
* Open questions.

## Review Checklist

Analyze the feature using this checklist:

### 1. Business Understanding

* What problem is this feature solving?
* Is the business rule explicit?
* Are there ambiguous requirements?
* What edge cases should be clarified?

### 2. Module Ownership

* Which module should own this behavior?
* Does this feature cross module boundaries?
* Is any module taking responsibilities that belong elsewhere?
* Is there a risk of creating a god service?

### 3. Dependency Direction

* Are dependencies flowing in a clean direction?
* Can domain logic remain isolated from infrastructure details?
* Are application services coordinating without absorbing business rules?

### 4. Data and Consistency

* What data needs to be persisted?
* What consistency guarantees are required?
* Is eventual consistency acceptable?
* Are transactions needed?
* Are race conditions possible?

### 5. API and Contract Design

* Is the API expressive and stable?
* Are request and response models clear?
* Are errors modeled explicitly?
* Are invalid operations rejected correctly?

### 6. Failure Scenarios

* What happens if a dependency fails?
* What happens if the same request is sent twice?
* What happens if data is partially updated?
* What behavior should be logged or monitored?

### 7. Testing Strategy

* What should be unit tested?
* What should be integration tested?
* Are there important edge cases?
* Are there failure scenarios to simulate?

### 8. Simplicity and Evolution

* Is the solution simpler than the problem?
* Is there unnecessary abstraction?
* Can the design evolve later?
* Is this decision premature?

## Expected Output

Return the review using this structure:

### Architecture Review

Summarize the feature and its architectural impact.

### Recommended Approach

Explain the recommended implementation direction.

### Alternatives Considered

Compare at least two alternatives when relevant.

For each alternative, include:

* Pros
* Cons
* When it would be a better choice

### Risks and Trade-offs

List the main risks, trade-offs and mitigation strategies.

### Module Boundary Recommendation

Explain which module should own each responsibility.

### Testing Strategy

List the recommended tests.

### ADR Recommendation

State whether an ADR is needed.

If yes, propose:

* ADR title
* Context
* Decision
* Alternatives
* Consequences

### Interview Explanation

Provide a short explanation of the decision as if answering a senior backend interview question.

## Rules

* Do not jump directly into implementation.
* Do not recommend microservices unless there is a clear reason.
* Prefer simple, testable designs.
* Explicitly call out overengineering.
* Prioritize correctness, maintainability and clear boundaries.
* Separate business logic from infrastructure concerns.
* Challenge assumptions instead of accepting the first idea.
