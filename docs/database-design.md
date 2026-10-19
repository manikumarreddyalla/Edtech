# Database Design

## Status

Shared logical design and current schema status, updated by Agent 1 on 2026-10-19.

## Shared schema rules

- PostgreSQL is the shared persistence store for the modular monolith.
- Use UUID primary keys and UTC `timestamptz` timestamps.
- Use Flyway SQL migrations; use monotonically ordered names such as `V1__shared_identity.sql`.
- Never rewrite a migration that may have been applied. Add a new migration and coordinate migration numbering with Agent 2 before adding migrations in parallel.
- Use explicit foreign keys, indexes and uniqueness constraints. Add `institution_id` to tenant-owned tables or enforce tenant scope through a parent relation and composite constraints.
- Avoid cascade deletion for audit-relevant records. Define archival and retention behavior before adding destructive operations.
- Store uploaded file bytes outside PostgreSQL; retain only metadata and an opaque storage key.

## Shared entities implemented by V1

| Table | Key fields and constraints |
|---|---|
| `institutions` | `id`, `name`, unique `code`, `status`, timestamps |
| `users` | `id`, `institution_id`, normalized unique `(institution_id, email)`, `display_name`, password hash, `status`, timestamps |
| `user_roles` | `user_id`, role enum/string; unique `(user_id, role)` |
| `departments` | `id`, `institution_id`, `name`, unique `(institution_id, code)`, `status`, timestamps |
| `academic_programs` | `id`, `institution_id`, `department_id`, `name`, unique `(institution_id, code)`, `status`, timestamps |
| `students` | `id`, `institution_id`, optional unique `user_id`, `department_id`, `academic_program_id`, unique `(institution_id, student_number)`, `status`, timestamps |
| `faculty_profiles` | `id`, `institution_id`, `user_id`, optional `department_id`, timestamps |
| `audit_events` | `id`, `institution_id`, `actor_user_id`, action, target type/id, timestamp, safe summary |

Role values initially follow `docs/architecture.md`. A user, department, program or profile must not be associated with a different institution through a foreign key. Where ordinary foreign keys cannot enforce same-tenant ownership, validate it in the service and add a database constraint/association design that prevents inconsistent joins.

## Module ownership

- Agent 1 owns the tables above plus records, documents and accreditation tables.
- Agent 2 owns training pathway, skill, cohort, enrollment, assignment, assessment and result tables.
- Agent 2's tables must reference shared `institutions`, `users` and `students` IDs and include institution scoping directly or through a verifiable parent.
- Cross-module tables and migration ordering are shared concerns and require coordination before modification.

## Documents and audit

V2 adds `documents` and `document_versions`. Documents carry institution, owner, optional department, title, category and lifecycle status. Immutable version rows carry tenant/document/uploader IDs, version number, sanitized original filename, allow-listed content type, byte size, SHA-256 checksum, opaque storage key and upload time. Composite tenant foreign keys prevent documents or versions from referencing another institution's user/department/document. The local storage adapter is dev/test-only; production must supply a private object-storage implementation.

Audit rows should be append-only for sensitive actions. Include no credentials, bearer tokens, raw file content or unnecessary student data in event summaries.

## Migration sequence

1. `V1`: institutions, users/roles, departments, academic programs, students/faculty, audit events.
2. `V2`–`V99`: reserved for Agent 1 records/documents/accreditation schema.
3. `V100+`: Agent 2 training schema, as recorded in `docs/agent-2-status.md`.

Never rewrite an applied migration or ship duplicate Flyway versions. Agent 2's current training skill migration is `V100__training_skill_catalog.sql`; verify only one copy of each migration remains under `target/classes` after a clean build.
The initial directory API uses the `departments`, `academic_programs` and `students` tables already created in shared V1; no schema migration was needed for this read/create slice. `audit_events` is mapped by the shared append-only `AuditEvent` entity and records each successful directory create in the same transaction.
The initial document management API uses V2. Document bytes remain outside PostgreSQL; DB transactions record metadata and audit events, and clean up a newly stored local file if the transaction rolls back.
