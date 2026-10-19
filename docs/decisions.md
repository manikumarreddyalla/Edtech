# Architecture and Contract Decisions

## 2026-10-19 — Shared Phase 0 baseline

- **Context:** Agent 2 is preparing the training module and requires stable shared identity, tenant, DTO, error and migration conventions.
- **Decision:** Use a modular monolith with Spring Boot, React/TypeScript and shared PostgreSQL; all REST endpoints live under `/api/v1`; shared IDs are UUIDs; API entities are DTOs; all tenant-owned data is institution-scoped and server-authorized.
- **Authentication baseline:** Use short-lived bearer access tokens for API calls. Token refresh/revocation details remain to be specified with the Phase 1 implementation and must be documented before frontend auth integration.
- **Tenant denial:** Do not disclose cross-tenant resource existence; return the documented `404` behavior for out-of-scope resource lookups.
- **Database baseline:** Use append-only Flyway migrations; Agent 1 owns shared schema; Agent 2 references shared identity tables and coordinates migration version allocation.
- **Alternatives considered:** Separate services/databases would duplicate authentication and tenant models and are inconsistent with the product specification. Trusting frontend filtering would not enforce tenant isolation. Exposing entities would couple public contracts to persistence.
- **Consequences:** Agent 2 can begin module work against shared IDs, role vocabulary and error/pagination shapes. Exact dependency versions and implementation details are established during Phase 1.

## Change process

Changes that affect shared identity, authorization, tenant scope, API errors, database conventions or frontend shell require a dated entry here, corresponding updates to `api-contracts.md` and/or `database-design.md`, and a status-file notice to the other agent before dependent work is considered integrated.
