# Orders Manual Validation

This guide validates the current Orders lifecycle manually against a locally running application.

Base URL:

```text
http://localhost:8080
```

Example seller ID used throughout:

```text
11111111-1111-1111-1111-111111111111
```

After the create step, replace `<ORDER_ID>` with the ID returned by the API.

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

## 1. Create Order -> CREATED

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
  --header 'Content-Type: application/json' \
  --data '{
    "sellerId": "11111111-1111-1111-1111-111111111111"
  }'
```

Expected:

* HTTP `201 Created`
* response `status = CREATED`

## 2. Try Deliver from CREATED -> 409

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/deliver'
```

Expected:

* HTTP `409 Conflict`
* `code = INVALID_ORDER_STATUS_TRANSITION`

## 3. Allocate Order -> ALLOCATED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/allocate'
```

Expected:

* HTTP `200 OK`
* response `status = ALLOCATED`

## 4. Ready to Ship -> READY_TO_SHIP

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/ready-to-ship'
```

Expected:

* HTTP `200 OK`
* response `status = READY_TO_SHIP`

## 5. Dispatch -> DISPATCHED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/dispatch'
```

Expected:

* HTTP `200 OK`
* response `status = DISPATCHED`

## 6. Deliver -> DELIVERED

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/deliver'
```

Expected:

* HTTP `200 OK`
* response `status = DELIVERED`

## 7. Get Order -> DELIVERED

```bash
curl --location 'http://localhost:8080/api/v1/orders/<ORDER_ID>'
```

Expected:

* HTTP `200 OK`
* response `status = DELIVERED`

## 8. Try Deliver Again -> 409

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/deliver'
```

Expected:

* HTTP `409 Conflict`
* `code = INVALID_ORDER_STATUS_TRANSITION`

## 9. Try Cancel after DELIVERED -> 409

```bash
curl --location --request POST 'http://localhost:8080/api/v1/orders/<ORDER_ID>/cancel'
```

Expected:

* HTTP `409 Conflict`
* `code = INVALID_ORDER_STATUS_TRANSITION`

## 10. Invalid UUID -> 400

```bash
curl --location 'http://localhost:8080/api/v1/orders/not-a-uuid'
```

Expected:

* HTTP `400 Bad Request`
* `code = INVALID_ORDER_ID`

## 11. Missing Order -> 404

```bash
curl --location 'http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000000'
```

Expected:

* HTTP `404 Not Found`
* `code = ORDER_NOT_FOUND`

## 12. Database Verification -> DELIVERED

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select id, seller_id, status, created_at, updated_at from orders where id = '<ORDER_ID>';"
```

Expected:

* one row returned
* `status = DELIVERED`
* `seller_id = 11111111-1111-1111-1111-111111111111`

## Error Response Shape

Lifecycle validation should return structured errors in this format:

```json
{
  "status": 409,
  "code": "INVALID_ORDER_STATUS_TRANSITION",
  "message": "Cannot transition order status from DELIVERED to CANCELLED",
  "path": "/api/v1/orders/<ORDER_ID>/cancel",
  "timestamp": "2026-06-27T12:00:00Z"
}
```
