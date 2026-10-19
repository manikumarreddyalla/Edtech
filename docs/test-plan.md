# Test Plan

## Status

Phase 0 test strategy. No application tests exist in the repository yet.

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
