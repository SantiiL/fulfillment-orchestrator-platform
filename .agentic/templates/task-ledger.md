# Task Ledger Template

Use this file as the canonical structure for `.agentic/runs/<task-id>.md`.

```md
# <TASK-ID>

## Task Metadata

| Field | Value |
| --- | --- |
| Task id | `<TASK-ID>` |
| Base branch | `<base-branch>` |
| Task branch | `<task-branch>` |
| Worktree path | `<absolute-worktree-path>` |
| Current stage | `readiness-preparation` |
| Derived writer set | `<agent-id-1>, <agent-id-2>` |
| Selected reviewer agentId | `<reviewer-agent-id>` |
| Selected reviewer role-contract path | `.agentic/roles/<reviewer-role>.md` |
| Reviewer independence expression | `PENDING: <selectedReviewerAgentId NOT IN writerSet>` |
| Reviewer independence result | `PENDING` |
| Reviewer verdict | `PENDING` |
| Human approval | `PENDING` |

## Writer Events

The active writer appends or updates its own writer event before relinquishing the writer role. The orchestrator validates the persisted writer events but never authors the ledger.

### Writer Event 1

* agentId: `<writer-agent-id>`
* role contract: `.agentic/roles/<writer-role>.md`
* stage: `<implementation|documentation|other>`
* session key or run ID: `<session-key-or-run-id>`
* files changed: `<path-1>, <path-2>`
* timestamp or ordering evidence: `<timestamp, sequence number, or equivalent evidence>`

## File Coverage

* every changed repository file must be covered by at least one writer event
* shared files may appear in multiple writer events
* missing writer-event evidence blocks progression
* `<path-1>` -> `Writer Event 1`
* `<path-2>` -> `Writer Event 1, Writer Event 2`

## Stage Notes

### Readiness

* record branch and worktree preparation evidence before final Definition of Ready approval

### Implementation

* append implementation and validation evidence without removing earlier stages

### Documentation

* append documentation evidence and any AI engineering log updates

### Review

* record the reviewer verdict exactly as `APPROVE`, `REQUEST_CHANGES`, or `BLOCKED`
* record the reviewer `agentId`, role-contract path, independence expression, and independence result before review starts
* refuse review when any changed repository file lacks writer-event coverage

### Human Approval

* record the final human decision and date when available
```
