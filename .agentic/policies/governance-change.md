# Governance Change Policy

A task is a governance change when it modifies the agentic control plane,
runtime-contract references, reviewer rules, task-ledger rules or workflow
gates.

Governance changes require:

1. explicit classification as `GOVERNANCE_CHANGE`;
2. human approval before implementation;
3. an isolated branch and worktree;
4. no claim that the changed rules independently approved themselves;
5. deterministic testing of scripts and repository safety;
6. human review of the complete diff;
7. regeneration of the external runtime-contract manifest when executable
   OpenClaw contracts changed;
8. human approval before merge.

Automated Architect, Security and Reviewer agents may provide advisory
findings, but human approval is the acceptance authority for the governance
change itself.
