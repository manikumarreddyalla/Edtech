package com.collegeops.training;

import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import com.collegeops.institution.Department;
import com.collegeops.institution.DepartmentRepository;
import com.collegeops.institution.AcademicProgram;
import com.collegeops.institution.AcademicProgramRepository;
import com.collegeops.institution.Student;
import com.collegeops.institution.StudentRepository;
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

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:collegeops-training-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class TrainingWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired InstitutionRepository institutions;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired DepartmentRepository departments;
    @Autowired AcademicProgramRepository programs;
    @Autowired StudentRepository students;
    @Autowired JdbcTemplate jdbc;
    private String trainer;
    private String faculty;
    private String otherTrainer;
    private String placement;
    private String studentToken;
    private UUID trainerId;
    private UUID studentId;

    @BeforeEach
    void setup() {
        Institution a=institutions.save(new Institution("Training Alpha","TA"+UUID.randomUUID().toString().substring(0,6)));
        Institution b=institutions.save(new Institution("Training Beta","TB"+UUID.randomUUID().toString().substring(0,6)));
        UserEntity trainerUser=users.save(new UserEntity(a,"trainer@example.edu","Trainer",encoder.encode("correct-horse-battery"),Set.of(UserRole.TRAINER)));
        users.save(new UserEntity(a,"faculty@example.edu","Faculty",encoder.encode("correct-horse-battery"),Set.of(UserRole.FACULTY)));
        users.save(new UserEntity(a,"placement@example.edu","Placement",encoder.encode("correct-horse-battery"),Set.of(UserRole.PLACEMENT_OFFICER)));
        UserEntity studentUser=users.save(new UserEntity(a,"student@example.edu","Student",encoder.encode("correct-horse-battery"),Set.of(UserRole.STUDENT)));
        users.save(new UserEntity(b,"trainer@example.edu","Trainer",encoder.encode("correct-horse-battery"),Set.of(UserRole.TRAINER)));
        Department department=departments.save(new Department(a,"Engineering","ENG"));
        AcademicProgram program=programs.save(new AcademicProgram(a,department,"Computer Engineering","CSE"));
        Student student=students.save(new Student(a,studentUser.getId(),department,program,"S-"+UUID.randomUUID().toString().substring(0,6)));
        trainerId=trainerUser.getId(); studentId=student.getId();
        trainer=login(a.getCode(),"trainer@example.edu");
        faculty=login(a.getCode(),"faculty@example.edu");
        placement=login(a.getCode(),"placement@example.edu");
        studentToken=login(a.getCode(),"student@example.edu");
        otherTrainer=login(b.getCode(),"trainer@example.edu");
    }

    @Test
    void trainerCanCreateVersionedPathwayAndPublishAfterAddingModule() throws Exception {
        String path= mvc.perform(post("/api/v1/training/pathways").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"API Foundations\",\"description\":\"Synthetic practice\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String id=com.jayway.jsonpath.JsonPath.read(path,"$.id");
        mvc.perform(post("/api/v1/training/pathways/"+id+"/publish").header("Authorization","Bearer "+trainer))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        mvc.perform(post("/api/v1/training/pathways/"+id+"/modules").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequence\":1,\"title\":\"HTTP basics\",\"estimatedHours\":4}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/training/pathways/"+id+"/publish").header("Authorization","Bearer "+trainer))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/training/pathways/"+id+"/publish").header("Authorization","Bearer "+trainer))
                .andExpect(status().isConflict());
    }

    @Test
    void nonTrainingRoleCannotWriteAndOtherInstitutionCannotReadPathway() throws Exception {
        String path=mvc.perform(post("/api/v1/training/pathways").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private Pathway\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=com.jayway.jsonpath.JsonPath.read(path,"$.id");
        mvc.perform(post("/api/v1/training/pathways").header("Authorization","Bearer "+faculty).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/training/pathways/"+id+"/modules").header("Authorization","Bearer "+otherTrainer))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPathwayRequestIsValidated() throws Exception {
        mvc.perform(post("/api/v1/training/pathways").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"description\":\"ok\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cohortEnrollmentAssignmentAttemptGradeAndFeedbackLifecycle() throws Exception {
        String pathwayBody=mvc.perform(post("/api/v1/training/pathways").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cohort pathway\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pathwayId=com.jayway.jsonpath.JsonPath.read(pathwayBody,"$.id");
        String moduleBody=mvc.perform(post("/api/v1/training/pathways/"+pathwayId+"/modules").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequence\":0,\"title\":\"Project module\",\"estimatedHours\":4}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String moduleId=com.jayway.jsonpath.JsonPath.read(moduleBody,"$.id");
        mvc.perform(post("/api/v1/training/pathways/"+pathwayId+"/publish").header("Authorization","Bearer "+trainer)).andExpect(status().isOk());
        String cohortBody=mvc.perform(post("/api/v1/training/cohorts").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Synthetic cohort\",\"pathwayId\":\""+pathwayId+"\",\"trainerUserId\":\""+trainerId+"\",\"startDate\":\"2026-10-21\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String cohortId=com.jayway.jsonpath.JsonPath.read(cohortBody,"$.id");
        mvc.perform(post("/api/v1/training/cohorts/"+cohortId+"/enrollments").header("Authorization","Bearer "+placement).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\""+studentId+"\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/training/cohorts/"+cohortId+"/activate").header("Authorization","Bearer "+trainer)).andExpect(status().isOk());
        String assignmentBody=mvc.perform(post("/api/v1/training/cohorts/"+cohortId+"/assignments").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleId\":\""+moduleId+"\",\"title\":\"Build a service\",\"instructions\":\"Submit a synthetic project\",\"rubric\":[{\"description\":\"Tests and API behavior\",\"maxPoints\":10}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String assignmentId=com.jayway.jsonpath.JsonPath.read(assignmentBody,"$.id");
        mvc.perform(post("/api/v1/training/assignments/"+assignmentId+"/attempts").header("Authorization","Bearer "+studentToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionText\":\"Synthetic project submission\"}"))
                .andExpect(status().isCreated());
        UUID attempt=jdbc.queryForObject("select id from training_assessment_attempts where assignment_id=?",UUID.class,UUID.fromString(assignmentId));
        UUID rubric=jdbc.queryForObject("select id from training_assignment_rubric_items where assignment_id=?",UUID.class,UUID.fromString(assignmentId));
        mvc.perform(post("/api/v1/training/attempts/"+attempt+"/grade").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"Solid work\",\"scores\":[{\"rubricItemId\":\""+rubric+"\",\"pointsAwarded\":8}]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/training/results").header("Authorization","Bearer "+studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("GRADED")).andExpect(jsonPath("$[0].score").value(8));
        mvc.perform(post("/api/v1/training/cohorts/"+cohortId+"/feedback").header("Authorization","Bearer "+trainer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\""+studentId+"\",\"message\":\"Continue practicing with tests.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/training/cohorts/"+cohortId+"/feedback").header("Authorization","Bearer "+studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].message").value("Continue practicing with tests."));
    }

    private String login(String code,String email) {
        try {
            String response=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionCode\":\""+code+"\",\"email\":\""+email+"\",\"password\":\"correct-horse-battery\"}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return com.jayway.jsonpath.JsonPath.read(response,"$.accessToken");
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
}
