package com.collegeops.accreditation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvidenceSubmissionRepository extends JpaRepository<EvidenceSubmission, UUID> {
    Page<EvidenceSubmission> findAllByAssignment_IdAndInstitutionId(
            UUID assignmentId, UUID institutionId, Pageable pageable);

    Optional<EvidenceSubmission> findByIdAndInstitutionId(UUID id, UUID institutionId);

    Optional<EvidenceSubmission> findTopByAssignment_IdAndInstitutionIdOrderBySubmissionNumberDesc(
            UUID assignmentId, UUID institutionId);

    long countByAssignment_IdAndInstitutionId(UUID assignmentId, UUID institutionId);
}
