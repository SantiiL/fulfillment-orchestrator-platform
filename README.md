# Fulfillment Orchestrator Platform

Senior-level Java 21 / Spring Boot logistics fulfillment platform built as a modular monolith. The current milestone delivers a complete first Orders lifecycle, a Fulfillment Node catalog, weekly working-days configuration per node, manual assignment of orders to active nodes, and capacity-aware allocation backed by PostgreSQL, Flyway, structured API errors, unit tests, integration tests, and manual cURL validation.

This repository is intentionally not a CRUD demo. The core allocation flow is now a real orchestration step:

```text
Order + Fulfillment Node + Active validation + Capacity validation -> ALLOCATED
```

## Current Capabilities

* Create, get, and progress orders through `CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED`.
* Cancel orders only from lifecycle states allowed by the domain policy.
* Create, get, and list fulfillment nodes.
* Get and replace fulfillment-node working days through `GET` and `PUT /api/v1/fulfillment-nodes/{id}/working-days`.
* Default new and legacy fulfillment nodes to all seven days.
* Return working days in deterministic Monday-to-Sunday order.
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
* allocation enforcement against configured working days
* holidays, date-specific calendars, cutoff times, or per-node time zones
* capacity reservation tables

## Available API Endpoints

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/fulfillment-nodes` | Create fulfillment node | `201 Created` |
| `GET` | `/api/v1/fulfillment-nodes/{id}` | Get fulfillment node by ID | `200 OK` |
| `GET` | `/api/v1/fulfillment-nodes` | List fulfillment nodes | `200 OK` |
| `GET` | `/api/v1/fulfillment-nodes/{id}/working-days` | Get fulfillment node working days | `200 OK` |
| `PUT` | `/api/v1/fulfillment-nodes/{id}/working-days` | Replace fulfillment node working days | `200 OK` |
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

Important working-days error codes:

* `INVALID_FULFILLMENT_NODE_ID`
* `FULFILLMENT_NODE_NOT_FOUND`
* `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`

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

## Continuous Integration

GitHub Actions runs the repository CI workflow from [`.github/workflows/ci.yml`](.github/workflows/ci.yml) on:

* pushes to `main` and `develop`
* pull requests targeting `main` and `develop`
* manual `workflow_dispatch`

The workflow runs on `ubuntu-latest`, uses immutable action pins for `actions/checkout` (`d23441a48e516b6c34aea4fa41551a30e30af803`, `v6`), `actions/setup-java` (`03ad4de0992f5dab5e18fcb136590ce7c4a0ac95`, `v5`), `gradle/actions/setup-gradle` (`3f131e8634966bd73d06cc69884922b02e6faf92`, `v6`), and `actions/upload-artifact` (`043fb46d1a93c77aae656e7c1c64a875d1fc6a0a`, `v7`). It installs Temurin Java 21, performs Gradle wrapper validation through `gradle/actions/setup-gradle` with `validate-wrappers: true`, primes Gradle caching, verifies that `./gradlew` is executable, and syntax-checks `scripts/agentic/*.sh` before running:

```bash
./gradlew clean test --no-daemon
```

There is no separate wrapper-validation action. Integration tests use Testcontainers against the GitHub-hosted Docker daemon to provision PostgreSQL during the test run, so the workflow does not need a separate PostgreSQL service container. When tests fail, CI uploads `build/reports/tests/test` and `build/test-results/test` as `gradle-test-artifacts` for 7 days to support diagnosis without broadening the workflow permissions.

## Manual API Validation

Manual cURL validation is documented in:

* [docs/api/orders-manual-validation.md](docs/api/orders-manual-validation.md)
* [docs/agentic/deterministic-specialist-runner.md](docs/agentic/deterministic-specialist-runner.md)

That guide covers:

* fulfillment node creation and listing
* fulfillment node working-days default, replacement, error handling, and PostgreSQL verification
* capacity-aware allocation success and failure paths
* lifecycle progression after allocation
* invalid UUID, missing-request-data, and not-found cases
* direct PostgreSQL verification of `fulfillment_node_id`, `allocated_at`, and normalized `fulfillment_node_working_days` rows

## Agentic Development Workflow

Work enters the repository through a task intake step that assigns a dedicated branch, an isolated worktree, and the smallest writer set needed for the task. Agents are selected according to task scope, so not every agent runs for every task.

Delivery then follows this path:

1. A task is prepared in its own worktree so repository writers do not overlap.
2. Specialist agents are routed only when their role matches the work, such as infrastructure, implementation, testing, or documentation.
3. Writers complete the task, record evidence in `.agentic/runs/`, and update supporting documentation such as AI engineering logs when relevant.
4. Quality gates check the change before review, including the validations appropriate to the task.
5. An independent reviewer must be outside the full writer set.
6. A human must approve the change before merge.

Autonomous merge is not enabled in this repository. The authoritative description of currently implemented behavior lives in [docs/architecture/current-architecture.md](docs/architecture/current-architecture.md); roadmap documents describe planned scope unless that behavior is also confirmed there.

The repository now also carries the source and governance documents for a deterministic specialist runner. The final documented implementation uses real fail-closed Landlock write confinement for specialist children, limits writes to the validated task worktree plus per-attempt runtime artifacts, and blocks directory-symlink write-through attempts without mutating external targets. The runner is still inactive for normal tasks until post-merge external installation, runtime-contract manifest regeneration, and installed-runtime verification complete. Operator details and the activation boundary are documented in [docs/agentic/deterministic-specialist-runner.md](docs/agentic/deterministic-specialist-runner.md).

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
* CD / deployment automation

## License

This project is licensed under the MIT License.
