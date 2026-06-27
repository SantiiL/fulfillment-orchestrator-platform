# AI Engineering Log 0007: Dispatch Order Use Case

## Context

This feature introduced the `DISPATCHED` lifecycle transition for existing orders.

The system can now dispatch a ready-to-ship order through:

```text
POST /api/v1/orders/{id}/dispatch
```

This builds on top of the existing order creation, retrieval, cancellation, allocation and ready-to-ship vertical slices.

At this stage, dispatch is intentionally modeled as a lifecycle status transition only.

The feature does not yet create shipment records, integrate with carriers, publish domain events, track delivery status or validate fulfillment operations. Those responsibilities are expected to be introduced in later slices.

## AI-Assisted Work

Codex was used to generate the initial implementation for:

* Dispatch order use case.
* Command object.
* Application service.
* REST endpoint.
* Unit tests.
* Integration tests.
* Reuse of the existing structured API error handling.
* Reuse of the existing repository update path.
* Manual cURL validation plan.

## Ponytail Review

Ponytail was used as an anti-overengineering review before committing the feature.

The review was used to check for unnecessary abstractions, duplicated mappings, avoidable boilerplate and violations of the existing Orders module pattern.

The command/use case/service pattern was intentionally kept for consistency across the Orders module.

## Human-Owned Decisions

The following decisions remained human-owned:

* Keeping domain logic free from Spring and JPA.
* Using `order.dispatch()` instead of setting status directly.
* Preserving the domain lifecycle policy as the source of truth for valid transitions.
* Mapping invalid lifecycle transitions to HTTP `409 Conflict`.
* Keeping dispatch as a focused lifecycle transition.
* Deferring delivery, shipment records, carrier integration and domain events.
* Reusing the existing orders table without adding a new Flyway migration.
* Reusing the existing structured API error response format.
* Requiring manual cURL validation in addition to automated tests.

## Review Focus

The generated implementation was reviewed for:

* Architecture boundaries.
* Domain lifecycle enforcement.
* Transactional application behavior.
* Persistence update behavior.
* `updated_at` changes on update.
* API error responses.
* Unit and integration test coverage.
* Manual API behavior through cURL/Postman-compatible requests.

## Validation

The following validations were performed:

* `./gradlew clean test`
* Manual `POST /api/v1/orders`
* Manual `POST /api/v1/orders/{id}/dispatch` from `CREATED`, expecting `409`
* Manual `POST /api/v1/orders/{id}/allocate`
* Manual `POST /api/v1/orders/{id}/ready-to-ship`
* Manual `POST /api/v1/orders/{id}/dispatch` from `READY_TO_SHIP`, expecting `200`
* Manual `GET /api/v1/orders/{id}` after dispatch
* Manual invalid UUID scenario
* Manual missing order scenario
* Structured error response validation
* PostgreSQL row verification

## Manual Validation cURLs

### 1. Create order

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data-raw '{"sellerId":"11111111-1111-1111-1111-111111111111"}'
```

Expected:

```text
HTTP 201 Created
status = CREATED
```

### 2. Try dispatch from CREATED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/dispatch'
```

Expected:

```text
HTTP 409 Conflict
code = INVALID_ORDER_STATUS_TRANSITION
```

### 3. Allocate order

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate'
```

Expected:

```text
HTTP 200 OK
status = ALLOCATED
```

### 4. Mark order ready to ship

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/ready-to-ship'
```

Expected:

```text
HTTP 200 OK
status = READY_TO_SHIP
```

### 5. Dispatch order

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/dispatch'
```

Expected:

```text
HTTP 200 OK
status = DISPATCHED
```

### 6. Retrieve order

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>'
```

Expected:

```text
HTTP 200 OK
status = DISPATCHED
```

### 7. Invalid UUID

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/not-a-uuid/dispatch'
```

Expected:

```text
HTTP 400 Bad Request
code = INVALID_ORDER_ID
```

### 8. Missing order

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/22222222-2222-2222-2222-222222222222/dispatch'
```

Expected:

```text
HTTP 404 Not Found
code = ORDER_NOT_FOUND
```

### 9. Database verification

```bash
psql "postgresql://fulfillment_user:fulfillment_password@localhost:15432/fulfillment_orchestrator" \
  -c "select id, seller_id, status, created_at, updated_at from orders where id = '<ORDER_ID>'::uuid;"
```

Expected:

```text
status = DISPATCHED
```

## Outcome

The feature introduced the dispatch transition in the Orders module.

The dispatch flow follows the intended modular monolith architecture:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

The application service loads the aggregate, invokes domain behavior through `order.dispatch()`, and persists the updated state.

The order lifecycle now supports the following API-driven transitions:

```text
CREATED -> ALLOCATED
CREATED -> CANCELLED
ALLOCATED -> READY_TO_SHIP
ALLOCATED -> CANCELLED
READY_TO_SHIP -> DISPATCHED
```

All tests passed successfully.
