# CollegeOps + IndustryReady
## Multi-Agent Autonomous Development Specification

**Project type:** B2B SaaS for colleges and universities  
**Initial market:** Engineering colleges in India  
**Development model:** Two collaborating coding agents  
**Initial team:** Solo founder + two AI coding agents  
**Backend:** Java 21+ and Spring Boot  
**Frontend:** React + TypeScript  
**Database:** PostgreSQL  
**Architecture:** Modular monolith with REST APIs

---

# 1. Mission

Build a production-minded, multi-tenant college intelligence platform that combines:

1. Institutional digital records and document management.
2. Accreditation evidence tracking and reporting support.
3. Industry-aligned student training and skill assessments.
4. Institutional dashboards for academic quality, skill gaps and training outcomes.

The platform is sold to colleges as a B2B institutional subscription. College administrators, principals, IQAC coordinators, faculty members and training and placement officers are the primary institutional users. Students use the training and assessment features provisioned by their institutions.

The goal is not to create a collection of disconnected dashboards. Build one integrated system with shared institutional identity, permissions, data contracts and reporting.

**Primary principle:** Build working, tested and integrated software. Do not stop after producing plans, mockups, placeholder pages or documentation.

# 2. Product Requirements

## 2.1 Institutional Records

Implement:

- Institution and department management.
- Academic programs and courses.
- Faculty and student profiles.
- Academic-year and semester organization.
- Document uploads with metadata and version history.
- Search and filtering.
- Record ownership and review status.
- Secure document access.
- Audit trails for important changes.
- Import and export capabilities where appropriate.

## 2.2 Accreditation Evidence Management

Create a configurable evidence management system supporting applicable institutional and program-level workflows.

Required capabilities:

- Evidence criteria and requirement categories.
- Framework, version, academic year and department association.
- Evidence submission and document attachment.
- Status tracking: NOT_STARTED, IN_PROGRESS, SUBMITTED, NEEDS_REVISION and VERIFIED.
- Reviewer comments and revision history.
- Missing-evidence reports.
- Assigned owners and due dates.
- Evidence completeness dashboards.
- PDF and CSV report generation.
- Source references and traceability for configured criteria.

Support configurable mappings for NBA, NAAC, AICTE-related documentation and NIRF data collection where applicable.

Do not hard-code regulatory requirements as permanently valid. Framework definitions must be versioned and maintainable.

Do not claim that document completeness guarantees accreditation, approval or ranking.

## 2.3 Industry Readiness and Training

Implement an initial Java/Spring Boot developer pathway.

Suggested skill sequence:

1. Java fundamentals and object-oriented programming.
2. Collections, exception handling and clean coding.
3. SQL and relational database fundamentals.
4. REST API design.
5. Spring Boot and dependency injection.
6. Persistence with Spring Data JPA.
7. Authentication and API security fundamentals.
8. Testing and debugging.
9. Git, GitHub and basic CI.
10. Practical project implementation.

The pathway must be configurable. It is not an official hiring curriculum for any company unless validated by that company.

Required features:

- Job-role and skill catalog.
- Role-specific skill requirements.
- Training pathways and modules.
- Student enrollment in institution-managed cohorts.
- Assignments and practical projects.
- Assessments with explicit rubrics.
- Assessment results and skill-level tracking.
- Baseline versus final assessment comparisons.
- Cohort-level performance reports.
- Trainer feedback and progress tracking.
- CSV exports for placement teams.

Use actual employer-provided job requirements only when authorized. Otherwise, label skill frameworks as internally defined or based on publicly available job descriptions.

Do not guarantee employment or infer that course completion alone proves job readiness.

## 2.4 Institutional Intelligence Dashboard

Provide role-appropriate dashboards showing:

- Institutional record completeness.
- Evidence pending review.
- Missing documents and overdue actions.
- Training participation and completion.
- Skill assessment distributions.
- Skill gaps by cohort and job role.
- Assessment improvement.
- Exportable institutional reports.

Keep metrics transparent. Every metric must have a defined calculation and source data.

# 3. Agent Organization

Two agents work in parallel under this specification.

## Agent 1 — Platform and Institutional Intelligence Lead

**Primary responsibility:** Build the shared platform foundation and the institutional records and accreditation modules.

### Ownership

- Repository structure and application bootstrap.
- Shared frontend shell and navigation.
- Authentication and role-based authorization.
- Institution and department management.
- Shared database conventions and migration strategy.
- Institutional records.
- Document metadata and secure storage integration.
- Accreditation criteria and evidence workflows.
- Evidence review, version history and audit trails.
- Institutional dashboard foundation.
- Shared API error handling.
- Docker-based local development.
- Integration coordination and final system verification.

### Required outputs

- Working Spring Boot application.
- Working React application shell.
- Database migrations for shared and institutional entities.
- Institution-scoped authorization.
- Records and evidence APIs.
- Records and evidence interfaces.
- API documentation.
- Automated tests.
- `docs/agent-1-status.md`.

Agent 1 must establish the shared contracts before Agent 2 builds dependent functionality.

## Agent 2 — Industry Readiness and Training Lead

**Primary responsibility:** Build the training, assessments, skill-gap analytics and industry-readiness module.

### Ownership

- Skill catalog and job-role mapping.
- Training pathways and modules.
- Cohorts and student enrollments.
- Assessments and rubrics.
- Assignment and project tracking.
- Assessment results and progress analytics.
- Training dashboard and student-facing interfaces.
- Trainer feedback workflows.
- Cohort-level skill-gap reports.
- Training-related API tests.
- `docs/agent-2-status.md`.

### Required outputs

- Working training module integrated into the shared application.
- Database migrations for training entities.
- Backend services and REST endpoints.
- React interfaces for students, trainers and placement officers.
- Sample training pathway and assessment data.
- Automated tests.
- API documentation.
- Integration notes and known limitations.

Agent 2 must use the agreed shared authentication, institution and user models. Do not create a second authentication system or a separate institution database.

# 4. Shared Architecture Contract

The two agents must agree on these conventions before implementing cross-module dependencies.

## Backend

- Java 21 or a newer compatible LTS release.
- Spring Boot 3.x or a compatible stable release.
- Maven.
- Spring Web.
- Spring Security.
- Spring Data JPA.
- Jakarta Bean Validation.
- PostgreSQL.
- Flyway database migrations.
- Spring Boot Actuator.
- OpenAPI documentation.
- JUnit 5 and Spring Boot testing support.

Select compatible dependency versions and document the exact versions in the build files.

Use a modular monolith, not microservices.

Suggested package structure:

    com.collegeops
      config/
      security/
      common/
      institution/
      users/
      records/
      documents/
      accreditation/
      training/
      assessment/
      analytics/
      reporting/

Each module should separate its API controllers, services, repositories, entities and DTOs where appropriate.

Business logic must reside in services, not controllers.

Do not expose JPA entities directly as public API responses.

## Frontend

- React.
- TypeScript with strict type checking.
- Vite.
- React Router.
- A consistent component and styling system.
- A centralized API client.
- Typed request and response models.
- Reusable form, table, dialog, loading, error and empty states.

Use a consistent application layout and navigation structure.

Do not build duplicate navigation shells or competing design systems.

## Database and tenancy

Use a shared PostgreSQL database with institution-scoped records.

Every institution-owned record must be associated with its institution directly or through an enforceable parent relationship.

Enforce tenant isolation in the backend. Never rely on frontend filtering alone.

Use UUIDs or another consistent identifier strategy. Use explicit foreign keys, indexes, uniqueness constraints and timestamps.

Store uploaded files in object storage or a suitable local development adapter. Store file metadata and storage keys in PostgreSQL rather than placing large document contents directly into database rows.

Never commit credentials, API keys or real student data.

# 5. Shared API Contract

Use `/api/v1` as the API prefix.

Expected endpoint families include:

    /api/v1/auth
    /api/v1/institutions
    /api/v1/departments
    /api/v1/users
    /api/v1/students
    /api/v1/faculty
    /api/v1/documents
    /api/v1/accreditation/frameworks
    /api/v1/accreditation/criteria
    /api/v1/accreditation/evidence
    /api/v1/training/skills
    /api/v1/training/job-roles
    /api/v1/training/pathways
    /api/v1/training/cohorts
    /api/v1/training/enrollments
    /api/v1/training/assessments
    /api/v1/training/results
    /api/v1/analytics

These are planned endpoint families, not permission to implement incompatible schemas independently.

Before Agent 2 implements endpoints, Agent 1 must publish the agreed models for:

- Institution.
- User.
- Role and permissions.
- Student.
- Department.
- Academic program.
- Audit metadata.
- Standard API errors.

Use DTOs and validation. Apply pagination and sorting to collection endpoints.

Use a consistent error response containing an error code, human-readable message and relevant validation details.

Document authentication requirements and authorization rules for each endpoint.

# 6. Agent Communication Protocol

The repository is the shared source of truth. Neither agent should depend on undocumented chat history or assume that the other agent knows about uncommitted changes.

Create these files:

    docs/
      architecture.md
      api-contracts.md
      database-design.md
      integration-plan.md
      agent-1-status.md
      agent-2-status.md
      decisions.md
      test-plan.md
      known-issues.md

## Before starting a task

Each agent must:

1. Read this README.
2. Read the architecture and API contracts.
3. Read both agent status files.
4. Inspect the current codebase and Git status.
5. Check existing migrations, endpoints and tests.
6. Identify dependencies and file ownership.
7. Record the task and expected deliverables.

Do not overwrite another agent's work or recreate an existing module without first inspecting it.

## After completing a task

Update the relevant status file with:

- Task and owner.
- Current status: NOT_STARTED, IN_PROGRESS, BLOCKED, READY_FOR_INTEGRATION or DONE.
- Files created or changed.
- API or schema changes.
- Tests executed and their results.
- Decisions made.
- Outstanding issues.
- Dependencies for the other agent.
- Exact next action.

Use ISO 8601 timestamps with timezone where practical.

## Change communication

When an agent changes a shared contract, it must:

1. Explain the reason for the change in `docs/decisions.md`.
2. Update `docs/api-contracts.md` and relevant architecture documentation.
3. Notify the other agent through the available shared task or communication mechanism.
4. Identify required changes to dependent code.
5. Avoid marking the work complete until dependent changes are addressed.

If the execution environment does not provide direct agent-to-agent messaging, use the shared status files and Git pull requests as the communication channel.

Do not claim to have notified another agent unless an actual notification was sent.

# 7. Git and File Ownership Rules

Use one repository and a shared integration branch.

Recommended branches:

    main
    integration
    agent-1/institutional-platform
    agent-2/industry-readiness

`main` must remain deployable. Use pull requests or an equivalent review process to integrate completed work.

Agent 1 owns the application bootstrap, shared contracts, security configuration and shared frontend shell.

Agent 2 owns the training domain and its dedicated UI components.

Both agents must coordinate before modifying shared configuration, security, database migration ordering, global styles, navigation or common DTOs.

Do not have both agents independently modify the same file.

Each agent must:

1. Inspect the latest integration branch.
2. Create or update its assigned branch.
3. Make small, coherent commits.
4. Run tests and build checks.
5. Update its status file.
6. Open or prepare a reviewable change.
7. Resolve conflicts without discarding another agent's work.
8. Re-run relevant tests after integration.

Never use destructive Git commands to erase another agent's work. Never force-push shared branches without explicit authorization.

# 8. Execution Phases

## Phase 0 — Repository Discovery and Design

Both agents inspect the repository and existing implementation.

Agent 1 defines the architecture, common data model, security conventions and application bootstrap.

Agent 2 reviews the proposed training data model, identifies dependencies and proposes the module's API requirements.

**Exit criteria:**

- Architecture documented.
- File ownership established.
- Shared entity and API contracts agreed.
- Initial database migration strategy documented.
- Both agents acknowledge dependencies.

Do not spend excessive time producing design documents. Once contracts are sufficiently clear, proceed to implementation.

## Phase 1 — Runnable Foundation

Agent 1 builds the application bootstrap, database connectivity, migrations, shared UI, authentication and role-based access.

Agent 2 builds against the agreed contracts and prepares the training module's schema, services and tests without independently redefining shared models.

**Exit criteria:**

- Backend starts successfully.
- Frontend starts successfully.
- Database migrations run from a clean database.
- Authentication and authorization have automated tests.
- Local development instructions work.

## Phase 2 — Parallel Module Implementation

Agent 1 implements institutional records, document management, evidence workflows and reporting.

Agent 2 implements skills, pathways, cohorts, assessments, results and training analytics.

Both agents maintain their own tests and update status files.

**Exit criteria:**

- Each module works independently against the shared contracts.
- APIs are documented.
- Validation and error handling are implemented.
- No module relies on mock responses for core functionality.
- Tests pass for the implemented features.

## Phase 3 — Integration

Connect the modules through shared identity and institution context.

Implement:

- Shared navigation.
- Role-aware access.
- Cross-module dashboards.
- Consistent API errors and loading states.
- Institution-scoped analytics.
- Integrated document and report workflows.

**Exit criteria:**

- A user can sign in and access authorized modules.
- Institutional records and training results appear in the correct institution's dashboards.
- Unauthorized cross-institution access is rejected.
- A clean installation runs using the documented procedure.

## Phase 4 — End-to-End Verification

Run the complete test plan against a clean environment.

Test both successful and failure scenarios.

Fix integration defects before expanding the feature set.

**Exit criteria:**

- Backend and frontend production builds succeed.
- Database migrations work from scratch.
- Critical API integration tests pass.
- Critical end-to-end user journeys pass.
- No known critical security issues remain unresolved.
- Setup and deployment documentation is accurate.

## Phase 5 — Pilot Readiness

Prepare:

- Synthetic demo data.
- A sample accreditation evidence workflow.
- A Java/Spring Boot training pathway.
- An institution management dashboard.
- A student skill-gap report.
- A short pilot onboarding guide.
- A documented list of current limitations.

Do not use fabricated customer testimonials, fake placement outcomes or unverified compliance claims.

# 9. Definition of Done

A feature is not complete merely because code has been generated.

Every feature must satisfy all applicable conditions:

- Implements the stated acceptance criteria.
- Follows the agreed architecture.
- Includes server-side validation.
- Enforces authentication and authorization.
- Enforces institution-level access boundaries.
- Handles errors and empty states.
- Includes appropriate automated tests.
- Uses database migrations for schema changes.
- Includes relevant API documentation.
- Has been built and tested in the actual project environment.
- Does not break existing functionality.
- Is documented in the agent status file.

If a feature cannot be completed, mark it BLOCKED and record the exact blocker and next action. Never mark an untested feature as DONE.

# 10. Security and Data Integrity

Implement security from the beginning.

Requirements:

- Role-based authorization enforced by the backend.
- Institution-level tenant isolation.
- Secure password hashing if local authentication is implemented.
- Safe session or token handling.
- Input validation and parameterized database access.
- File size and type restrictions.
- Secure file access and storage keys.
- Audit logs for sensitive record changes.
- Secrets managed through environment variables or a secret manager.
- No real student data in development fixtures.
- Backup and restoration instructions.
- Appropriate retention and deletion policies.
- Protection against insecure direct object references.

Use synthetic data for demonstrations and tests.

Avoid storing unnecessary personal information. Document how student data is collected, accessed, retained and deleted.

# 11. Testing Strategy

Include:

1. Unit tests for business logic.
2. Repository and database integration tests.
3. API tests for success and failure cases.
4. Authorization tests for each important role.
5. Tenant-isolation tests.
6. File access and upload validation tests.
7. Frontend component and form tests.
8. Cross-module integration tests.
9. Critical end-to-end workflows.
10. Clean installation and production build checks.

Minimum critical journeys:

**Journey A — Institutional evidence**

An authorized faculty member submits evidence, an authorized reviewer reviews it, a revision is requested if necessary, and the final verified status appears in the institutional dashboard.

**Journey B — Student training**

A placement officer creates a training cohort, enrolls authorized students, students complete assessments, trainers review project submissions, and the dashboard shows the results.

**Journey C — Skill-gap analysis**

A placement officer selects a job role, the platform compares its defined skill requirements against student assessment results, and the platform displays transparent cohort-level gaps.

**Journey D — Security isolation**

A user from Institution A attempts to access Institution B's student, document, assessment or evidence records. The request must be denied without exposing the other institution's data.

# 12. Definition of the First Release

The first release must provide a coherent, integrated workflow rather than maximum feature count.

Release scope:

- Institution and department setup.
- User roles and authorization.
- Faculty and student records.
- Document upload and metadata.
- Configurable evidence criteria.
- Evidence review and completeness reporting.
- One Java/Spring Boot training pathway.
- Cohort management.
- Practical assessments and results.
- Skill-gap analytics.
- PDF/CSV exports.
- Audit history.
- Setup documentation and synthetic demo data.

Defer complex AI features until the underlying data is reliable.

If AI-assisted document extraction is added later, extracted fields must retain their source references and be reviewed before becoming verified institutional records.

# 13. Final Instructions to Both Agents

You are responsible for delivering working software, not merely describing a solution.

Start by inspecting the repository. Preserve useful existing code. Identify the current implementation status before making changes.

Work in your assigned domain, follow shared contracts, communicate changes through repository artifacts, and test every meaningful feature.

Prioritize correctness, security, maintainability and integration over unnecessary complexity.

Do not invent API results, claim tests passed when they were not run, or silently ignore blockers.

When one module depends on the other, document the dependency and coordinate using the shared communication protocol.

At the end of each task, report:

1. What was implemented.
2. Which files changed.
3. Which tests ran and their actual results.
4. Whether the changes are integrated.
5. What remains incomplete.
6. The next concrete action.

**Final objective:** Deliver a runnable, secure, tested and integrated B2B college platform that can be demonstrated to a pilot institution and extended without rewriting its architecture.

Begin with Phase 0, then continue through the phases until all acceptance criteria are met. Do not stop at planning when implementation is possible.
