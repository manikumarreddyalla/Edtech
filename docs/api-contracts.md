# Shared API Contracts

## Status

Phase 0 baseline, published by Agent 1 on 2026-10-19. Agent 2 should use these shared DTO and authorization conventions and document any required change before implementing against a different shape.

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
