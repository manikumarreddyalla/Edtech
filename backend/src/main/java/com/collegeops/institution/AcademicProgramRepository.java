package com.collegeops.institution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademicProgramRepository extends JpaRepository<AcademicProgram, UUID> {
    Page<AcademicProgram> findAllByInstitution_Id(UUID institutionId, Pageable pageable);

    Optional<AcademicProgram> findByIdAndInstitution_Id(UUID id, UUID institutionId);
}
