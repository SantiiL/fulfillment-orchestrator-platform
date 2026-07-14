# Fulfillment Orchestrator Platform

Senior-level Java 21 / Spring Boot logistics fulfillment platform built as a modular monolith. The current milestone delivers a complete first Orders lifecycle, a Fulfillment Node catalog, manual assignment of orders to active nodes, and capacity-aware allocation backed by PostgreSQL, Flyway, structured API errors, unit tests, integration tests, and manual cURL validation.

This repository is intentionally not a CRUD demo. The core allocation flow is now a real orchestration step:

```text
Order + Fulfillment Node + Active validation + Capacity validation -> ALLOCATED
```

## Current Capabilities

* Create, get, and progress orders through `CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED`.
* Cancel orders only from lifecycle states allowed by the domain policy.
* Create, get, and list fulfillment nodes.
* Allocate an order to an existing active fulfillment node with `POST /api/v1/orders/{id}/allocate`.
* Validate `maxDailyCapacity` against the current UTC day and return `409 FULFILLMENT_NODE_CAPACITY_EXCEEDED` when the node is full.
* Persist `fulfillment_node_id` and `allocated_at` when allocation succeeds.
* Return structured API errors with `status`, `code`, `message`, `path`, and `timestamp`.
* Persist state in PostgreSQL with Flyway-managed schema changes.
* Validate domain and application behavior with unit tests and the full HTTP-to-database flow with Testcontainers-backed integration tests.

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

The project starts as a modular monolith organized by business capability. The currently implemented business modules are `orders` and `fulfillment`.

The request flow is:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

Within that flow:

* `api` handles HTTP requests, responses, and API error shaping.
* `application` coordinates use cases and cross-module orchestration.
* `domain` owns business behavior and stays free from Spring, JPA, and web annotations.
* `infrastructure` contains JPA entities, Spring Data repositories, and persistence adapters.

Allocation is the most explicit cross-module orchestration path today:

1. The Orders API receives `orderId` plus `fulfillmentNodeId`.
2. `AllocateOrderService` loads the order and fulfillment node.
3. The application layer validates that the node exists, is active, and still has remaining UTC-day capacity.
4. The domain transitions the order with `order.allocate(...)`.
5. Persistence stores the new order status plus `fulfillment_node_id` and `allocated_at`.

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

Allocation is no longer a status-only transition. `POST /api/v1/orders/{id}/allocate` now requires:

* an existing order
* an existing fulfillment node
* an active fulfillment node
* a valid `CREATED -> ALLOCATED` lifecycle transition
* remaining node capacity for the current UTC day

When allocation succeeds, the order response includes `assignedFulfillmentNodeId`, and persistence stores `fulfillment_node_id` plus `allocated_at`.

Current capacity behavior:

* capacity is counted per fulfillment node
* the day boundary is UTC
* capacity is consumed when allocation succeeds
* later lifecycle transitions do not release capacity
* cancelled or delivered orders still count for that allocation day
* legacy orders with `allocated_at = null` remain valid

Not implemented yet:

* automatic node selection
* working days or cutoff times
* capacity reservation tables

## Available API Endpoints

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/fulfillment-nodes` | Create fulfillment node | `201 Created` |
| `GET` | `/api/v1/fulfillment-nodes/{id}` | Get fulfillment node by ID | `200 OK` |
| `GET` | `/api/v1/fulfillment-nodes` | List fulfillment nodes | `200 OK` |
| `POST` | `/api/v1/orders` | Create order | `201 Created` |
| `GET` | `/api/v1/orders/{id}` | Get order by ID | `200 OK` |
| `POST` | `/api/v1/orders/{id}/allocate` | Allocate order to a fulfillment node | `200 OK` |
| `POST` | `/api/v1/orders/{id}/ready-to-ship` | Mark order ready to ship | `200 OK` |
| `POST` | `/api/v1/orders/{id}/dispatch` | Dispatch order | `200 OK` |
| `POST` | `/api/v1/orders/{id}/deliver` | Deliver order | `200 OK` |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel order | `200 OK` |

Important allocation error codes:

* `FULFILLMENT_NODE_NOT_FOUND`
* `FULFILLMENT_NODE_INACTIVE`
* `FULFILLMENT_NODE_CAPACITY_EXCEEDED`
* `INVALID_ORDER_STATUS_TRANSITION`

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

Manual cURL validation is documented in:

* [docs/api/orders-manual-validation.md](docs/api/orders-manual-validation.md)

That guide covers:

* fulfillment node creation and listing
* capacity-aware allocation success and failure paths
* lifecycle progression after allocation
* invalid UUID, missing-request-data, and not-found cases
* direct PostgreSQL verification of `fulfillment_node_id` and `allocated_at`

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

The current milestone is complete:

* complete Orders lifecycle
* Fulfillment Node foundation
* manual assignment of orders to active fulfillment nodes
* capacity-aware allocation with UTC-day validation
* persistence of assigned node and allocation timestamp
* structured API errors, unit tests, integration tests, and manual validation

## Next Roadmap Items

* Working Days / Business Calendar
* Operational availability rules
* Incidents / Failure Handling
* Domain Events and Transactional Outbox
* Idempotency
* Observability
* CI/CD

## License

This project is licensed under the MIT License.
