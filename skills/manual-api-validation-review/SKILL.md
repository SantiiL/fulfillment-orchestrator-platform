# Manual API Validation Review Skill

## Purpose

Use this skill whenever implementing or reviewing an API-facing feature.

The goal is to ensure that every API feature is validated not only with automated tests, but also with manual cURL examples that can be imported into Postman or executed from the terminal.

## When To Use

Use this skill for any task that adds or changes:

* REST endpoints
* Request or response DTOs
* API error responses
* HTTP status behavior
* Controller logic
* Application use cases exposed through HTTP
* Persistence behavior that should be manually verified through the API

## Requirements

For every API-facing implementation, the final response must include:

1. Automated test summary
2. Manual validation cURLs
3. Expected responses
4. Error scenario cURLs
5. Database verification cURL or SQL command when persistence is involved

## Manual Validation cURL Requirements

The final summary must include cURLs for:

* Happy path request
* Retrieval request when applicable
* Invalid UUID or invalid input scenario
* Not found scenario when applicable
* Invalid lifecycle transition or conflict scenario when applicable
* Database verification command when the feature persists or updates data

## Postman Compatibility

cURLs should be written in a way that can be imported into Postman.

Prefer this format:

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"sellerId":"2f9f6f5e-8b3d-4db7-9d3a-4c5f2a6e9d11"}'
```

## Expected Final Summary Format

At the end of the implementation, include a section like:

```text
Manual validation cURLs used/recommended:

1. Create order
<curl>

Expected:
HTTP 201 Created
status = CREATED

2. Execute feature endpoint
<curl>

Expected:
HTTP 200 OK
status = EXPECTED_STATUS

3. Retrieve order
<curl>

Expected:
HTTP 200 OK
status = EXPECTED_STATUS

4. Invalid UUID
<curl>

Expected:
HTTP 400 Bad Request
code = INVALID_ORDER_ID

5. Not found
<curl>

Expected:
HTTP 404 Not Found
code = ORDER_NOT_FOUND

6. Invalid transition or conflict
<curl>

Expected:
HTTP 409 Conflict
code = INVALID_ORDER_STATUS_TRANSITION

7. Database verification
<command>

Expected:
Database row reflects the expected status/update.
```

## Review Checklist

Before considering the task complete, verify:

* Automated tests pass.
* cURLs are included in the final summary.
* cURLs match the implemented endpoints.
* Expected HTTP statuses are documented.
* Expected business error codes are documented.
* Manual validation includes both success and failure scenarios.
* Persistence changes are verified with a DB query when relevant.

## Important Notes

Do not replace automated tests with manual cURL validation.

Manual cURLs are an additional validation layer to make the feature easier to verify locally and through Postman.

Do not invent endpoints that were not implemented.

Do not include unrelated cURLs outside the feature scope.
