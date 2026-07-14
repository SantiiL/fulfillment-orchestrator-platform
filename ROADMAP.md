# Roadmap

This roadmap tracks the evolution of the Fulfillment Orchestrator Platform from the current modular monolith milestone toward richer logistics orchestration capabilities.

## Completed

The first Orders and Fulfillment orchestration milestone is complete.

* Project scaffold
* Orders domain foundation
* Create order
* Get order by ID
* Cancel order
* Allocate order
* Mark order ready to ship
* Dispatch order
* Deliver order
* Orders lifecycle cleanup/refactor
* Fulfillment Node foundation
* Order Fulfillment Assignment
* Capacity-Aware Allocation
* Persistence of assigned node and allocation timestamp
* Manual validation of lifecycle and capacity scenarios

## Current State

The repository now contains:

* a framework-free Orders domain lifecycle
* a Fulfillment Node catalog
* order assignment to active fulfillment nodes
* UTC-day capacity validation during allocation
* REST APIs, application services, and PostgreSQL persistence with Flyway
* structured API error responses
* unit tests, integration tests, and manual cURL validation

## Next Milestones

* Working Days / Business Calendar
* Operational availability rules
* Incidents / Failure Handling
* Domain Events and Transactional Outbox
* Idempotency
* Observability
* CI/CD

## Direction

The project will keep growing as a modular monolith until the business boundaries and communication patterns are stable enough to justify more distributed architecture work.
