# Orders Lifecycle Refactor Review Skill

## Purpose

Use this skill after completing the first full Orders lifecycle.

The goal is to improve maintainability, reduce duplication and clean up the codebase without changing behavior or weakening the architecture.

## Context

The Orders module supports:

* POST /api/v1/orders
* GET /api/v1/orders/{id}
* POST /api/v1/orders/{id}/cancel
* POST /api/v1/orders/{id}/allocate
* POST /api/v1/orders/{id}/ready-to-ship
* POST /api/v1/orders/{id}/dispatch
* POST /api/v1/orders/{id}/deliver

The expected lifecycle is:

```text
CREATED -> ALLOCATED -> READY_TO_SHIP -> DISPATCHED -> DELIVERED
```

Cancellation is allowed only from statuses where the domain lifecycle policy allows it.

## Refactor Goals

Look for opportunities to:

* Reduce duplicated controller response mapping.
* Reduce duplicated endpoint orchestration when it is safe and readable.
* Reduce duplicated integration test setup.
* Reduce duplicated structured error assertions.
* Reduce duplicated SQL insert logic in tests.
* Reduce repeated fake repository logic in application tests only if the result stays simple.
* Improve method names and local readability.
* Keep classes from growing unnecessarily large.
* Extract private helpers when duplication is local to one class.
* Extract small test fixtures or builders only when they clearly improve readability.

## Rules

Do not change behavior.

Do not change API contracts.

Do not change endpoint paths.

Do not change HTTP status codes.

Do not change error response shape.

Do not change domain lifecycle rules.

Do not change persistence schema.

Do not introduce new infrastructure.

Do not introduce generic Utils classes unless there is a clear multi-class need.

Prefer private helper methods when duplication exists only inside one class.

Do not move domain behavior into API, application or persistence layers.

Do not add Spring, JPA, Hibernate or web dependencies to the domain layer.

Keep API DTOs separate from application results.

Keep the command/use case/service pattern unless there is a project-wide decision to change it.

## Good Refactor Examples

Good examples:

```java
private GetOrderResponse toResponse(OrderResult result) {
    return new GetOrderResponse(result.id(), result.sellerId(), result.status());
}
```

```java
private void assertOrderError(ResultActions result, HttpStatus status, String code) {
    result
        .andExpect(status().is(status.value()))
        .andExpect(jsonPath("$.status").value(status.value()))
        .andExpect(jsonPath("$.code").value(code));
}
```

```java
private void insertOrder(UUID id, UUID sellerId, String status) {
    jdbcTemplate.update(
        "insert into orders (id, seller_id, status, created_at, updated_at) values (?, ?, ?, now(), now())",
        id,
        sellerId,
        status
    );
}
```

## Bad Refactor Examples

Avoid broad generic utility classes like:

```java
OrderUtils
ControllerUtils
TestUtils
LifecycleUtils
```

unless the duplication clearly spans multiple classes and the utility has a precise responsibility.

Avoid hiding simple code behind abstractions that make tests harder to read.

Avoid creating mapper classes just because one response is created in a few places.

Avoid reducing line count at the cost of clarity.

## Review Checklist

Before finishing the refactor, verify:

* All tests pass.
* The domain layer remains framework-free.
* No status transition bypasses domain methods.
* No public status setter was introduced.
* API responses remain unchanged.
* Error responses remain unchanged.
* Integration tests still cover success, 400, 404 and 409 cases.
* Manual cURL regression flow is included in the final summary.
* Any extracted helper has a clear reason to exist.
* Any duplication intentionally left in place is explained.

## Final Summary Requirements

The final summary must include:

* Files changed.
* Refactors applied.
* Why each helper or extracted method exists.
* Duplication intentionally left in place.
* Tests executed.
* Full lifecycle regression cURLs.
* Any risk or manual review note.
