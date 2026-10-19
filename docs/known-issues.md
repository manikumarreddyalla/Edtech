# Known Issues and Open Decisions

Updated: 2026-10-19

| ID | Status | Issue | Owner / next action |
|---|---|---|---|
| KI-001 | RESOLVED | Repository initially contained only the product README and Agent 2 Phase 0 status. Agent 1 has added a backend/frontend foundation; build/test status is tracked in `agent-1-status.md`. | Agent 1: maintain build and test evidence. |
| KI-002 | FOLLOW_UP | The Maven advisory scan found two critical Spring Web MVC advisories on `6.2.19`; the reported patched line is Spring Framework `7.0.9` (major). The frontend scan is clean. Avoid using the affected XSLT/SSE fragment features; plan and validate a Boot 4/Spring 7 migration before production. | Agent 1: assess the major framework upgrade in a separate coordinated change. |
| KI-003 | OPEN | Local development bootstrap provisions an admin from environment variables, but production account provisioning, token refresh/revocation and password reset flows are not implemented. | Agent 1: define before production authentication; access tokens currently expire after 10 minutes. |
| KI-004 | OPEN | Production object storage provider and retention/deletion policy are undecided. | Agent 1: define before secure document deployment. |
| KI-005 | RESOLVED | Agent 2 accepted the shared contracts and reserved migration versions V100+ for training. The initial skill catalog is integrated into the shared UI route. | Both agents: coordinate future shared model and navigation changes. |
| KI-006 | OPEN | Full `mvn test` has one failing Agent 2-owned assertion: the test constructs an Institution with a generated UUID, then separately supplies a different institution UUID and expects them to match. | Agent 2: align the test fixture and rerun the suite. |
