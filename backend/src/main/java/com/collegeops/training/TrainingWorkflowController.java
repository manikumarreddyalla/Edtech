package com.collegeops.training;

import com.collegeops.common.RequestIdentity;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/training")
@Tag(name="Training workflows")
public class TrainingWorkflowController {
    private final TrainingWorkflowService service;
    public TrainingWorkflowController(TrainingWorkflowService service) { this.service=service; }

    @GetMapping("/job-roles")
    public List<Map<String,Object>> roles(JwtAuthenticationToken auth) { return service.jobRoles(id(auth).institutionId()); }
    @PostMapping("/job-roles") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public Map<String,Object> createRole(JwtAuthenticationToken auth,@Valid @RequestBody RoleRequest body) { var i=id(auth); return service.createRole(i.institutionId(),i.userId(),body.name(),body.description()); }
    @GetMapping("/job-roles/{role}/skills")
    public List<Map<String,Object>> roleSkills(JwtAuthenticationToken auth,@PathVariable UUID role) { return service.roleSkills(id(auth).institutionId(),role); }
    @PostMapping("/job-roles/{role}/skills") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public void requireSkill(JwtAuthenticationToken auth,@PathVariable UUID role,@Valid @RequestBody RoleSkillRequest body) { var i=id(auth); service.requireSkill(i.institutionId(),i.userId(),role,body.skillId(),body.targetLevel()); }

    @GetMapping("/pathways")
    public List<Map<String,Object>> pathways(JwtAuthenticationToken auth) { return service.pathways(id(auth).institutionId()); }
    @PostMapping("/pathways") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public Map<String,Object> createPathway(JwtAuthenticationToken auth,@Valid @RequestBody PathwayRequest body) { var i=id(auth); return service.createPathway(i.institutionId(),i.userId(),body.name(),body.description()); }
    @GetMapping("/pathways/{pathway}/modules")
    public List<Map<String,Object>> modules(JwtAuthenticationToken auth,@PathVariable UUID pathway) { return service.modules(id(auth).institutionId(),pathway); }
    @PostMapping("/pathways/{pathway}/modules") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public Map<String,Object> addModule(JwtAuthenticationToken auth,@PathVariable UUID pathway,@Valid @RequestBody ModuleRequest body) { var i=id(auth); return service.addModule(i.institutionId(),i.userId(),pathway,body.sequence(),body.title(),body.description(),body.estimatedHours()); }
    @PostMapping("/pathways/{pathway}/publish") @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public void publish(JwtAuthenticationToken auth,@PathVariable UUID pathway) { var i=id(auth); service.publishPathway(i.institutionId(),i.userId(),pathway); }

    @GetMapping("/cohorts")
    public List<Map<String,Object>> cohorts(JwtAuthenticationToken auth) { var i=id(auth); return service.cohorts(i.institutionId(),i.userId(),student(auth)); }
    @PostMapping("/cohorts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public Map<String,Object> createCohort(JwtAuthenticationToken auth,@Valid @RequestBody CohortRequest body) { var i=id(auth); return service.createCohort(i.institutionId(),i.userId(),body.trainerUserId(),body.pathwayId(),body.name(),body.startDate(),body.endDate()); }
    @PostMapping("/cohorts/{cohort}/activate") @PreAuthorize("hasAnyRole('TRAINER','PLACEMENT_OFFICER')")
    public void activate(JwtAuthenticationToken auth,@PathVariable UUID cohort) { var i=id(auth); service.activateCohort(i.institutionId(),i.userId(),cohort); }
    @PostMapping("/cohorts/{cohort}/enrollments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    public void enroll(JwtAuthenticationToken auth,@PathVariable UUID cohort,@Valid @RequestBody EnrollmentRequest body) { var i=id(auth); service.enroll(i.institutionId(),i.userId(),cohort,body.studentId()); }

    @GetMapping("/cohorts/{cohort}/assignments")
    public List<Map<String,Object>> assignments(JwtAuthenticationToken auth,@PathVariable UUID cohort) { var i=id(auth); return service.assignments(i.institutionId(),i.userId(),cohort,student(auth)); }
    @PostMapping("/cohorts/{cohort}/assignments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('TRAINER')")
    public Map<String,Object> assignment(JwtAuthenticationToken auth,@PathVariable UUID cohort,@Valid @RequestBody AssignmentRequest body) { var i=id(auth); return service.createAssignment(i.institutionId(),i.userId(),cohort,body.moduleId(),body.title(),body.instructions(),body.dueAt(),body.rubric().stream().map(r->new TrainingWorkflowService.RubricItem(r.description(),r.maxPoints())).toList()); }
    @PostMapping("/assignments/{assignment}/attempts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')")
    public void submit(JwtAuthenticationToken auth,@PathVariable UUID assignment,@Valid @RequestBody SubmissionRequest body) { var i=id(auth); service.submit(i.institutionId(),i.userId(),assignment,service.studentId(i.institutionId(),i.userId()),body.submissionText()); }
    @GetMapping("/assignments/{assignment}/rubric") @PreAuthorize("hasRole('TRAINER')")
    public List<Map<String,Object>> rubric(JwtAuthenticationToken auth,@PathVariable UUID assignment) { return service.rubric(id(auth).institutionId(),assignment); }
    @PostMapping("/attempts/{attempt}/grade") @PreAuthorize("hasRole('TRAINER')")
    public void grade(JwtAuthenticationToken auth,@PathVariable UUID attempt,@Valid @RequestBody GradeRequest body) { var i=id(auth); service.grade(i.institutionId(),i.userId(),attempt,body.feedback(),body.scores().stream().map(s->new TrainingWorkflowService.RubricScore(s.rubricItemId(),s.pointsAwarded())).toList()); }
    @GetMapping("/results")
    public List<Map<String,Object>> results(JwtAuthenticationToken auth) { var i=id(auth); return service.results(i.institutionId(),i.userId(),student(auth)); }
    @PostMapping("/cohorts/{cohort}/feedback") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('TRAINER')")
    public void feedback(JwtAuthenticationToken auth,@PathVariable UUID cohort,@Valid @RequestBody FeedbackRequest body) { var i=id(auth); service.feedback(i.institutionId(),i.userId(),cohort,body.studentId(),body.message()); }
    @GetMapping("/cohorts/{cohort}/feedback")
    public List<Map<String,Object>> feedback(JwtAuthenticationToken auth,@PathVariable UUID cohort) { var i=id(auth); return service.feedback(i.institutionId(),i.userId(),cohort,student(auth)); }

    private RequestIdentity id(JwtAuthenticationToken auth) { return RequestIdentity.from(auth); }
    private boolean student(JwtAuthenticationToken auth) { return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_STUDENT")); }

    public record RoleRequest(@NotBlank @Size(max=160) String name,@Size(max=2000) String description) {}
    public record RoleSkillRequest(@NotNull UUID skillId,@Min(1) @Max(5) int targetLevel) {}
    public record PathwayRequest(@NotBlank @Size(max=180) String name,@Size(max=3000) String description) {}
    public record ModuleRequest(@Min(0) int sequence,@NotBlank @Size(max=180) String title,@Size(max=2000) String description,@Min(1) @Max(1000) int estimatedHours) {}
    public record CohortRequest(@NotNull UUID pathwayId,@NotNull UUID trainerUserId,@NotBlank @Size(max=180) String name,@NotNull LocalDate startDate,LocalDate endDate) {}
    public record EnrollmentRequest(@NotNull UUID studentId) {}
    public record RubricItemRequest(@NotBlank @Size(max=500) String description,@Min(1) @Max(10000) int maxPoints) {}
    public record AssignmentRequest(@NotNull UUID moduleId,@NotBlank @Size(max=180) String title,@NotBlank @Size(max=5000) String instructions,Instant dueAt,@NotEmpty @Size(max=30) List<@Valid RubricItemRequest> rubric) {}
    public record SubmissionRequest(@NotBlank @Size(max=10000) String submissionText) {}
    public record RubricScoreRequest(@NotNull UUID rubricItemId,@Min(0) int pointsAwarded) {}
    public record GradeRequest(@NotBlank @Size(max=3000) String feedback,@NotEmpty List<@Valid RubricScoreRequest> scores) {}
    public record FeedbackRequest(@NotNull UUID studentId,@NotBlank @Size(max=3000) String message) {}
}
