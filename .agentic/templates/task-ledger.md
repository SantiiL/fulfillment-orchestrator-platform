# Task Ledger Template

Use this file as the canonical structure for `.agentic/runs/<task-id>.md`.

```md
# <TASK-ID>

## Task Metadata

| Field | Value |
| --- | --- |
| Task id | `<TASK-ID>` |
| Classification | `<FEATURE|GOVERNANCE_CHANGE|...>` |
| Base branch | `<base-branch>` |
| Task branch | `<task-branch>` |
| Worktree path | `<absolute-worktree-path>` |
| Current stage | `<stage-id>` |
| Derived writer set | `<agent-id-1>, <agent-id-2>` |
| Selected reviewer agentId | `<reviewer-agent-id>` |
| Reviewer independence expression | `PENDING: <selectedReviewerAgentId NOT IN writerSet>` |
| Reviewer independence result | `PENDING` |
| Reviewer verdict | `PENDING` |
| Human approval | `PENDING` |

## Writer Events

### Writer Event 1

* agentId: `<writer-agent-id>`
* role contract: `.agentic/roles/<writer-role>.md`
* stage: `<implementation|documentation|other>`
* session key or run ID: `<session-key-or-run-id>`
* files changed: `<path-1>, <path-2>`
* timestamp or ordering evidence: `<timestamp>`

## File Coverage

* `<path-1>` -> `Writer Event 1`
* `<path-2>` -> `Writer Event 1, Writer Event 2`

## Quality Gates

| Check | Result | Evidence |
| --- | --- | --- |
| Deterministic validation | `PENDING` | `<evidence>` |
| Reviewer verdict | `PENDING` | `<evidence>` |
| Human approval | `PENDING` | `<evidence>` |

## Governance Notes

* repository ledger text is audit evidence, not runtime authority
* authoritative external runner state is recorded outside the repository when
  the deterministic runtime is active
* governance tasks record runtime-manifest and external-installation status
```
