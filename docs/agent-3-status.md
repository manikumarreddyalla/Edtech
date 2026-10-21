# Agent 3 Status — Training Domain Workflows

- Owner: agent-3
- Task: Extend the institution-scoped training catalog into core training workflows.
- Status: READY_FOR_INTEGRATION — requested training lifecycle slice is implemented and the clean local backend/frontend checks pass; PostgreSQL runtime remains unverified.
- Timestamp: 2026-10-21

## Decision and preserved work

- Restored the committed `TrainingSkill.java` as directed. Its `Institution` relationship, normalized name and audit timestamps match V100 and the existing service/repository/response contracts.
- Fixed `TrainingSkillServiceTest.createTrimsInputAndUsesTheAuthenticatedInstitution` to take the ID from the mock institution while retaining the exact institution-scope assertion.
- Added only V101 for new training workflow tables. V1–V3 and V100 are unchanged.

## Delivered in this task

- Tenant-owned job roles and skill target requirements.
- Versioned draft/published pathways and ordered modules.
- Planned/active cohorts, placement-officer enrollment of same-tenant active student profiles, assignments/projects with rubrics, student attempts, one-time rubric grading/results and trainer feedback.
- JWT tenant scoping, role-gated writes, input validation, lifecycle transition checks and audit events for sensitive writes.
- A dev-profile-only pathway seed labeled synthetic and internally defined, with explicit no-employer/no-placement claims.
- Training workflow section added to the already mounted training page; no shared shell, router or global styles were edited.
- The training page includes job role/skill-target maintenance, pathways/modules, cohort setup/activation, placement enrollment, assignment creation/submission, rubric grading/results, and trainer feedback.

## Files

- `backend/src/main/java/com/collegeops/training/TrainingWorkflowController.java`
- `backend/src/main/java/com/collegeops/training/TrainingWorkflowService.java`
- `backend/src/main/java/com/collegeops/training/SyntheticTrainingSeed.java`
- `backend/src/main/resources/db/migration/V101__training_workflows.sql`
- `backend/src/test/java/com/collegeops/training/TrainingWorkflowIntegrationTest.java`
- `backend/src/test/java/com/collegeops/training/TrainingSkillServiceTest.java`
- `frontend/src/training/TrainingSkillsPage.tsx`
- `frontend/src/training/TrainingSkillsPage.css`
- `docs/api-contracts.md`, `docs/database-design.md`, `docs/agent-3-status.md`

## Known limitations

- PostgreSQL/Docker runtime startup has not been verified in this environment; integration tests use PostgreSQL-mode H2.
- Cohort assignment uses the trainer or student profile UUID where a directory picker/provisioning flow is not available.
- Aggregate cohort reports, baseline/final skill comparison, skill attainment per requirement, and CSV skill-gap exports remain future work.
- End-to-end browser and live PostgreSQL checks remain open.

## Exact next action

- Run the clean migration and API smoke workflow on PostgreSQL when Docker/PostgreSQL is available; do not claim PostgreSQL verification from H2 coverage.

## Validation

- `mvn clean test` (2026-10-21): passed, 21 tests, 0 failures/errors. Flyway applied V1–V3, V100 and V101 to isolated PostgreSQL-mode H2 databases.
- Focused `TrainingWorkflowIntegrationTest`: passed, 4/4, covering role denial, invalid transition and request validation, cross-tenant denial, and pathway→cohort→enrollment→assignment→submission→grading/results→feedback.
- `npm test`: passed, 7/7.
- `npm run typecheck`: passed.
- `npm run build`: passed.
- Docker and a local PostgreSQL service are unavailable; no live PostgreSQL startup/migration smoke test was run.
