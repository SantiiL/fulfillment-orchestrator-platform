# ADR 0001: Start with a Modular Monolith

## Status

Accepted

## Context

The project simulates a logistics fulfillment platform with multiple business capabilities, including order management, fulfillment node assignment, working days, incidents, notifications and tracking.

A common approach for distributed systems is to start directly with microservices. However, doing so too early can introduce unnecessary complexity before the domain boundaries are clear.

The project is intended to demonstrate senior backend engineering decisions, not just the use of distributed technologies for their own sake.

## Decision

The system will start as a modular monolith.

Each business capability will be implemented as a separate module with clear boundaries, internal domain logic and controlled dependencies.

Potential modules include:

- orders
- fulfillment
- working days
- incidents
- notifications
- tracking

The initial implementation will run as a single deployable application.

Service extraction may be considered later if the module boundaries become stable and there is a clear technical or organizational reason to split the system.

## Alternatives Considered

### Start directly with microservices

Pros:

- Independent deployment
- Clear service ownership
- Better scalability for isolated components

Cons:

- Higher operational complexity
- More infrastructure required
- Distributed transactions and consistency challenges
- Harder local development
- Premature boundaries may lead to costly refactors

### Build a simple layered monolith

Pros:

- Fast to implement
- Easy to understand
- Low operational overhead

Cons:

- Business boundaries may become blurred
- Risk of tightly coupled services
- Harder to evolve into distributed services later

### Start with a modular monolith

Pros:

- Keeps operational simplicity
- Encourages clear domain boundaries
- Supports future service extraction
- Easier to test and refactor early
- More realistic for early-stage product development

Cons:

- Requires discipline to maintain boundaries
- Modules are not independently deployable at first
- Scaling is initially at application level

## Consequences

The project will prioritize clear package boundaries, explicit module ownership and documentation of dependencies between modules.

Architecture reviews will focus on preventing accidental coupling between modules.

Future ADRs may document when and why a module should be extracted into a separate service.

## Decision Owner

Santiago Lugani