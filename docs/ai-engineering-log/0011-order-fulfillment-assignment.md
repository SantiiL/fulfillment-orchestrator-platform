# AI Engineering Log 0011: Order Fulfillment Assignment

## Context

This feature connects the completed Orders lifecycle with the Fulfillment module.

Before this change, order allocation was only a lifecycle transition:

```text
CREATED -> ALLOCATED
```

After this change, order allocation now assigns the order to an existing active fulfillment node.

The Orders module already supported:

```text
CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED
```

The Fulfillment module already supported a fulfillment node catalog through:

```text
POST /api/v1/fulfillment-nodes
GET  /api/v1/fulfillment-nodes/{id}
GET  /api/v1/fulfillment-nodes
```

This feature turns allocation into the first real orchestration step between Orders and Fulfillment.

## Scope

The feature updates:

```text
POST /api/v1/orders/{id}/allocate
```

The endpoint now requires a fulfillment node id:

```json
{
  "fulfillmentNodeId": "uuid"
}
```

Successful allocation now returns:

```json
{
  "id": "uuid",
  "sellerId": "uuid",
  "status": "ALLOCATED",
  "assignedFulfillmentNodeId": "uuid"
}
```

The assignment is persisted and returned by later order lifecycle responses.

## Architecture

The feature keeps the existing modular monolith boundaries:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

Orders and Fulfillment collaborate at the application layer.

The Orders domain does not import Fulfillment domain objects. Instead, Orders stores its own assignment value object:

```text
AssignedFulfillmentNodeId
```

The Orders application service validates the fulfillment node through the Fulfillment module repository before assigning the order.

This keeps the domain boundary clean while allowing modules to collaborate inside the modular monolith.

## Domain Changes

The Orders aggregate now stores an optional assigned fulfillment node id.

Orders that are not yet allocated can have:

```text
assignedFulfillmentNodeId = null
```

Allocated orders must have a fulfillment node assignment.

The allocation behavior now requires an assigned fulfillment node id and still respects the existing lifecycle policy.

The domain still prevents direct state mutation. Allocation happens through domain behavior instead of a public status setter.

## Application Changes

`AllocateOrderService` now performs the following flow:

```text
load order
validate fulfillment node exists
validate fulfillment node is active
call order.allocate(assignedFulfillmentNodeId)
save order
return OrderResult
```

The service rejects:

* missing order
* missing fulfillment node
* inactive fulfillment node
* invalid lifecycle transition

The implementation intentionally does not introduce a generic assignment engine, selector abstraction or lifecycle executor.

## Persistence Changes

A Flyway migration was added to store the assignment:

```text
V3__add_orders_fulfillment_node_id.sql
```

The `orders` table now has a nullable fulfillment node reference.

Existing orders remain valid with a null assignment.

The assignment is persisted when an order is allocated and reconstituted when orders are loaded from the database.

## API Changes

The allocate endpoint now accepts a request body:

```json
{
  "fulfillmentNodeId": "uuid"
}
```

The Orders API response now includes:

```text
assignedFulfillmentNodeId
```

A single `OrderResponse` DTO is used for order responses.

This replaced duplicated response DTOs that had become identical after introducing fulfillment assignment.

## Error Handling

The API keeps the existing structured error shape:

```json
{
  "status": 400,
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "path": "/api/v1/orders/{id}/allocate",
  "timestamp": "..."
}
```

Handled cases include:

* invalid order id
* missing fulfillment node id
* invalid fulfillment node id
* missing order
* missing fulfillment node
* inactive fulfillment node
* invalid order lifecycle transition

## AI-Assisted Work

Codex was used to generate the initial implementation.

The generated work included:

* domain assignment value object
* updated Order aggregate behavior
* updated allocate command and service
* fulfillment node validation through application-layer collaboration
* persistence migration
* JPA mapping updates
* API request parsing
* structured error handling
* updated tests
* manual validation cURLs

## Ponytail Review

Ponytail reviewed the implementation for overengineering and architecture risk.

The review confirmed:

* Orders stores its own assignment id instead of importing Fulfillment domain types.
* The application collaboration is direct and understandable.
* The persistence migration is minimal and safe.
* The endpoint remains understandable.
* The feature stays focused on manual assignment only.
* No capacity, working days, events, outbox, idempotency or automatic selection logic slipped in.
* Tests remain focused and readable.

Ponytail recommended one cleanup: replacing duplicated `CreateOrderResponse` and `GetOrderResponse` DTOs with one local `OrderResponse`.

That cleanup was applied without changing the response JSON shape.

## Human-Owned Decisions

The following decisions remained human-owned:

* Keep Orders and Fulfillment domain layers decoupled.
* Store `AssignedFulfillmentNodeId` inside the Orders domain.
* Validate fulfillment node existence and active status in the application layer.
* Do not introduce automatic node selection yet.
* Do not implement capacity consumption yet.
* Do not implement working days rules yet.
* Do not introduce outbox, Kafka, idempotency or observability in this branch.
* Keep duplicate API error infrastructure local for now instead of doing a broad shared API refactor.
* Keep the feature focused on manual assignment to an existing active fulfillment node.

## Validation

The following validations were performed:

* `./gradlew clean test`
* Manual create fulfillment node
* Manual create order
* Manual allocate order with fulfillment node id
* Manual get order after allocation
* Manual ready-to-ship after allocation
* Manual dispatch after allocation
* Manual deliver after allocation
* Manual missing fulfillment node id scenario
* Manual invalid fulfillment node id scenario
* Manual missing fulfillment node scenario
* Manual already allocated order scenario
* PostgreSQL assignment verification

## Manual Validation cURLs

### 1. Create fulfillment node

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes' \
  --header 'Content-Type: application/json' \
  --data '{
    "code": "AR-BUE-01",
    "name": "Buenos Aires Node 1",
    "maxDailyCapacity": 100
  }'
```

Expected:

```text
HTTP 201 Created
```

Save the returned id as:

```text
<FULFILLMENT_NODE_ID>
```

### 2. Create order

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "11111111-1111-1111-1111-111111111111"
  }'
```

Expected:

```text
HTTP 201 Created
status = CREATED
assignedFulfillmentNodeId = null
```

Save the returned id as:

```text
<ORDER_ID>
```

### 3. Allocate order with fulfillmentNodeId

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "<FULFILLMENT_NODE_ID>"
  }'
```

Expected:

```text
HTTP 200 OK
status = ALLOCATED
assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>
```

### 4. Get order

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>'
```

Expected:

```text
HTTP 200 OK
status = ALLOCATED
assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>
```

### 5. Ready to ship

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/ready-to-ship'
```

Expected:

```text
HTTP 200 OK
status = READY_TO_SHIP
assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>
```

### 6. Dispatch

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/dispatch'
```

Expected:

```text
HTTP 200 OK
status = DISPATCHED
assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>
```

### 7. Deliver

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/deliver'
```

Expected:

```text
HTTP 200 OK
status = DELIVERED
assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>
```

### 8. Allocate with missing fulfillmentNodeId

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

Expected:

```text
HTTP 400 Bad Request
code = MISSING_FULFILLMENT_NODE_ID
```

### 9. Allocate with invalid fulfillmentNodeId

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "not-a-uuid"
  }'
```

Expected:

```text
HTTP 400 Bad Request
code = INVALID_FULFILLMENT_NODE_ID
```

### 10. Allocate with missing fulfillment node

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "22222222-2222-2222-2222-222222222222"
  }'
```

Expected:

```text
HTTP 404 Not Found
code = FULFILLMENT_NODE_NOT_FOUND
```

### 11. Allocate already allocated order

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "<FULFILLMENT_NODE_ID>"
  }'
```

Expected:

```text
HTTP 409 Conflict
code = INVALID_ORDER_STATUS_TRANSITION
```

### 12. Database verification

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select id, seller_id, status, fulfillment_node_id from orders where id = '<ORDER_ID>'::uuid;"
```

Expected:

```text
status = ALLOCATED, READY_TO_SHIP, DISPATCHED or DELIVERED depending on the lifecycle step
fulfillment_node_id = <FULFILLMENT_NODE_ID>
```

## Outcome

Order allocation now assigns orders to existing active fulfillment nodes.

This is the first feature where the Orders and Fulfillment modules collaborate to perform a real orchestration step.

The project now moves beyond lifecycle-only behavior and starts modeling actual fulfillment assignment.

The next natural milestone is capacity-aware allocation or fulfillment node assignment rules.
