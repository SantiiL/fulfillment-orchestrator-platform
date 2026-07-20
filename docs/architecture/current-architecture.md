# Current Architecture

## Overview

The Fulfillment Orchestrator Platform currently runs as a modular monolith.

That means one deployable application, but code organized around business capabilities instead of a flat controller/service/repository split.

This document is the authoritative source for the architecture that is currently implemented in code. It is the primary reference for current behavior, and roadmap or planning documents must not be interpreted as production behavior unless the same behavior is also present in this document, code, and tests.

The currently implemented business modules are `orders` and `fulfillment`.

## Main Modules

Current or planned module boundaries:

* `orders` - implemented; owns order lifecycle, assignment state, and order persistence
* `fulfillment` - implemented first slice; owns fulfillment node catalog and node data used during allocation
* `workingdays` - planned; will own business-day and operating-day rules
* `incidents` - planned; will own operational failure representation and handling
* `notifications` - planned; will own outbound communication concerns
* `shared` - small cross-cutting package for truly reusable concepts only

## Current Implemented Flow

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

For a typical order mutation:

1. A REST endpoint receives a request.
2. The controller converts HTTP input into an application command or query.
3. An application service orchestrates the use case.
4. The domain model applies lifecycle behavior such as `order.allocate(...)`, `order.dispatch()`, or `order.deliver()`.
5. The persistence adapter saves or loads state through JPA.
6. PostgreSQL stores the final state.

## Orders and Fulfillment Collaboration

Allocation is the main place where the two implemented modules collaborate today.

The current `POST /api/v1/orders/{id}/allocate` flow is:

1. The Orders API parses `orderId` and `fulfillmentNodeId`.
2. `AllocateOrderService` loads the order from the Orders repository port.
3. `AllocateOrderService` loads the fulfillment node from the Fulfillment repository port.
4. The application layer validates that the node exists and is active.
5. The application layer calculates the current UTC day window and asks the Orders repository for the current allocation count for that node.
6. If capacity remains, the Orders domain transitions the aggregate with `order.allocate(assignedFulfillmentNodeId, allocatedAt)`.
7. The Orders persistence adapter saves the new status plus assignment data.

This keeps the collaboration in the application layer instead of coupling the two domain models directly.

## Layer Responsibilities

### API

The API packages own:

* controllers
* request and response DTOs
* HTTP status mapping
* structured API error shaping

They do not own business rules.

### Application

The application packages own:

* use case interfaces
* commands and queries
* application services
* repository ports
* application-level result objects
* cross-module orchestration

They coordinate workflows but do not replace the domain model.

### Domain

The Orders domain owns:

* `Order`
* `OrderId`
* `SellerId`
* `AssignedFulfillmentNodeId`
* `OrderStatus`
* `OrderLifecyclePolicy`
* lifecycle exceptions

The Fulfillment domain owns:

* `FulfillmentNode`
* `FulfillmentNodeId`
* node validation rules

The Orders domain does not import Fulfillment domain types. It stores assignment through `AssignedFulfillmentNodeId`, which keeps the Order aggregate explicit about its own responsibility without turning the Orders domain into a mirror of the Fulfillment module.

### Infrastructure

The persistence packages own:

* JPA entities
* Spring Data repository interfaces
* persistence adapter implementations

They translate between the database model and the domain model.

## Why the Domain Layer Is Framework-Free

The domain model stays free from Spring, JPA, Hibernate, and web annotations so that business rules remain:

* easier to test with plain unit tests
* independent from technical frameworks
* explicit about business behavior
* reusable if infrastructure changes later

That is why lifecycle operations live in methods such as `order.allocate(...)` and `order.markReadyToShip()` instead of in controllers or entity setters.

## Why Capacity Validation Lives in the Application Layer

The capacity rule is not only about the current `Order` aggregate.

It depends on:

* another business object: the fulfillment node and its `maxDailyCapacity`
* current UTC time to derive the active allocation day window
* a cross-order count query for allocations already persisted for that node

That makes it an orchestration rule around the allocation use case, not an invariant that can be decided by a single Order instance in isolation.

The aggregate still protects its own lifecycle transition through `OrderLifecyclePolicy`. The application layer adds the external checks required before calling `order.allocate(...)`.

## Current Persistence Strategy

The current persistence approach is:

* PostgreSQL as the system database
* Flyway as the schema source of truth
* JPA in the infrastructure layer only
* domain objects kept free of persistence annotations

The current schema stores:

### `orders`

* `id`
* `seller_id`
* `status`
* `fulfillment_node_id`
* `allocated_at`
* `created_at`
* `updated_at`

### `fulfillment_nodes`

* `id`
* `code`
* `name`
* `max_daily_capacity`
* `active`
* `created_at`
* `updated_at`

## Current Repository Count Strategy

Capacity validation currently uses a focused repository method:

* `countAllocationsForFulfillmentNode(fulfillmentNodeId, startInclusive, endExclusive)`

The JPA adapter delegates that to a Spring Data derived query over the `orders` table filtered by:

* `fulfillment_node_id`
* `allocated_at >= startInclusive`
* `allocated_at < endExclusive`

This is intentionally simple for the current milestone. There is no reservation table, no precomputed capacity ledger, and no automatic node selection.

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

* fulfillment node creation and listing
* capacity-aware allocation success and failure paths
* lifecycle progression after successful allocation
* invalid UUID, not-found, and invalid-request handling
* direct database state verification for `fulfillment_node_id` and `allocated_at`

## Current Limitations

The current design intentionally stops before:

* working-day-aware capacity
* cutoff times
* automatic node selection
* reservation tables
* domain events and outbox
* idempotent mutation handling
* operational availability rules beyond the current `active` flag

Legacy orders with `allocated_at = null` remain valid and are intentionally tolerated by the current counting strategy.

## Future Architectural Direction

Planned next architectural steps include:

* working days / business calendar
* operational availability rules
* outbox-backed domain events
* incident handling
* idempotent mutation handling
* observability improvements

Those concerns are intentionally deferred until the current module boundaries are stable enough to support them cleanly.
