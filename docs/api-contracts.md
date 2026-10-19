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

## Training API — initial skill catalog

All routes require a bearer access token. The server derives `institutionId` from the verified `institution_id` token claim; clients cannot choose or override the tenant.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/v1/training/skills?page=0&size=20` | Authenticated institution member | Lists only skills belonging to the caller's institution, ordered by name. `size` is limited to 100. |
| `POST /api/v1/training/skills` | `TRAINER` or `PLACEMENT_OFFICER` | Creates a skill in the caller's institution. `name` is required (1–120 characters); `description` is optional (up to 1000 characters). |

The collection response uses the shared pagination shape. Skill responses include `id`, `institutionId`, `name`, `description`, `createdAt` and `updatedAt`. Duplicate names within one institution return `409 CONFLICT`; the same skill name may be used by another institution. No update or deletion operation is exposed in this initial slice.
