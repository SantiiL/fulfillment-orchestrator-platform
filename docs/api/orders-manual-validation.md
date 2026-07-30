# Orders And Fulfillment Manual Validation

This guide validates the current Fulfillment Node catalog, weekly working-days configuration, and capacity-aware Orders flow manually against a locally running application.

Base URL:

```text
http://localhost:8080
```

Use a unique fulfillment node code for each run to avoid duplicate-code conflicts:

```text
AR-BUE-WD-<timestamp>
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
    "code": "AR-BUE-WD-<timestamp>",
    "name": "Buenos Aires Working Days Node",
    "maxDailyCapacity": 1
  }'
```

Expected:

* HTTP `201 Created`
* `active = true`
* `maxDailyCapacity = 1`

Save the returned ID as `<FULFILLMENT_NODE_ID>`.

## 2. Get Default Working Days -> 200

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days'
```

Expected:

* HTTP `200 OK`
* `id = <FULFILLMENT_NODE_ID>`
* `workingDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]`

This verifies the seven-day default for newly created nodes.

## 3. Replace Working Days -> 200

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days' \
  --header 'Content-Type: application/json' \
  --data '{
    "workingDays": ["SUNDAY", "MONDAY", "FRIDAY"]
  }'
```

Expected:

* HTTP `200 OK`
* `id = <FULFILLMENT_NODE_ID>`
* `workingDays = ["MONDAY", "FRIDAY", "SUNDAY"]`

The API sorts the response deterministically from Monday through Sunday, regardless of request order.

## 4. Repeat The Same Replacement -> 200

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days' \
  --header 'Content-Type: application/json' \
  --data '{
    "workingDays": ["SUNDAY", "MONDAY", "FRIDAY"]
  }'
```

Expected:

* HTTP `200 OK`
* `workingDays = ["MONDAY", "FRIDAY", "SUNDAY"]` again

This verifies whole-set replacement idempotency.

## 5. List Fulfillment Nodes -> 200

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes'
```

Expected:

* HTTP `200 OK`
* the created node appears in the list

## 6. Get Working Days With Invalid UUID -> 400

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes/not-a-uuid/working-days'
```

Expected:

* HTTP `400 Bad Request`
* `code = INVALID_FULFILLMENT_NODE_ID`

## 7. Replace Working Days With Invalid UUID -> 400

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/not-a-uuid/working-days' \
  --header 'Content-Type: application/json' \
  --data '{
    "workingDays": ["MONDAY"]
  }'
```

Expected:

* HTTP `400 Bad Request`
* `code = INVALID_FULFILLMENT_NODE_ID`

## 8. Get Working Days For Missing Node -> 404

```bash
curl --location 'http://localhost:8080/api/v1/fulfillment-nodes/22222222-2222-2222-2222-222222222222/working-days'
```

Expected:

* HTTP `404 Not Found`
* `code = FULFILLMENT_NODE_NOT_FOUND`

## 9. Replace Working Days For Missing Node -> 404

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/22222222-2222-2222-2222-222222222222/working-days' \
  --header 'Content-Type: application/json' \
  --data '{
    "workingDays": ["MONDAY"]
  }'
```

Expected:

* HTTP `404 Not Found`
* `code = FULFILLMENT_NODE_NOT_FOUND`

## 10. Replace Working Days With Invalid Body -> 400

Missing `workingDays`:

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

Invalid token:

```bash
curl --location --request PUT 'http://localhost:8080/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days' \
  --header 'Content-Type: application/json' \
  --data '{
    "workingDays": ["FUNDAY"]
  }'
```

Expected for both:

* HTTP `400 Bad Request`
* `code = INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`

## 11. Database Verification For Working Days

First identify the local PostgreSQL container:

```bash
docker compose ps
```

Then query the normalized working-days table:

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select fulfillment_node_id, day_of_week from fulfillment_node_working_days where fulfillment_node_id = '<FULFILLMENT_NODE_ID>'::uuid order by case day_of_week when 'MONDAY' then 1 when 'TUESDAY' then 2 when 'WEDNESDAY' then 3 when 'THURSDAY' then 4 when 'FRIDAY' then 5 when 'SATURDAY' then 6 when 'SUNDAY' then 7 end;"
```

Expected:

* exactly three rows for `<FULFILLMENT_NODE_ID>`
* rows for `MONDAY`, `FRIDAY`, and `SUNDAY`
* rows returned in Monday-to-Sunday order when using the explicit `case` ordering above

## 12. Create First Order -> CREATED

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

## 13. Allocate First Order -> ALLOCATED

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

Allocation still ignores configured working days in this milestone. `FOP-WORKING-DAYS-002` is the deferred task for enforcement.

## 14. Get First Order -> ALLOCATED

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>'
```

Expected:

* HTTP `200 OK`
* `status = ALLOCATED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 15. Create Second Order -> CREATED

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

## 16. Allocate Second Order To Full Node -> 409

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

## 17. Ready To Ship -> READY_TO_SHIP

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/ready-to-ship'
```

Expected:

* HTTP `200 OK`
* `status = READY_TO_SHIP`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 18. Dispatch -> DISPATCHED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/dispatch'
```

Expected:

* HTTP `200 OK`
* `status = DISPATCHED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 19. Deliver -> DELIVERED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_1_ID>/deliver'
```

Expected:

* HTTP `200 OK`
* `status = DELIVERED`
* `assignedFulfillmentNodeId = <FULFILLMENT_NODE_ID>`

## 20. Try Allocate Delivered Order Again -> 409

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

## 21. Create Third Order -> CREATED

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

## 22. Allocate With Missing `fulfillmentNodeId` -> 400

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_3_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

Expected:

* HTTP `400 Bad Request`
* `code = MISSING_FULFILLMENT_NODE_ID`

## 23. Allocate With Invalid `fulfillmentNodeId` -> 400

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

## 24. Create Fourth Order -> CREATED

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

## 25. Allocate With Missing Fulfillment Node -> 404

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_4_ID>/allocate' \
  --header 'Content-Type: application/json' \
  --data '{
    "fulfillmentNodeId": "55555555-5555-5555-5555-555555555555"
  }'
```

Expected:

* HTTP `404 Not Found`
* `code = FULFILLMENT_NODE_NOT_FOUND`

## 26. Database Verification For Orders

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

## Error Response Shapes

Working-days validation errors use the structured API shape:

```json
{
  "status": 400,
  "code": "INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST",
  "message": "workingDays must contain at least one valid day of week",
  "path": "/api/v1/fulfillment-nodes/<FULFILLMENT_NODE_ID>/working-days",
  "timestamp": "2026-07-30T12:00:00Z"
}
```

Capacity and lifecycle validation use the same shape:

```json
{
  "status": 409,
  "code": "FULFILLMENT_NODE_CAPACITY_EXCEEDED",
  "message": "Fulfillment node daily capacity has been reached",
  "path": "/api/v1/orders/<ORDER_ID>/allocate",
  "timestamp": "2026-07-30T12:00:00Z"
}
```
