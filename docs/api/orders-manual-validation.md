# Orders Manual Validation

This guide validates the current fulfillment-aware and capacity-aware Orders flow manually against a locally running application.

Base URL:

```text
http://localhost:8080
```

Use a unique fulfillment node code for each run to avoid duplicate-code conflicts:

```text
AR-BUE-CAP-<timestamp>
```

Example seller IDs used throughout:

```text
11111111-1111-1111-1111-111111111111
22222222-2222-2222-2222-222222222222
33333333-3333-3333-3333-333333333333
44444444-4444-4444-4444-444444444444
```

After the create steps, replace placeholders with IDs returned by the API.

## Prerequisites

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Start the application:

```bash
./gradlew bootRun
```

Windows PowerShell equivalent:

```powershell
.\gradlew.bat bootRun
```

## 1. Create Fulfillment Node -> 201

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes' \
  --header 'Content-Type: application/json' \
  --data '{
    "code": "AR-BUE-CAP-<timestamp>",
    "name": "Buenos Aires Capacity Node",
    "maxDailyCapacity": 1
  }'
```

Expected:

* HTTP `201 Created`
* `active = true`
* `maxDailyCapacity = 1`

Save the returned ID as `<FULFILLMENT_NODE_ID>`.

## 2. List Fulfillment Nodes -> 200

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes'
```

Expected:

* HTTP `200 OK`
* the created node appears in the list

## 3. Create First Order -> CREATED

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "11111111-1111-1111-1111-111111111111"
  }'
```

Expected:

* HTTP `201 Created`
* `status = CREATED`
* `assignedFulfillmentNodeId = null`

Save the returned ID as `<ORDER_1_ID>`.

## 4. Allocate First Order -> ALLOCATED

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "<FULFILLMENT_NODE_ID>"
  }'
```

Expected:

* HTTP `200 OK`
* `status = ALLOCATED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 5. Get First Order -> ALLOCATED

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>'
```

Expected:

* HTTP `200 OK`
* `status = ALLOCATED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 6. Create Second Order -> CREATED

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "22222222-2222-2222-2222-222222222222"
  }'
```

Expected:

* HTTP `201 Created`
* `status = CREATED`

Save the returned ID as `<ORDER_2_ID>`.

## 7. Allocate Second Order to Full Node -> 409

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_2_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "<FULFILLMENT_NODE_ID>"
  }'
```

Expected:

* HTTP `409 Conflict`
* `code = FULFILLMENT_NODE_CAPACITY_EXCEEDED`

## 8. Ready to Ship -> READY_TO_SHIP

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/ready-to-ship'
```

Expected:

* HTTP `200 OK`
* `status = READY_TO_SHIP`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 9. Dispatch -> DISPATCHED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/dispatch'
```

Expected:

* HTTP `200 OK`
* `status = DISPATCHED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 10. Deliver -> DELIVERED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/deliver'
```

Expected:

* HTTP `200 OK`
* `status = DELIVERED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 11. Try Allocate Delivered Order Again -> 409

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "<FULFILLMENT_NODE_ID>"
  }'
```

Expected:

* HTTP `409 Conflict`
* `code = INVALID_ORDER_STATUS_TRANSITION`

## 12. Create Third Order -> CREATED

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "33333333-3333-3333-3333-333333333333"
  }'
```

Expected:

* HTTP `201 Created`
* `status = CREATED`

Save the returned ID as `<ORDER_3_ID>`.

## 13. Allocate with Missing fulfillmentNodeId -> 400

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_3_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

Expected:

* HTTP `400 Bad Request`
* `code = MISSING_FULFILLMENT_NODE_ID`

## 14. Allocate with Invalid fulfillmentNodeId -> 400

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_3_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "not-a-uuid"
  }'
```

Expected:

* HTTP `400 Bad Request`
* `code = INVALID_FULFILLMENT_NODE_ID`

## 15. Create Fourth Order -> CREATED

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "44444444-4444-4444-4444-444444444444"
  }'
```

Expected:

* HTTP `201 Created`
* `status = CREATED`

Save the returned ID as `<ORDER_4_ID>`.

## 16. Allocate with Missing Fulfillment Node -> 404

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_4_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "22222222-2222-2222-2222-222222222222"
  }'
```

Expected:

* HTTP `404 Not Found`
* `code = FULFILLMENT_NODE_NOT_FOUND`

## 17. Database Verification

First identify the local PostgreSQL container:

```bash
docker compose ps
```

Then query the orders table:

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select id, seller_id, status, fulfillment_node_id, allocated_at from orders where id in ('<ORDER_1_ID>'::uuid, '<ORDER_2_ID>'::uuid, '<ORDER_3_ID>'::uuid, '<ORDER_4_ID>'::uuid);"
```

Expected:

* `<ORDER_1_ID>` has `status = DELIVERED`
* `<ORDER_1_ID>` has `fulfillment_node_id = <FULFILLMENT_NODE_ID>`
* `<ORDER_1_ID>` has `allocated_at` not null
* `<ORDER_2_ID>`, `<ORDER_3_ID>`, and `<ORDER_4_ID>` remain `CREATED`
* `<ORDER_2_ID>`, `<ORDER_3_ID>`, and `<ORDER_4_ID>` have `fulfillment_node_id = null`
* `<ORDER_2_ID>`, `<ORDER_3_ID>`, and `<ORDER_4_ID>` have `allocated_at = null`

## 18. Optional Fulfillment Node Verification

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select id, code, max_daily_capacity, active from fulfillment_nodes where id = '<FULFILLMENT_NODE_ID>'::uuid;"
```

Expected:

* `code = AR-BUE-CAP-<timestamp>`
* `max_daily_capacity = 1`
* `active = true`

## Error Response Shape

Capacity and lifecycle validation should return structured errors in this format:

```json
{
  "status": 409,
  "code": "FULFILLMENT_NODE_CAPACITY_EXCEEDED",
  "message": "Fulfillment node daily capacity has been reached",
  "path": "/api/v1/orders/<ORDER_ID>/allocate",
  "timestamp": "2026-07-14T12:00:00Z"
}
```
