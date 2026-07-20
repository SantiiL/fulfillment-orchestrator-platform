# AI Engineering Log 0013: Agentic Documentation Governance

## Context

This change aligns the repository documentation with the agentic delivery workflow already described in `.agentic/`.

The goal is to make the current operating model explicit in the top-level documentation and to prevent roadmap language from being mistaken for implemented behavior.

## Scope

This documentation-only task updates:

* `README.md`
* `docs/architecture/current-architecture.md`
* `docs/architecture/package-structure.md`
* `docs/architecture/system-overview.md`
* `docs/roadmap/mvp-scope.md`
* `.agentic/runs/FOP-AGENTIC-001.md`

The change adds:

* a concise README section describing task intake, isolated worktrees, specialist routing, quality gates, independent review, and mandatory human approval
* explicit guidance that agents are selected by task scope and not every agent participates in every task
* explicit guidance that autonomous merge is not enabled
* stronger linkage from roadmap language to the current architecture document as the source of truth for implemented behavior
* alignment of the package-structure and system-overview documents with the current-architecture source of truth
* task evidence for the documentation writer and changed files

## Architecture And Delivery Clarification

The repository now documents a simple agentic delivery model:

```text
task intake -> isolated worktree -> scoped specialist writers -> quality gates -> independent review -> human approval
```

This is process documentation only. It does not change the application architecture, runtime flow, persistence model, or API behavior.

## Roadmap Clarification

The roadmap now states more explicitly that:

* working-days behavior is planned unless marked as implemented in the current architecture document
* modules missing from the current architecture, code, and tests are not implemented
* roadmap requirements are not production behavior by default

This reduces the risk of interview or reviewer confusion when comparing planned scope against the codebase.

## AI-Assisted Work

Codex was used to:

* inspect the current documentation set
* identify the existing AI engineering log convention
* draft concise workflow and disclaimer updates
* update the task run ledger with documentation evidence

## Human-Owned Decisions

The following decisions remained human-owned:

* keeping the change documentation-only
* treating `docs/architecture/current-architecture.md` as the source of truth for implemented behavior
* requiring explicit wording that autonomous merge is not enabled
* preserving reviewer independence by keeping the reviewer outside the writer set
