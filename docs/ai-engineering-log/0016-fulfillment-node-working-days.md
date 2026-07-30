# AI Engineering Log 0016: Fulfillment Node Working Days

## Context

This feature adds weekly working-days storage and API management to fulfillment nodes.

Before this change, the Fulfillment module supported node creation, lookup, and listing, and the Orders module could allocate against active nodes with daily capacity checks. There was no persisted weekly operating-day configuration per node.

After this change, fulfillment nodes own a weekly working-days set with explicit GET and PUT contracts, seven-day compatibility defaults, deterministic ordering, normalized persistence, and structured API errors.

Allocation enforcement remains deferred to `FOP-WORKING-DAYS-002`.

## Scope

The feature adds:

* `GET /api/v1/fulfillment-nodes/{id}/working-days`
* `PUT /api/v1/fulfillment-nodes/{id}/working-days`
* `fulfillment_node_working_days` persistence through Flyway `V5`
* seven-day defaults for new and legacy nodes
* deterministic Monday-to-Sunday ordering in the API response

The feature intentionally does not add:

* allocation-time working-days enforcement
* holidays or exceptional dates
* per-node time zones or cutoffs
* automatic node selection
* a standalone calendar subsystem

## Architecture

The implementation keeps the current modular-monolith boundaries:

```text
API -> Application -> Domain -> Infrastructure -> PostgreSQL
```

Working days are currently owned by `FulfillmentNode` because they are node-scoped configuration, not yet a reusable cross-module calendar capability.

The API layer parses UUIDs and request bodies, the application layer coordinates GET and replacement use cases, the domain enforces non-null and non-empty set rules, and infrastructure owns the JPA mapping plus Flyway migration.

## Persistence Decision

The chosen schema is a normalized join table:

```text
fulfillment_node_working_days(fulfillment_node_id, day_of_week)
```

This avoided speculative designs such as JSON storage or a broader calendar engine. The migration backfills all existing nodes with seven rows so the feature is backward compatible from the start.

## API Contracts

Success responses return:

```json
{
  "id": "uuid",
  "workingDays": ["MONDAY", "FRIDAY", "SUNDAY"]
}
```

Error responses reuse the existing structured API envelope with codes such as:

* `INVALID_FULFILLMENT_NODE_ID`
* `FULFILLMENT_NODE_NOT_FOUND`
* `INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST`

The response ordering is deterministic even when the request ordering is not.

## Recovery History

The path to the final state required recovery work:

* Backend attempt 1 exited without an acceptable handoff, so no canonical PASS evidence was retained.
* Backend attempt 2 recovered the working implementation but also touched unauthorized paths, so the runner restored those disallowed changes and kept only the approved Fulfillment, migration, and Fulfillment-test surface.
* The first read-only testing gate then requested changes for two exact gaps: missing application-layer idempotency coverage for `ReplaceFulfillmentNodeWorkingDaysService`, and missing PUT invalid-UUID plus missing-node integration coverage for `/api/v1/fulfillment-nodes/{id}/working-days`.
* The targeted correction stage added only those missing tests and reran the focused classes plus the clean suite successfully.

## AI-Assisted Work

Codex assisted with:

* the original implementation structure
* the recovery-oriented documentation reconciliation
* contract and persistence explanations
* manual cURL guidance
* ledger evidence normalization

Human ownership remained on scope control, deferral decisions, and final acceptance boundaries.

## Human-Owned Decisions

The following decisions remained human-owned:

* keep weekly working days inside the Fulfillment module for this slice
* use a normalized join table and backfill every legacy node to all seven days
* keep GET and PUT as the only public contracts
* require whole-set replacement instead of partial mutation operations
* defer allocation enforcement to `FOP-WORKING-DAYS-002`
* avoid holidays, exceptional dates, cutoffs, time zones, and automatic fallback logic in this task

## Validation

Preserved recovery and correction evidence:

* `2026-07-27T19:16:10Z` focused recovery tests passed for the Fulfillment domain, working-days services, and controller integration coverage
* `2026-07-27T19:19:51Z` focused `OrderControllerIntegrationTest` passed after restoring legacy all-days compatibility for nodes without persisted working-days rows
* `2026-07-27T19:20:35Z` clean test suite passed after backend recovery
* `2026-07-27T19:38:32Z` the testing gate requested the two exact coverage fixes listed above
* `2026-07-27T20:25:57Z` focused correction rerun passed for `ReplaceFulfillmentNodeWorkingDaysServiceTest` and `FulfillmentNodeControllerIntegrationTest`
* `2026-07-27T20:26:48Z` clean test suite passed after the targeted correction
* `2026-07-27T20:32:46Z` containerized `git diff --check` passed for the mounted worktree
* `2026-07-30T17:15:51Z` documentation-stage read-only QA and security reconciliation confirmed the code, migration, and tests still match the documented contracts, persistence model, and deferred allocation behavior

## Trade-Off Summary

This slice prefers a small, durable contract over an ambitious scheduling subsystem. That choice kept the implementation easy to test, easy to explain in interviews, and compatible with the existing modular-monolith boundaries while preserving a clean next step for `FOP-WORKING-DAYS-002`.
