# Test Plan

## Status

Test strategy and current evidence, updated by Agent 1 on 2026-10-19.

## Test layers

1. **Backend unit tests:** service validation, status transitions, role rules and calculations.
2. **Backend persistence/API tests:** repositories and REST endpoints against PostgreSQL-compatible test infrastructure; migrations run from an empty database.
3. **Authorization and tenant tests:** unauthenticated, insufficient-role and cross-institution requests for every sensitive resource family.
4. **Frontend tests:** shared shell, route authorization behavior, typed API error display, forms and table empty/loading states.
5. **Integration tests:** identity shared across records, accreditation and training; institution-scoped analytics.
6. **Release checks:** clean backend build, frontend typecheck/build, migration from scratch and documented local startup.

## Initial critical cases

- Valid login issues only the documented token claims; invalid credentials do not reveal whether an account exists.
- Institution A users cannot read, update, download or enumerate Institution B records, evidence, documents, students, assessments or results.
- Users without required roles cannot invoke write/review/export operations, even if they know a resource UUID.
- Invalid DTOs return `400` with stable field details and no stack trace.
- Database uniqueness and invalid parent/tenant associations are rejected.
- Uploads enforce configured byte-size and content-type restrictions; unauthorized users cannot retrieve storage objects.
- Pagination enforces maximum size and rejects unsafe sort properties.
- Migration set applies successfully to a clean PostgreSQL database.

## Evidence policy

Status files must report the actual command and result. Do not mark a feature DONE if its applicable checks were not run. Use synthetic test data only.

## Phase 1–2 evidence

- `mvn -q clean -Dtest=AuthIntegrationTest,InstitutionDirectoryIntegrationTest test`: passed (7 tests); the clean build applies shared V1 and Agent 2's V100 migration to isolated H2 databases. Directory tests cover tenant scoping, role enforcement, validation, duplicate conflicts, related-tenant checks and audit writes.
- `mvn clean test`: 10 of 11 tests pass. The overall suite fails only in `TrainingSkillServiceTest.createTrimsInputAndUsesTheAuthenticatedInstitution` because the test-created Institution has a generated UUID different from the separately supplied institution UUID. The failing test belongs to Agent 2.
- `npm test`: passed (4 tests), including the login flow and authenticated route to the training skill page.
- `npm run typecheck`: passed.
- `npm run build`: passed.
- `npm audit --audit-level=moderate`: passed with zero known npm vulnerabilities.
- Maven dependency advisory scan: two critical Spring Web MVC findings remain at `6.2.19`; the scanner's patched version is Spring `7.0.9`, a major framework upgrade. Track this before production rather than silently upgrading the Boot 3 application.
- PostgreSQL service startup was not tested because Docker is not installed in the environment.

## Phase 3 document evidence

- `mvn -q clean -Dtest=AuthIntegrationTest,InstitutionDirectoryIntegrationTest,DocumentManagementIntegrationTest test`: passed (10 tests).
- `mvn clean test`: 13 of 14 tests pass; the only failure is the existing Agent 2-owned training service fixture assertion.
- Document integration cases verify signature/content-type mismatch rejection, configured size limits, generated opaque storage metadata, immutable version increments, authenticated content download, faculty-owner filtering, cross-tenant denial, and no response disclosure of storage keys.
- `npm test`: passed (6 tests), including multipart upload headers and authenticated document download filename handling.
- `npm run typecheck`: passed.
- `npm run build`: passed.
- The local filesystem adapter's `dev`/`test` profile restriction is verified by configuration/code inspection; no production object-storage adapter exists.

## Phase 4 accreditation evidence

- `mvn clean -Dtest=AuthIntegrationTest,InstitutionDirectoryIntegrationTest,DocumentManagementIntegrationTest,AccreditationIntegrationTest test`: passed (13 tests) after a clean build; Flyway V1–V3 applied to isolated PostgreSQL-mode H2 databases.
- `mvn clean test`: 16/17 tests pass. The only failure remains Agent 2's existing `TrainingSkillServiceTest.createTrimsInputAndUsesTheAuthenticatedInstitution` fixture UUID mismatch (assertion at `TrainingSkillServiceTest.java:63`); no Agent 2-owned source or tests were modified.
- Accreditation integration coverage verifies configured framework/criterion creation, owner and tenant scoping, cross-tenant criterion rejection, the full assignment/start/submit/revision/resubmit/verify lifecycle, immutable numbered submission history, attached document-version metadata without storage-key disclosure, review-history retention, missing-evidence reporting, audit writes, and reviewer-role enforcement.
- `npm test`: passed (7 tests), including the accreditation route and active evidence-owner lookup.
- `npm run typecheck` and `npm run build`: passed.
- PostgreSQL runtime remains unverified because Docker is unavailable in this environment.
