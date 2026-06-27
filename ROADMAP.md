# Roadmap

This roadmap tracks the evolution of the Fulfillment Orchestrator Platform from the current modular monolith milestone toward richer logistics orchestration capabilities.

## Completed

The first Orders lifecycle milestone is complete.

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
* Manual cURL validation process

## Current State

The repository now contains a working Orders module with:

* framework-free domain lifecycle rules
* application use cases and services
* REST API endpoints
* PostgreSQL persistence with Flyway
* structured API error responses
* unit and integration tests

## Next Milestones

* Fulfillment Node Assignment
* Working Days / Business Rules
* Incidents / Failure Handling
* Domain Events and Outbox
* Idempotency
* Observability
* CI/CD

## Direction

The project will keep growing as a modular monolith until the business boundaries and communication patterns are stable enough to justify more distributed architecture work.
