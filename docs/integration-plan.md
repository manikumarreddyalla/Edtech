# Integration Plan

## Status

Phase 0–1 plan, updated by Agent 1 on 2026-10-19.

## Ownership boundaries

| Area | Owner | Coordination required |
|---|---|---|
| Spring Boot bootstrap, shared configuration, security and auth | Agent 1 | Agent 2 consumes the published principal/authorization APIs |
| Institution, department, program, user, student and faculty shared models | Agent 1 | Notify Agent 2 before shared model/schema changes |
| Records, documents and accreditation | Agent 1 | API and migration changes documented in this repo |
| Training and assessment domain and dedicated UI | Agent 2 | Must use shared contracts; no parallel identity/tenant schema |
| Shared React shell, navigation, API client, global styles | Agent 1 | Agent 2 provides routes/components for integration; avoid parallel shell edits |
| Agent status files | Each agent edits own file | Read both before shared changes |
| Common build files, migration ordering, API error DTOs, global navigation | Shared | Coordinate before changing; explain breaking changes in `decisions.md` |

## Contract-to-integration sequence

1. Agent 1 publishes the logical architecture, API DTO/error/auth conventions and shared database model.
2. Agent 2 reviews the contract in its own status file and records any dependency/blocker. Agent 1 does not claim Agent 2 has acknowledged until that update is present.
3. Agent 1 implements the runnable API/UI shell and shared model/migration baseline. Publish exact build versions, API behavior and test evidence in Agent 1 status.
4. Agent 2 implements training features against those concrete models and APIs, using a coordinated migration version.
5. Integrate routes and menu entries in the Agent 1-owned shell after Agent 2 provides route/component interfaces.
6. Run backend/frontend builds and targeted integration/tenant-isolation tests after merging both modules.

## Shared API integration checklist

- All endpoints use `/api/v1`, DTO validation and documented role requirements.
- All records resolve institution scope from authenticated context and deny cross-tenant access.
- Shared entities use UUIDs and consistent timestamps.
- Errors use the common error DTO, including validation details and trace ID.
- Collection endpoints use bounded pagination and allow-listed sort fields.
- OpenAPI documents request/response schemas and authentication rules.
- Training results and accreditation data are not surfaced in cross-module dashboards until source data and metric calculations are defined.

## Database and UI integration checklist

- Agent 2 migrations reference existing shared tables and use unique Flyway versions.
- No module edits another module's tables through repository-level writes.
- React modules mount through the shared router/layout and use the centralized typed API client.
- Navigation is permission-aware for usability; backend authorization remains authoritative.
- Loading, empty, validation and error states use shared UI patterns.
- Synthetic demo data is isolated from production and contains no real student information.

## Current integration state

Agent 2 accepted the shared identity, tenant, API and migration contracts in its status file. The initial skill catalog route is mounted at `/training/skills` in the Agent 1 shell and uses the shared API client and auth context. Agent 1's initial department/program/student directory routes are implemented with tenant scoping, role guards, bounded pagination and transactional audit events. Agent 2 owns the training page and backend module. Full backend suite currently has one Agent 2-owned test failure in the training skill service fixture; see `docs/known-issues.md`. Broader institutional CRUD and evidence integration remain next.
