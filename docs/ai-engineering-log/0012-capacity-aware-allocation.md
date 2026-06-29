# AI Engineering Log 0012: Capacity-Aware Allocation

## Context

This feature adds capacity validation to order allocation.

Before this change, order allocation already required an existing active fulfillment node and persisted the assignment:

```text
Order -> assignedFulfillmentNodeId
```

After this change, allocation also checks the fulfillment node's daily capacity before assigning the order.

This moves the project closer to real fulfillment orchestration behavior.

## Scope

The feature updates:

```text
POST /api/v1/orders/{id}/allocate
```

Allocation now validates:

* the order exists
* the fulfillment node exists
* the fulfillment node is active
* the order lifecycle allows allocation
* the fulfillment node has available daily capacity

The feature intentionally does not introduce:

* automatic node selection
* capacity reservation tables
* working day rules
* cutoff times
* outbox/events
* Kafka
* idempotency
* observability

## Business Rule

A fulfillment node has a `maxDailyCapacity`.

An allocation is rejected when the number of orders assigned to that node during the current UTC day is greater than or equal to the node's `maxDailyCapacity`.

For this first implementation:

* capacity is counted by current UTC day
* capacity is consumed when allocation succeeds
* later lifecycle transitions do not release capacity
* cancelled or delivered orders still count if they were allocated during the same day
* legacy orders with `allocatedAt = null` remain valid and do not consume today's capacity

## Architecture

The feature keeps the existing modular monolith flow:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

Capacity validation lives in the Orders application layer because it depends on:

* current UTC time
* querying multiple orders
* validating a fulfillment node
* orchestrating allocation behavior

This is not single-aggregate domain state, so it does not belong inside the `Order` aggregate.

Orders and Fulfillment remain decoupled at the domain layer.

Orders still only stores its own assignment value object:

```text
AssignedFulfillmentNodeId
```

The cross-module collaboration happens in the application layer.

## Domain Changes

The Orders aggregate now records allocation time.

`allocatedAt` is set when allocation succeeds.

Orders that are not allocated have:

```text
allocatedAt = null
```

Allocated orders have:

```text
allocatedAt != null
```

The allocation behavior still goes through domain behavior and does not expose a public status setter.

## Application Changes

`AllocateOrderService` now follows this flow:

```text
load order
load fulfillment node
validate node is active
validate lifecycle can allocate
count today's allocations for node
reject when capacity is reached
allocate order with assigned node and allocatedAt
save order
return result
```

A new application exception handles capacity failures:

```text
FulfillmentNodeCapacityExceededException
```

This maps to:

```text
HTTP 409 Conflict
FULFILLMENT_NODE_CAPACITY_EXCEEDED
```

## Persistence Changes

A Flyway migration was added:

```text
V4__add_orders_allocated_at.sql
```

The `orders` table now stores:

```text
allocated_at TIMESTAMP WITH TIME ZONE NULL
```

The repository now supports a focused count method for capacity validation.

The persistence change is intentionally minimal: one nullable column, no speculative tables or reservation model.

## API Changes

The allocate request shape remains unchanged:

```json
{
  "fulfillmentNodeId": "uuid"
}
```

The success response shape remains unchanged.

`allocatedAt` is persisted but not exposed through the API.

Capacity exceeded responses use the existing structured API error shape:

```json
{
  "status": 409,
  "code": "FULFILLMENT_NODE_CAPACITY_EXCEEDED",
  "message": "Fulfillment node daily capacity has been reached",
  "path": "/api/v1/orders/{id}/allocate",
  "timestamp": "..."
}
```

## AI-Assisted Work

Codex was used to generate the initial implementation.

The generated work included:

* `allocatedAt` support in the Orders domain
* migration for `orders.allocated_at`
* repository count method for daily allocations
* capacity validation in `AllocateOrderService`
* capacity exceeded exception and API handling
* unit test updates
* integration test updates
* manual validation cURL flow

## Ponytail Review

Ponytail reviewed the implementation for overengineering and architecture risk.

The review confirmed:

* capacity validation belongs in the application layer
* Orders and Fulfillment domains remain decoupled
* `allocatedAt` is the minimum useful state for daily capacity counting
* the repository count method is focused and readable
* the Flyway migration is minimal and safe
* API error handling stays consistent
* tests remain readable and focused on manual allocation plus capacity

Ponytail recommended one cheap cleanup in integration tests: removing an unnecessary `persistedSellerId(...)` helper if it was only used by one test.

## Human-Owned Decisions

The following decisions remained human-owned:

* count capacity by current UTC day
* count capacity once allocation succeeds
* do not release capacity after delivery or cancellation
* keep `allocatedAt` internal and not exposed by the API
* avoid reservation tables for this first implementation
* avoid automatic node selection
* avoid working day/cutoff time logic
* avoid outbox/events/idempotency/observability in this feature
* keep the repository count method specific instead of generic

## Validation

The following validations were performed:

* `./gradlew clean test`
* manual creation of a fulfillment node with capacity 1
* manual allocation of the first order
* manual rejection of the second order due to capacity
* manual continuation of the first order through ready-to-ship, dispatch and delivery
* manual invalid lifecycle transition after delivery
* manual missing fulfillment node id scenario
* manual invalid fulfillment node id scenario
* manual missing fulfillment node scenario
* PostgreSQL verification of `fulfillment_node_id` and `allocated_at`

## Manual Validation Results

Generated IDs from the manual validation run:

```text
FULFILLMENT_NODE_ID: ddcc8c03-97ca-490b-af67-56bdcf56c258
ORDER_1_ID: 128bcb6d-1687-4e5f-8685-aec293482236
ORDER_2_ID: b2f62e71-3a7b-4e9b-ad7a-b9d38a6a4e51
ORDER_3_ID: ad2aa45f-054f-4abe-95b4-d01930ac2500
ORDER_4_ID: 4ba4f970-f136-4ad2-8f13-42cf19ddca02
Generated node code: AR-BUE-CAP-20260628230400
```

Manual validation passed for:

```text
POST /api/v1/fulfillment-nodes -> 201
GET /api/v1/fulfillment-nodes -> 200
POST /api/v1/orders -> 201
POST /api/v1/orders/{ORDER_1_ID}/allocate -> 200
GET /api/v1/orders/{ORDER_1_ID} -> 200
POST /api/v1/orders/{ORDER_2_ID}/allocate -> 409 FULFILLMENT_NODE_CAPACITY_EXCEEDED
POST /api/v1/orders/{ORDER_1_ID}/ready-to-ship -> 200
POST /api/v1/orders/{ORDER_1_ID}/dispatch -> 200
POST /api/v1/orders/{ORDER_1_ID}/deliver -> 200
POST /api/v1/orders/{ORDER_1_ID}/allocate -> 409 INVALID_ORDER_STATUS_TRANSITION
POST /api/v1/orders/{ORDER_3_ID}/allocate with missing fulfillmentNodeId -> 400 MISSING_FULFILLMENT_NODE_ID
POST /api/v1/orders/{ORDER_3_ID}/allocate with invalid UUID -> 400 INVALID_FULFILLMENT_NODE_ID
POST /api/v1/orders/{ORDER_4_ID}/allocate with missing node -> 404 FULFILLMENT_NODE_NOT_FOUND
```

Database verification confirmed:

```text
ORDER_1_ID:
status = DELIVERED
fulfillment_node_id = ddcc8c03-97ca-490b-af67-56bdcf56c258
allocated_at = 2026-06-29 02:04:00.600531+00

ORDER_2_ID:
status = CREATED
fulfillment_node_id = null
allocated_at = null

ORDER_3_ID:
status = CREATED
fulfillment_node_id = null
allocated_at = null

ORDER_4_ID:
status = CREATED
fulfillment_node_id = null
allocated_at = null
```

Fulfillment node verification confirmed:

```text
id = ddcc8c03-97ca-490b-af67-56bdcf56c258
code = AR-BUE-CAP-20260628230400
max_daily_capacity = 1
active = true
```

Mismatches found: none.

Code changes needed: none.

## Outcome

Capacity-aware allocation is working end to end.

The system now prevents over-allocation of a fulfillment node for the current UTC day.

This is the first operational rule in the fulfillment orchestration flow and moves the platform beyond simple node assignment into real allocation constraints.
