# Orders Lifecycle

## Purpose of the Orders Module

The Orders module owns the lifecycle of an order inside the fulfillment platform.

Its current responsibilities are:

* create orders for sellers
* expose order state through the API
* enforce valid lifecycle transitions
* reject invalid state changes
* persist order state changes in PostgreSQL

This module does not implement fulfillment node assignment yet. In the current version, allocation is a lifecycle transition only.

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

The order has moved forward in the lifecycle and is eligible for the next operational step. This does not yet mean a fulfillment node has been assigned.

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

## Invalid Transition Behavior

Any transition not listed above is invalid.

The domain rejects invalid transitions through `OrderLifecyclePolicy`. When a transition is not allowed, the domain throws `InvalidOrderStatusTransitionException`.

At the API level, that exception is returned as:

* HTTP `409 Conflict`
* business error code `INVALID_ORDER_STATUS_TRANSITION`

Example:

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
| Allocate order | `POST` | `/api/v1/orders/{id}/allocate` | `ALLOCATED` |
| Mark ready to ship | `POST` | `/api/v1/orders/{id}/ready-to-ship` | `READY_TO_SHIP` |
| Dispatch order | `POST` | `/api/v1/orders/{id}/dispatch` | `DISPATCHED` |
| Deliver order | `POST` | `/api/v1/orders/{id}/deliver` | `DELIVERED` |

## How Domain Rules Are Enforced

The `Order` aggregate exposes behavior methods instead of allowing arbitrary status mutation:

* `order.allocate()`
* `order.cancel()`
* `order.markReadyToShip()`
* `order.dispatch()`
* `order.deliver()`

Each behavior routes through a single internal transition path. That path calls `OrderLifecyclePolicy.validateTransition(...)` before the status changes.

This keeps transition rules centralized in one place while keeping the aggregate API explicit and easy to read.

## Why Behavior Methods Instead of Setters

A public status setter would allow callers to bypass the lifecycle rules and move an order into impossible states.

Behavior methods are used instead because they:

* make the business intent explicit
* preserve invariants inside the aggregate
* keep the transition policy enforceable
* make invalid transitions fail immediately

`order.dispatch()` expresses business meaning. `setStatus(DISPATCHED)` does not.

## Current Limitations

The current Orders module intentionally stops at lifecycle management.

Not implemented yet:

* fulfillment node assignment
* stock or capacity validation
* working day checks
* shipment tracking or carrier integration
* proof of delivery
* domain events and outbox
* idempotency controls
* observability-specific lifecycle telemetry

## Future Improvements

Likely next steps around the Orders module:

* integrate allocation with fulfillment node assignment
* publish lifecycle events through an outbox-backed flow
* add idempotency for mutation endpoints
* enrich operational visibility with logs, metrics, and traces
* connect lifecycle transitions with incidents and failure handling
