# MVP Scope

This document defines the scope of the first working version of the Fulfillment Orchestrator Platform.

The MVP is intentionally limited. The goal is to validate the core domain and architecture before introducing distributed system complexity.

> [!WARNING]
> Roadmap requirements in this document describe target MVP scope and must not be interpreted as current production behavior.
>
> * Weekly working-days configuration is implemented for fulfillment nodes, but allocation enforcement against those configured days remains future-state scope until it is promoted into [../architecture/current-architecture.md](../architecture/current-architecture.md).
> * Modules absent from the current architecture document, code, and tests are not currently implemented.
> * When this roadmap and the current implementation differ, [../architecture/current-architecture.md](../architecture/current-architecture.md) is the authoritative source for implemented behavior.

## MVP Goal

Build a working backend application that can:

* Create orders.
* Store orders in PostgreSQL.
* Assign orders to fulfillment nodes.
* Configure weekly fulfillment-node working days.
* Move orders through a controlled lifecycle.
* Expose REST APIs.
* Document the HTTP API and manual validation flow.
* Include unit and integration tests.
* Run locally with Docker Compose.

## In Scope

### Order Management

The system must support:

* Creating an order.
* Retrieving an order by ID.
* Listing orders.
* Updating order status through controlled transitions.
* Preventing invalid state transitions.

Initial order statuses:

* `CREATED`
* `ALLOCATED`
* `READY_TO_SHIP`
* `DISPATCHED`
* `DELIVERED`
* `CANCELLED`

### Fulfillment Node Assignment

The system must support assigning an order to a fulfillment node based on simple business rules.

Current implemented assignment criteria:

* Node is active.
* Node has available capacity.

Planned next assignment criteria:

* Node operates on the requested day.
* Node supports the requested logistics type.

### Working Days

The system must support configuring the weekly operating days of a fulfillment node.

Current implemented rules:

* Nodes own a weekly working-days set.
* New and legacy nodes default to all seven days.
* Working days can be queried and replaced through the Fulfillment API.
* Responses are returned in deterministic Monday-to-Sunday order.

Planned next rule:

* Orders cannot be assigned to nodes that are closed on the requested date.

### Persistence

The MVP must use PostgreSQL for persistence.

Initial persisted entities:

* orders
* fulfillment nodes
* working day configurations

### Testing

The MVP must include:

* Unit tests for domain logic.
* Integration tests for persistence.
* API tests for main use cases.
* Testcontainers for PostgreSQL integration tests.

### Documentation

The MVP must include:

* README updates.
* API endpoint and manual validation documentation.
* Architecture overview.
* ADRs for major decisions.
* AI-assisted engineering logs for relevant implementation tasks.

## Out of Scope

The following items are intentionally excluded from the MVP:

* Real microservices.
* Kafka.
* Redis.
* Full authentication and authorization.
* Real external provider integrations.
* Kubernetes.
* Production deployment.
* Payment processing.
* Real geolocation or routing.
* Advanced optimization algorithms.
* Distributed tracing.
* Circuit breakers.

These items may be introduced in later phases after the core domain is stable.

## Success Criteria

The MVP is considered successful when:

* The application can be started locally.
* Orders can be created through the API.
* Orders are persisted in PostgreSQL.
* Orders can be assigned to valid fulfillment nodes.
* Fulfillment-node working days can be queried and replaced.
* Invalid node assignments are rejected.
* Invalid order state transitions are rejected.
* Tests run successfully in CI.
* The project documentation explains the main technical decisions.

## Design Principle

The MVP should be simple but not simplistic.

The goal is not to implement every possible logistics feature. The goal is to create a strong foundation that can evolve toward more advanced backend concerns.
