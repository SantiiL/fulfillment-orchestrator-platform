# Fulfillment Node Working Days

## What This Feature Does

Fulfillment nodes now own a weekly working-days configuration that can be queried and replaced through the Fulfillment API.

Implemented contracts:

* `GET /api/v1/fulfillment-nodes/{id}/working-days`
* `PUT /api/v1/fulfillment-nodes/{id}/working-days`

The feature is intentionally limited to configuration management. Allocation enforcement remains deferred to `FOP-WORKING-DAYS-002`.

## Contract Summary

### GET

```text
GET /api/v1/fulfillment-nodes/{id}/working-days
```

Success response:

```json
{
  "id": "2d2d2d2d-2d2d-2d2d-2d2d-2d2d2d2d2d2d",
  "workingDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
}
```

Behavior:

* returns `200 OK` for an existing node
* returns the node id plus configured days
* sorts the response deterministically from Monday through Sunday
* returns `400 INVALID_FULFILLMENT_NODE_ID` for malformed UUIDs
* returns `404 FULFILLMENT_NODE_NOT_FOUND` when the node does not exist

### PUT

```text
PUT /api/v1/fulfillment-nodes/{id}/working-days
```

Request body:

```json
{
  "workingDays": ["SUNDAY", "MONDAY", "FRIDAY"]
}
```

Success response:

```json
{
  "id": "2d2d2d2d-2d2d-2d2d-2d2d-2d2d2d2d2d2d",
  "workingDays": ["MONDAY", "FRIDAY", "SUNDAY"]
}
```

Behavior:

* replaces the full set, not a partial patch
* is idempotent when the same full set is sent again
* rejects missing, null, empty, blank, or invalid day tokens with `400 INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`
* returns the normalized Monday-to-Sunday ordering regardless of request order

## Seven-Day Default

The compatibility rule is intentionally simple:

* new `FulfillmentNode` aggregates start with all seven days
* existing rows are backfilled by Flyway `V5__add_fulfillment_node_working_days.sql`
* legacy persistence edge cases that would rehydrate an empty set are normalized back to all seven days inside `FulfillmentNodeJpaEntity`

This keeps older nodes readable without introducing migration-time branching in the application layer.

## Persistence Model

Weekly working days are stored in a normalized join table:

```text
fulfillment_node_working_days
  fulfillment_node_id UUID not null
  day_of_week VARCHAR not null
  primary key (fulfillment_node_id, day_of_week)
```

Why this shape was chosen:

* it matches the domain's set semantics
* it prevents duplicate day rows with the composite primary key
* it avoids packing business data into a comma-separated string or JSON blob
* it keeps JPA mapping local to infrastructure through `@ElementCollection`

## Structured Errors

The feature reuses the existing Fulfillment API error envelope:

```json
{
  "status": 400,
  "code": "INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST",
  "message": "workingDays must contain at least one valid day of week",
  "path": "/api/v1/fulfillment-nodes/<id>/working-days",
  "timestamp": "2026-07-30T12:00:00Z"
}
```

Relevant codes:

* `INVALID_FULFILLMENT_NODE_ID`
* `FULFILLMENT_NODE_NOT_FOUND`
* `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`

## Trade-Offs

Chosen trade-offs:

* keep weekly working days inside the Fulfillment module because they are currently node-owned configuration, not a reusable calendar subsystem
* prefer whole-set replacement over partial add/remove endpoints because the state is small and the contract is easier to validate and reason about
* keep ordering deterministic in the application result object so both GET and PUT share one response rule
* keep allocation untouched in this slice so storage and contracts can land before introducing cross-module day-aware allocation behavior

Alternatives intentionally deferred:

* a top-level `workingdays` module for date-aware calendars
* holiday tables or exceptional closures
* per-node time zones or cutoffs
* allocation-time enforcement or automatic fallback selection

## Risks And Limits

Current limits:

* working days are weekly only; there is no date-specific override model
* allocation can still assign an order to a node whose configured working days would eventually reject that date
* the API accepts uppercase Java `DayOfWeek` enum tokens only

These are acceptable for the current milestone because the task goal is storage, retrieval, replacement, and compatibility, not full scheduling behavior.

## Interview-Ready Explanation

A concise explanation for reviewers:

> We modeled weekly working days as Fulfillment-node-owned configuration first, because that let us land a minimal normalized schema, deterministic GET/PUT contracts, and backward-compatible defaults without prematurely committing to a broader calendar engine. The next slice can enforce those days during allocation once the data contract is stable.

## Validation References

See also:

* [../api/orders-manual-validation.md](../api/orders-manual-validation.md)
* [../architecture/current-architecture.md](../architecture/current-architecture.md)
* [../ai-engineering-log/0016-fulfillment-node-working-days.md](../ai-engineering-log/0016-fulfillment-node-working-days.md)
