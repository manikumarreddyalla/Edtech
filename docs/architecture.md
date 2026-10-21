# Architecture

## Status

Shared contract and implementation status, updated by Agent 1 on 2026-10-19. Implementation details that are not built must not be treated as completed functionality.

## System shape

CollegeOps is a modular monolith: one Spring Boot API, one React single-page application, and one PostgreSQL database. Modules share identity and tenant boundaries but own their domain services and tables. The frontend calls the API under `/api/v1`; no module-specific authentication or database is permitted.

```text
Browser
  React + TypeScript + Vite
       | HTTPS / JSON, Authorization: Bearer
       v
Spring Boot modular monolith
  security / common / institution / users / records / documents /
  accreditation / training / assessment / analytics / reporting
       | JPA + Flyway
       v
PostgreSQL
```

Local development may use Docker Compose for PostgreSQL. Uploaded content is stored through a storage adapter (local filesystem for development; object storage for a deployed environment); PostgreSQL stores metadata and opaque storage keys only.

## Technology baseline

- Java 21+, Spring Boot 3.x, Maven.
- Spring Web, Spring Security, Spring Data JPA, Jakarta Validation, Flyway, Actuator and OpenAPI.
- PostgreSQL with UUID primary keys, UTC timestamps and explicit foreign keys.
- React, TypeScript strict mode, Vite and React Router.
- JUnit 5 and frontend tests using the test tooling selected in Phase 1.

Exact dependency versions are pinned in the Maven/npm manifests. Keep versions mutually compatible; coordinate framework changes while another module is integrating.

## Module boundaries

- `security` and `users`: authentication, roles, current-user context and authorization.
- `institution`: institutions, departments, academic programs and student/faculty profiles.
- `records` and `documents`: institution records and secure file metadata/storage.
- `accreditation`: versioned frameworks/criteria and evidence review workflows.
- `training` and `assessment`: Agent 2 domain, using the shared institution/user/student model.
- `analytics` and `reporting`: institution-scoped, explainable projections and exports.
- `common`: API errors, pagination conventions, audit fields and shared validation.

Domain logic belongs in services. Controllers accept and return validated DTOs, never JPA entities. Each module owns its repositories; cross-module calls use service-level contracts rather than direct writes to another module's tables.

## Identity, authorization and tenancy

The initial contract uses short-lived bearer access tokens issued by the API. The authenticated principal has a user UUID, an institution UUID (nullable only for explicitly platform-scoped operators), and one or more roles. The API must derive the institution scope from the verified principal, not trust a client-supplied institution ID as proof of access.

Initial institution roles:

| Role | Intended scope |
|---|---|
| `INSTITUTION_ADMIN` | Manage their institution's users, departments and configuration |
| `IQAC_COORDINATOR` | Manage and review their institution's accreditation evidence |
| `FACULTY` | Access assigned academic records and submit evidence |
| `TRAINER` | Manage assigned training activities and review submissions |
| `PLACEMENT_OFFICER` | Manage cohorts and view institution-scoped training analytics |
| `STUDENT` | Access only their own profile, enrollment, assignments and results |

Authorization is enforced on every backend request. A valid UUID is not authorization. Cross-institution access must return the standard not-found response (to avoid disclosing the existence of another tenant's record) or a consistently documented forbidden response; it must never return the record. Platform administration is not part of the first-release tenant roles and must not be inferred from an institution role.

## Shared persistence conventions

- UUID primary keys; `created_at` and `updated_at` stored as UTC instants.
- Every tenant-owned record has an institution association directly or through an enforced parent relationship.
- Foreign keys, indexes and unique constraints are created in Flyway migrations.
- Audit events record actor, action, target type/id, institution, timestamp and a safe change summary. Do not put document contents, passwords, tokens or unnecessary personal data in audit detail.
- Migrations are append-only after sharing; coordinate migration ordering with Agent 2.
- Seed data is synthetic, deterministic and development/test-only.

## Operational requirements

Expose health/readiness through Actuator without returning secrets or tenant data. Load secrets from environment/configuration providers, never source control. Set upload limits and allow-listed content types. Production deployment must use HTTPS and a private or managed file-storage implementation.

## Current implementation status

The repository contains the shared database/authentication foundation, directory APIs, document management, configurable accreditation evidence workflows and the training skill catalog. The training module now includes tenant-owned roles and skill targets, pathways, cohorts, enrollment, assignments, rubric grading and trainer feedback. The institutional dashboard and report APIs provide tenant-scoped evidence metrics, training participation counts, formula-safe CSV exports and an accreditation evidence PDF. Skill-gap values remain explicitly unavailable until assessed student skill levels are recorded against job-role requirements. Directory updates/archiving, faculty profiles, production account lifecycle, production object storage/retention and malware scanning remain incomplete. PostgreSQL-backed runtime has not been verified in this environment because Docker and local PostgreSQL are unavailable; backend integration tests use isolated H2 databases.
