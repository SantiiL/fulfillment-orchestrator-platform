# Current Architecture

## Overview

The Fulfillment Orchestrator Platform currently runs as a modular monolith.

That means one deployable application, but code organized around business capabilities instead of a flat controller/service/repository split.

This document is the authoritative source for the architecture that is currently implemented in code. It is the primary reference for current behavior, and roadmap or planning documents must not be interpreted as production behavior unless the same behavior is also present in this document, code, and tests.

The currently implemented business modules are `orders` and `fulfillment`.

## Main Modules

Current or planned module boundaries:

* `orders` - implemented; owns order lifecycle, assignment state, and order persistence
* `fulfillment` - implemented; owns fulfillment node catalog, weekly working-days configuration, and node data used during allocation
* `workingdays` - planned; reserved for future date-aware business-calendar rules beyond the current weekly node-owned configuration
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

Working-days configuration does not participate in allocation yet. The current allocation flow still depends only on order lifecycle, fulfillment-node existence, fulfillment-node active status, and UTC-day capacity. Allocation enforcement against configured working days is explicitly deferred to `FOP-WORKING-DAYS-002`.

## Fulfillment Working-Days Flow

The Fulfillment API now exposes two node-scoped contracts:

* `GET /api/v1/fulfillment-nodes/{id}/working-days`
* `PUT /api/v1/fulfillment-nodes/{id}/working-days`

The implemented flow is:

1. `FulfillmentNodeController` parses the fulfillment-node UUID.
2. GET loads the aggregate and returns the configured set.
3. PUT validates that the request contains at least one valid `DayOfWeek` token.
4. `ReplaceFulfillmentNodeWorkingDaysService` loads the aggregate, replaces the full set, saves it, and returns the normalized result.
5. `FulfillmentNodeWorkingDaysResult` sorts the response deterministically from Monday through Sunday before the API returns it.

This keeps weekly working days as node-owned state without introducing a separate calendar engine before the business rules need one.

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
* the immutable non-empty weekly working-days set attached to each node

The Orders domain does not import Fulfillment domain types. It stores assignment through `AssignedFulfillmentNodeId`, which keeps the Order aggregate explicit about its own responsibility without turning the Orders domain into a mirror of the Fulfillment module.

### Infrastructure

The persistence packages own:

* JPA entities
* Spring Data repository interfaces
* persistence adapter implementations

They translate between the database model and the domain model.

`FulfillmentNodeJpaEntity` keeps the weekly working-days mapping inside infrastructure through JPA `@ElementCollection`, while the domain model remains free from persistence annotations.

## Why the Domain Layer Is Framework-Free

The domain model stays free from Spring, JPA, Hibernate, and web annotations so that business rules remain:

* easier to test with plain unit tests
* independent from technical frameworks
* explicit about business behavior
* reusable if infrastructure changes later

That is why lifecycle operations live in methods such as `order.allocate(...)` and `order.markReadyToShip()` instead of in controllers or entity setters.

The same rule applies to working days: `FulfillmentNode` owns validation such as non-null, non-empty, null-free working-day replacement, while persistence-specific concerns stay out of the aggregate.

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

### `fulfillment_node_working_days`

* `fulfillment_node_id`
* `day_of_week`

`V5__add_fulfillment_node_working_days.sql` adds the normalized join table, constrains rows with a composite primary key, and backfills every existing fulfillment node with all seven days. The JPA adapter also normalizes empty persisted sets back to all seven days so legacy rows remain compatible.

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

The working-days coverage now includes:

* domain validation for replacement invariants
* application coverage for GET, full-set replacement, and idempotent replacement
* integration coverage for GET and PUT contracts, deterministic ordering, invalid UUID handling, missing-node handling, invalid request bodies, seven-day backfill, and normalized persistence rows

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
* working-days defaults, replacement, structured errors, and normalized persistence rows
* capacity-aware allocation success and failure paths
* lifecycle progression after successful allocation
* invalid UUID, not-found, and invalid-request handling
* direct database state verification for `fulfillment_node_id`, `allocated_at`, and `fulfillment_node_working_days`

## Current Limitations

The current design intentionally stops before:

* allocation enforcement against configured working days
* holidays or date-specific calendars
* cutoff times
* per-node time zones
* automatic node selection
* reservation tables
* domain events and outbox
* idempotent mutation handling beyond whole-replacement semantics for the working-days PUT
* operational availability rules beyond the current `active` flag

Legacy orders with `allocated_at = null` remain valid and are intentionally tolerated by the current counting strategy.

## Future Architectural Direction

Planned next architectural steps include:

* working-day-aware allocation enforcement in `FOP-WORKING-DAYS-002`
* broader business-calendar and operational-availability rules
* outbox-backed domain events
* incident handling
* idempotent mutation handling
* observability improvements

Those concerns are intentionally deferred until the current module boundaries are stable enough to support them cleanly.
