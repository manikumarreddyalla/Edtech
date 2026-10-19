# Agent 1 Status — Platform and Institutional Intelligence Lead

- Owner: agent-1
- Task: Phase 4 configurable accreditation evidence workflow
- Status: IN_PROGRESS — Phases 0–4 delivered; additional platform capabilities remain
- Timestamp: 2026-10-19

## Files created or changed
- `docs/architecture.md`
- `docs/api-contracts.md`
- `docs/database-design.md`
- `docs/integration-plan.md`
- `docs/decisions.md`
- `docs/test-plan.md`
- `docs/known-issues.md`
- `docs/agent-1-status.md`
- `backend/pom.xml`
- `backend/src/main/java/com/collegeops/`
- `backend/src/main/resources/`
- `backend/src/test/java/com/collegeops/security/AuthIntegrationTest.java`
- `frontend/`
- `docker-compose.yml`
- `.env.example`
- `.gitignore`
- `docs/local-development.md`
- `Readme.md`
- `backend/src/main/java/com/collegeops/institution/`
- `backend/src/main/java/com/collegeops/common/`
- `backend/src/test/java/com/collegeops/institution/InstitutionDirectoryIntegrationTest.java`
- `backend/src/main/java/com/collegeops/documents/`
- `backend/src/test/java/com/collegeops/documents/DocumentManagementIntegrationTest.java`
- `backend/src/main/resources/db/migration/V2__documents.sql`
- `backend/src/main/resources/application.yml`
- `.env.example`
- `.gitignore`
- `frontend/src/documents/`
- `frontend/src/api/client.test.ts`
- `frontend/src/App.tsx`
- `frontend/src/App.test.tsx`
- `frontend/src/pages/AppLayout.tsx`
- `backend/src/main/java/com/collegeops/accreditation/`
- `backend/src/main/resources/db/migration/V3__accreditation_evidence.sql`
- `backend/src/test/java/com/collegeops/accreditation/AccreditationIntegrationTest.java`
- `frontend/src/accreditation/`

## API or schema changes
- Published shared Institution, User, Role, Department, AcademicProgram, Student, audit metadata, error, pagination, authentication and tenant-isolation contracts.
- Added Flyway V1 shared tenant/identity schema, BCrypt-backed local admin bootstrap, 10-minute HS256 bearer authentication, `/api/v1/auth/login` and `/api/v1/auth/me`, standard API errors, CORS, actuator health and OpenAPI.
- Added the React login/dashboard shell and mounted Agent 2's `/training/skills` page using the shared router/auth/API client.
- Added tenant-scoped, paginated read and administrator-create routes for departments, academic programs and minimal student directory entries.
- Directory creation writes an audit event in the same transaction; linked student users must be active, same-tenant users with the `STUDENT` role.
- Added document upload/search/filter, private download and immutable version-history APIs with V2 metadata, SHA-256 checksums, allow-listed file signatures, size limits and transactional audit/rollback cleanup.
- Local filesystem storage is restricted to `dev`/`test`; production storage is intentionally not configured.
- Added a role-aware document library page for upload, search, version history and private downloads using multipart requests and the shared authenticated API client.
- Added institution-configurable framework and criterion APIs, eligible-owner lookup, evidence assignment and status transitions, immutable numbered submissions referencing document versions, IQAC reviews with retained history, missing-evidence reporting and transactional audit events.
- Added the accreditation workbench route for framework/criterion setup, owner assignment, evidence start/submission, review and history.
- Directory updates/archiving, faculty profile management, accreditation completeness dashboards and report exports remain unimplemented.

## Tests executed
- `mvn -q clean -Dtest=AuthIntegrationTest,InstitutionDirectoryIntegrationTest,DocumentManagementIntegrationTest test`: passed (10 tests).
- `mvn clean -Dtest=AuthIntegrationTest,InstitutionDirectoryIntegrationTest,DocumentManagementIntegrationTest,AccreditationIntegrationTest test`: passed (13 tests); clean runs applied V1–V3 and exercised the evidence lifecycle against PostgreSQL-mode H2.
- `mvn clean test`: 16/17 tests pass; one Agent 2-owned `TrainingSkillServiceTest` fixture assertion fails at line 63.
- `npm test`: passed (7 tests).
- `npm run typecheck`: passed.
- `npm run build`: passed.
- `npm audit --audit-level=moderate`: passed, 0 vulnerabilities.
- Maven advisory scan: 2 critical Spring Web MVC findings remain; patched line reported is Spring 7.0.9, a major upgrade. PostgreSQL/Docker runtime was not available to test.

## Decisions made
- Modular monolith; Spring Boot API, React/TypeScript frontend and PostgreSQL.
- UUID identifiers, `/api/v1`, DTO-only API boundary, bounded pagination and a consistent error response.
- Short-lived bearer access-token baseline; institution scope must be resolved and enforced server-side.
- Shared append-only Flyway migration strategy; Agent 1 owns shared identity schema.
- Keep V2–V99 for Agent 1 institutional migrations; Agent 2 reserved V100+ for training.
- Keep bearer tokens in frontend memory; reload signs the user out rather than persisting tokens in browser storage.

## Outstanding issues
- Agent 2's training service unit test has a fixture mismatch and currently fails the full Maven suite; Agent 2 owns that test.
- Two Spring Web MVC critical advisories require a major Spring 7 / Boot 4 upgrade, tracked as a follow-up.
- Production token lifecycle and private object storage/retention remain open; the production profile has no storage adapter until one is selected and implemented. See `docs/known-issues.md`.
- Production document malware scanning/content disarm is not configured; see `KI-007`.

## Dependencies for the other agent
- Agent 2 has accepted shared contracts and its skills page is mounted in the shared shell.
- Agent 2 owns the training module, V100+ migration range, training components and training tests. Do not edit those files.

## Exact next action
- Implement and validate the next shared platform slice (directory lifecycle, production object storage, or institution intelligence dashboards) after coordinating scope with Agent 2; production storage and report exports are not implied by this accreditation workflow.
