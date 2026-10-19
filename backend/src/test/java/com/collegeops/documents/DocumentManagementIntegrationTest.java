package com.collegeops.documents;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collegeops-documents-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.documents.storage-root=target/test-document-storage",
        "app.documents.max-file-bytes=64"
})
class DocumentManagementIntegrationTest {
    private static final String PASSWORD = "correct-horse-battery";
    private static final byte[] FIRST_PDF = "%PDF-1.7\nfirst-version\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SECOND_PDF = "%PDF-1.7\nsecond-version\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private DocumentRecordRepository documents;

    @Autowired
    private DocumentVersionRepository versions;

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
        versions.deleteAll();
        documents.deleteAll();
        auditEvents.deleteAll();
        users.deleteAll();
        institutions.deleteAll();

        alpha = institutions.save(new Institution("Alpha College", "ALPHA-DOCS"));
        beta = institutions.save(new Institution("Beta College", "BETA-DOCS"));
        users.save(user(alpha, "admin@alpha.edu", UserRole.INSTITUTION_ADMIN));
        users.save(user(alpha, "faculty@alpha.edu", UserRole.FACULTY));
        users.save(user(alpha, "other-faculty@alpha.edu", UserRole.FACULTY));
        users.save(user(beta, "admin@beta.edu", UserRole.INSTITUTION_ADMIN));
    }

    @Test
    void uploadsVersionsAndDownloadsPrivateContent() throws Exception {
        String adminToken = login("ALPHA-DOCS", "admin@alpha.edu");
        String documentId = upload(adminToken, "Quality plan", "ACCREDITATION", FIRST_PDF);

        mockMvc.perform(multipart("/api/v1/documents/{documentId}/versions", documentId)
                        .file(pdf(SECOND_PDF))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andExpect(jsonPath("$.versionNumber").value(2));
        assertEquals(2, versions.count());
        assertEquals(2, auditEvents.count());

        mockMvc.perform(get("/api/v1/documents/{documentId}/versions", documentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].versionNumber").value(2))
                .andExpect(jsonPath("$.content[0].storageKey").doesNotExist());

        mockMvc.perform(get("/api/v1/documents/{documentId}/versions/1/content", documentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(FIRST_PDF));
    }

    @Test
    void scopesDocumentsByTenantAndFacultyOwner() throws Exception {
        String adminToken = login("ALPHA-DOCS", "admin@alpha.edu");
        String facultyToken = login("ALPHA-DOCS", "faculty@alpha.edu");
        String otherFacultyToken = login("ALPHA-DOCS", "other-faculty@alpha.edu");
        String betaToken = login("BETA-DOCS", "admin@beta.edu");
        String documentId = upload(facultyToken, "Faculty handbook", "POLICY", FIRST_PDF);

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + facultyToken)
                        .param("q", "handbook")
                        .param("category", "policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ownerUserId").isNotEmpty());

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + otherFacultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/documents/{documentId}/versions/1/content", documentId)
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/documents/{documentId}/versions/1/content", documentId)
                        .header("Authorization", "Bearer " + otherFacultyToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsContentTypeSpoofingAndReportsAStandardValidationError() throws Exception {
        String adminToken = login("ALPHA-DOCS", "admin@alpha.edu");
        MockMultipartFile invalidPdf = new MockMultipartFile(
                "file", "not-a-pdf.pdf", "application/pdf", "plain text".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(invalidPdf)
                        .param("title", "Invalid upload")
                        .param("category", "TEST")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(pdf(new byte[65]))
                        .param("title", "Oversized upload")
                        .param("category", "TEST")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertEquals(0, documents.count());
        assertEquals(0, versions.count());
        assertEquals(0, auditEvents.count());
    }

    private UserEntity user(Institution institution, String email, UserRole role) {
        return new UserEntity(institution, email, email, passwordEncoder.encode(PASSWORD), Set.of(role));
    }

    private MockMultipartFile pdf(byte[] content) {
        return new MockMultipartFile("file", "quality.pdf", "application/pdf", content);
    }

    private String upload(String token, String title, String category, byte[] content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(pdf(content))
                        .param("title", title)
                        .param("category", category)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version.versionNumber").value(1))
                .andExpect(jsonPath("$.version.sha256").isNotEmpty())
                .andExpect(jsonPath("$.version.storageKey").doesNotExist())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.document.id");
    }

    private String login(String institutionCode, String email) throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
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
}
