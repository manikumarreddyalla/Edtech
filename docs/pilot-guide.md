# Synthetic Pilot Preparation Guide

## Status and safety

This guide prepares a synthetic demonstration; it does not authorize a production pilot.
Phase 4 and Phase 5 remain **BLOCKED** pending PostgreSQL verification, resolution of the
critical Spring advisories, production storage and malware controls, and completion of the
remaining onboarding and training analytics work.

Use only synthetic institution, staff, student, document and assessment data. Do not use
real student records or describe demo scores as employer outcomes, placement results,
regulatory compliance, accreditation success or ranking evidence. Frameworks and criteria
created in the demo are examples, not official or permanently valid checklists.

## Prerequisites

- JDK 21+, Maven 3.9+, Node.js 22+, npm and a PostgreSQL-capable local environment.
- Follow [local-development.md](./local-development.md) to configure PostgreSQL and
  environment variables. Keep `.env` private and use throwaway credentials.
- Start with an institution admin created by the dev-profile bootstrap. Create additional
  test users only through an approved development/test provisioning flow; do not reuse
  production identities.
- The Java/Spring Boot pathway seeded by the `dev` profile is synthetic and internally
  defined.

## Demonstration flow

1. Sign in to the demo institution and inspect the institutional dashboard for an academic
   year that has synthetic records.
2. Open **Accreditation** and create a clearly labeled demo framework version and criterion.
   Do not copy a regulator's framework into the demo without confirming version and reuse
   rights.
3. Assign the criterion to a synthetic faculty user. Sign in as that user, start the
   assignment, upload a synthetic PDF, and submit the selected document version.
4. Sign in as a synthetic IQAC coordinator, request a revision with a sample comment, then
   verify the revised submission after the synthetic faculty user resubmits.
5. Review status counts and download the evidence CSV/PDF as an authorized institution
   admin or IQAC coordinator. Use the training cohort CSV only for an authorized,
   institution-owned cohort.
6. Open **Skill catalog** and demonstrate only the pathway/cohort actions supported by the
   integrated training API. Do not describe pathway completion as proof of job readiness.
7. Review the dashboard metric definitions in [api-contracts.md](./api-contracts.md).
   Skill-gap values are intentionally unavailable until student skill-level assessments
   are stored against role targets.

## Current limitations

- The guide has not been run as a complete browser journey against PostgreSQL.
- Skill-gap analysis and baseline/final skill comparison are not implemented; the dashboard
  says so rather than inferring skill attainment from assignment scores.
- Production object storage, retention/deletion policy, malware scanning/content disarm,
  production user provisioning and token lifecycle are open.
- The PDF evidence report currently contains status totals and criterion codes, not attached
  document contents or a regulator compliance assessment.
- A real pilot requires a separate security and operations review and formal release-gate
  sign-off.
