package com.collegeops.reporting;

import com.collegeops.accreditation.AccreditationCriterion;
import com.collegeops.accreditation.AccreditationCriterionRepository;
import com.collegeops.accreditation.AccreditationFramework;
import com.collegeops.accreditation.AccreditationFrameworkRepository;
import com.collegeops.accreditation.EvidenceAssignment;
import com.collegeops.accreditation.EvidenceAssignmentRepository;
import com.collegeops.accreditation.EvidenceStatus;
import com.collegeops.common.AuditEventRepository;
import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collegeops-reporting-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class ReportingIntegrationTest {
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstitutionRepository institutions;
    @Autowired
    private UserRepository users;
    @Autowired
    private AccreditationFrameworkRepository frameworks;
    @Autowired
    private AccreditationCriterionRepository criteria;
    @Autowired
    private EvidenceAssignmentRepository assignments;
    @Autowired
    private AuditEventRepository auditEvents;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Institution alpha;
    private UUID betaCohortId;
    private UUID alphaCohortId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from training_attempt_rubric_scores");
        jdbcTemplate.update("delete from training_assessment_attempts");
        jdbcTemplate.update("delete from training_assignment_rubric_items");
        jdbcTemplate.update("delete from training_assignments");
        jdbcTemplate.update("delete from training_enrollments");
        jdbcTemplate.update("delete from training_feedback");
        jdbcTemplate.update("delete from training_cohorts");
        jdbcTemplate.update("delete from training_pathway_modules");
        jdbcTemplate.update("delete from training_pathways");
        jdbcTemplate.update("delete from students");
        jdbcTemplate.update("delete from academic_programs");
        jdbcTemplate.update("delete from departments");
        assignments.deleteAll();
        criteria.deleteAll();
        frameworks.deleteAll();
        auditEvents.deleteAll();
        users.deleteAll();
        institutions.deleteAll();

        alpha = institutions.save(new Institution("Alpha College", "ALPHA-REPORT"));
        Institution beta = institutions.save(new Institution("Beta College", "BETA-REPORT"));
        UserEntity admin = users.save(user(alpha, "admin@alpha.edu", UserRole.INSTITUTION_ADMIN));
        users.save(user(alpha, "faculty@alpha.edu", UserRole.FACULTY));
        UserEntity placementOfficer = users.save(
                user(alpha, "placement@alpha.edu", UserRole.PLACEMENT_OFFICER));
        UserEntity betaAdmin = users.save(user(beta, "admin@beta.edu", UserRole.INSTITUTION_ADMIN));

        AccreditationFramework alphaFramework = frameworks.save(
                new AccreditationFramework(alpha, "Internal", "2026"));
        AccreditationFramework betaFramework = frameworks.save(
                new AccreditationFramework(beta, "Internal", "2026"));
        AccreditationCriterion alphaCriterion = criteria.save(new AccreditationCriterion(
                alpha, alphaFramework, "A1", "=HYPERLINK(\"https://example.invalid\",\"Open\")",
                "QUALITY", null));
        AccreditationCriterion betaCriterion = criteria.save(new AccreditationCriterion(
                beta, betaFramework, "BETA-SECRET", "Private criterion", "QUALITY", null));

        EvidenceAssignment pending = new EvidenceAssignment(alpha, alphaCriterion, "2026-27", null,
                admin.getId(), LocalDate.now().minusDays(1));
        pending.setStatus(EvidenceStatus.SUBMITTED);
        assignments.save(pending);
        EvidenceAssignment missing = new EvidenceAssignment(alpha, alphaCriterion, "2026-27", null,
                admin.getId(), LocalDate.now().minusDays(1));
        missing.setStatus(EvidenceStatus.NEEDS_REVISION);
        assignments.save(missing);
        EvidenceAssignment otherTenant = new EvidenceAssignment(beta, betaCriterion, "2026-27", null,
                betaAdmin.getId(), LocalDate.now().minusDays(1));
        otherTenant.setStatus(EvidenceStatus.SUBMITTED);
        assignments.save(otherTenant);

        UUID betaPathwayId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into training_pathways
                    (id, institution_id, name, version, status, synthetic, created_at, updated_at)
                values (?, ?, ?, 1, 'PUBLISHED', false, ?, ?)
                """,
                betaPathwayId, beta.getId(), "Private pathway", Instant.now(), Instant.now());
        betaCohortId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into training_cohorts
                    (id, institution_id, pathway_id, name, status, start_date, trainer_user_id, created_at, updated_at)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                """,
                betaCohortId, beta.getId(), betaPathwayId, "Private cohort",
                java.sql.Date.valueOf(LocalDate.now()), betaAdmin.getId(), Instant.now(), Instant.now());

        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID pathwayId = UUID.randomUUID();
        alphaCohortId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into departments (id, institution_id, name, code, status, created_at, updated_at)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                departmentId, alpha.getId(), "Synthetic Computing", "SYN-CS", Instant.now(), Instant.now());
        jdbcTemplate.update(
                """
                insert into academic_programs
                    (id, institution_id, department_id, name, code, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                programId, alpha.getId(), departmentId, "Synthetic Program", "SYN-BTECH",
                Instant.now(), Instant.now());
        jdbcTemplate.update(
                """
                insert into students
                    (id, institution_id, user_id, department_id, academic_program_id,
                     student_number, status, created_at, updated_at)
                values (?, ?, null, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                studentId, alpha.getId(), departmentId, programId, "SYN-STUDENT-001",
                Instant.now(), Instant.now());
        jdbcTemplate.update(
                """
                insert into training_pathways
                    (id, institution_id, name, version, status, synthetic, created_at, updated_at)
                values (?, ?, ?, 1, 'PUBLISHED', true, ?, ?)
                """,
                pathwayId, alpha.getId(), "Synthetic pathway", Instant.now(), Instant.now());
        jdbcTemplate.update(
                """
                insert into training_cohorts
                    (id, institution_id, pathway_id, name, status, start_date, trainer_user_id, created_at, updated_at)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                """,
                alphaCohortId, alpha.getId(), pathwayId, "Synthetic cohort",
                java.sql.Date.valueOf(LocalDate.now()), placementOfficer.getId(), Instant.now(), Instant.now());
        jdbcTemplate.update(
                """
                insert into training_enrollments
                    (id, institution_id, cohort_id, student_id, status, enrolled_at)
                values (?, ?, ?, ?, 'ENROLLED', ?)
                """,
                UUID.randomUUID(), alpha.getId(), alphaCohortId, studentId, Instant.now());
        jdbcTemplate.update(
                """
                insert into training_assignments
                    (id, institution_id, cohort_id, module_id, title, instructions, due_at, created_by, created_at)
                values (?, ?, ?, null, ?, ?, null, ?, ?)
                """,
                assignmentId, alpha.getId(), alphaCohortId, "Synthetic assignment",
                "Synthetic test assignment", placementOfficer.getId(), Instant.now());
        jdbcTemplate.update(
                """
                insert into training_assessment_attempts
                    (id, institution_id, assignment_id, student_id, attempt_number, status,
                     submission_text, score, feedback, submitted_at)
                values (?, ?, ?, ?, 1, 'GRADED', ?, 80, ?, ?)
                """,
                UUID.randomUUID(), alpha.getId(), assignmentId, studentId, "Synthetic submission",
                "=HYPERLINK(\"https://example.invalid\",\"Feedback\")", Instant.now());
    }

    @Test
    void dashboardAggregatesOnlyAuthenticatedInstitutionEvidence() throws Exception {
        String token = login("ALPHA-REPORT", "admin@alpha.edu");

        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("academicYear", "2026-27")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingReview").value(1))
                .andExpect(jsonPath("$.overdue").value(2))
                .andExpect(jsonPath("$.missingEvidence").value(1))
                .andExpect(jsonPath("$.evidenceStatusCounts.SUBMITTED").value(1))
                .andExpect(jsonPath("$.evidenceStatusCounts.NEEDS_REVISION").value(1))
                .andExpect(jsonPath("$.skillGapMetricsAvailable").value(false));
    }

    @Test
    void evidenceCsvIsTenantScopedAndEscapesSpreadsheetFormulas() throws Exception {
        String token = login("ALPHA-REPORT", "admin@alpha.edu");

        MvcResult result = mockMvc.perform(get("/api/v1/reports/evidence.csv")
                        .param("academicYear", "2026-27")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("evidence-report-2026-27.csv")))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"'=HYPERLINK"));
        assertFalse(csv.contains("BETA-SECRET"));
        assertEquals(1, auditEvents.count());
    }

    @Test
    void pdfReportIsValidAndEvidenceExportsRequireManagerRole() throws Exception {
        String adminToken = login("ALPHA-REPORT", "admin@alpha.edu");
        String facultyToken = login("ALPHA-REPORT", "faculty@alpha.edu");

        byte[] pdf = mockMvc.perform(get("/api/v1/reports/evidence.pdf")
                        .param("academicYear", "2026-27")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));

        mockMvc.perform(get("/api/v1/reports/evidence.csv")
                        .param("academicYear", "2026-27")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void trainingCohortExportHidesOtherInstitutions() throws Exception {
        String token = login("ALPHA-REPORT", "placement@alpha.edu");

        mockMvc.perform(get("/api/v1/reports/training/cohorts/{cohortId}.csv", betaCohortId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCohortCsvIncludesOnlyAuthorizedCohortResults() throws Exception {
        String token = login("ALPHA-REPORT", "placement@alpha.edu");

        String csv = mockMvc.perform(get("/api/v1/reports/training/cohorts/{cohortId}.csv", alphaCohortId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(csv.contains("SYN-STUDENT-001"));
        assertTrue(csv.contains("\"'=HYPERLINK"));
        assertFalse(csv.contains("BETA-REPORT"));
    }

    private UserEntity user(Institution institution, String email, UserRole role) {
        return new UserEntity(institution, email, "Report User", passwordEncoder.encode(PASSWORD), Set.of(role));
    }

    private String login(String institutionCode, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institutionCode":"%s","email":"%s","password":"%s"}
                                """.formatted(institutionCode, email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
