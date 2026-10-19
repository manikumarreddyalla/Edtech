package com.collegeops.institution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Page<Department> findAllByInstitution_Id(UUID institutionId, Pageable pageable);

    Optional<Department> findByIdAndInstitution_Id(UUID id, UUID institutionId);
}
