# Roadmap

This roadmap defines the planned evolution of the Fulfillment Orchestrator Platform.

The goal is to grow the project incrementally, starting with a modular monolith and evolving toward more advanced backend engineering concerns such as event-driven communication, resilience, observability and service extraction.

## Phase 1: Project Foundation

Goal: establish the project structure, documentation standards and engineering workflow.

Planned work:

* Define project scope.
* Define MVP boundaries.
* Document the initial architecture.
* Create Architecture Decision Records.
* Document the AI-assisted engineering workflow.
* Define contribution and pull request standards.
* Create reusable engineering skills.

Expected outcome:

A clear technical foundation before implementation starts.

## Phase 2: Modular Monolith MVP

Goal: implement the first working version of the fulfillment platform as a modular monolith.

Planned work:

* Create the Spring Boot application.
* Define core modules:

  * orders
  * fulfillment
  * working days
  * incidents
  * notifications
* Implement order creation.
* Implement order lifecycle transitions.
* Implement fulfillment node assignment.
* Implement working day validation.
* Add PostgreSQL persistence.
* Add unit tests.
* Add integration tests with Testcontainers.
* Add OpenAPI documentation.
* Add Docker Compose for local development.

Expected outcome:

A working backend application with clear module boundaries and tested business logic.

## Phase 3: Event-Driven Capabilities

Goal: introduce asynchronous communication and domain events.

Planned work:

* Define domain events.
* Publish events when relevant state changes happen.
* Add Kafka for asynchronous messaging.
* Implement the Outbox Pattern.
* Add idempotent event consumers.
* Add retry and failure handling.
* Add event-related integration tests.

Expected outcome:

A more realistic backend system where modules can react to business events without tight coupling.

## Phase 4: Resilience and Observability

Goal: make the system easier to operate, monitor and debug.

Planned work:

* Add structured logging.
* Add correlation IDs.
* Add health checks.
* Add metrics with Micrometer.
* Add distributed tracing with OpenTelemetry.
* Add Prometheus and Grafana.
* Add circuit breaker around external provider simulations.
* Add fallback behavior for provider failures.

Expected outcome:

A system that demonstrates production-oriented backend concerns such as debugging, monitoring and graceful failure handling.

## Phase 5: Architecture Evolution

Goal: evaluate whether some modules should be extracted into independent services.

Planned work:

* Review module boundaries.
* Identify high-change or high-complexity modules.
* Evaluate service extraction candidates.
* Document trade-offs in ADRs.
* Extract a service only if the technical justification is strong.
* Keep the system runnable locally.

Expected outcome:

A documented architecture evolution path based on real trade-offs, not premature microservice adoption.

## Phase 6: Portfolio Hardening

Goal: prepare the project to be reviewed by recruiters and interviewers.

Planned work:

* Improve README documentation.
* Add architecture diagrams.
* Add example API requests.
* Add Postman collection.
* Add GitHub Actions CI.
* Add code coverage reporting.
* Add demo data.
* Add technical interview notes.
* Add project summary for CV and LinkedIn.

Expected outcome:

A polished senior backend portfolio project that can be discussed confidently in interviews.
