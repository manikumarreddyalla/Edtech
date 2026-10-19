# Known Issues and Open Decisions

Updated: 2026-10-19

| ID | Status | Issue | Owner / next action |
|---|---|---|---|
| KI-001 | RESOLVED | Repository initially contained only the product README and Agent 2 Phase 0 status. Agent 1 has added a backend/frontend foundation; build/test status is tracked in `agent-1-status.md`. | Agent 1: maintain build and test evidence. |
| KI-002 | IN_PROGRESS | Initial dependency scans found vulnerabilities in the first backend/frontend package versions. Compatible patched versions have been applied and must be rescanned before Phase 1 is published. | Agent 1: finish dependency remediation and verification. |
| KI-003 | OPEN | Token refresh/revocation strategy, initial account provisioning and password reset flow are not specified. | Agent 1: document before production authentication; avoid representing auth as production-ready until addressed. |
| KI-004 | OPEN | Production object storage provider and retention/deletion policy are undecided. | Agent 1: define before secure document deployment. |
| KI-005 | OPEN | Agent 2 has not yet acknowledged the shared Phase 0 contracts. | Agent 2: review contracts and record acknowledgement or requested changes in `agent-2-status.md`. |
