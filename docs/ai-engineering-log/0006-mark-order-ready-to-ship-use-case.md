# AI Engineering Log 0006: Mark Order Ready To Ship Use Case

## Context

This feature introduced the `READY_TO_SHIP` lifecycle transition for existing orders.

The system can now mark an allocated order as ready to ship through:

```text
POST /api/v1/orders/{id}/ready-to-ship
```

This builds on top of the existing order creation, retrieval, cancellation and allocation vertical slices.

At this stage, ready-to-ship is intentionally modeled as a lifecycle status transition only.

The feature does not yet dispatch the order, create shipment records, integrate with carriers, validate warehouse operations or publish domain events. Those responsibilities are expected to be introduced in later slices.

## AI-Assisted Work

Codex was used to generate the initial implementation for:

* Mark order ready to ship use case.
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

The review confirmed that the production code was lean and consistent with the existing Orders module patterns.

It recommended only test-local cleanup around repeated structured error assertions and direct SQL setup helpers. No production architecture changes were needed.

The command/use case/service pattern was intentionally kept for consistency across the Orders module.

## Human-Owned Decisions

The following decisions remained human-owned:

* Keeping domain logic free from Spring and JPA.
* Using `order.markReadyToShip()` instead of setting status directly.
* Preserving the domain lifecycle policy as the source of truth for valid transitions.
* Mapping invalid lifecycle transitions to HTTP `409 Conflict`.
* Keeping ready-to-ship as a focused lifecycle transition.
* Deferring dispatch, delivery, shipment records and carrier integration.
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
* Manual `POST /api/v1/orders/{id}/ready-to-ship` from `CREATED`, expecting `409`
* Manual `POST /api/v1/orders/{id}/allocate`
* Manual `POST /api/v1/orders/{id}/ready-to-ship` from `ALLOCATED`, expecting `200`
* Manual `GET /api/v1/orders/{id}` after ready-to-ship
* Manual invalid UUID scenario
* Manual missing order scenario
* Structured error response validation
* PostgreSQL row verification

## Manual Validation cURLs

### 1. Create order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"sellerId":"2f9f6f5e-8b3d-4db7-9d3a-4c5f2a6e9d11"}'
```

Expected:

```text
HTTP 201 Created
status = CREATED
```

### 2. Try ready-to-ship from CREATED

```bash
curl -X POST http://localhost:8080/api/v1/orders/<ORDER_ID>/ready-to-ship
```

Expected:

```text
HTTP 409 Conflict
code = INVALID_ORDER_STATUS_TRANSITION
```

### 3. Allocate order

```bash
curl -X POST http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate
```

Expected:

```text
HTTP 200 OK
status = ALLOCATED
```

### 4. Mark order ready to ship

```bash
curl -X POST http://localhost:8080/api/v1/orders/<ORDER_ID>/ready-to-ship
```

Expected:

```text
HTTP 200 OK
status = READY_TO_SHIP
```

### 5. Retrieve order

```bash
curl -X GET http://localhost:8080/api/v1/orders/<ORDER_ID>
```

Expected:

```text
HTTP 200 OK
status = READY_TO_SHIP
```

### 6. Invalid UUID

```bash
curl -X POST http://localhost:8080/api/v1/orders/not-a-uuid/ready-to-ship
```

Expected:

```text
HTTP 400 Bad Request
code = INVALID_ORDER_ID
```

### 7. Missing order

```bash
curl -X POST http://localhost:8080/api/v1/orders/22222222-2222-2222-2222-222222222222/ready-to-ship
```

Expected:

```text
HTTP 404 Not Found
code = ORDER_NOT_FOUND
```

### 8. Database verification

```bash
docker exec fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select id, seller_id, status, created_at, updated_at from orders where id = '<ORDER_ID>'::uuid;"
```

Expected:

```text
status = READY_TO_SHIP
```

## Outcome

The feature introduced the ready-to-ship transition in the Orders module.

The ready-to-ship flow follows the intended modular monolith architecture:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

The application service loads the aggregate, invokes domain behavior through `order.markReadyToShip()`, and persists the updated state.

The order lifecycle now supports the following API-driven transitions:

```text
CREATED -> ALLOCATED
CREATED -> CANCELLED
ALLOCATED -> READY_TO_SHIP
ALLOCATED -> CANCELLED
```

All tests passed successfully.
