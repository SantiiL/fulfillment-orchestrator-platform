# Fulfillment Orchestrator Platform

Senior-level Java 21 / Spring Boot logistics fulfillment platform built as a modular monolith. The current milestone delivers a full first version of the Orders lifecycle with explicit domain rules, PostgreSQL persistence, structured API errors, unit tests, integration tests, and manual API validation.

This repository is intentionally not a CRUD demo. The Orders module models business transitions through domain behavior, keeps orchestration in the application layer, isolates JPA in infrastructure, and validates the full HTTP-to-database flow with Testcontainers-backed integration tests.

## Current Capabilities

* Create an order for a seller.
* Retrieve an order by ID.
* Allocate an order.
* Mark an allocated order as ready to ship.
* Dispatch a ready-to-ship order.
* Deliver a dispatched order.
* Cancel an order only from lifecycle states allowed by the domain policy.
* Return structured API errors with `status`, `code`, `message`, `path`, and `timestamp`.
* Persist orders in PostgreSQL with Flyway-managed schema changes.
* Validate the module with pure unit tests and integration tests.

## Tech Stack

* Java 21
* Spring Boot
* Spring Web MVC
* Spring Data JPA
* Bean Validation
* PostgreSQL
* Flyway
* Docker Compose
* Testcontainers
* JUnit 5
* Gradle

## Architecture Summary

The project starts as a modular monolith organized by business capability. The first implemented business module is `orders`.

The current request flow is:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

Within the Orders module:

* `api` handles HTTP requests, responses, and API error shaping.
* `application` coordinates use cases and repository ports.
* `domain` owns lifecycle rules and behavior methods such as `order.allocate()` and `order.deliver()`.
* `infrastructure` contains JPA persistence adapters and database mapping.

The domain layer remains framework-free. Spring, JPA, and web annotations stay out of the core business model.

## Orders Lifecycle Overview

Primary lifecycle:

```text
CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED
```

Cancellation is currently allowed from:

* `CREATED`
* `ALLOCATED`
* `READY_TO_SHIP`

Terminal states:

* `DELIVERED`
* `CANCELLED`

Invalid transitions are rejected by the domain policy and returned by the API as `409 Conflict` with the business error code `INVALID_ORDER_STATUS_TRANSITION`.

## Available API Endpoints

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/orders` | Create order | `201 Created` |
| `GET` | `/api/v1/orders/{id}` | Get order by ID | `200 OK` |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel order | `200 OK` |
| `POST` | `/api/v1/orders/{id}/allocate` | Allocate order | `200 OK` |
| `POST` | `/api/v1/orders/{id}/ready-to-ship` | Mark order ready to ship | `200 OK` |
| `POST` | `/api/v1/orders/{id}/dispatch` | Dispatch order | `200 OK` |
| `POST` | `/api/v1/orders/{id}/deliver` | Deliver order | `200 OK` |

## Run Locally

### Requirements

* Java 21
* Docker Desktop

### Start PostgreSQL

```bash
docker compose up -d postgres
```

Local database connection:

```text
Host: localhost
Port: 15432
Database: fulfillment_orchestrator
Username: fulfillment_user
Password: fulfillment_password
```

### Start the Application

```bash
./gradlew bootRun
```

Windows PowerShell equivalent:

```powershell
.\gradlew.bat bootRun
```

Application base URL:

```text
http://localhost:8080
```

Optional health check:

```bash
curl http://localhost:8080/actuator/health
```

### Stop Local Services

```bash
docker compose down
```

Remove local database data:

```bash
docker compose down -v
```

## Run Tests

```bash
./gradlew clean test
```

Windows PowerShell equivalent:

```powershell
.\gradlew.bat clean test
```

The automated test suite uses Testcontainers for integration tests, so it does not require a manually running local PostgreSQL container.

## Manual API Validation

Manual lifecycle validation cURLs are documented in:

* [docs/api/orders-manual-validation.md](docs/api/orders-manual-validation.md)

That guide covers:

* full lifecycle progression from `CREATED` to `DELIVERED`
* invalid transition checks
* invalid UUID and missing-order cases
* direct PostgreSQL verification

## Documentation

* [README.md](README.md)
* [ROADMAP.md](ROADMAP.md)
* [docs/orders/orders-lifecycle.md](docs/orders/orders-lifecycle.md)
* [docs/architecture/current-architecture.md](docs/architecture/current-architecture.md)
* [docs/architecture/system-overview.md](docs/architecture/system-overview.md)
* [docs/architecture/package-structure.md](docs/architecture/package-structure.md)
* [docs/api/orders-manual-validation.md](docs/api/orders-manual-validation.md)
* [docs/adr/0001-use-modular-monolith-first.md](docs/adr/0001-use-modular-monolith-first.md)
* [docs/ai-engineering-log/](docs/ai-engineering-log/)

## Current Project Status

The first real business milestone is complete: the Orders module now supports the full initial lifecycle and persistence flow end to end.

Implemented today:

* modular Orders domain model and lifecycle policy
* REST endpoints for creation, retrieval, cancellation, allocation, ready-to-ship, dispatch, and delivery
* PostgreSQL persistence with Flyway
* structured API error responses
* lifecycle-focused unit and integration tests
* manual validation cURLs
* post-lifecycle cleanup/refactor

## Next Roadmap Items

* Fulfillment node assignment
* Working days / business rules
* Incidents / failure handling
* Domain events and outbox
* Idempotency
* Observability
* CI/CD

## License

This project is licensed under the MIT License.
