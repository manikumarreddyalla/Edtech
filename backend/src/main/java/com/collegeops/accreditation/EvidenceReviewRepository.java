package com.collegeops.accreditation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvidenceReviewRepository extends JpaRepository<EvidenceReview, UUID> {
    Page<EvidenceReview> findAllBySubmission_IdAndInstitutionId(
            UUID submissionId, UUID institutionId, Pageable pageable);

    Page<EvidenceReview> findAllBySubmission_Assignment_IdAndInstitutionId(
            UUID assignmentId, UUID institutionId, Pageable pageable);
}
