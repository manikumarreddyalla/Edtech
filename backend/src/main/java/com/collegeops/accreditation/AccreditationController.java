package com.collegeops.accreditation;

import com.collegeops.common.PageResponse;
import com.collegeops.common.RequestIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accreditation")
@Tag(name = "Accreditation Evidence")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY')")
public class AccreditationController {
    private final AccreditationService service;

    public AccreditationController(AccreditationService service) {
        this.service = service;
    }

    @GetMapping("/frameworks")
    @Operation(summary = "List accreditation frameworks for the authenticated institution")
    public PageResponse<AccreditationFrameworkResponse> listFrameworks(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listFrameworks(RequestIdentity.from(authentication).institutionId(), page, size);
    }

    @PostMapping("/frameworks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Create an accreditation framework")
    public AccreditationFrameworkResponse createFramework(
            JwtAuthenticationToken authentication, @Valid @RequestBody AccreditationFrameworkRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createFramework(identity.institutionId(), identity.userId(), request);
    }

    @GetMapping("/criteria")
    @Operation(summary = "List criteria, optionally filtered by framework")
    public PageResponse<AccreditationCriterionResponse> listCriteria(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) UUID frameworkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listCriteria(RequestIdentity.from(authentication).institutionId(), frameworkId, page, size);
    }

    @GetMapping("/evidence/owners")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "List active faculty and IQAC users eligible to own evidence")
    public List<EvidenceOwnerResponse> listEvidenceOwners(JwtAuthenticationToken authentication) {
        return service.listEvidenceOwners(RequestIdentity.from(authentication).institutionId());
    }

    @PostMapping("/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Create an accreditation criterion")
    public AccreditationCriterionResponse createCriterion(
            JwtAuthenticationToken authentication, @Valid @RequestBody AccreditationCriterionRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createCriterion(identity.institutionId(), identity.userId(), request);
    }

    @GetMapping("/evidence")
    @Operation(summary = "List evidence assignments visible to the authenticated user")
    public PageResponse<EvidenceAssignmentResponse> listEvidence(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) EvidenceStatus status,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.listEvidence(identity.institutionId(), identity.userId(), roleAuthorities(authentication),
                academicYear, status, departmentId, page, size);
    }

    @PostMapping("/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Assign an evidence criterion to an owner")
    public EvidenceAssignmentResponse createAssignment(
            JwtAuthenticationToken authentication, @Valid @RequestBody EvidenceAssignmentRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.createAssignment(identity.institutionId(), identity.userId(), request);
    }

    @PostMapping("/evidence/{evidenceId}/start")
    @Operation(summary = "Start work on an evidence assignment")
    public EvidenceAssignmentResponse start(JwtAuthenticationToken authentication,
                                            @PathVariable UUID evidenceId) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.start(identity.institutionId(), identity.userId(), roleAuthorities(authentication), evidenceId);
    }

    @PostMapping("/evidence/{evidenceId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an immutable set of document versions as evidence")
    public EvidenceSubmissionResponse submit(
            JwtAuthenticationToken authentication,
            @PathVariable UUID evidenceId,
            @Valid @RequestBody EvidenceSubmissionRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.submit(identity.institutionId(), identity.userId(), roleAuthorities(authentication),
                evidenceId, request);
    }

    @GetMapping("/evidence/{evidenceId}/submissions")
    @Operation(summary = "List submission history for an evidence assignment")
    public PageResponse<EvidenceSubmissionResponse> listSubmissions(
            JwtAuthenticationToken authentication,
            @PathVariable UUID evidenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.listSubmissions(identity.institutionId(), identity.userId(),
                roleAuthorities(authentication), evidenceId, page, size);
    }

    @GetMapping("/evidence/{evidenceId}/reviews")
    @Operation(summary = "List review history for an evidence assignment")
    public PageResponse<EvidenceReviewResponse> listReviews(
            JwtAuthenticationToken authentication,
            @PathVariable UUID evidenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.listReviews(identity.institutionId(), identity.userId(),
                roleAuthorities(authentication), evidenceId, page, size);
    }

    @PostMapping("/evidence/{evidenceId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('IQAC_COORDINATOR')")
    @Operation(summary = "Review a submitted evidence package")
    public EvidenceReviewResponse review(
            JwtAuthenticationToken authentication,
            @PathVariable UUID evidenceId,
            @Valid @RequestBody EvidenceReviewRequest request) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.review(identity.institutionId(), identity.userId(), evidenceId, request);
    }

    @GetMapping("/reports/missing")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Report evidence assignments that have not been submitted or verified")
    public PageResponse<EvidenceAssignmentResponse> missingEvidence(
            JwtAuthenticationToken authentication,
            @RequestParam String academicYear,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.missingEvidence(RequestIdentity.from(authentication).institutionId(),
                academicYear, departmentId, page, size);
    }

    private Set<String> roleAuthorities(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }
}
