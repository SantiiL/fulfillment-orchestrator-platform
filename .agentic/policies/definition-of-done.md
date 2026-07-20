# Definition Of Done

A task is done only when all of the following are true:

* the approved scope is implemented
* automated validation completed successfully
* manual validation evidence exists when required by the task
* documentation is updated when repository guidance or behavior changed
* an AI engineering log entry was added or updated under `docs/ai-engineering-log/`
* the task ledger records the final writer events, derived writer set, reviewer selection, reviewer verdict, and human approval state
* review was performed only after `selectedReviewerAgentId NOT IN writerSet` was recorded as a passing independence result
* reviewer findings were addressed or explicitly accepted by a human
* human approval is still pending or completed through the normal pull request flow

Done does not mean merged automatically. No agent may merge a pull request.
