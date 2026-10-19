package com.collegeops.institution;

import com.collegeops.common.PageResponse;
import com.collegeops.common.RequestIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Institution Directory")
public class InstitutionDirectoryController {
    private final InstitutionDirectoryService service;

    public InstitutionDirectoryController(InstitutionDirectoryService service) {
        this.service = service;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY', 'TRAINER', 'PLACEMENT_OFFICER')")
    @Operation(summary = "List departments in the authenticated institution")
    public PageResponse<DepartmentResponse> listDepartments(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listDepartments(RequestIdentity.from(authentication).institutionId(), page, size);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    @Operation(summary = "Create a department in the authenticated institution")
    public DepartmentResponse createDepartment(JwtAuthenticationToken authentication,
                                               @Valid @RequestBody DepartmentRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createDepartment(identity.institutionId(), identity.userId(), request);
    }

    @GetMapping("/academic-programs")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY', 'TRAINER', 'PLACEMENT_OFFICER')")
    @Operation(summary = "List academic programs in the authenticated institution")
    public PageResponse<AcademicProgramResponse> listPrograms(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPrograms(RequestIdentity.from(authentication).institutionId(), page, size);
    }

    @PostMapping("/academic-programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    @Operation(summary = "Create an academic program in the authenticated institution")
    public AcademicProgramResponse createProgram(JwtAuthenticationToken authentication,
                                                 @Valid @RequestBody AcademicProgramRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createProgram(identity.institutionId(), identity.userId(), request);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY', 'PLACEMENT_OFFICER')")
    @Operation(summary = "List minimal student directory records in the authenticated institution")
    public PageResponse<StudentResponse> listStudents(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listStudents(RequestIdentity.from(authentication).institutionId(), page, size);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    @Operation(summary = "Create a student directory record in the authenticated institution")
    public StudentResponse createStudent(JwtAuthenticationToken authentication,
                                         @Valid @RequestBody StudentRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createStudent(identity.institutionId(), identity.userId(), request);
    }
}
