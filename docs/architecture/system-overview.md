# System Overview

The Fulfillment Orchestrator Platform is a backend system that simulates the orchestration of logistics orders in an ecommerce environment.

The system starts as a modular monolith. This means it is deployed as a single application, but internally organized around clear business capabilities.

## High-Level Responsibilities

The platform is responsible for:

* Receiving order creation requests.
* Persisting orders.
* Assigning orders to fulfillment nodes.
* Validating operational constraints.
* Managing order lifecycle transitions.
* Recording relevant business events.
* Preparing the system for future asynchronous communication.

## Initial Architecture Style

The initial architecture style is:

* Modular monolith
* Domain-oriented modules
* REST APIs
* PostgreSQL persistence
* Explicit business rules
* Testable application services

## Planned Modules

### Orders Module

Responsible for:

* Order creation.
* Order retrieval.
* Order lifecycle management.
* Order status validation.
* Order-related domain rules.

### Fulfillment Module

Responsible for:

* Fulfillment node selection.
* Node capacity validation.
* Logistics type support validation.
* Assignment strategy.

### Working Days Module

Responsible for:

* Node operating day configuration.
* Validating whether a node can operate on a given date.
* Preventing assignment to unavailable nodes.

### Incidents Module

Responsible for:

* Representing operational problems.
* Tracking failed assignments or processing issues.
* Supporting future incident-based workflows.

This module may remain minimal in the MVP.

### Notifications Module

Responsible for:

* Representing outbound communication needs.
* Preparing future event-driven notifications.

This module may remain minimal in the MVP.

## Dependency Direction

The project should avoid uncontrolled dependencies between modules.

Application services may coordinate use cases, but domain logic should remain inside the module that owns the business rule.

For example:

* Order lifecycle rules belong to the Orders module.
* Fulfillment assignment rules belong to the Fulfillment module.
* Operating day rules belong to the Working Days module.

## Initial Request Flow

Example: create and assign an order.

1. Client sends a request to create an order.
2. Orders module validates and creates the order.
3. Fulfillment module evaluates available nodes.
4. Working Days module validates node availability.
5. Fulfillment module assigns the selected node.
6. Orders module stores the resulting order state.
7. The system records the relevant domain event internally.

## Future Evolution

The system is designed to evolve toward:

* Kafka-based domain events.
* Outbox Pattern.
* Idempotent consumers.
* Resilience patterns.
* Observability.
* Potential service extraction.

Service extraction will only be considered after module boundaries are validated.

## Architecture Principle

The architecture should optimize for clarity first.

The system should be easy to understand, test and evolve before adding distributed complexity.
