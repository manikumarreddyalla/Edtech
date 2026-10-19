package com.collegeops.accreditation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccreditationFrameworkRepository extends JpaRepository<AccreditationFramework, UUID> {
    Page<AccreditationFramework> findAllByInstitution_Id(UUID institutionId, Pageable pageable);

    Optional<AccreditationFramework> findByIdAndInstitution_Id(UUID id, UUID institutionId);

    boolean existsByInstitution_IdAndNameIgnoreCaseAndVersionLabelIgnoreCase(
            UUID institutionId, String name, String versionLabel);
}
