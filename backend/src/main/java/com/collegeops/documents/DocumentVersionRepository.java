package com.collegeops.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    Page<DocumentVersion> findAllByDocument_IdAndInstitutionId(UUID documentId, UUID institutionId,
                                                               Pageable pageable);

    Optional<DocumentVersion> findByDocument_IdAndInstitutionIdAndVersionNumber(
            UUID documentId, UUID institutionId, int versionNumber);

    Optional<DocumentVersion> findTopByDocument_IdAndInstitutionIdOrderByVersionNumberDesc(
            UUID documentId, UUID institutionId);
}
