# Fulfillment Orchestrator Platform

Senior-level Java/Spring Boot logistics fulfillment platform focused on order orchestration, fulfillment node assignment, event-driven architecture, resilience, observability, testing and AI-assisted engineering workflows.

## Project Goal

This project is designed as a senior backend engineering portfolio project.

It simulates a logistics fulfillment platform where orders are created, assigned to fulfillment nodes, processed through a lifecycle, published as domain events and monitored through observability tools.

The goal is not to build a simple CRUD application, but to demonstrate real backend engineering decisions around:

* Domain modeling
* Modular architecture
* Event-driven communication
* Idempotency
* Resilience patterns
* Testing strategy
* Observability
* Documentation
* AI-assisted development workflows

## Core Domain

The platform will model a simplified logistics operation:

* Sellers create orders.
* Orders are assigned to fulfillment nodes.
* Fulfillment nodes have capacity, working days and operational constraints.
* Orders move through a controlled lifecycle.
* Domain events are published for relevant state changes.
* Failures, retries and incidents are handled explicitly.

## Planned Tech Stack

* Java 21
* Spring Boot
* Spring Data JPA
* PostgreSQL
* Flyway
* Docker Compose
* Testcontainers
* JUnit 5
* Mockito
* Spring Boot Actuator
* OpenAPI / Swagger
* Kafka
* Redis
* Micrometer
* OpenTelemetry
* Prometheus
* Grafana
* GitHub Actions

## Current Implemented Stack

The current project scaffold includes:

* Java 21
* Spring Boot
* Gradle
* Spring Web MVC
* Spring Data JPA
* Bean Validation
* PostgreSQL Driver
* Flyway
* Spring Boot Actuator
* Docker Compose
* Testcontainers
* JUnit 5

Kafka, Redis, OpenTelemetry, Prometheus and Grafana are planned for later phases and are intentionally not included in the initial scaffold.

## Architecture Approach

The project starts as a modular monolith.

This is an intentional decision to validate domain boundaries before extracting services. The system will be organized into clear modules such as orders, fulfillment, working days, incidents and notifications.

Service extraction will only be considered when the module boundaries and communication patterns become stable.

See:

* `docs/adr/0001-use-modular-monolith-first.md`
* `docs/architecture/system-overview.md`

## AI-Assisted Engineering Workflow

This project is also used to document a professional AI-assisted engineering workflow.

AI tools may be used for:

* Feature planning
* Architecture alternatives
* Refactor suggestions
* Test strategy
* Documentation drafts
* Code review assistance
* PR summaries

Human-owned decisions include:

* Architecture
* Domain modeling
* Trade-offs
* Security boundaries
* Final code review
* Merge decisions

AI usage is documented in:

* `docs/ai-engineering-log/`

Reusable skills are documented in:

* `skills/`

## Running Locally

### Requirements

* Java 21
* Docker Desktop
* Git

### Start PostgreSQL

```bash
docker compose up -d postgres
```

PostgreSQL runs on the host using port `15432`:

```text
localhost:15432 -> container:5432
```

The container uses the following local development credentials:

```text
Database: fulfillment_orchestrator
Username: fulfillment_user
Password: fulfillment_password
```

### Validate PostgreSQL

```bash
docker ps
```

Expected result:

```text
fulfillment-orchestrator-postgres   Up ... (healthy)
```

You can also validate the database connection with:

```bash
docker exec -e PGPASSWORD=fulfillment_password fulfillment-orchestrator-postgres \
  psql -U fulfillment_user -d fulfillment_orchestrator \
  -c "select current_user, current_database();"
```

Expected result:

```text
current_user      | current_database
------------------+--------------------------
fulfillment_user  | fulfillment_orchestrator
```

### Run Tests

```bash
./gradlew clean test
```

The test suite uses Testcontainers to start an isolated PostgreSQL container for integration tests.

### Start the Application

```bash
./gradlew bootRun
```

The application starts on:

```text
http://localhost:8080
```

### Health Check

Using Git Bash:

```bash
curl http://localhost:8080/actuator/health
```

Using PowerShell:

```powershell
curl.exe http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### Stop the Application

If the application is running with `bootRun`, stop it with:

```text
CTRL + C
```

### Stop Local Services

```bash
docker compose down
```

To remove local database data:

```bash
docker compose down -v
```

## Database Migrations

Flyway is enabled and will manage database schema evolution.

Migration files should be added under:

```text
src/main/resources/db/migration/
```

Example:

```text
V1__create_initial_schema.sql
```

Hibernate is configured with:

```yaml
ddl-auto: validate
```

This means Hibernate validates the schema but does not create or update database tables automatically. Flyway is the source of truth for database schema changes.

## Testing Strategy

The project uses different types of tests:

* Unit tests for business logic.
* Integration tests for persistence and application context.
* Testcontainers for PostgreSQL-based integration tests.

The goal is to avoid relying only on mocks for persistence behavior and validate database-related behavior against a real PostgreSQL instance.

## Repository Workflow

This project follows a lightweight Gitflow workflow:

* `main`: stable public version
* `develop`: integration branch
* `feature/*`: feature branches
* Pull requests are required before merging into `develop`

## Current Status

The project documentation foundation has been created.

The Spring Boot application scaffold is initialized and can:

* Build successfully.
* Run tests successfully.
* Start locally with PostgreSQL through Docker Compose.
* Expose an Actuator health endpoint.

Domain implementation has not started yet.

## Next Steps

Planned next steps:

* Define the initial package/module structure.
* Add the Orders module.
* Add the first Flyway migration.
* Implement order creation.
* Add unit and integration tests for the Orders module.
* Document the first business feature through an issue and pull request.

## License

This project is licensed under the MIT License.
