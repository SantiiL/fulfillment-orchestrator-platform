# Orders Lifecycle

## Purpose of the Orders Module

The Orders module owns the lifecycle of an order inside the fulfillment platform.

Its current responsibilities are:

* create orders for sellers
* expose order state through the API
* enforce valid lifecycle transitions
* reject invalid state changes
* assign orders to fulfillment nodes during allocation
* persist order state changes in PostgreSQL, including assignment data

## Lifecycle Diagram

```text
CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED

CREATED -> CANCELLED
ALLOCATED -> CANCELLED
READY_TO_SHIP -> CANCELLED
```

## Status Descriptions

### CREATED

The order exists and has been persisted, but no downstream fulfillment work has started.

### ALLOCATED

The order has been assigned to a fulfillment node, its lifecycle moved forward, and the allocation consumed capacity for the current UTC day. The persisted order now carries `assignedFulfillmentNodeId`, and the database row stores `fulfillment_node_id` plus `allocated_at`.

### READY_TO_SHIP

The order is prepared for dispatch.

### DISPATCHED

The order has left the internal preparation stage and is considered shipped.

### DELIVERED

The order reached its final successful state.

### CANCELLED

The order reached its final unsuccessful state through a valid cancellation path.

## Valid Transitions

| From | To |
| --- | --- |
| `CREATED` | `ALLOCATED` |
| `CREATED` | `CANCELLED` |
| `ALLOCATED` | `READY_TO_SHIP` |
| `ALLOCATED` | `CANCELLED` |
| `READY_TO_SHIP` | `DISPATCHED` |
| `READY_TO_SHIP` | `CANCELLED` |
| `DISPATCHED` | `DELIVERED` |

## Allocation Requirements

`CREATED -> ALLOCATED` is still a domain lifecycle transition, but it is no longer status-only.

The current allocation API requires a request body:

```json
{
  "fulfillmentNodeId": "uuid"
}
```

For allocation to succeed:

* the order must exist
* the fulfillment node must exist
* the fulfillment node must be active
* the lifecycle must allow `CREATED -> ALLOCATED`
* the node must still have remaining `maxDailyCapacity` for the current UTC day

Capacity rules in the current implementation:

* capacity is counted per fulfillment node
* the day boundary is UTC
* capacity is consumed when allocation succeeds
* later lifecycle transitions do not release capacity
* cancelled or delivered orders still count for that allocation day
* legacy orders with `allocated_at = null` remain valid

Not implemented in the current milestone:

* automatic node selection
* working days
* cutoff times
* capacity reservation tables

## Invalid Transition and Allocation Behavior

Any transition not listed above is invalid.

The domain rejects invalid transitions through `OrderLifecyclePolicy`. When a transition is not allowed, the domain throws `InvalidOrderStatusTransitionException`.

At the API level:

* invalid lifecycle transitions return HTTP `409 Conflict` with `INVALID_ORDER_STATUS_TRANSITION`
* missing `fulfillmentNodeId` returns HTTP `400 Bad Request` with `MISSING_FULFILLMENT_NODE_ID`
* invalid `fulfillmentNodeId` returns HTTP `400 Bad Request` with `INVALID_FULFILLMENT_NODE_ID`
* missing fulfillment nodes return HTTP `404 Not Found` with `FULFILLMENT_NODE_NOT_FOUND`
* inactive fulfillment nodes return HTTP `409 Conflict` with `FULFILLMENT_NODE_INACTIVE`
* full fulfillment nodes return HTTP `409 Conflict` with `FULFILLMENT_NODE_CAPACITY_EXCEEDED`

Example lifecycle error:

```text
Cannot transition order status from CREATED to DELIVERED
```

## Terminal States

Terminal states are:

* `DELIVERED`
* `CANCELLED`

Once an order reaches either state, it cannot transition again.

## API Endpoints by Lifecycle Action

| Action | Method | Path | Expected status after success |
| --- | --- | --- | --- |
| Create order | `POST` | `/api/v1/orders` | `CREATED` |
| Get order | `GET` | `/api/v1/orders/{id}` | current status |
| Cancel order | `POST` | `/api/v1/orders/{id}/cancel` | `CANCELLED` |
| Allocate order to fulfillment node | `POST` | `/api/v1/orders/{id}/allocate` | `ALLOCATED` |
| Mark ready to ship | `POST` | `/api/v1/orders/{id}/ready-to-ship` | `READY_TO_SHIP` |
| Dispatch order | `POST` | `/api/v1/orders/{id}/dispatch` | `DISPATCHED` |
| Deliver order | `POST` | `/api/v1/orders/{id}/deliver` | `DELIVERED` |

## How Domain Rules Are Enforced

The `Order` aggregate exposes behavior methods instead of allowing arbitrary status mutation:

* `order.allocate(assignedFulfillmentNodeId, allocatedAt)`
* `order.cancel()`
* `order.markReadyToShip()`
* `order.dispatch()`
* `order.deliver()`

Each lifecycle behavior routes through a single transition path. Allocation first validates the target status, then records assignment data and the allocation timestamp. Other lifecycle mutations use the common transition helper guarded by `OrderLifecyclePolicy`.

Capacity validation is intentionally not embedded inside the aggregate. It depends on current UTC time and on a cross-order count query for allocations already persisted for the same node, so the application layer performs that orchestration before calling `order.allocate(...)`.

## Why Behavior Methods Instead of Setters

A public status setter would allow callers to bypass the lifecycle rules and move an order into impossible states.

Behavior methods are used instead because they:

* make the business intent explicit
* preserve invariants inside the aggregate
* keep the transition policy enforceable
* make invalid transitions fail immediately
* keep assignment data and timestamps coupled to the allocation action

`order.dispatch()` expresses business meaning. `setStatus(DISPATCHED)` does not.

## Current Limitations

The current Orders module intentionally stops before:

* automatic node selection
* working-day-aware capacity
* cutoff times
* stock-aware allocation
* capacity reservation tables
* shipment tracking or carrier integration
* proof of delivery
* domain events and outbox
* idempotency controls
* observability-specific lifecycle telemetry

## Future Improvements

Likely next steps around the Orders module:

* integrate allocation with working-day and operational availability rules
* publish lifecycle events through an outbox-backed flow
* add idempotency for mutation endpoints
* enrich operational visibility with logs, metrics, and traces
* connect lifecycle transitions with incidents and failure handling
