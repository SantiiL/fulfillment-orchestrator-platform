\# Fulfillment Orchestrator Platform



Senior-level Java/Spring Boot logistics fulfillment platform focused on order orchestration, fulfillment node assignment, event-driven architecture, resilience, observability, testing and AI-assisted engineering workflows.



\## Project Goal



This project is designed as a senior backend engineering portfolio project.



It simulates a logistics fulfillment platform where orders are created, assigned to fulfillment nodes, processed through a lifecycle, published as domain events and monitored through observability tools.



The goal is not to build a simple CRUD application, but to demonstrate real backend engineering decisions around:



\- Domain modeling

\- Modular architecture

\- Event-driven communication

\- Idempotency

\- Resilience patterns

\- Testing strategy

\- Observability

\- Documentation

\- AI-assisted development workflows



\## Core Domain



The platform will model a simplified logistics operation:



\- Sellers create orders.

\- Orders are assigned to fulfillment nodes.

\- Fulfillment nodes have capacity, working days and operational constraints.

\- Orders move through a controlled lifecycle.

\- Domain events are published for relevant state changes.

\- Failures, retries and incidents are handled explicitly.



\## Planned Tech Stack



\- Java 21

\- Spring Boot

\- Spring Data JPA

\- PostgreSQL

\- Kafka

\- Redis

\- Docker Compose

\- Testcontainers

\- JUnit 5

\- Mockito

\- OpenAPI / Swagger

\- Micrometer

\- OpenTelemetry

\- Prometheus

\- Grafana

\- GitHub Actions



\## Architecture Approach



The project starts as a modular monolith.



This is an intentional decision to validate domain boundaries before extracting services. The system will be organized into clear modules such as orders, fulfillment, working days, incidents and notifications.



Service extraction will only be considered when the module boundaries and communication patterns become stable.



See:



\- `docs/adr/0001-use-modular-monolith-first.md`

\- `docs/architecture/system-overview.md`



\## AI-Assisted Engineering Workflow



This project is also used to document a professional AI-assisted engineering workflow.



AI tools may be used for:



\- Feature planning

\- Architecture alternatives

\- Refactor suggestions

\- Test strategy

\- Documentation drafts

\- Code review assistance

\- PR summaries



Human-owned decisions include:



\- Architecture

\- Domain modeling

\- Trade-offs

\- Security boundaries

\- Final code review

\- Merge decisions



AI usage is documented in:



\- `docs/ai-engineering-log/`



Reusable skills are documented in:



\- `skills/`



\## Current Status



Project documentation foundation is being created.



Implementation has not started yet.



\## Repository Workflow



This project follows a lightweight Gitflow workflow:



\- `main`: stable public version

\- `develop`: integration branch

\- `feature/\*`: feature branches

\- Pull requests are required before merging into `develop`



\## License



This project is licensed under the MIT License.

