package com.collegeops.institution;

import com.collegeops.common.AuditEventRepository;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collegeops-directory-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class InstitutionDirectoryIntegrationTest {
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private AcademicProgramRepository programs;

    @Autowired
    private StudentRepository students;

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
        students.deleteAll();
        programs.deleteAll();
        departments.deleteAll();
        auditEvents.deleteAll();
        users.deleteAll();
        institutions.deleteAll();

        alpha = institutions.save(new Institution("Alpha College", "ALPHA"));
        beta = institutions.save(new Institution("Beta College", "BETA"));
        users.save(new UserEntity(alpha, "admin@alpha.edu", "Alpha Admin",
                passwordEncoder.encode(PASSWORD), Set.of(UserRole.INSTITUTION_ADMIN)));
        users.save(new UserEntity(alpha, "faculty@alpha.edu", "Alpha Faculty",
                passwordEncoder.encode(PASSWORD), Set.of(UserRole.FACULTY)));
        users.save(new UserEntity(beta, "admin@beta.edu", "Beta Admin",
                passwordEncoder.encode(PASSWORD), Set.of(UserRole.INSTITUTION_ADMIN)));
        departments.save(new Department(beta, "Beta Engineering", "BENG"));
    }

    @Test
    void createsAndListsDepartmentsOnlyWithinAuthenticatedTenant() throws Exception {
        String alphaToken = login("ALPHA", "admin@alpha.edu");
        String betaToken = login("BETA", "admin@beta.edu");

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Computer Science","code":"cse"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionId").value(alpha.getId().toString()))
                .andExpect(jsonPath("$.code").value("CSE"));
        assertEquals(1, auditEvents.count());

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate Department","code":"CSE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertEquals(1, auditEvents.count());

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("CSE")))
                .andExpect(jsonPath("$.content[*].code", not(hasItem("BENG"))));

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("BENG"));
    }

    @Test
    void createsProgramAndMinimalStudentAndRejectsCrossTenantRelations() throws Exception {
        String alphaToken = login("ALPHA", "admin@alpha.edu");
        Department alphaDepartment = departments.save(new Department(alpha, "Computer Science", "CSE"));
        Department betaDepartment = departments.findAll().stream()
                .filter(department -> department.getInstitution().getId().equals(beta.getId()))
                .findFirst()
                .orElseThrow();

        MvcResult programResult = mockMvc.perform(post("/api/v1/academic-programs")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":"%s","name":"Bachelor of Computer Science","code":"bsc-cs"}
                                """.formatted(alphaDepartment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionId").value(alpha.getId().toString()))
                .andExpect(jsonPath("$.code").value("BSC-CS"))
                .andReturn();
        assertEquals(1, auditEvents.count());
        String programId = com.jayway.jsonpath.JsonPath.read(
                programResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentNumber":"2026-CSE-001","departmentId":"%s","academicProgramId":"%s"}
                                """.formatted(alphaDepartment.getId(), programId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentNumber").value("2026-CSE-001"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.institutionId").value(alpha.getId().toString()));
        assertEquals(2, auditEvents.count());

        mockMvc.perform(post("/api/v1/academic-programs")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":"%s","name":"Foreign Program","code":"FOREIGN"}
                                """.formatted(betaDepartment.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void enforcesRolesAndPaginationBounds() throws Exception {
        String facultyToken = login("ALPHA", "faculty@alpha.edu");

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized Department","code":"NOPE"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + facultyToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
}
