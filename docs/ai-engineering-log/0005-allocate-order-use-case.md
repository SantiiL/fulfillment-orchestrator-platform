# AI Engineering Log 0005: Allocate Order Use Case

## Context

This feature introduced the allocation lifecycle transition for existing orders.

The system can now allocate an order through:

```text
POST /api/v1/orders/{id}/allocate
```

This builds on top of the existing order creation, order retrieval and order cancellation vertical slices.

At this stage, allocation is intentionally modeled as a lifecycle status transition only.

The feature does not yet assign a real fulfillment node, validate capacity, check working days or reserve stock. Those responsibilities are expected to be introduced in future fulfillment-related slices.

## AI-Assisted Work

Codex was used to generate the initial implementation for:

* Allocate order use case.
* Command object.
* Application service.
* REST endpoint.
* Unit tests.
* Integration tests.
* Reuse of the existing structured API error handling.
* Reuse of the existing repository update path.

## Ponytail Review

Ponytail was used as an anti-overengineering review before committing the feature.

The review confirmed that the implementation was mostly lean and that the allocation endpoint correctly reused existing response models, error handling and lifecycle behavior.

It also identified that `AllocateOrderCommand` is currently a one-field wrapper. This was intentionally kept for consistency with the existing command/use case pattern across the Orders module.

No broader cleanup was applied in this PR to avoid mixing feature implementation with unrelated refactoring.

## Human-Owned Decisions

The following decisions remained human-owned:

* Keeping domain logic free from Spring and JPA.
* Using `order.allocate()` instead of setting status directly.
* Preserving the domain lifecycle policy as the source of truth for valid transitions.
* Mapping invalid lifecycle transitions to HTTP `409 Conflict`.
* Keeping allocation as a focused lifecycle transition.
* Deferring fulfillment node assignment, capacity rules, working days and stock validation.
* Reusing the existing orders table without adding a new Flyway migration.
* Reusing the existing structured API error response format.

## Review Focus

The generated implementation was reviewed for:

* Architecture boundaries.
* Domain lifecycle enforcement.
* Transactional application behavior.
* Persistence update behavior.
* `updated_at` changes on update.
* API error responses.
* Unit and integration test coverage.
* Manual API behavior.

## Validation

The following validations were performed:

* `./gradlew clean test`
* Manual `POST /api/v1/orders`
* Manual `POST /api/v1/orders/{id}/allocate`
* Manual `GET /api/v1/orders/{id}` after allocation
* Manual invalid UUID scenario
* Manual missing order scenario
* Manual invalid transition scenario
* Structured error response validation
* PostgreSQL row verification

## Outcome

The feature introduced the allocation transition in the Orders module.

The allocation flow follows the intended modular monolith architecture:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

The application service loads the aggregate, invokes domain behavior through `order.allocate()`, and persists the updated state.

The order lifecycle now supports the following API-driven transitions:

```text
CREATED -> ALLOCATED
CREATED -> CANCELLED
ALLOCATED -> CANCELLED
```

All tests passed successfully.
