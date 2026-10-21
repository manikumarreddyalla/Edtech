# Shared API Contracts

## Status

Shared baseline, updated by Agent 1 on 2026-10-19. Agent 2 should use these shared DTO and authorization conventions and document any required change before implementing against a different shape.

## General conventions

- Base path: `/api/v1`.
- Content type: `application/json`; timestamps are ISO-8601 UTC instants.
- Resource identifiers are UUID strings.
- Request DTOs are validated server-side. JPA entities are never serialized directly.
- Collection endpoints are paginated. Query parameters: `page` (zero-based, default `0`), `size` (default `20`, maximum `100`), `sort` (allow-listed property and direction).
- Tenant scope is resolved from the authenticated principal. A client-supplied institution identifier may select a target only when the endpoint explicitly permits it and the server verifies access.
- Authenticated requests use `Authorization: Bearer <access-token>`. The token is short-lived; refresh/revocation behavior will be documented with the authentication implementation.

## Shared models

### Institution

```json
{
  "id": "UUID",
  "name": "Example Engineering College",
  "code": "EXAMPLE-ENG",
  "status": "ACTIVE",
  "createdAt": "2026-10-19T00:00:00Z",
  "updatedAt": "2026-10-19T00:00:00Z"
}
```

`code` is unique. Initial statuses: `ACTIVE`, `INACTIVE`.

### User

```json
{
  "id": "UUID",
  "institutionId": "UUID",
  "email": "person@example.edu",
  "displayName": "Example User",
  "roles": ["FACULTY"],
  "status": "ACTIVE"
}
```

Passwords, password hashes, token material and authentication internals are never included in user responses. Roles are drawn from the institution role set in `architecture.md`; users may have multiple roles only where explicitly granted.

### Department

```json
{
  "id": "UUID",
  "institutionId": "UUID",
  "name": "Computer Science and Engineering",
  "code": "CSE",
  "status": "ACTIVE"
}
```

The pair `(institutionId, code)` is unique.

### AcademicProgram

```json
{
  "id": "UUID",
  "institutionId": "UUID",
  "departmentId": "UUID",
  "name": "B.Tech Computer Science and Engineering",
  "code": "BTECH-CSE",
  "status": "ACTIVE"
}
```

The department must belong to the same institution. The pair `(institutionId, code)` is unique.

### Student

```json
{
  "id": "UUID",
  "institutionId": "UUID",
  "userId": "UUID",
  "departmentId": "UUID",
  "academicProgramId": "UUID",
  "studentNumber": "2026-CSE-001",
  "status": "ACTIVE"
}
```

`userId` may be absent until a student login is provisioned. Student number is unique within its institution. Student responses must omit unnecessary sensitive personal data.

### Audit metadata

Persisted records use `createdAt`, `updatedAt`, and where appropriate `createdBy`/`updatedBy` UUIDs. These fields are set by the server; clients cannot set or overwrite them.

## Standard error response

All API errors use this shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "details": [
    { "field": "name", "message": "must not be blank" }
  ],
  "traceId": "request-correlation-id"
}
```

`details` may be empty or omitted for errors without field-level detail. Never return stack traces, SQL, secrets or internal exception messages. Use stable machine-readable codes, including `VALIDATION_ERROR`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT` and `INTERNAL_ERROR`.

| HTTP status | Meaning |
|---|---|
| `400` | Malformed request or validation failure |
| `401` | Missing, invalid or expired authentication |
| `403` | Authenticated but not permitted |
| `404` | Resource absent or outside the caller's tenant scope |
| `409` | Uniqueness or state-transition conflict |
| `413` | Uploaded file or request exceeds its configured size limit |
| `415` | Unsupported request content type |
| `500` | Unexpected server error; response contains no internal details |

## Pagination response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## Endpoint families and initial authorization

### Login

`POST /api/v1/auth/login` is public and accepts an institution code because email addresses are unique within an institution rather than globally:

```json
{
  "institutionCode": "DEMO",
  "email": "admin@example.edu",
  "password": "user-supplied-password"
}
```

On success it returns `accessToken`, `tokenType` (`Bearer`), `expiresAt` and the safe `user` profile shown above. The access token expires after 10 minutes. The initial implementation keeps the token in frontend memory; reloading the page requires signing in again. `GET /api/v1/auth/me` requires a valid bearer token and returns the current user profile. Credentials are never echoed in responses.

| Family | Initial access rule |
|---|---|
| `/auth/**` | Public login; authenticated identity/logout actions as applicable |
| `/institutions/**` | Institution admin for own institution; platform-scoped access only if separately implemented |
| `/departments/**`, `/users/**`, `/students/**` | Institution admin; scoped read access for explicitly assigned staff |
| `/documents/**` | Authenticated institution member with resource-specific permission; student access is own records only |
| `/accreditation/**` | IQAC coordinator/reviewer; faculty may submit assigned evidence |
| `/training/**` | Trainer and placement officer by action; students limited to their own participation |
| `/analytics/**` | Institution-scoped placement officer, IQAC coordinator or institution admin according to metric |

Every endpoint must document its final role rule in OpenAPI. These family rules are not a substitute for per-resource ownership checks.

## Agent 2 integration requirement

Training endpoints should use the same user, student and institution IDs, standard errors, pagination and tenant resolution. Agent 2 must not create parallel login, institution, student or audit models. Training API additions belong in the training section of OpenAPI and must state role and institution scope.

## Institutional directory APIs — initial slice

All routes require a bearer access token. The API derives `institutionId` from the verified token claim; clients cannot submit an institution ID to select or change the tenant. Collection responses use the shared pagination object and are ordered by the documented field. Invalid pagination returns `400 VALIDATION_ERROR`.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/v1/departments?page=0&size=20` | Institution admin, IQAC coordinator, faculty, trainer or placement officer | Lists departments in the caller's institution, ordered by name. |
| `POST /api/v1/departments` | Institution admin | Creates a department with `name` (1–180 characters) and `code` (1–40 ASCII letters, digits, `_` or `-`, beginning with a letter/digit). Code is normalized to uppercase. |
| `GET /api/v1/academic-programs?page=0&size=20` | Institution admin, IQAC coordinator, faculty, trainer or placement officer | Lists programs in the caller's institution, ordered by name. |
| `POST /api/v1/academic-programs` | Institution admin | Creates a program with `departmentId`, `name` (1–200 characters) and `code` (1–50 ASCII letters, digits, `_` or `-`, beginning with a letter/digit). The department must belong to the caller's institution. Code is normalized to uppercase. |
| `GET /api/v1/students?page=0&size=20` | Institution admin, IQAC coordinator, faculty or placement officer | Lists minimal student directory records in the caller's institution, ordered by student number. |
| `POST /api/v1/students` | Institution admin | Creates a student with `studentNumber`, `departmentId` and `academicProgramId`; optional `userId` must refer to a same-institution user with the `STUDENT` role. Department and program must both belong to the caller's institution and program department must match. |

Department/program/student responses include their IDs, institution ID, related IDs/names where useful, status and server-managed `createdAt`/`updatedAt`. Student directory responses do not expose email, password, address, date of birth or other unnecessary personal data. A cross-tenant or missing related resource returns `404 NOT_FOUND`; duplicate codes/numbers return `409 CONFLICT`. This initial slice has no update, delete or status-transition operation.

## Document management APIs — initial slice

All routes require a bearer token and derive tenant scope from the verified principal. Local development uses a private filesystem storage adapter; no unauthenticated or public storage URL is returned. The initial supported uploads are PDF, JPEG, PNG and valid UTF-8 plain text, up to 10 MiB.

| Method and path | Access | Behavior |
|---|---|---|
| `POST /api/v1/documents` | Institution admin, IQAC coordinator or faculty | Multipart fields: `title`, `category`, optional `departmentId`, and `file`. Creates a logical document and immutable version 1. |
| `GET /api/v1/documents?page=0&size=20&q=&category=&departmentId=` | Institution admin, IQAC coordinator or faculty | Lists only the tenant's documents; faculty results are restricted to documents they own. Supports title search and category/department filters. |
| `POST /api/v1/documents/{documentId}/versions` | Institution admin, IQAC coordinator, or owning faculty | Adds an immutable version to an existing document. |
| `GET /api/v1/documents/{documentId}/versions` | Institution admin, IQAC coordinator, or owning faculty | Lists safe metadata for version history; never returns storage keys. |
| `GET /api/v1/documents/{documentId}/versions/{versionNumber}/content` | Institution admin, IQAC coordinator, or owning faculty | Streams private content only after tenant and ownership authorization. |

The storage adapter uses generated opaque keys, validates file signatures against the allow-listed content type, records a SHA-256 checksum and cleans newly written content if the database transaction rolls back. Cross-tenant or unauthorized resource access returns `404 NOT_FOUND`. Production object storage is not configured by this local-only slice and remains a deployment blocker; do not enable the local adapter in production.

## Accreditation evidence APIs — initial workflow

Frameworks and criteria are institution-configurable and versioned by `(name, versionLabel)`; the platform does not assert that a configured checklist is current or guarantees accreditation. All routes derive tenant scope from the verified principal.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/v1/accreditation/frameworks?page=0&size=20` | Institution admin, IQAC coordinator, faculty | Lists configured framework versions. |
| `POST /api/v1/accreditation/frameworks` | Institution admin or IQAC coordinator | Creates a framework name and version label. |
| `GET /api/v1/accreditation/criteria?frameworkId=&page=0&size=20` | Institution admin, IQAC coordinator, faculty | Lists tenant criteria, optionally filtered to a same-tenant framework. |
| `POST /api/v1/accreditation/criteria` | Institution admin or IQAC coordinator | Adds criterion code/title/category/description to a same-tenant framework version. |
| `GET /api/v1/accreditation/evidence/owners` | Institution admin or IQAC coordinator | Lists active same-tenant faculty and IQAC users eligible to own an assignment. |
| `GET /api/v1/accreditation/evidence?academicYear=&status=&departmentId=&page=0&size=20` | Institution admin, IQAC coordinator, faculty | Lists assignments; faculty see only assignments owned by their user ID. |
| `POST /api/v1/accreditation/evidence` | Institution admin or IQAC coordinator | Assigns a criterion for an academic year, optional same-tenant department, owner and due date. Initial status is `NOT_STARTED`. |
| `POST /api/v1/accreditation/evidence/{id}/start` | Owner faculty, IQAC coordinator or institution admin | Moves `NOT_STARTED` to `IN_PROGRESS`. |
| `POST /api/v1/accreditation/evidence/{id}/submissions` | Owner faculty, IQAC coordinator or institution admin | Submits one or more same-tenant, immutable document-version references; submission history is retained. |
| `GET /api/v1/accreditation/evidence/{id}/submissions?page=0&size=20` | Institution admin, IQAC coordinator, owning faculty | Lists submission and attached-version metadata, not storage keys. |
| `POST /api/v1/accreditation/evidence/{id}/reviews` | IQAC coordinator | Requires a comment and decision `NEEDS_REVISION` or `VERIFIED`; records a separate review-history row. |
| `GET /api/v1/accreditation/evidence/{id}/reviews?page=0&size=20` | Institution admin, IQAC coordinator, owning faculty | Lists append-only review decisions and comments for the assignment. |
| `GET /api/v1/accreditation/reports/missing?academicYear=&departmentId=` | Institution admin or IQAC coordinator | Lists assignments in `NOT_STARTED`, `IN_PROGRESS` or `NEEDS_REVISION`. |

Allowed evidence transitions: `NOT_STARTED → IN_PROGRESS → SUBMITTED`; a review changes `SUBMITTED → NEEDS_REVISION` or `SUBMITTED → VERIFIED`; an owner may submit a new numbered revision from `NEEDS_REVISION`. `VERIFIED` is terminal for that assignment. Cross-tenant IDs are hidden as `404 NOT_FOUND`; every create, submit and review transition is audited. This workflow does not include report PDF/CSV exports or a claim that evidence is regulator-approved.

## Training API — initial skill catalog

All routes require a bearer access token. The server derives `institutionId` from the verified `institution_id` token claim; clients cannot choose or override the tenant.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/v1/training/skills?page=0&size=20` | Authenticated institution member | Lists only skills belonging to the caller's institution, ordered by name. `size` is limited to 100. |
| `POST /api/v1/training/skills` | `TRAINER` or `PLACEMENT_OFFICER` | Creates a skill in the caller's institution. `name` is required (1–120 characters); `description` is optional (up to 1000 characters). |

The collection response uses the shared pagination shape. Skill responses include `id`, `institutionId`, `name`, `description`, `createdAt` and `updatedAt`. Duplicate names within one institution return `409 CONFLICT`; the same skill name may be used by another institution. No update or deletion operation is exposed in this initial slice.

## Training workflows — initial lifecycle slice

All routes below require a bearer token. Institution scope comes from the verified `institution_id` claim. Cross-institution IDs are returned as `404 NOT_FOUND`. Validated input errors return `400`; invalid lifecycle transitions return `409 INVALID_TRANSITION`; every successful create, publish, enrollment, submit, grade, feedback, activation or seed write is audited.

| Method and path | Access | Behavior |
|---|---|---|
| `GET/POST /api/v1/training/job-roles` | Authenticated member / `TRAINER`, `PLACEMENT_OFFICER` | List tenant roles / create an internally defined role. |
| `GET/POST /api/v1/training/job-roles/{roleId}/skills` | Authenticated member / `TRAINER`, `PLACEMENT_OFFICER` | List or set same-tenant skill target levels (1–5). |
| `GET/POST /api/v1/training/pathways` | Authenticated member / `TRAINER`, `PLACEMENT_OFFICER` | List pathway versions / create next draft version for a name. |
| `GET/POST /api/v1/training/pathways/{pathwayId}/modules` | Authenticated member / `TRAINER`, `PLACEMENT_OFFICER` | List ordered modules / append a module to a draft (`sequence=0` selects the next sequence). |
| `POST /api/v1/training/pathways/{pathwayId}/publish` | `TRAINER`, `PLACEMENT_OFFICER` | Publish a draft only after it has at least one module. Published versions are immutable. |
| `GET/POST /api/v1/training/cohorts` | Authenticated member / `TRAINER`, `PLACEMENT_OFFICER` | List tenant cohorts (students see their own) / create a planned cohort with a same-tenant trainer and published pathway. |
| `POST /api/v1/training/cohorts/{cohortId}/activate` | `TRAINER`, `PLACEMENT_OFFICER` | Move `PLANNED` to `ACTIVE`. |
| `POST /api/v1/training/cohorts/{cohortId}/enrollments` | `PLACEMENT_OFFICER` | Enroll an active same-tenant student profile. Duplicate enrollment is rejected by a unique constraint. |
| `GET/POST /api/v1/training/cohorts/{cohortId}/assignments` | Authenticated member / `TRAINER` | List eligible cohort assignments / create an assignment or project with rubric items for an active cohort. |
| `POST /api/v1/training/assignments/{assignmentId}/attempts` | `STUDENT` | Submit an attempt only for the student's enrolled cohort; late submissions are rejected. |
| `GET /api/v1/training/assignments/{assignmentId}/rubric` | `TRAINER` | Lists the assignment's ordered rubric items for grading. |
| `POST /api/v1/training/attempts/{attemptId}/grade` | `TRAINER` | Grade a submitted attempt once, with one bounded score per rubric item; persists total and feedback. |
| `GET /api/v1/training/results` | Authenticated member | Trainers and placement officers see tenant results; students see only their own attempts/results. |
| `GET/POST /api/v1/training/cohorts/{cohortId}/feedback` | Authenticated member / `TRAINER` | List permitted cohort feedback / add feedback only for an enrolled student. |

Transitions are `DRAFT → PUBLISHED` for pathways, `PLANNED → ACTIVE` for cohorts, and `SUBMITTED → GRADED` for assessment attempts. No endpoint permits editing a published pathway or regrading an attempt. The dev-only `Synthetic Java and Spring Boot Foundations` pathway is explicitly marked synthetic and internally defined; it makes no employer or placement claims. Aggregate skill-gap analytics are not included in this slice.

## Institutional dashboard and exports

Reporting data is derived from the authenticated `institution_id`; clients cannot select
another institution. Evidence dashboard and evidence exports require `INSTITUTION_ADMIN`
or `IQAC_COORDINATOR`. The training cohort export requires `INSTITUTION_ADMIN` or
`PLACEMENT_OFFICER`.

| Method | Path | Access | Contract |
|---|---|---|---|
| `GET` | `/api/v1/reports/dashboard?academicYear=YYYY-YY` | Institution admin, IQAC coordinator, placement officer | Returns evidence counts and training participation metrics for the authenticated institution. `academicYear` is required. |
| `GET` | `/api/v1/reports/evidence.csv?academicYear=YYYY-YY` | Institution admin, IQAC coordinator | Downloads institution/year-scoped evidence assignment rows as formula-safe CSV. Maximum 10,000 rows. |
| `GET` | `/api/v1/reports/evidence.pdf?academicYear=YYYY-YY` | Institution admin, IQAC coordinator | Downloads a PDF status summary and criterion-code listing for the institution/year. |
| `GET` | `/api/v1/reports/training/cohorts/{cohortId}.csv` | Institution admin, placement officer | Downloads institution-scoped student attempt/result rows for the cohort. A cohort outside the institution returns `404`. Maximum 10,000 rows. |

### Dashboard metric definitions

- `evidenceStatusCounts`: count of evidence assignments by status for the requested academic year.
- `pendingReview`: assignments with status `SUBMITTED`.
- `overdue`: assignments with a due date before the current database date and status other than `VERIFIED`.
- `missingEvidence`: assigned criteria whose assignment is `NOT_STARTED`, `IN_PROGRESS`, or `NEEDS_REVISION`. This does not count criteria without an assignment.
- `activeCohorts`: training cohorts with status `ACTIVE`.
- `enrolledStudents`: distinct students with an `ENROLLED` enrollment in an active cohort.
- `completedStudents`: distinct students with at least one `COMPLETED` enrollment.
- `skillGapMetricsAvailable` is currently `false`. The training module does not yet store a student's assessed skill level against a job-role target, so the dashboard does not infer or fabricate skill-gap values.

Report exports write an audit event. CSV fields are quoted and values beginning with spreadsheet formula prefixes (including prefixes after whitespace) are apostrophe-prefixed. Report row limits prevent unbounded in-memory exports.
