# Current Architecture

## Overview

The Fulfillment Orchestrator Platform currently runs as a modular monolith.

That means one deployable application, but code organized around business capabilities instead of a flat controller/service/repository split.

The first implemented business module is `orders`.

## Main Modules

Current or planned module boundaries:

* `orders` - implemented first; owns order lifecycle and order persistence
* `fulfillment` - planned; will own fulfillment node assignment
* `workingdays` - planned; will own business-day and operating-day rules
* `incidents` - planned; will own operational failure representation and handling
* `notifications` - planned; will own outbound communication concerns
* `shared` - small cross-cutting package for truly reusable concepts only

## Current Implemented Flow

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

For the Orders module, the current flow is:

1. A REST endpoint receives a request.
2. The controller converts HTTP input into an application command or query.
3. An application service orchestrates the use case.
4. The domain model applies lifecycle behavior such as `order.allocate()` or `order.deliver()`.
5. The persistence adapter saves or loads the order through JPA.
6. PostgreSQL stores the final state.

## Layer Responsibilities

### API

The `orders/api` package owns:

* controllers
* request and response DTOs
* HTTP status mapping
* structured API error shaping

It does not own business rules.

### Application

The `orders/application` package owns:

* use case interfaces
* commands and queries
* application services
* repository ports
* application-level result objects

It coordinates workflows but does not replace the domain model.

### Domain

The `orders/domain` package owns:

* `Order`
* `OrderId`
* `SellerId`
* `OrderStatus`
* `OrderLifecyclePolicy`
* lifecycle exceptions

This layer contains the lifecycle rules and behavior methods.

### Infrastructure

The `orders/infrastructure/persistence` package owns:

* JPA entities
* Spring Data repository interfaces
* persistence adapter implementations

It translates between the database model and the domain model.

## Why the Domain Layer Is Framework-Free

The domain model stays free from Spring, JPA, Hibernate, and web annotations so that business rules remain:

* easier to test with plain unit tests
* independent from technical frameworks
* explicit about business behavior
* reusable if infrastructure changes later

That is why lifecycle operations live in methods such as `order.markReadyToShip()` instead of in controllers or entity setters.

## Why Start as a Modular Monolith

The project starts as a modular monolith for practical reasons:

* module boundaries can be validated before network boundaries exist
* the first business capabilities can be delivered faster
* local development stays simple
* testing remains cheaper than a distributed setup
* service extraction can be based on real pressure instead of speculation

This is deliberate, not an intermediate accident.

## Current Persistence Strategy

The current persistence approach is:

* PostgreSQL as the system database
* Flyway as the schema source of truth
* JPA in the infrastructure layer only
* domain objects kept free of persistence annotations

The current schema stores orders in a single `orders` table with:

* `id`
* `seller_id`
* `status`
* `created_at`
* `updated_at`

## Current Testing Strategy

The project currently uses two main test styles:

* pure JUnit unit tests for domain and application behavior
* Spring Boot integration tests for the API and persistence flow

Integration tests use Testcontainers with PostgreSQL, so the suite does not depend on a manually running local database container.

## AI-Assisted Engineering Workflow

The repository treats AI as an engineering accelerator, not as an architecture owner.

Current workflow elements:

* AI-assisted implementation support
* AI-assisted refactor support
* anti-overengineering review passes
* engineering decisions recorded in `docs/ai-engineering-log/`

Human ownership remains on architecture, domain boundaries, trade-offs, and final review.

## Manual API Validation Workflow

Automated tests cover the main behavior, but the repository also keeps a manual validation path for local review and demos.

The current cURL-based workflow is documented in:

* [../api/orders-manual-validation.md](../api/orders-manual-validation.md)

That guide validates:

* successful lifecycle progression
* invalid transition handling
* invalid UUID handling
* not-found handling
* direct database state verification

## Future Architectural Direction

Planned next architectural steps include:

* fulfillment assignment
* outbox-backed domain events
* event-driven module communication
* idempotent mutation handling
* observability improvements

Those concerns are intentionally deferred until the current module boundaries are stable enough to support them cleanly.
