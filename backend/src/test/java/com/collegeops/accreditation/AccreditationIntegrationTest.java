package com.collegeops.accreditation;

import com.collegeops.common.AuditEventRepository;
import com.collegeops.documents.DocumentRecordRepository;
import com.collegeops.documents.DocumentVersionRepository;
import com.collegeops.institution.Department;
import com.collegeops.institution.DepartmentRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collegeops-accreditation-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.documents.storage-root=target/test-accreditation-document-storage"
})
class AccreditationIntegrationTest {
    private static final String PASSWORD = "correct-horse-battery";
    private static final byte[] PDF = "%PDF-1.7\naccreditation-evidence\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private AccreditationFrameworkRepository frameworks;

    @Autowired
    private AccreditationCriterionRepository criteria;

    @Autowired
    private EvidenceAssignmentRepository assignments;

    @Autowired
    private EvidenceSubmissionRepository submissions;

    @Autowired
    private EvidenceAttachmentRepository attachments;

    @Autowired
    private EvidenceReviewRepository reviews;

    @Autowired
    private DocumentVersionRepository documentVersions;

    @Autowired
    private DocumentRecordRepository documents;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Institution alpha;
    private Institution beta;

    @BeforeEach
    void setUp() {
        reviews.deleteAll();
        attachments.deleteAll();
        submissions.deleteAll();
        assignments.deleteAll();
        criteria.deleteAll();
        frameworks.deleteAll();
        documentVersions.deleteAll();
        documents.deleteAll();
        auditEvents.deleteAll();
        users.deleteAll();
        departments.deleteAll();
        institutions.deleteAll();

        alpha = institutions.save(new Institution("Alpha College", "ALPHA-ACC"));
        beta = institutions.save(new Institution("Beta College", "BETA-ACC"));
        users.save(user(alpha, "admin@alpha.edu", UserRole.INSTITUTION_ADMIN));
        users.save(user(alpha, "faculty@alpha.edu", UserRole.FACULTY));
        users.save(user(alpha, "other-faculty@alpha.edu", UserRole.FACULTY));
        users.save(user(alpha, "iqac@alpha.edu", UserRole.IQAC_COORDINATOR));
        users.save(user(beta, "admin@beta.edu", UserRole.INSTITUTION_ADMIN));
    }

    @Test
    void assignsSubmitsReviewsAndRetainsImmutableHistory() throws Exception {
        String adminToken = login("ALPHA-ACC", "admin@alpha.edu");
        String facultyToken = login("ALPHA-ACC", "faculty@alpha.edu");
        String iqacToken = login("ALPHA-ACC", "iqac@alpha.edu");
        String otherFacultyToken = login("ALPHA-ACC", "other-faculty@alpha.edu");
        String betaToken = login("BETA-ACC", "admin@beta.edu");

        mockMvc.perform(get("/api/v1/accreditation/evidence/owners")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        String frameworkId = createFramework(adminToken);
        String criterionId = createCriterion(adminToken, frameworkId);
        String evidenceId = createAssignment(adminToken, criterionId);

        mockMvc.perform(get("/api/v1/accreditation/evidence")
                        .header("Authorization", "Bearer " + otherFacultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get("/api/v1/accreditation/evidence/{evidenceId}/submissions", evidenceId)
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/start", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        String documentId = uploadDocument(facultyToken);
        String submitBody = """
                {"documents":[{"documentId":"%s","versionNumber":1}]}
                """.formatted(documentId);
        MvcResult firstSubmission = mockMvc.perform(post(
                                "/api/v1/accreditation/evidence/{evidenceId}/submissions", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionNumber").value(1))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.documents[0].sha256").isNotEmpty())
                .andExpect(jsonPath("$.documents[0].storageKey").doesNotExist())
                .andReturn();
        String firstSubmissionId = json(firstSubmission, "$.id");

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/reviews", evidenceId)
                        .header("Authorization", "Bearer " + iqacToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"NEEDS_REVISION","comment":"Please attach the signed committee minutes."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionId").value(firstSubmissionId));

        mockMvc.perform(get("/api/v1/accreditation/evidence/{evidenceId}/reviews", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].decision").value("NEEDS_REVISION"));

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/submissions", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionNumber").value(2));

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/reviews", evidenceId)
                        .header("Authorization", "Bearer " + iqacToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"VERIFIED","comment":"Evidence package accepted."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("VERIFIED"));

        mockMvc.perform(get("/api/v1/accreditation/evidence/{evidenceId}/submissions", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].submissionNumber").value(1))
                .andExpect(jsonPath("$.content[1].submissionNumber").value(2))
                .andExpect(jsonPath("$.content[0].status").value("NEEDS_REVISION"))
                .andExpect(jsonPath("$.content[1].status").value("VERIFIED"));

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/start", evidenceId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isBadRequest());

        assertEquals(9, auditEvents.count());
        assertEquals(2, submissions.count());
        assertEquals(2, attachments.count());
        assertEquals(2, reviews.count());
    }

    @Test
    void rejectsCrossTenantCriteriaAndNonReviewerReviews() throws Exception {
        String alphaAdmin = login("ALPHA-ACC", "admin@alpha.edu");
        String betaAdmin = login("BETA-ACC", "admin@beta.edu");
        String facultyToken = login("ALPHA-ACC", "faculty@alpha.edu");
        String frameworkId = createFramework(betaAdmin);

        mockMvc.perform(post("/api/v1/accreditation/criteria")
                        .header("Authorization", "Bearer " + alphaAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frameworkId":"%s","code":"CROSS-01","title":"Cross Tenant",
                                 "category":"GOVERNANCE","description":null}
                                """.formatted(frameworkId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/reviews", UUID.randomUUID())
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"VERIFIED","comment":"Must not be accepted."}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/accreditation/evidence/{evidenceId}/reviews", UUID.randomUUID())
                        .header("Authorization", "Bearer " + alphaAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"VERIFIED","comment":"Only IQAC may review."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsMissingEvidenceAndValidatesAssignments() throws Exception {
        String adminToken = login("ALPHA-ACC", "admin@alpha.edu");
        String frameworkId = createFramework(adminToken);
        String criterionId = createCriterion(adminToken, frameworkId);
        String evidenceId = createAssignment(adminToken, criterionId);

        mockMvc.perform(get("/api/v1/accreditation/reports/missing")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("academicYear", "2025-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(evidenceId));

        mockMvc.perform(post("/api/v1/accreditation/evidence")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"criterionId":"%s","academicYear":"2025-26","ownerUserId":"%s","dueDate":null}
                                """.formatted(criterionId,
                                users.findByInstitution_IdAndEmailIgnoreCase(alpha.getId(), "faculty@alpha.edu")
                                        .orElseThrow().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private UserEntity user(Institution institution, String email, UserRole role) {
        return new UserEntity(institution, email, email, passwordEncoder.encode(PASSWORD), Set.of(role));
    }

    private String createFramework(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accreditation/frameworks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Institutional Quality Framework","versionLabel":"2025"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result, "$.id");
    }

    private String createCriterion(String token, String frameworkId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accreditation/criteria")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frameworkId":"%s","code":" 1.1 ","title":"Research Governance",
                                 "category":"GOVERNANCE","description":"Approved research policy"}
                                """.formatted(frameworkId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1.1"))
                .andReturn();
        return json(result, "$.id");
    }

    private String createAssignment(String token, String criterionId) throws Exception {
        String facultyId = users.findByInstitution_IdAndEmailIgnoreCase(alpha.getId(), "faculty@alpha.edu")
                .orElseThrow().getId().toString();
        MvcResult result = mockMvc.perform(post("/api/v1/accreditation/evidence")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"criterionId":"%s","academicYear":"2025-26","ownerUserId":"%s","dueDate":"2026-06-30"}
                                """.formatted(criterionId, facultyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andReturn();
        return json(result, "$.id");
    }

    private String uploadDocument(String token) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "evidence.pdf", "application/pdf", PDF);
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(pdf)
                        .param("title", "Research Governance Evidence")
                        .param("category", "ACCREDITATION")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result, "$.document.id");
    }

    private String login(String institutionCode, String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institutionCode":"%s","email":"%s","password":"%s"}
                                """.formatted(institutionCode, email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.accessToken");
    }

    private String json(MvcResult result, String path) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
