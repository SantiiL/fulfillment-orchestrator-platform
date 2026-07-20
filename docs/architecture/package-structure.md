# Package Structure

The project follows a business-capability-oriented package structure.

Instead of organizing code only by technical layers such as `controller`, `service`, `repository` and `entity`, the application is organized around business modules.

## Main Modules

Initial modules:

- `orders`
- `fulfillment`
- `workingdays`
- `incidents`
- `notifications`
- `shared`

In the current codebase, only `orders` and `fulfillment` contain implemented business slices. `workingdays`, `incidents`, and `notifications` currently exist as placeholder package boundaries for planned modules. For the authoritative current-state architecture, use [current-architecture.md](./current-architecture.md).

Each business module may contain internal subpackages:

- `api`
- `application`
- `domain`
- `infrastructure`

## Subpackage Responsibilities

### api

Contains inbound adapters such as REST controllers, request DTOs and response DTOs.

The `api` package should not contain business rules.

### application

Contains use cases and application services.

Application services coordinate workflows between domain objects, repositories and other modules.

They should orchestrate behavior but avoid becoming a place where all business logic accumulates.

### domain

Contains business concepts, entities, value objects, domain services, policies and business rules.

This package should be as independent as possible from frameworks and infrastructure details.

### infrastructure

Contains persistence adapters, JPA entities, repository implementations, external clients and technical configuration related to the module.

Infrastructure code may depend on frameworks. Domain code should not depend on infrastructure code.

### shared

Contains cross-cutting concepts that are truly reusable across modules.

The shared package should be kept small to avoid becoming a dumping ground.

## Why Package by Business Capability?

A technical-layer structure can become harder to maintain as the system grows.

For example:

```text
controller/
service/
repository/
entity/