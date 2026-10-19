package com.collegeops.accreditation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceAttachmentRepository extends JpaRepository<EvidenceAttachment, UUID> {
    List<EvidenceAttachment> findAllBySubmission_IdAndInstitutionId(UUID submissionId, UUID institutionId);
}
