package com.collegeops.institution;

import com.collegeops.common.AuditEvent;
import com.collegeops.common.AuditEventRepository;
import com.collegeops.common.InvalidRequestException;
import com.collegeops.common.PageResponse;
import com.collegeops.common.ResourceNotFoundException;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import com.collegeops.users.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class InstitutionDirectoryService {
    private final InstitutionRepository institutions;
    private final DepartmentRepository departments;
    private final AcademicProgramRepository programs;
    private final StudentRepository students;
    private final UserRepository users;
    private final AuditEventRepository auditEvents;

    public InstitutionDirectoryService(InstitutionRepository institutions, DepartmentRepository departments,
                                       AcademicProgramRepository programs, StudentRepository students,
                                       UserRepository users, AuditEventRepository auditEvents) {
        this.institutions = institutions;
        this.departments = departments;
        this.programs = programs;
        this.students = students;
        this.users = users;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listDepartments(UUID institutionId, int page, int size) {
        validatePage(page, size);
        return PageResponse.from(departments.findAllByInstitution_Id(institutionId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")))
                .map(DepartmentResponse::from));
    }

    @Transactional
    public DepartmentResponse createDepartment(UUID institutionId, UUID actorId, DepartmentRequest request) {
        Institution institution = institution(institutionId);
        String name = request.name().strip();
        String code = normalizeCode(request.code());
        Department department = departments.save(new Department(institution, name, code));
        auditEvents.save(new AuditEvent(institutionId, actorId, "DEPARTMENT_CREATED", "DEPARTMENT",
                department.getId(), "Department created."));
        return DepartmentResponse.from(department);
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademicProgramResponse> listPrograms(UUID institutionId, int page, int size) {
        validatePage(page, size);
        return PageResponse.from(programs.findAllByInstitution_Id(institutionId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")))
                .map(AcademicProgramResponse::from));
    }

    @Transactional
    public AcademicProgramResponse createProgram(UUID institutionId, UUID actorId,
                                                 AcademicProgramRequest request) {
        Institution institution = institution(institutionId);
        Department department = departments.findByIdAndInstitution_Id(request.departmentId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Department"));
        AcademicProgram program = programs.save(new AcademicProgram(institution, department,
                request.name().strip(), normalizeCode(request.code())));
        auditEvents.save(new AuditEvent(institutionId, actorId, "ACADEMIC_PROGRAM_CREATED",
                "ACADEMIC_PROGRAM", program.getId(), "Academic program created."));
        return AcademicProgramResponse.from(program);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> listStudents(UUID institutionId, int page, int size) {
        validatePage(page, size);
        return PageResponse.from(students.findAllByInstitution_Id(institutionId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "studentNumber")))
                .map(StudentResponse::from));
    }

    @Transactional
    public StudentResponse createStudent(UUID institutionId, UUID actorId, StudentRequest request) {
        Institution institution = institution(institutionId);
        Department department = departments.findByIdAndInstitution_Id(request.departmentId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Department"));
        AcademicProgram program = programs.findByIdAndInstitution_Id(request.academicProgramId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic program"));
        if (!program.getDepartment().getId().equals(department.getId())) {
            throw new InvalidRequestException("The academic program must belong to the selected department.");
        }

        UUID studentUserId = request.userId();
        if (studentUserId != null) {
            UserEntity user = users.findByIdAndInstitution_Id(studentUserId, institutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student user"));
            if (user.getStatus() != UserStatus.ACTIVE || !user.getRoles().contains(UserRole.STUDENT)) {
                throw new InvalidRequestException("The linked user must be active and have the STUDENT role.");
            }
        }

        Student student = students.save(new Student(institution, studentUserId, department, program,
                request.studentNumber().strip()));
        auditEvents.save(new AuditEvent(institutionId, actorId, "STUDENT_CREATED", "STUDENT",
                student.getId(), "Student directory record created."));
        return StudentResponse.from(student);
    }

    private Institution institution(UUID institutionId) {
        return institutions.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private String normalizeCode(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }
}
