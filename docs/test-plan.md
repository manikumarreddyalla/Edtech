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

## Phase 1 evidence

- `mvn -q clean -Dtest=AuthIntegrationTest test`: passed (4 tests); the clean build applies shared V1 and Agent 2's V100 migration to an isolated H2 database.
- `mvn test`: AuthIntegrationTest passed (4 tests); the overall suite failed in `TrainingSkillServiceTest.createTrimsInputAndUsesTheAuthenticatedInstitution` because the test-created Institution has a generated UUID different from the separately supplied institution UUID. The failing test belongs to Agent 2.
- `npm test`: passed (4 tests), including the login flow and authenticated route to the training skill page.
- `npm run typecheck`: passed.
- `npm run build`: passed.
- `npm audit --audit-level=moderate`: passed with zero known npm vulnerabilities.
- Maven dependency advisory scan: two critical Spring Web MVC findings remain at `6.2.19`; the scanner's patched version is Spring `7.0.9`, a major framework upgrade. Track this before production rather than silently upgrading the Boot 3 application.
- PostgreSQL service startup was not tested because Docker is not installed in the environment.
