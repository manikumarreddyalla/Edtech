package com.collegeops.accreditation;

import com.collegeops.common.AuditEvent;
import com.collegeops.common.AuditEventRepository;
import com.collegeops.common.InvalidRequestException;
import com.collegeops.common.PageResponse;
import com.collegeops.common.ResourceNotFoundException;
import com.collegeops.documents.DocumentVersion;
import com.collegeops.documents.DocumentVersionRepository;
import com.collegeops.institution.Department;
import com.collegeops.institution.DepartmentRepository;
import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import com.collegeops.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AccreditationService {
    private static final String FRAMEWORK = "AccreditationFramework";
    private static final String CRITERION = "AccreditationCriterion";
    private static final String EVIDENCE = "EvidenceAssignment";
    private static final String SUBMISSION = "EvidenceSubmission";

    private final AccreditationFrameworkRepository frameworks;
    private final AccreditationCriterionRepository criteria;
    private final EvidenceAssignmentRepository assignments;
    private final EvidenceSubmissionRepository submissions;
    private final EvidenceAttachmentRepository attachments;
    private final EvidenceReviewRepository reviews;
    private final InstitutionRepository institutions;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final DocumentVersionRepository documentVersions;
    private final AuditEventRepository auditEvents;

    public AccreditationService(AccreditationFrameworkRepository frameworks,
                                AccreditationCriterionRepository criteria,
                                EvidenceAssignmentRepository assignments,
                                EvidenceSubmissionRepository submissions,
                                EvidenceAttachmentRepository attachments,
                                EvidenceReviewRepository reviews,
                                InstitutionRepository institutions,
                                DepartmentRepository departments,
                                UserRepository users,
                                DocumentVersionRepository documentVersions,
                                AuditEventRepository auditEvents) {
        this.frameworks = frameworks;
        this.criteria = criteria;
        this.assignments = assignments;
        this.submissions = submissions;
        this.attachments = attachments;
        this.reviews = reviews;
        this.institutions = institutions;
        this.departments = departments;
        this.users = users;
        this.documentVersions = documentVersions;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public PageResponse<AccreditationFrameworkResponse> listFrameworks(UUID institutionId, int page, int size) {
        return PageResponse.from(frameworks.findAllByInstitution_Id(institutionId, pageable(page, size))
                .map(AccreditationFrameworkResponse::from));
    }

    @Transactional
    public AccreditationFrameworkResponse createFramework(UUID institutionId, UUID actorId,
                                                           AccreditationFrameworkRequest request) {
        String name = request.name().strip();
        String version = request.versionLabel().strip();
        if (frameworks.existsByInstitution_IdAndNameIgnoreCaseAndVersionLabelIgnoreCase(
                institutionId, name, version)) {
            throw new InvalidRequestException("An accreditation framework with this name and version already exists.");
        }
        Institution institution = requireInstitution(institutionId);
        AccreditationFramework framework = frameworks.save(new AccreditationFramework(institution, name, version));
        audit(institutionId, actorId, "ACCREDITATION_FRAMEWORK_CREATED", FRAMEWORK,
                framework.getId(), name + " " + version);
        return AccreditationFrameworkResponse.from(framework);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccreditationCriterionResponse> listCriteria(UUID institutionId, UUID frameworkId,
                                                                     int page, int size) {
        if (frameworkId != null) {
            requireFramework(institutionId, frameworkId);
        }
        return PageResponse.from(criteria.search(institutionId, frameworkId, pageable(page, size))
                .map(AccreditationCriterionResponse::from));
    }

    @Transactional(readOnly = true)
    public List<EvidenceOwnerResponse> listEvidenceOwners(UUID institutionId) {
        Pageable firstPage = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "displayName"));
        Map<UUID, UserEntity> owners = Stream.of(UserRole.FACULTY, UserRole.IQAC_COORDINATOR)
                .flatMap(role -> users.findDistinctByInstitution_IdAndStatusAndRolesContaining(
                        institutionId, UserStatus.ACTIVE, role, firstPage).getContent().stream())
                .collect(Collectors.toMap(UserEntity::getId, owner -> owner, (existing, ignored) -> existing));
        return owners.values().stream().map(EvidenceOwnerResponse::from)
                .sorted(java.util.Comparator.comparing(EvidenceOwnerResponse::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public AccreditationCriterionResponse createCriterion(UUID institutionId, UUID actorId,
                                                           AccreditationCriterionRequest request) {
        AccreditationFramework framework = requireFramework(institutionId, request.frameworkId());
        String code = request.code().strip().toUpperCase();
        if (criteria.existsByFramework_IdAndCodeIgnoreCase(framework.getId(), code)) {
            throw new InvalidRequestException("A criterion with this code already exists in the framework.");
        }
        AccreditationCriterion criterion = criteria.save(new AccreditationCriterion(
                framework.getInstitution(), framework, code, request.title().strip(),
                request.category().strip(), normalizeOptional(request.description())));
        audit(institutionId, actorId, "ACCREDITATION_CRITERION_CREATED", CRITERION,
                criterion.getId(), code);
        return AccreditationCriterionResponse.from(criterion);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceAssignmentResponse> listEvidence(
            UUID institutionId, UUID userId, Set<String> roles, String academicYear, EvidenceStatus status,
            UUID departmentId, int page, int size) {
        if (departmentId != null) {
            requireDepartment(institutionId, departmentId);
        }
        UUID ownerFilter = canManage(roles) ? null : userId;
        return PageResponse.from(assignments.search(institutionId, ownerFilter,
                        normalizeOptional(academicYear), status, departmentId, pageable(page, size))
                .map(EvidenceAssignmentResponse::from));
    }

    @Transactional
    public EvidenceAssignmentResponse createAssignment(UUID institutionId, UUID actorId,
                                                        EvidenceAssignmentRequest request) {
        AccreditationCriterion criterion = requireCriterion(institutionId, request.criterionId());
        Department department = request.departmentId() == null ? null
                : requireDepartment(institutionId, request.departmentId());
        UserEntity owner = users.findByIdAndInstitution_Id(request.ownerUserId(), institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence owner"));
        if (owner.getStatus() != UserStatus.ACTIVE
                || owner.getRoles().stream().noneMatch(role -> role == UserRole.FACULTY
                || role == UserRole.IQAC_COORDINATOR)) {
            throw new InvalidRequestException("The evidence owner must be an active faculty member or IQAC coordinator.");
        }
        String academicYear = request.academicYear().strip();
        boolean duplicate = department == null
                ? assignments.existsByInstitution_IdAndCriterion_IdAndAcademicYearAndDepartmentIsNull(
                    institutionId, criterion.getId(), academicYear)
                : assignments.existsByInstitution_IdAndCriterion_IdAndAcademicYearAndDepartment_Id(
                    institutionId, criterion.getId(), academicYear, department.getId());
        if (duplicate) {
            throw new InvalidRequestException("Evidence is already assigned for this criterion, year and department.");
        }
        EvidenceAssignment assignment = assignments.save(new EvidenceAssignment(
                criterion.getInstitution(), criterion, academicYear, department, owner.getId(), request.dueDate()));
        audit(institutionId, actorId, "EVIDENCE_ASSIGNED", EVIDENCE, assignment.getId(), academicYear);
        return EvidenceAssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceAssignmentResponse> missingEvidence(
            UUID institutionId, String academicYear, UUID departmentId, int page, int size) {
        if (departmentId != null) {
            requireDepartment(institutionId, departmentId);
        }
        List<EvidenceStatus> missingStatuses = List.of(
                EvidenceStatus.NOT_STARTED, EvidenceStatus.IN_PROGRESS, EvidenceStatus.NEEDS_REVISION);
        return PageResponse.from(assignments.findMissing(institutionId, academicYear.strip(), departmentId,
                        missingStatuses, pageable(page, size))
                .map(EvidenceAssignmentResponse::from));
    }

    @Transactional
    public EvidenceAssignmentResponse start(UUID institutionId, UUID actorId, Set<String> roles, UUID evidenceId) {
        EvidenceAssignment assignment = requireAssignmentForUpdate(institutionId, evidenceId);
        requireOwnerOrManager(assignment, actorId, roles);
        if (assignment.getStatus() != EvidenceStatus.NOT_STARTED) {
            throw new InvalidRequestException("Only evidence that has not started can be started.");
        }
        assignment.setStatus(EvidenceStatus.IN_PROGRESS);
        audit(institutionId, actorId, "EVIDENCE_STARTED", EVIDENCE, evidenceId, null);
        return EvidenceAssignmentResponse.from(assignment);
    }

    @Transactional
    public EvidenceSubmissionResponse submit(UUID institutionId, UUID actorId, Set<String> roles,
                                              UUID evidenceId, EvidenceSubmissionRequest request) {
        EvidenceAssignment assignment = requireAssignmentForUpdate(institutionId, evidenceId);
        requireOwnerOrManager(assignment, actorId, roles);
        if (assignment.getStatus() != EvidenceStatus.IN_PROGRESS
                && assignment.getStatus() != EvidenceStatus.NEEDS_REVISION) {
            throw new InvalidRequestException("Evidence must be in progress or require revision before submission.");
        }
        Set<String> documentKeys = new HashSet<>();
        List<DocumentVersion> versions = request.documents().stream().map(reference -> {
            String key = reference.documentId() + ":" + reference.versionNumber();
            if (!documentKeys.add(key)) {
                throw new InvalidRequestException("A document version may only be attached once per submission.");
            }
            DocumentVersion version = documentVersions
                    .findByDocument_IdAndInstitutionIdAndVersionNumber(
                            reference.documentId(), institutionId, reference.versionNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Document version"));
            if (!canManage(roles) && !version.getDocument().getOwnerUserId().equals(actorId)) {
                throw new ResourceNotFoundException("Document version");
            }
            return version;
        }).toList();

        EvidenceSubmission submission = submissions.save(new EvidenceSubmission(
                assignment, (int) submissions.countByAssignment_IdAndInstitutionId(evidenceId, institutionId) + 1,
                actorId));
        for (DocumentVersion version : versions) {
            attachments.save(new EvidenceAttachment(submission,
                    version.getDocument().getId(), version.getVersionNumber()));
        }
        assignment.setStatus(EvidenceStatus.SUBMITTED);
        audit(institutionId, actorId, "EVIDENCE_SUBMITTED", SUBMISSION,
                submission.getId(), "Submission " + submission.getSubmissionNumber());
        return submissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceSubmissionResponse> listSubmissions(
            UUID institutionId, UUID userId, Set<String> roles, UUID evidenceId, int page, int size) {
        EvidenceAssignment assignment = requireAssignment(institutionId, evidenceId);
        requireOwnerOrManager(assignment, userId, roles);
        return PageResponse.from(submissions.findAllByAssignment_IdAndInstitutionId(
                        evidenceId, institutionId, pageable(page, size, "submissionNumber"))
                .map(this::submissionResponse));
    }

    @Transactional
    public EvidenceReviewResponse review(UUID institutionId, UUID actorId, UUID evidenceId,
                                         EvidenceReviewRequest request) {
        EvidenceAssignment assignment = requireAssignmentForUpdate(institutionId, evidenceId);
        EvidenceSubmission submission = submissions
                .findTopByAssignment_IdAndInstitutionIdOrderBySubmissionNumberDesc(evidenceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence submission"));
        if (assignment.getStatus() != EvidenceStatus.SUBMITTED
                || submission.getStatus() != EvidenceStatus.SUBMITTED) {
            throw new InvalidRequestException("Only the current submitted evidence can be reviewed.");
        }
        EvidenceStatus nextStatus = request.decision() == EvidenceReviewDecision.VERIFIED
                ? EvidenceStatus.VERIFIED : EvidenceStatus.NEEDS_REVISION;
        EvidenceReview review = reviews.save(new EvidenceReview(
                submission, actorId, request.decision(), request.comment().strip()));
        submission.setStatus(nextStatus);
        assignment.setStatus(nextStatus);
        audit(institutionId, actorId, "EVIDENCE_REVIEWED", SUBMISSION,
                submission.getId(), request.decision().name());
        return EvidenceReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceReviewResponse> listReviews(
            UUID institutionId, UUID userId, Set<String> roles, UUID evidenceId, int page, int size) {
        EvidenceAssignment assignment = requireAssignment(institutionId, evidenceId);
        requireOwnerOrManager(assignment, userId, roles);
        return PageResponse.from(reviews.findAllBySubmission_Assignment_IdAndInstitutionId(
                        evidenceId, institutionId, pageable(page, size))
                .map(EvidenceReviewResponse::from));
    }

    private EvidenceSubmissionResponse submissionResponse(EvidenceSubmission submission) {
        List<EvidenceAttachmentResponse> documentResponses = attachments
                .findAllBySubmission_IdAndInstitutionId(submission.getId(), submission.getInstitutionId())
                .stream()
                .map(attachment -> documentVersions.findByDocument_IdAndInstitutionIdAndVersionNumber(
                                attachment.getDocumentId(), submission.getInstitutionId(),
                                attachment.getVersionNumber())
                        .orElseThrow(() -> new ResourceNotFoundException("Attached document version")))
                .map(EvidenceAttachmentResponse::from)
                .toList();
        return new EvidenceSubmissionResponse(submission.getId(), submission.getAssignment().getId(),
                submission.getSubmissionNumber(), submission.getSubmitterUserId(),
                submission.getStatus(), submission.getSubmittedAt(), documentResponses);
    }

    private Institution requireInstitution(UUID institutionId) {
        return institutions.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution"));
    }

    private AccreditationFramework requireFramework(UUID institutionId, UUID frameworkId) {
        return frameworks.findByIdAndInstitution_Id(frameworkId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Accreditation framework"));
    }

    private AccreditationCriterion requireCriterion(UUID institutionId, UUID criterionId) {
        return criteria.findByIdAndInstitution_Id(criterionId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Accreditation criterion"));
    }

    private Department requireDepartment(UUID institutionId, UUID departmentId) {
        return departments.findByIdAndInstitution_Id(departmentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Department"));
    }

    private EvidenceAssignment requireAssignment(UUID institutionId, UUID evidenceId) {
        return assignments.findByIdAndInstitution_Id(evidenceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence assignment"));
    }

    private EvidenceAssignment requireAssignmentForUpdate(UUID institutionId, UUID evidenceId) {
        return assignments.findForUpdateByIdAndInstitutionId(evidenceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence assignment"));
    }

    private void requireOwnerOrManager(EvidenceAssignment assignment, UUID userId, Set<String> roles) {
        if (!assignment.getOwnerUserId().equals(userId) && !canManage(roles)) {
            throw new ResourceNotFoundException("Evidence assignment");
        }
    }

    private boolean canManage(Set<String> roles) {
        return roles.contains("ROLE_INSTITUTION_ADMIN") || roles.contains("ROLE_IQAC_COORDINATOR");
    }

    private Pageable pageable(int page, int size) {
        return pageable(page, size, "createdAt");
    }

    private Pageable pageable(int page, int size, String sortProperty) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sortProperty));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void audit(UUID institutionId, UUID actorId, String action, String targetType,
                       UUID targetId, String summary) {
        auditEvents.save(new AuditEvent(institutionId, actorId, action, targetType, targetId, summary));
    }
}
